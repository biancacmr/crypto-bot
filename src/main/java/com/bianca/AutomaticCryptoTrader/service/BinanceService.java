package com.bianca.AutomaticCryptoTrader.service;

import com.bianca.AutomaticCryptoTrader.config.BinanceConfig;
import com.bianca.AutomaticCryptoTrader.model.AccountData;
import com.bianca.AutomaticCryptoTrader.model.Balance;
import com.bianca.AutomaticCryptoTrader.model.LogHelper;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class BinanceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BotExecution.class);
    private static final LogHelper logHelper = new LogHelper(LOGGER);

    @Autowired
    private final EmailService emailService;

    private final BinanceConfig config;
    private final SpotClient client;

    private ArrayList<Order> openOrders;
    private ArrayList<Double> rollingVolatility;
    private boolean lastTradeDecision = false;
    private boolean actualTradePosition;
    private ArrayList<StockData> stockData;
    private Double lastStockAccountBalance;
    private AccountData accountData;

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
        rollingVolatility = calculateRollingVolatility();
        openOrders = updateOpenOrders();
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

    private boolean updateActualTradePosition() {
//        return this.lastStockAccountBalance > 0.1; // PARA MOEDAS PEQUENAS
        return this.lastStockAccountBalance > 0.001; // PARA MOEDAS GRANDES
    }

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
            }

            LOGGER.info("Todas as open orders canceladas.");
        }
    }

    public boolean buyLimitedOrder() throws Exception {
        double price = 0.0; // Pode ser ajustado posteriormente

        JSONObject symbolInfo = getSymbolInfo();
        JSONArray filters = symbolInfo.getJSONArray("symbols").getJSONObject(0).getJSONArray("filters");

        // Extract PRICE_FILTER and get tickSize using Streams
        Optional<Double> tickSize = filters.toList().stream()
                .map(obj -> new JSONObject((java.util.Map<?, ?>) obj)) // Convert Map back to JSONObject
                .filter(filter -> "PRICE_FILTER".equals(filter.getString("filterType")))
                .map(filter -> filter.getDouble("tickSize"))
                .findFirst();

        // Extract LOT_SIZE and get stepSize using Streams
        Optional<Double> stepSize = filters.toList().stream()
                .map(obj -> new JSONObject((java.util.Map<?, ?>) obj)) // Convert Map back to JSONObject
                .filter(filter -> "LOT_SIZE".equals(filter.getString("filterType")))
                .map(filter -> filter.getDouble("stepSize"))
                .findFirst();

        double tickSizeValue = tickSize.orElseThrow(() -> new RuntimeException("Tick Size not found"));
        double stepSizeValue = stepSize.orElseThrow(() -> new RuntimeException("Step Size not found"));

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

        // Ensure precision is 8 digits
        BigDecimal preciseQuantity = BigDecimal.valueOf(quantity).setScale(config.getStockPrecisionDigits(), RoundingMode.HALF_UP);
        quantity = preciseQuantity.doubleValue();

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

//            String rawResponse = client.createTrade().newOrder(parameters);
            String rawResponse = "TESTE";
//            JSONObject response = new JSONObject(rawResponse);

//            if (response.has("code") && response.has("msg")) {
//                throw new RuntimeException("Erro ao realizar request 'newOrder': " + response);
//            }

            this.actualTradePosition = true;

            LOGGER.info("Ordem de COMPRA limitada enviada com sucesso:");
//            logHelper.createLogOrder(response);

            // Envia email avisando da ação realizada
//            emailService.sendEmail(config.getEmailReceiver(), "Robô Binance - Compra Limitada Executada", createBodyOrder(response));
            emailService.sendEmail(config.getEmailReceiver(), "Robô Binance - Compra Limitada Executada", "SÓ TESTE hehe: " + parameters.toString());
            emailService.sendEmail("gabrielsilvabaptista@gmail.com", "Robô Binance - Compra Limitada Executada", "SÓ TESTE hehe: " + parameters.toString());
            LOGGER.info("Email enviado.");

            return true;
        } catch (Exception e) {
            LOGGER.error("Erro ao enviar ordem limitada de compra: ", e);
            throw e;
        }
    }

    public JSONObject getSymbolInfo() {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", config.getOperationCode());

        String rawResponse = client.createMarket().exchangeInfo(parameters);
        JSONObject response = new JSONObject(rawResponse);

        if (response.has("code") && response.has("msg")) {
            throw new RuntimeException("Erro ao realizar request 'getSymbolInfo': " + response);
        }

        return response;
    }

    public boolean sellLimitedOrder() throws Exception {
        try {
            double price = 0.0; // Pode ser trocado depois

            // Get symbol info to respect filters
            JSONObject symbolInfo = getSymbolInfo();
            JSONArray filters = symbolInfo.getJSONArray("symbols").getJSONObject(0).getJSONArray("filters");

            // Extract PRICE_FILTER and get tickSize using Streams
            Optional<Double> tickSize = filters.toList().stream()
                    .map(obj -> new JSONObject((java.util.Map<?, ?>) obj)) // Convert Map back to JSONObject
                    .filter(filter -> "PRICE_FILTER".equals(filter.getString("filterType")))
                    .map(filter -> filter.getDouble("tickSize"))
                    .findFirst();

            // Extract LOT_SIZE and get stepSize using Streams
            Optional<Double> stepSize = filters.toList().stream()
                    .map(obj -> new JSONObject((java.util.Map<?, ?>) obj)) // Convert Map back to JSONObject
                    .filter(filter -> "LOT_SIZE".equals(filter.getString("filterType")))
                    .map(filter -> filter.getDouble("stepSize"))
                    .findFirst();

            double tickSizeValue = tickSize.orElseThrow(() -> new RuntimeException("Tick Size not found"));
            double stepSizeValue = stepSize.orElseThrow(() -> new RuntimeException("Step Size not found"));

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

            // Ensure precision is 8 digits
            BigDecimal preciseQuantity = BigDecimal.valueOf(quantity).setScale(config.getStockPrecisionDigits(), RoundingMode.HALF_UP);
            quantity = preciseQuantity.doubleValue();

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

//            String rawResponse = client.createTrade().newOrder(parameters);
            String rawResponse = "TESTE";
//            JSONObject response = new JSONObject(rawResponse);
//
//            if (response.has("code") && response.has("msg")) {
//                throw new RuntimeException("Erro ao realizar request 'newOrder': " + response);
//            }

            actualTradePosition = false; // Update position to sold
//            LOGGER.info("Ordem VENDA limitada enviada com sucesso: " + response);
            LOGGER.info("Ordem VENDA limitada enviada com sucesso: " + rawResponse);

//            logHelper.createLogOrder(response); // Create a log

            // Envia email avisando da ação realizada
            emailService.sendEmail(config.getEmailReceiver(), "Robô Binance - Venda Limitada Executada", "SÓ TESTE hehe: " + parameters.toString());
            emailService.sendEmail("gabrielsilvabaptista@gmail.com", "Robô Binance - Venda Limitada Executada", "SÓ TESTE hehe: " + parameters);
            LOGGER.info("Email enviado.");

            return true;
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
                .append("<h2>Ordem Enviada " + (type.equals("BUY") ? "COMPRA":"VENDA") + "</h2>")
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
//                .append("<tr><th>Complete Order</th><td>").append(order).append("</td></tr>")
                .append("</table>")
                .append("<div class=\"divider\"></div>")
                .append("</div>")
                .append("</body>")
                .append("</html>");

        return htmlContent.toString();
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

    public void setLastTradeDecision(boolean lastTradeDecision) {
        this.lastTradeDecision = lastTradeDecision;
    }

    public ArrayList<Double> getRollingVolatility() {
        return rollingVolatility;
    }
}
