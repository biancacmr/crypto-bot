package com.bianca.AutomaticCryptoTrader.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class BinanceConfig {
    @Value("${binance.apiKey}")
    private String apiKey;

    @Value("${binance.secretKey}")
    private String secretKey;

    @Value("${binance.url}")
    private String url;

    @Value("${binance.stockCode}")
    private String stockCode;

    @Value("${binance.operationCode}")
    private String operationCode;

    @Value("${binance.candlePeriod}")
    private String candlePeriod;

    @Value("${binance.tradedQuantity}")
    private Double tradedQuantity;

    @Value("${binance.volatilityFactor}")
    private Double volatilityFactor;

    @Value("${binance.stopLossPercentage}")
    private Double stopLossPercentage;

    @Value("${binance.acceptableLossPercentage}")
    private Double acceptableLossPercentage;

    @Value("${binance.fallbackActive}")
    private boolean fallbackActive;

    @Value("${receiversList}")
    private String emailReceiver;

    @Value("${spring.mail.host}")
    private String emailHost;

    @Value("${spring.mail.port}")
    private String emailPort;

    @Value("${spring.mail.username}")
    private String emailUsername;

    @Value("${spring.mail.password}")
    private String emailPassword;

    @Value("${binance.maxBuyValue}")
    private Double maxBuyValue;

    @Value("${binance.maFastWindow}")
    private int maFastWindow;

    @Value("${binance.maSlowWindow}")
    private int maSlowWindow;

    @Value("${binance.rsiWindow}")
    private int rsiWindow;

    @Value("${binance.volatilityWindow}")
    private int volatilityWindow;

    @Value("${binance.fastWindowMACD}")
    private int fastWindowMACD;

    @Value("${binance.slowWindowMACD}")
    private int slowWindowMACD;

    @Value("${binance.smoothingParameterMACD}")
    private int smoothingParameterMACD;

    public int getFastWindowMACD() {
        return fastWindowMACD;
    }

    public int getSlowWindowMACD() {
        return slowWindowMACD;
    }

    public int getSmoothingParameterMACD() {
        return smoothingParameterMACD;
    }

    public int getRsiWindow() {
        return this.rsiWindow;
    }

    public int getVolatilityWindow() {
        return this.volatilityWindow;
    }

    public int getMaSlowWindow() {
        return this.maSlowWindow;
    }

    public int getMaFastWindow() {
        return maFastWindow;
    }

    public List<String> getEmailReceiverList() {
        List<String> emailList = new ArrayList<>();
        if (emailReceiver != null && !emailReceiver.isEmpty()) {
            emailList = Arrays.asList(emailReceiver.split(","));
        }
        return emailList;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getUrl() {
        return url;
    }

    public String getStockCode() {
        return stockCode;
    }

    public String getOperationCode() {
        return operationCode;
    }

    public String getCandlePeriod() {
        return candlePeriod;
    }

    public Double getTradedQuantity() {
        return tradedQuantity;
    }

    public Double getVolatilityFactor() {
        return volatilityFactor;
    }

    public String getEmailReceiver() {
        return emailReceiver;
    }

    public String getEmailHost() {
        return emailHost;
    }

    public String getEmailPort() {
        return emailPort;
    }

    public String getEmailUsername() {
        return emailUsername;
    }

    public String getEmailPassword() {
        return emailPassword;
    }

    public Double getStopLossPercentage() {
        return stopLossPercentage / 100;
    }

    public Double getAcceptableLossPercentage() {
        return acceptableLossPercentage / 100;
    }

    public boolean isFallbackActive() {
        return fallbackActive;
    }

    public double getMaxBuyValue() {
        return maxBuyValue;
    }
}
