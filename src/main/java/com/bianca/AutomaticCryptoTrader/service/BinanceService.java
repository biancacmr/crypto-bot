package com.bianca.AutomaticCryptoTrader.service;

import com.bianca.AutomaticCryptoTrader.config.BinanceConfig;
import com.bianca.AutomaticCryptoTrader.indicators.Indicators;
import com.bianca.AutomaticCryptoTrader.indicators.MovingAverageCalculator;
import com.bianca.AutomaticCryptoTrader.model.*;
import com.binance.connector.client.SpotClient;
import com.binance.connector.client.impl.SpotClientImpl;
import jakarta.mail.MessagingException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class BinanceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BinanceService.class);
    private static final LogService LOG_SERVICE = new LogService(LOGGER);

    private ArrayList<Order> openOrders;
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

    @Autowired
    private BinanceConfig config;
    private SpotClient client;
    private Indicators indicators;

    public BinanceService(BinanceConfig config, Indicators indicators) {
        this.indicators = indicators;
        this.config = config;
        this.client = new SpotClientImpl(config.getApiKey(), config.getSecretKey(), config.getUrl());
    }

    public void updateAllData() {
        accountData = updatedAccountData();
        lastStockAccountBalance = updatedLastStockAccountBalance();
        tickSize = getAssetTickSize();
        stepSize = getAssetStepSize();
        actualTradePosition = updateActualTradePosition();
        stockData = updateStockData();
        openOrders = updateOpenOrders();
        lastBuyPrice = updateLastBuyPrice();
        lastSellPrice = updateLastSellPrice();
    }

    /**
     * Retorna o histórico de ordens do par analisado
     */
    private ArrayList<Order> getOrdersHistory() {
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

    /**
     * Obtém os dados da última ordem executada com base no lado (SELL ou BUY) e status FILLED.
     */
    public Optional<Order> getLastExecutedOrder(String side) {
        try {
            ArrayList<Order> orderHistory = getOrdersHistory();

            // Filtra as ordens com base no lado (SELL ou BUY) e status FILLED
            List<Order> executedOrders = orderHistory.stream()
                    .filter(order -> side.equals(order.getSide()) && "FILLED".equals(order.getStatus()))
                    .toList();

            // Retorna a ordem mais recente (se existir)
            return executedOrders.stream()
                    .max(Comparator.comparingLong(Order::getTime));
        } catch (Exception e) {
            LOGGER.error("Erro ao obter a última ordem executada para o lado {}.", side, e);
            return Optional.empty();
        }
    }

    /**
     * Salva o preço da última ordem de COMPRA ou VENDA realizada
     */
    private Double updateLastOrderPrice(String side, String operationType) {
        try {
            Optional<Order> lastExecutedOrderOpt = getLastExecutedOrder(side);

            if (lastExecutedOrderOpt.isPresent()) {
                Order lastExecutedOrder = lastExecutedOrderOpt.get();

                // Calcula o preço da última ordem executada
                double lastOrderPrice = Double.parseDouble(lastExecutedOrder.getCumulativeQuoteQty()) /
                        Double.parseDouble(lastExecutedOrder.getExecutedQty());

                // Corrige o timestamp para exibição
                String datetimeTransact = formatTimestamp(lastExecutedOrder.getTime());

                LOGGER.info("Última ordem de {} executada para {}:", operationType, config.getOperationCode());
                LOGGER.info(" | Data: {}", datetimeTransact);
                LOGGER.info(" | Preço: {}", adjustToStepStr(lastOrderPrice));
                LOGGER.info(" | Quantidade: {}", adjustToStepStr(Double.parseDouble(lastExecutedOrder.getOrigQty())));

                return lastOrderPrice;
            } else {
                LOGGER.error("Não há ordens de {} executadas para {}.", operationType, config.getOperationCode());
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

    /*  Ajusta o valor para o múltiplo mais próximo do passo definido, lidando com problemas de precisão
    e garantindo que o resultado não seja retornado em notação científica. **/
    public static Double adjustToTickDouble(double value, double tickSize) {
        // Determinar o número de casas decimais do step
        int decimalPlaces = tickSize < 1 ? Math.max(0, (int) Math.ceil(-Math.log10(tickSize))) : 0;

        // Ajustar o valor ao step usando floor
        double adjustedValue = Math.floor(value / tickSize) * tickSize;

        // Garantir que o resultado tenha a mesma precisão do step
        BigDecimal result = BigDecimal.valueOf(adjustedValue).setScale(decimalPlaces, RoundingMode.HALF_UP);

        return result.doubleValue();
    }

    /*  Ajusta o valor para o múltiplo mais próximo do passo definido, lidando com problemas de precisão
    e garantindo que o resultado não seja retornado em notação científica. **/
    public String adjustToTickStr(double value) {
        // Determinar o número de casas decimais do step
        int decimalPlaces = tickSize < 1 ? Math.max(0, (int) Math.ceil(-Math.log10(tickSize))) : 0;

        // Ajustar o valor ao step usando floor
        double adjustedValue = Math.floor(value / tickSize) * tickSize;

        // Garantir que o resultado tenha a mesma precisão do step
        BigDecimal result = BigDecimal.valueOf(adjustedValue).setScale(decimalPlaces, RoundingMode.HALF_UP);

        return result.toPlainString();
    }

    public String adjustToStepStr(double value) {
        // Determinar o número de casas decimais do step
        int decimalPlaces = stepSize < 1 ? Math.max(0, (int) Math.ceil(-Math.log10(stepSize))) : 0;

        // Ajustar o valor ao step usando floor
        double adjustedValue = Math.floor(value / stepSize) * stepSize;

        // Garantir que o resultado tenha a mesma precisão do step
        BigDecimal result = BigDecimal.valueOf(adjustedValue).setScale(decimalPlaces, RoundingMode.HALF_UP);

        // Retornar como String
        return result.toPlainString();
    }

    public String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(timestamp));
    }

    /**
     * Atualiza os dados da conta, como os dados da carteira (balance).
     * Para mais detalhes visualizar a classe AccountData.
     */
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

    /**
     * Recupera e salva as ordens que ainda estão abertas na conta
     */
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

    /**
     * Atualiza as informações do ativo analisado, retornando os dados das candles
     */
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
     * Verifica se há pelo menos uma fração mínima da moeda em questão na conta (step size).
     * Caso haja, define a posição atual como COMPRADA, se não define como VENDIDA.
     */
    private boolean updateActualTradePosition() {
        Double stepSize = getAssetStepSize();
        return this.lastStockAccountBalance >= stepSize;
    }

    /**
     * Recupera o Step Size do ativo (fração mínima obtível do ativo).
     */
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

    /**
     * Recupera o Tick Size do ativo (fração mínima do preço de venda ou compra de um ativo).
     */
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

    /**
     * Ativa o mecanismo de stop-loss com base no preço atual, no preço ponderado e na porcentagem de stop-loss.
     * Este metodo verifica se o preço atual da ação e o preço ponderado estão abaixo do preço calculado
     * de stop-loss. Caso as condições sejam atendidas e a posição atual de trade esteja ativa, ele cancela
     * todas as ordens existentes, aguarda um pequeno intervalo e executa uma ordem de venda a mercado.
     *
     * @return {@code true} se o stop-loss for ativado e a venda for executada, ou {@code false} caso contrário.
     */
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
//                emailService.sendEmail(config.getEmailReceiverList(), "Robô Binance - Venda de Mercado Executada", createBodyOrder(response));
//                LOGGER.info("Email enviado.");
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

    /**
     * Cria uma ordem de compra, seja baseada em quantidade ou em valor.
     *
     * @param quantity      Valor da quantidade (se for uma compra por quantidade).
     * @param purchaseValue Valor em USDT (se for uma compra baseada em valor).
     * @return
     * @throws Exception Em caso de erro ao enviar a ordem.
     */
    public OrderResponseFull createLimitedOrder(Double quantity, Double purchaseValue) throws Exception {
        double closePrice = stockData.getLast().getClosePrice(); // Pega o valor do candle atual
        double volume = stockData.getLast().getVolume(); // Volume atual do mercado
        double averageVolume = calculateLastAverageVolume(); // Calcula o último volume médio
        double rsi = indicators.getRsi().getLast();

        // Determina o preço limite com base no RSI e volume
        double limitPrice = calculateLimitPrice(closePrice, volume, averageVolume, rsi);

        // Calcula a quantidade com base no valor, se necessário
        if (purchaseValue != null) {
            quantity = purchaseValue / limitPrice;
        }

        // Ajusta o preço limite e a quantidade para os tamanhos permitidos
        limitPrice = adjustToTickDouble(limitPrice, tickSize);
        quantity = adjustToStepDouble(quantity, stepSize);

        LOGGER.info("Enviando ordem limitada de COMPRA para {}", config.getOperationCode());
        LOGGER.info(" - Tipo de Ordem: {}", purchaseValue != null ? "Por Valor" : "Por Quantidade");
        if (purchaseValue != null) LOGGER.info(" - Valor Total (USDT): {}", purchaseValue);
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
            parameters.put("price", limitPrice);

            String rawResponse = client.createTrade().newOrder(parameters);
            JSONObject response = new JSONObject(rawResponse);

            if (response.has("code") && response.has("msg")) {
                throw new RuntimeException("Erro ao realizar request 'newOrder': " + response);
            }

            this.actualTradePosition = true;

            LOGGER.info("Ordem de COMPRA limitada enviada com sucesso:");
            LOG_SERVICE.createLogOrder(response);

            return new OrderResponseFull(response);

            // Envia e-mail avisando da ação realizada
//            emailService.sendEmail(config.getEmailReceiverList(), "Robô Binance - Compra Limitada Executada", createBodyOrder(response));
//            LOGGER.info("Email enviado.");
        } catch (Exception e) {
            LOGGER.error("Erro ao enviar ordem limitada de compra: ", e);
            throw e;
        }
    }

    /**
     * Calcula o preço limite com base nas condições de mercado.
     *
     * @param closePrice Preço de fechamento atual.
     * @param volume Volume atual do mercado.
     * @param averageVolume Volume médio calculado.
     * @param rsi Valor do RSI.
     * @return Preço limite calculado.
     */
    private double calculateLimitPrice(double closePrice, double volume, double averageVolume, double rsi) {
        if (rsi < 30) {
            // Mercado sobrevendido, tenta comprar um pouco mais abaixo
            return closePrice - (0.002 * closePrice);
        } else if (volume < averageVolume) {
            // Volume baixo (mercado lateral), ajuste pequeno acima
            return closePrice + (0.002 * closePrice);
        } else {
            // Volume alto (mercado volátil), ajuste maior acima
            return closePrice + (0.005 * closePrice);
        }
    }

    /**
     * Cria uma ordem de compra baseada em quantidade.
     *
     * @throws Exception Em caso de erro ao enviar a ordem.
     */
    public OrderResponseFull buyLimitedOrder() throws Exception {
        return createLimitedOrder(config.getTradedQuantity() - partialQuantityDiscount, null);
    }

    /**
     * Cria uma ordem de compra baseada em valor.
     *
     * @param purchaseValue Valor para a compra.
     * @return
     * @throws Exception Em caso de erro ao enviar a ordem.
     */
    public OrderResponseFull buyLimitedOrderByValue(double purchaseValue) throws Exception {
        return createLimitedOrder(null, purchaseValue);
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

    /**
     * Cria uma ordem de venda por um preço mínimo (Limited Order),
     * utilizando o RSI e o Volume Médio para calcular o valor.
     */
    public OrderResponseFull sellLimitedOrder() throws Exception {
        try {
            double closePrice = stockData.getLast().getClosePrice(); // Pega o valor do candle atual
            double volume = stockData.getLast().getVolume(); // Volume atual do mercado
            double averageVolume = calculateLastAverageVolume(); // Calcula o último volume médio
            double rsi = indicators.getRsi().getLast();

            double price = 0.0; // Pode ser trocado depois
            double limitPrice;

            if (price == 0) {
                if (rsi > 70.0) {
                    // Mercado sobrecomprado, tenta vender um pouco acima
                    limitPrice = closePrice + (0.002 * closePrice);
                } else if (volume < averageVolume) {
                    // Volume baixo (mercado lateral), ajuste pequeno abaixo
                    limitPrice = closePrice - (0.002 * closePrice);
                } else {
                    // Volume alto (mercado volátil), ajuste maior abaixo (caso caia muito rápido)
                    limitPrice = closePrice - (0.005 * closePrice);
                }

                // Garantindo que o preço limite seja maior que o mínimo aceitável
                if (limitPrice < (lastBuyPrice * (1 - config.getAcceptableLossPercentage()))) {
                    LOGGER.info("Ajuste de venda aceitável ({}%):", config.getAcceptableLossPercentage() * 100);
                    LOGGER.info(" - De: {}", limitPrice);
                    limitPrice = getMinimumPriceToSell();
                    LOGGER.info(" - Para: {}", limitPrice);
                }
            } else {
                limitPrice = price;
            }

            // Ajustar o preço limite para o tickSize permitido
            limitPrice = adjustToTickDouble(limitPrice, tickSize);

            // Ajustar a quantidade para o stepSize permitido
            double quantity = adjustToStepDouble(lastStockAccountBalance, stepSize);

            // Log information
            LOGGER.info("Enviando ordem limitada de venda para " + config.getOperationCode() + ":");
            LOGGER.info(" - RSI: " + rsi);
            LOGGER.info(" - Quantidade: " + quantity);
            LOGGER.info(" - Close Price: " + closePrice);
            LOGGER.info(" - Preço Limite: " + limitPrice);

            // Enviando ordem de venda
            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("symbol", config.getOperationCode());
            parameters.put("side", "SELL");
            parameters.put("type", "LIMIT");
            parameters.put("timeInForce", "GTC");
            parameters.put("quantity", quantity);
            parameters.put("price", limitPrice);

            String rawResponse = client.createTrade().newOrder(parameters);
            JSONObject response = new JSONObject(rawResponse);

            if (response.has("code") && response.has("msg")) {
                throw new RuntimeException("Erro ao realizar request 'newOrder': " + response);
            }

            OrderResponseFull orderResponseFull = new OrderResponseFull(response);

            actualTradePosition = false; // Update position to sold
            LOGGER.info("Ordem VENDA limitada enviada com sucesso: ");
            LOG_SERVICE.createLogOrder(response); // Create a log

            return new OrderResponseFull(response);
        } catch (Exception e) {
            LOGGER.error("Erro ao enviar ordem limitada de venda: ", e);
            throw e;
        }

    }

    private double calculateLastAverageVolume() {
        MovingAverageCalculator movingAverageCalculator = new MovingAverageCalculator();
        List<Double> volumes = stockData.stream().map(StockData::getVolume).toList();
        return movingAverageCalculator.calculateMovingAverage(volumes, 20).getLast();
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
                LOGGER.info(" - Não há ordens de {} abertas para {}.",
                        side.equalsIgnoreCase("SELL") ? "VENDA" : "COMPRA",
                        config.getOperationCode());
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
}
