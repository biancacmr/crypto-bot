package com.bianca.AutomaticCryptoTrader.strategies;

import com.bianca.AutomaticCryptoTrader.config.BinanceConfig;
import com.bianca.AutomaticCryptoTrader.indicators.Indicators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class MovingAverageStrategy {
    private final Logger LOGGER = LoggerFactory.getLogger(MovingAverageStrategy.class);

    @Autowired
    private final BinanceConfig binanceConfig;
    @Autowired
    private final Indicators indicators;

    public MovingAverageStrategy(BinanceConfig binanceConfig, Indicators indicators) {
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
