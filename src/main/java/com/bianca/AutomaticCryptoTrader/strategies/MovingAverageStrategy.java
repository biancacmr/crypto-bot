package com.bianca.AutomaticCryptoTrader.strategies;

import com.bianca.AutomaticCryptoTrader.config.BinanceConfig;
import com.bianca.AutomaticCryptoTrader.indicators.Indicators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovingAverageStrategy implements Strategy {
    private final Logger LOGGER = LoggerFactory.getLogger(MovingAverageStrategy.class);

    private final BinanceConfig binanceConfig;
    private final Indicators indicators;

    public MovingAverageStrategy(BinanceConfig binanceConfig, Indicators indicators) {
        this.binanceConfig = binanceConfig;
        this.indicators = indicators;
    }

    @Override
    public TradeSignal generateSignal() {
        // Get the last values of each moving average
        double lastMaFast = indicators.getMaFast().getLast(); // Última Média Rápida
        double lastMaSlow = indicators.getMaSlow().getLast();

        // Determine trade decision
        // Rápida > Lenta = COMPRAR | Lenta > Rápida = VENDER
        TradeSignal tradeDecision;

        if (lastMaFast > lastMaSlow) {
            tradeDecision = TradeSignal.BUY;
        } else {
            tradeDecision = TradeSignal.SELL;
        }

        LOGGER.info("\n---------------------------------------\n");
        LOGGER.info("Estratégia executada: Moving Average");
        LOGGER.info("({})", binanceConfig.getOperationCode());
        LOGGER.info(" | Última Média Rápida = {}", lastMaFast);
        LOGGER.info(" | Última Média Lenta = {}", lastMaSlow);
        LOGGER.info(" | Decisão = " + tradeDecision.name());
        LOGGER.info("\n---------------------------------------\n");

        return tradeDecision;
    }
}
