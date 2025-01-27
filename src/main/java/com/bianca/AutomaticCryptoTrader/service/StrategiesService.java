package com.bianca.AutomaticCryptoTrader.service;

import com.bianca.AutomaticCryptoTrader.config.BinanceConfig;
import com.bianca.AutomaticCryptoTrader.indicators.Indicators;
import com.bianca.AutomaticCryptoTrader.indicators.IndicatorsCalculator;
import com.bianca.AutomaticCryptoTrader.strategies.MovingAverageAntecipationStrategy;
import com.bianca.AutomaticCryptoTrader.strategies.MovingAverageStrategy;
import org.slf4j.Logger;

public class StrategiesService {
    private final BinanceConfig binanceConfig;
    private final Indicators indicators;
    private final Logger LOGGER;

    public StrategiesService(Logger logger, BinanceConfig binanceConfig, Indicators indicators) {
        this.LOGGER = logger;
        this.binanceConfig = binanceConfig;
        this.indicators = indicators;
    }

    public Boolean getFinalDecision() {
        // Estratégia principal: Moving Average Antecipation
        MovingAverageAntecipationStrategy movingAverageAntecipationStrategy = new MovingAverageAntecipationStrategy(LOGGER, binanceConfig, indicators);
        Boolean tradeDecisionMAANT = movingAverageAntecipationStrategy.getTradeDecision();

        if (tradeDecisionMAANT == null && binanceConfig.isFallbackActive()) {
            LOGGER.info("Estratégia de MA Antecipation inconclusiva");
            LOGGER.info("Executando estratégia de fallback...");

            MovingAverageStrategy movingAverageStrategy = new MovingAverageStrategy(LOGGER, binanceConfig, indicators);
            return movingAverageStrategy.getTradeDecision();
        }

        return tradeDecisionMAANT;
    }
}
