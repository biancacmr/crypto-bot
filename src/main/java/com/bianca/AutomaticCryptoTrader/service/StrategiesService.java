package com.bianca.AutomaticCryptoTrader.service;

import com.bianca.AutomaticCryptoTrader.config.BinanceConfig;
import com.bianca.AutomaticCryptoTrader.indicators.Indicators;
import com.bianca.AutomaticCryptoTrader.strategies.MovingAverageAntecipationStrategy;
import com.bianca.AutomaticCryptoTrader.strategies.MovingAverageStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StrategiesService {
    private final Logger LOGGER = LoggerFactory.getLogger(StrategiesService.class);

    private BinanceConfig binanceConfig;
    private Indicators indicators;

    @Autowired
    public StrategiesService(BinanceConfig binanceConfig, Indicators indicators) {
        this.binanceConfig = binanceConfig;
        this.indicators = indicators;
    }

    public Boolean getFinalDecision() {
        // Estratégia principal: Moving Average Antecipation
        MovingAverageAntecipationStrategy movingAverageAntecipationStrategy = new MovingAverageAntecipationStrategy(binanceConfig, indicators);
        Boolean tradeDecisionMAANT = movingAverageAntecipationStrategy.getTradeDecision();

        if (tradeDecisionMAANT == null && binanceConfig.isFallbackActive()) {
            LOGGER.info("Estratégia de MA Antecipation inconclusiva");
            LOGGER.info("Executando estratégia de fallback...");

            MovingAverageStrategy movingAverageStrategy = new MovingAverageStrategy(binanceConfig, indicators);
            return movingAverageStrategy.getTradeDecision();
        }

        return tradeDecisionMAANT;
    }
}
