package com.bianca.AutomaticCryptoTrader.service;

import com.bianca.AutomaticCryptoTrader.config.BinanceConfig;
import com.bianca.AutomaticCryptoTrader.strategies.MovingAverageAntecipationStrategy;
import com.bianca.AutomaticCryptoTrader.strategies.MovingAverageStrategy;
import org.slf4j.Logger;

public class StrategyRunner {
    private final BinanceConfig binanceConfig;
    private final BinanceService binanceService;
    private final Logger LOGGER;

    public StrategyRunner(BinanceService binanceService, Logger logger, BinanceConfig binanceConfig) {
        this.binanceService = binanceService;
        this.LOGGER = logger;
        this.binanceConfig = binanceConfig;
    }

    public Boolean getFinalDecision() {
        // Estratégia principal: Moving Average Antecipation
        MovingAverageAntecipationStrategy movingAverageAntecipationStrategy = new MovingAverageAntecipationStrategy(binanceService, LOGGER, binanceConfig);
        Boolean tradeDecisionMAANT = movingAverageAntecipationStrategy.getTradeDecision();

        if (tradeDecisionMAANT == null && binanceConfig.isFallbackActive()) {
            LOGGER.info("Estratégia de MA Antecipation inconclusiva");
            LOGGER.info("Executando estratégia de fallback...");

            MovingAverageStrategy movingAverageStrategy = new MovingAverageStrategy(binanceService, LOGGER, binanceConfig);
            return movingAverageStrategy.getTradeDecision();
        }

        return tradeDecisionMAANT;
    }
}
