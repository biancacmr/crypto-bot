package com.bianca.AutomaticCryptoTrader.strategies;

import com.bianca.AutomaticCryptoTrader.config.BinanceConfig;
import com.bianca.AutomaticCryptoTrader.indicators.Indicators;
import com.bianca.AutomaticCryptoTrader.indicators.MovingAverageCalculator;
import com.bianca.AutomaticCryptoTrader.model.StockData;
import com.bianca.AutomaticCryptoTrader.service.BinanceService;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MovingAverageStrategy {
    private final MovingAverageCalculator movingAverageCalculator = new MovingAverageCalculator();
    private final BinanceConfig binanceConfig;
    private final Logger LOGGER;
    private final Indicators indicators;

    public MovingAverageStrategy(Logger logger, BinanceConfig binanceConfig, Indicators indicators) {
        this.LOGGER = logger;
        this.binanceConfig = binanceConfig;
        this.indicators = indicators;
    }

    public boolean getTradeDecision() {
        // Get the last values of each moving average
        double lastMaFast = indicators.getMaFast().getLast(); // Última Média Rápida
        double lastMaSlow = indicators.getMaSlow().getLast();

        // Determine trade decision
        // Rápida > Lenta = COMPRAR | Lenta > Rápida = VENDER
        boolean tradeDecision = lastMaFast > lastMaSlow;

        LOGGER.info("\n---------------------------------------\n");
        LOGGER.info("Estratégia executada: Moving Average");
        LOGGER.info("({})", binanceConfig.getOperationCode());
        LOGGER.info(" | Última Média Rápida = {}", lastMaFast);
        LOGGER.info(" | Última Média Lenta = {}", lastMaSlow);
        LOGGER.info(" | Decisão = " + (tradeDecision ? "COMPRAR" : "VENDER"));
        LOGGER.info("\n---------------------------------------\n");

        return tradeDecision;
    }
}
