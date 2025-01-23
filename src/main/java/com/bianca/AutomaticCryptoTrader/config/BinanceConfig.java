package com.bianca.AutomaticCryptoTrader.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BinanceConfig {
    @Getter
    @Value("${binance.apiKey}")
    private String apiKey;

    @Getter
    @Value("${binance.secretKey}")
    private String secretKey;

    @Getter
    @Value("${binance.url}")
    private String url;

    @Getter
    @Value("${binance.stockCode}")
    private String stockCode;

    @Getter
    @Value("${binance.operationCode}")
    private String operationCode;

    @Getter
    @Value("${binance.candlePeriod}")
    private String candlePeriod;

    @Getter
    @Value("${binance.tradedQuantity}")
    private Double tradedQuantity;

    @Getter
    @Value("${binance.volatilityFactor}")
    private Double volatilityFactor;

    @Getter
    @Value("${binance.emailReceiver}")
    private String emailReceiver;

    @Getter
    @Value("${spring.mail.host}")
    private String emailHost;

    @Getter
    @Value("${spring.mail.port}")
    private String emailPort;

    @Getter
    @Value("${spring.mail.username}")
    private String emailUsername;

    @Getter
    @Value("${spring.mail.password}")
    private String emailPassword;

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
}
