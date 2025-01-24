package com.bianca.AutomaticCryptoTrader.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

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

    @Value("${binance.emailReceiver}")
    private String emailReceiver;

    @Value("${spring.mail.host}")
    private String emailHost;

    @Value("${spring.mail.port}")
    private String emailPort;

    @Value("${spring.mail.username}")
    private String emailUsername;

    @Value("${spring.mail.password}")
    private String emailPassword;

    @Value("${binance.maxBuyPrice}")
    private String maxBuyPrice;

    @Value("${binance.buyCurrency}")
    private String buyCurrency;

    @Value("${binance.stopLossPercentage}")
    private Double stopLossPercentage;

    @Value("${binance.acceptableLossPercentage}")
    private Double acceptableLossPercentage;

    @Value("${binance.fallbackActive}")
    private boolean fallbackActive;

    public Double getAcceptableLossPercentage() {
        return acceptableLossPercentage/100;
    }

    public Double getStopLossPercentage() {
        return stopLossPercentage/100;
    }

    public String getMaxBuyPrice() {
        return maxBuyPrice;
    }

    public String getBuyCurrency() {
        return buyCurrency;
    }

    public String getEmailPassword() {
        return emailPassword;
    }

    public String getEmailUsername() {
        return emailUsername;
    }

    public String getEmailPort() {
        return emailPort;
    }

    public String getEmailHost() {
        return emailHost;
    }

    public String getEmailReceiver() {
        return emailReceiver;
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

    public String getCandlePeriod() {
        return candlePeriod;
    }

    public Double getTradedQuantity() {
        return tradedQuantity;
    }

    public String getOperationCode() {
        return operationCode;
    }

    public Double getVolatilityFactor() {
        return volatilityFactor;
    }

    public boolean isFallbackActive() {
        return fallbackActive;
    }
}
