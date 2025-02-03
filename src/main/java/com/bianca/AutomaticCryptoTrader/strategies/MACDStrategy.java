package com.bianca.AutomaticCryptoTrader.strategies;

import com.bianca.AutomaticCryptoTrader.config.BinanceConfig;
import com.bianca.AutomaticCryptoTrader.indicators.Indicators;
import okhttp3.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class MACDStrategy {
    private final Logger LOGGER = LoggerFactory.getLogger(MACDStrategy.class);

    @Autowired
    private final BinanceConfig binanceConfig;

    @Autowired
    private final Indicators indicators;

    public MACDStrategy(BinanceConfig binanceConfig, Indicators indicators) {
        this.binanceConfig = binanceConfig;
        this.indicators = indicators;
    }

    public Boolean getTradeDecision() {
        List<Double> macdLine = indicators.getMACDLine();
        List<Double> signalLine = indicators.getMACDSignalLine();

        double macdPrevious = macdLine.get(macdLine.size() - 2);
        double macdCurrent = macdLine.getLast();
        double signalPrevious = signalLine.get(signalLine.size() - 2);
        double signalCurrent = signalLine.getLast();

        Boolean tradeDecision = null;

        if (macdPrevious < signalPrevious && macdCurrent > signalCurrent) {
            tradeDecision = true;
        } else if (macdPrevious > signalPrevious && macdCurrent < signalCurrent) {
            tradeDecision = false;
        }

        // Calcula o gradiente (mudança) das linhas
        double macdGradient = macdCurrent - macdPrevious;
        double signalGradient = signalCurrent - signalPrevious;

        LOGGER.info("\n---------------------------------------------\n");
        LOGGER.info("Estratégia executada: MACD (Moving Average Convergence/Divergence)");
        LOGGER.info("({})", binanceConfig.getOperationCode());
        LOGGER.info(" | MACD Line Atual: {}", macdCurrent);
        LOGGER.info(" | MACD Line Anterior: {}", macdPrevious);
        LOGGER.info(" | Gradiente MACD Line: {} ({})", macdGradient, (macdGradient > 0 ? "SUBINDO" : "DESCENDO"));
        LOGGER.info(" | MACD Signal Line Atual: {}", signalCurrent);
        LOGGER.info(" | MACD Signal Line Anterior: {}", signalPrevious);
        LOGGER.info(" | Gradiente MACD Signal Line: {} ({})", signalGradient, (signalGradient > 0 ? "SUBINDO" : "DESCENDO"));
        LOGGER.info(" | Decisão: {}",
                tradeDecision == null ? "HOLD" : tradeDecision ? "COMPRAR" : "VENDER");
        LOGGER.info("\n---------------------------------------------\n");

        return tradeDecision;
    }
}
