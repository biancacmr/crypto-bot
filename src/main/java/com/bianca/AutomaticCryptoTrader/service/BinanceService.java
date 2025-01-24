package com.bianca.AutomaticCryptoTrader.service;

import com.bianca.AutomaticCryptoTrader.config.BinanceConfig;
import com.bianca.AutomaticCryptoTrader.model.AccountData;
import com.bianca.AutomaticCryptoTrader.model.Balance;
import com.bianca.AutomaticCryptoTrader.model.Order;
import com.bianca.AutomaticCryptoTrader.model.StockData;
import com.bianca.AutomaticCryptoTrader.task.BotExecution;
import com.binance.connector.client.SpotClient;
import com.binance.connector.client.impl.SpotClientImpl;
import jakarta.mail.MessagingException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class BinanceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BotExecution.class);
    private static final LogService LOG_SERVICE = new LogService(LOGGER);

    @Autowired
    private final EmailService emailService;

    private final BinanceConfig config;
    private final SpotClient client;

    private ArrayList<Order> openOrders;
    private ArrayList<Double> rollingVolatility;
    private Boolean lastTradeDecision = null;
    private boolean actualTradePosition;
    private ArrayList<StockData> stockData;
    private Double lastStockAccountBalance;
    private AccountData accountData;
    private Double lastBuyPrice = 0.0;
    private Double lastSellPrice = 0.0;
    private Double partialQuantityDiscount = 0.0; // Valor que já foi executado e que será descontado da quantidade, caso uma ordem não seja completamente executada
    private Double tickSize;
    private Double stepSize;

    public BinanceService(EmailService emailService, BinanceConfig config) {
        this.emailService = emailService;
        this.config = config;
        this.client = new SpotClientImpl(config.getApiKey(), config.getSecretKey(), config.getUrl());
    }

    public void updateAllData() {
        accountData = updatedAccountData();
        lastStockAccountBalance = updatedLastStockAccountBalance();
        actualTradePosition = updateActualTradePosition();
        stockData = updateStockData();
        openOrders = updateOpenOrders();
        rollingVolatility = calculateRollingVolatility();
        lastBuyPrice = updateLastBuyPrice();
        lastSellPrice = updateLastSellPrice();
        tickSize = getAssetTickSize();
        stepSize = getAssetStepSize();
    }

    private ArrayList<Order> getOrdersHistory() {
        // Obtém o histórico de ordens do par configurado
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", config.getOperationCode());
        parameters.put("limit", 100);

        String rawResponse = client.createTrade().getOrders(parameters);

        if (rawResponse.startsWith("{")) {
            JSONObject response = new JSONObject(rawResponse);

            if (response.has("code") && response.has("msg")) {
                throw new RuntimeException("Erro ao realizar request 'get orders': " + response);
            }

            throw new RuntimeException("Erro ao pegar histórico das orders. Resposta não reconhecida.");
        } else {
            JSONArray response = new JSONArray(rawResponse);

            if (response.isEmpty()) {
                return new ArrayList<Order>();
            }

            ArrayList<Order> orders = new ArrayList<>();

            for (int i = 0; i < response.length(); i++) {
                orders.add(new Order(response.getJSONObject(i)));
            }

            if (orders.isEmpty()) {
                throw new RuntimeException("Erro ao pegar histórico das orders.");
            }

            return orders;
        }
    }

    private Double updateLastOrderPrice(String side, String operationType) {
        try {
            ArrayList<Order> orderHistory = getOrdersHistory();

            // Filtra as ordens com base no lado (SELL ou BUY) e status FILLED
            List<Order> executedOrders = orderHistory.stream()
                    .filter(order -> side.equals(order.getSide()) && "FILLED".equals(order.getStatus()))
                    .toList();

            if (!executedOrders.isEmpty()) {
                // Ordena as ordens por tempo (timestamp) para obter a mais recente
                Optional<Order> lastExecutedOrderOpt = executedOrders.stream()
                        .max(Comparator.comparingLong(Order::getTime));

                Order lastExecutedOrder = lastExecutedOrderOpt.get();

                // Calcula o preço da última ordem executada
                double lastOrderPrice = Double.parseDouble(lastExecutedOrder.getCumulativeQuoteQty()) /
                        Double.parseDouble(lastExecutedOrder.getExecutedQty());

                // Corrige o timestamp para exibição
                String datetimeTransact = formatTimestamp(lastExecutedOrder.getTime());

                LOGGER.info("Última ordem de {} executada para {}:", operationType, config.getOperationCode());
                LOGGER.info(" | Data: {}", datetimeTransact);
                LOGGER.info(" | Preço: {}", adjustToStepStr(lastOrderPrice, tickSize));
                LOGGER.info(" | Quantidade: {}", adjustToStepStr(Double.parseDouble(lastExecutedOrder.getOrigQty()), stepSize));

                return lastOrderPrice;
            } else {
                LOGGER.error("Não há ordens de {} executadas para {}", operationType, config.getOperationCode());
                return 0.0;
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao verificar a última ordem de " + operationType + " executada para " + config.getOperationCode(), e);
        }
    }

    public Double updateLastSellPrice() {
        return updateLastOrderPrice("SELL", "VENDA");
    }

    public Double updateLastBuyPrice() {
        return updateLastOrderPrice("BUY", "COMPRA");
    }

    /*  Ajusta o valor para o múltiplo mais próximo do passo definido, lidando com problemas de precisão
        e garantindo que o resultado não seja retornado em notação científica. **/
    public static Double adjustToStepDouble(double value, double step) {
        // Determinar o número de casas decimais do step
        int decimalPlaces = step < 1 ? Math.max(0, (int) Math.ceil(-Math.log10(step))) : 0;

        // Ajustar o valor ao step usando floor
        double adjustedValue = Math.floor(value / step) * step;

        // Garantir que o resultado tenha a mesma precisão do step
        BigDecimal result = BigDecimal.valueOf(adjustedValue).setScale(decimalPlaces, RoundingMode.HALF_UP);

        return result.doubleValue();
    }

    public static String adjustToStepStr(double value, double step) {
        // Determinar o número de casas decimais do step
        int decimalPlaces = step < 1 ? Math.max(0, (int) Math.ceil(-Math.log10(step))) : 0;

        // Ajustar o valor ao step usando floor
        double adjustedValue = Math.floor(value / step) * step;

        // Garantir que o resultado tenha a mesma precisão do step
        BigDecimal result = BigDecimal.valueOf(adjustedValue).setScale(decimalPlaces, RoundingMode.HALF_UP);

        // Retornar como String
        return result.toPlainString();
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(timestamp));
    }

    private AccountData updatedAccountData() {
        AccountData updatedAccountData = new AccountData();

        Map<String, Object> parameters = getDefaultParameters();
        String rawResponse = client.createTrade().account(parameters);
        JSONObject response = new JSONObject(rawResponse);

        if (response.has("code") && response.has("msg")) {
            throw new RuntimeException("Erro ao realizar request 'account': " + response);
        }

        updatedAccountData.updateDataFromJSONObject(response);
        return updatedAccountData;
    }

    private ArrayList<Order> updateOpenOrders() {
        Map<String, Object> parameters = getDefaultParameters();
        parameters.put("symbol", config.getOperationCode());

        String rawResponse = client.createTrade().getOpenOrders(parameters);

        if (rawResponse.startsWith("{")) {
            JSONObject response = new JSONObject(rawResponse);

            if (response.has("code") && response.has("msg")) {
                throw new RuntimeException("Erro ao realizar request 'openOrders': " + response);
            }

            throw new RuntimeException("Erro ao atualizar open orders. Resposta não reconhecida.");
        } else {
            JSONArray response = new JSONArray(rawResponse);

            if (response.isEmpty()) {
                return new ArrayList<Order>();
            }

            ArrayList<Order> openOrders = new ArrayList<>();

            for (int i = 0; i < response.length(); i++) {
                openOrders.add(new Order(response.getJSONObject(i)));
            }

            if (openOrders.isEmpty()) {
                throw new RuntimeException("Erro ao atualizar open orders. StockData vazio.");
            }

            return openOrders;
        }
    }

    private ArrayList<StockData> updateStockData() {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", config.getOperationCode());
        parameters.put("interval", config.getCandlePeriod());
        parameters.put("limit", 500);

        String rawResponse = client.createMarket().klines(parameters);

        if (rawResponse.startsWith("{")) {
            JSONObject response = new JSONObject(rawResponse);

            if (response.has("code") && response.has("msg")) {
                throw new RuntimeException("Erro ao realizar request 'klines': " + response);
            }

            throw new RuntimeException("Erro ao atualizar stock data. Resposta não reconhecida.");
        } else {
            JSONArray response = new JSONArray(rawResponse);

            if (response.isEmpty()) {
                throw new RuntimeException("Erro ao atualizar stock data. Resposta vazia.");
            }

            ArrayList<StockData> stockData = new ArrayList<>();

            for (int i = 0; i < response.length(); i++) {
                stockData.add(new StockData(response.getJSONArray(i)));
            }

            if (stockData.isEmpty()) {
                throw new RuntimeException("Erro ao atualizar stockData. StockData vazio.");
            }

            return stockData;
        }
    }

    /**
     * Verifica se há pelo menos uma fração mínima da moeda em questão na conta (step size)
     */
    private boolean updateActualTradePosition() {
        Double stepSize = getAssetStepSize();
        return this.lastStockAccountBalance > stepSize;
    }

    private Double getAssetStepSize() {
        JSONObject symbolInfo = getSymbolInfo(config.getOperationCode());

        JSONArray filters = symbolInfo.getJSONArray("symbols").getJSONObject(0).getJSONArray("filters");

        // Extract LOT_SIZE and get stepSize using Streams
        Optional<Double> stepSize = filters.toList().stream()
                .map(obj -> new JSONObject((java.util.Map<?, ?>) obj)) // Convert Map back to JSONObject
                .filter(filter -> "LOT_SIZE".equals(filter.getString("filterType")))
                .map(filter -> filter.getDouble("stepSize"))
                .findFirst();

        Double stepSizeVal = stepSize.orElseThrow(() -> new RuntimeException("Step Size not found"));

        if (stepSizeVal <= 0) throw new RuntimeException("Invalid Step Size for asset");

        return stepSizeVal;
    }

    private Double getAssetTickSize() {
        JSONObject symbolInfo = getSymbolInfo(config.getOperationCode());

        JSONArray filters = symbolInfo.getJSONArray("symbols").getJSONObject(0).getJSONArray("filters");

        // Extract PRICE_FILTER and get tickSize using Streams
        Optional<Double> tickSize = filters.toList().stream()
                .map(obj -> new JSONObject((java.util.Map<?, ?>) obj)) // Convert Map back to JSONObject
                .filter(filter -> "PRICE_FILTER".equals(filter.getString("filterType")))
                .map(filter -> filter.getDouble("tickSize"))
                .findFirst();

        return tickSize.orElseThrow(() -> new RuntimeException("Tick Size not found"));
    }

    /**
     * Atualiza o valor livre atual da moeda em questão na carteira
     */
    private Double updatedLastStockAccountBalance() throws RuntimeException {
        ArrayList<Balance> accountBalance = accountData.getBalances();

        if (accountBalance.isEmpty()) {
            throw new RuntimeException("Error while returning 'lastStockAccountBalance': Account balance not found.");
        }

        for (Balance balance : accountBalance) {
            if (balance.getAsset().equals(config.getStockCode())) {
                return balance.getFree();
            }
        }

        throw new RuntimeException("No free balance for chosen asset.");
    }

    public ArrayList<Double> calculateRollingVolatility() {
        List<Double> closePrices = stockData.stream().map(StockData::getClosePrice).toList();
        int windowSize = 40;

        DescriptiveStatistics stats = new DescriptiveStatistics();
        stats.setWindowSize(windowSize);

        ArrayList<Double> rollingStds = new ArrayList<>();

        for (Double price : closePrices) {
            stats.addValue(price);
            if (stats.getN() == windowSize) {
                rollingStds.add(stats.getStandardDeviation());
            }
        }

        return rollingStds;
    }

    public void printStock(String assetStockCode) {
        ArrayList<Balance> balances = accountData.getBalances();

        Balance balanceAsset = balances.stream()
                .filter(balance -> balance.getAsset().equals(assetStockCode))
                .findFirst()
                .orElse(null); // Or handle the case where no match is found

        if (balanceAsset == null) {
            throw new RuntimeException();
        }

        LOGGER.info(balanceAsset.toString());
    }

    public boolean stopLossTrigger() throws MessagingException {
        double closePrice = stockData.getLast().getClosePrice();
        double weightedPrice = stockData.get(stockData.size() - 2).getClosePrice(); // Preço ponderado pelo candle anterior
        double stopLossPrice = lastBuyPrice * (1 - config.getStopLossPercentage());

        LOGGER.info(" - Preço atual: {}", closePrice);
        LOGGER.info(" - Preço mínimo para vender: {}", getMinimumPriceToSell());
        LOGGER.info(" - Stop Loss em: {} ({}%)", stopLossPrice, (config.getStopLossPercentage() * 100));

        if (closePrice < stopLossPrice && weightedPrice < stopLossPrice && actualTradePosition) {
            LOGGER.info("Ativando STOP LOSS...");
            cancelAllOrders();

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Erro ao pausar a thread.", e);
            }

            sellMarketOrder();
            return true;
        }

        return false;
    }

    private void sellMarketOrder() throws MessagingException {
        try {
            if (actualTradePosition) { // Se a posição for comprada
                // Ajusta a quantidade para o stepSize
                Double quantity = adjustToStepDouble(lastStockAccountBalance, stepSize);

                Map<String, Object> parameters = new LinkedHashMap<>();
                parameters.put("symbol", config.getOperationCode());
                parameters.put("side", "SELL");
                parameters.put("type", "MARKET");
                parameters.put("quantity", quantity);

                String rawResponse = client.createTrade().newOrder(parameters);
                JSONObject response = new JSONObject(rawResponse);

                if (response.has("code") && response.has("msg")) {
                    throw new RuntimeException("Erro ao realizar request 'newOrder': " + response);
                }

                actualTradePosition = false; // Define posição atual como VENDIDO

                LOGGER.info("Ordem MARKET SELL enviada com sucesso: ");
                LOG_SERVICE.createLogOrder(response);

                // Envia e-mail avisando da ação realizada
                ArrayList<String> destinatarios = new ArrayList<>();
                destinatarios.add(config.getEmailReceiver());
                destinatarios.add("gabrielsilvabaptista@gmail.com");

                emailService.sendEmail(destinatarios, "Robô Binance - Venda de Mercado Executada", createBodyOrder(response));
                LOGGER.info("Email enviado.");
            } else {
                LOGGER.info("ERRO ao vender: Posição já vendida.");
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao enviar ordem de mercado de venda: ", e);
            throw e;
        }
    }

    private Double getMinimumPriceToSell() {
        return lastBuyPrice * (1 - config.getAcceptableLossPercentage());
    }

    /**
     * Cancela todas as ordens abertas de COMPRA e VENDA, retornando a quantidade de ordens canceladas
     */
    public void cancelAllOrders() {
        if (!openOrders.isEmpty()) {
            LOGGER.info("Cancelando todas as open orders...");

            for (Order order : openOrders) {
                Map<String, Object> parameters = new LinkedHashMap<>();
                parameters.put("symbol", config.getOperationCode());
                parameters.put("orderId", order.getOrderId());

                String rawResponse = client.createTrade().cancelOrder(parameters);
                JSONObject response = new JSONObject(rawResponse);

                LOGGER.info(response.toString());

                if (response.has("code") && response.has("msg")) {
                    throw new RuntimeException("Erro ao realizar request 'cancelOrder': " + response);
                }

                if (!response.getString("status").equals("FILLED")) {
                    throw new RuntimeException("Erro ao cancelar ordem: " + order);
                }

                LOGGER.info(" - Ordem {} cancelada", order.getOrderId());
            }

            LOGGER.info("Todas as open orders canceladas.");
        }
    }

    public void buyLimitedOrder() throws Exception {
        double price = 0.0; // Pode ser ajustado posteriormente

        double tickSizeValue = getAssetTickSize();
        double stepSizeValue = getAssetStepSize();

        // Pega o valor do candle atual
        double closePrice = stockData.getLast().getClosePrice();
        double limitPrice = 0.0;

        if (price == 0.0) {
            limitPrice = closePrice + (config.getVolatilityFactor() * rollingVolatility.getLast());
        } else {
            limitPrice = price;
        }

        // Ajustar o preço limite para o tickSize permitido
        limitPrice = Math.floor(limitPrice / tickSizeValue) * tickSizeValue;

        // Ajustar a quantidade para o stepSize permitido
        double quantity = Math.floor(config.getTradedQuantity() / stepSizeValue) * stepSizeValue;

        LOGGER.info("Enviando ordem limitada de compra para {}", config.getOperationCode());
        LOGGER.info(" - Quantidade: {}", quantity);
        LOGGER.info(" - Close price: {}", closePrice);
        LOGGER.info(" - Preço Limite: {}", limitPrice);

        try {
            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("symbol", config.getOperationCode());
            parameters.put("side", "BUY");
            parameters.put("type", "LIMIT");
            parameters.put("timeInForce", "GTC");
            parameters.put("quantity", quantity);
            parameters.put("price", Math.round(limitPrice * 100.0) / 100.0);

            String rawResponse = client.createTrade().newOrder(parameters);
            JSONObject response = new JSONObject(rawResponse);

            if (response.has("code") && response.has("msg")) {
                throw new RuntimeException("Erro ao realizar request 'newOrder': " + response);
            }

            this.actualTradePosition = true;

            LOGGER.info("Ordem de COMPRA limitada enviada com sucesso:");
            LOG_SERVICE.createLogOrder(response);

            // Envia e-mail avisando da ação realizada
            ArrayList<String> destinatarios = new ArrayList<>();
            destinatarios.add(config.getEmailReceiver());
            destinatarios.add("gabrielsilvabaptista@gmail.com");

            emailService.sendEmail(destinatarios, "Robô Binance - Compra Limitada Executada", createBodyOrder(response));
            LOGGER.info("Email enviado.");
        } catch (Exception e) {
            LOGGER.error("Erro ao enviar ordem limitada de compra: ", e);
            throw e;
        }
    }

    public JSONObject getSymbolInfo(String symbol) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", symbol);

        String rawResponse = client.createMarket().exchangeInfo(parameters);
        JSONObject response = new JSONObject(rawResponse);

        if (response.has("code") && response.has("msg")) {
            throw new RuntimeException("Erro ao realizar request 'getSymbolInfo': " + response);
        }

        return response;
    }

    public void sellLimitedOrder() throws Exception {
        try {
            double price = 0.0; // Pode ser trocado depois

            double tickSizeValue = getAssetTickSize();
            double stepSizeValue = getAssetStepSize();

            // Pega o valor do candle atual
            double closePrice = stockData.getLast().getClosePrice();
            double limitPrice;

            if (price == 0) {
                // Definir o preço limite para a ordem
                limitPrice = closePrice - (config.getVolatilityFactor() * rollingVolatility.getLast());
            } else {
                limitPrice = price;
            }

            // Ajustar o preço limite para o tickSize permitido
            limitPrice = Math.floor(limitPrice / tickSizeValue) * tickSizeValue;

            // Ajustar a quantidade para o stepSize permitido
            double quantity = Math.floor(lastStockAccountBalance / stepSizeValue) * stepSizeValue;

            // Log information
            LOGGER.info("Enviando ordem limitada de venda para " + config.getOperationCode() + ":");
            LOGGER.info(" - Quantidade: " + quantity);
            LOGGER.info(" - Close Price: " + closePrice);
            LOGGER.info(" - Preço Limite: " + limitPrice);

            // Enviando ordem de compra
            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("symbol", config.getOperationCode());
            parameters.put("side", "SELL");
            parameters.put("type", "LIMIT");
            parameters.put("timeInForce", "GTC");
            parameters.put("quantity", quantity);
            parameters.put("price", Math.round(limitPrice * 100.0) / 100.0);

            String rawResponse = client.createTrade().newOrder(parameters);
            JSONObject response = new JSONObject(rawResponse);

            if (response.has("code") && response.has("msg")) {
                throw new RuntimeException("Erro ao realizar request 'newOrder': " + response);
            }

            actualTradePosition = false; // Update position to sold
            LOGGER.info("Ordem VENDA limitada enviada com sucesso: ");
            LOG_SERVICE.createLogOrder(response); // Create a log

            // Envia e-mail avisando da ação realizada
            ArrayList<String> destinatarios = new ArrayList<>();
            destinatarios.add(config.getEmailReceiver());
            destinatarios.add("gabrielsilvabaptista@gmail.com");

            emailService.sendEmail(destinatarios, "Robô Binance - Venda Limitada Executada", createBodyOrder(response));
            LOGGER.info("Email enviado.");
        } catch (Exception e) {
            LOGGER.error("Erro ao enviar ordem limitada de venda: ", e);
            throw e;
        }

    }

    private String createBodyOrder(JSONObject order) {
        StringBuilder htmlContent = new StringBuilder();

        // Extracting necessary information
        String side = order.getString("side");
        String type = order.getString("type");
        double quantity = order.getDouble("executedQty");
        String asset = order.getString("symbol");
        double totalValue = order.getDouble("cummulativeQuoteQty");
        long timestamp = order.getLong("transactTime");
        String status = order.getString("status");

        // Handling optional fields
        String pricePerUnit = "-";
        String currency = "-";

        if (order.has("fills") && !order.getJSONArray("fills").isEmpty()) {
            JSONObject fill = order.getJSONArray("fills").getJSONObject(0);
            pricePerUnit = fill.optString("price", "-");
            currency = fill.optString("commissionAsset", "-");
        }

        // Convert timestamp to human-readable date/time
        LocalDateTime dateTimeTransact = Instant.ofEpochMilli(timestamp)
                .atOffset(ZoneOffset.UTC)
                .toLocalDateTime();
        String formattedDate = dateTimeTransact.format(DateTimeFormatter.ofPattern("HH:mm:ss yyyy-MM-dd"));

        htmlContent.append("<!DOCTYPE html>")
                .append("<html lang=\"en\">")
                .append("<head>")
                .append("<meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                .append("<title>Ordem Enviada</title>")
                .append("<style>")
                .append("body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px; }")
                .append(".email-container { background-color: #ffffff; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); }")
                .append("h2 { color: #333; }")
                .append("table { width: 100%; margin-top: 20px; border-collapse: collapse; }")
                .append("table, th, td { border: 1px solid #ddd; }")
                .append("th, td { padding: 12px; text-align: left; }")
                .append("th { background-color: #f4f4f4; color: #333; }")
                .append("td { background-color: #fafafa; }")
                .append(".divider { margin-top: 20px; border-top: 2px solid #ddd; }")
                .append("</style>")
                .append("</head>")
                .append("<body>")
                .append("<div class=\"email-container\">")
                .append("<h2>Ordem Enviada " + (type.trim().equalsIgnoreCase("BUY") ? "COMPRA" : "VENDA") + "</h2>")
                .append("<table>")
                .append("<tr><th>Status</th><td>").append(status).append("</td></tr>")
                .append("<tr><th>Side</th><td>").append(side).append("</td></tr>")
                .append("<tr><th>Ativo</th><td>").append(asset).append("</td></tr>")
                .append("<tr><th>Quantidade</th><td>").append(quantity).append("</td></tr>")
                .append("<tr><th>Valor na Venda</th><td>").append(pricePerUnit).append("</td></tr>")
                .append("<tr><th>Moeda</th><td>").append(currency).append("</td></tr>")
                .append("<tr><th>Valor em ").append(currency).append("</th><td>").append(totalValue).append("</td></tr>")
                .append("<tr><th>Tipo</th><td>").append(type).append("</td></tr>")
                .append("<tr><th>Data/Hora</th><td>").append(formattedDate).append("</td></tr>")
                .append("</table>")
                .append("<div class=\"divider\"></div>")
                .append("</div>")
                .append("</body>")
                .append("</html>");

        return htmlContent.toString();
    }

    /**
     * Verifica se há ordens de compra/venda abertas para o ativo configurado.
     * Se houver:
     * - Salva a quantidade já executada em partialQuantityDiscount.
     * - Salva o maior preço parcialmente executado em lastBuyPrice.
     *
     * @return true se houver ordens abertas, false caso contrário.
     */
    public boolean hasOpenOrders(String side) {
        try {
            openOrders = updateOpenOrders();

            // Filtra as ordens de compra
            List<Order> buyOrders = openOrders.stream()
                    .filter(order -> side.equalsIgnoreCase(order.getSide()))
                    .toList();

            if (!buyOrders.isEmpty()) {
                partialQuantityDiscount = 0.0;
                lastBuyPrice = 0.0;

                LOGGER.info("Ordens de compra do tipo {} abertas para {}:",
                        side.equals("BUY") ? "COMPRA" : "VENDA",
                        config.getOperationCode());

                for (Order order : buyOrders) {
                    double executedQty = Double.parseDouble(order.getExecutedQty()); // Quantidade já executada
                    double price = Double.parseDouble(order.getPrice()); // Preço da ordem

                    LOGGER.info(" - ID da Ordem: {}, Preço: {}, Qtd.: {}, Qtd. Executada: {}",
                            order.getOrderId(), price, order.getOrigQty(), executedQty);

                    // Atualiza a quantidade parcial executada
                    partialQuantityDiscount += executedQty;

                    // Atualiza o maior preço parcialmente executado
                    if (side.equalsIgnoreCase("BUY")) {
                        if (executedQty > 0 && price > lastBuyPrice) {
                            lastBuyPrice = price;
                        }
                    } else if (side.equalsIgnoreCase("SELL")) {
                        if (executedQty > 0 && price > lastSellPrice) {
                            lastSellPrice = price;
                        }
                    }
                }

                LOGGER.info(" - Quantidade parcial executada no total: {}", partialQuantityDiscount);
                LOGGER.info(" - Maior preço parcialmente executado: {}",
                        side.equalsIgnoreCase("SELL") ? lastSellPrice : lastBuyPrice);
                return true;
            } else {
                LOGGER.info(" - Não há ordens de compra abertas para {}.", config.getOperationCode());
                return false;
            }

        } catch (Exception e) {
            LOGGER.error("Erro ao verificar ordens abertas para " + config.getOperationCode() + ": " + e.getMessage());
            throw new RuntimeException("Erro ao verificar ordens abertas.");
        }
    }

    private Map<String, Object> getDefaultParameters() {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("recvWindow", "30000");
        return parameters;
    }

    public ArrayList<Order> getOpenOrders() {
        return openOrders;
    }

    public boolean getLastTradeDecision() {
        return lastTradeDecision;
    }

    public boolean getActualTradePosition() {
        return actualTradePosition;
    }

    public Double getLastStockAccountBalance() {
        return lastStockAccountBalance;
    }

    public ArrayList<StockData> getStockData() {
        return stockData;
    }

    public AccountData getAccountData() {
        return accountData;
    }

    public void setLastTradeDecision(Boolean lastTradeDecision) {
        this.lastTradeDecision = lastTradeDecision;
    }

    public ArrayList<Double> getRollingVolatility() {
        return rollingVolatility;
    }
}
