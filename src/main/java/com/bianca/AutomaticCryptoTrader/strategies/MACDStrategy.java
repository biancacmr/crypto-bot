package com.bianca.AutomaticCryptoTrader.strategies;

import com.bianca.AutomaticCryptoTrader.config.BinanceConfig;
import com.bianca.AutomaticCryptoTrader.indicators.Indicators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MACDStrategy implements Strategy {
    private final Logger LOGGER = LoggerFactory.getLogger(MACDStrategy.class);

    private BinanceConfig binanceConfig;
    private Indicators indicators;

    public MACDStrategy(BinanceConfig binanceConfig, Indicators indicators) {
        this.binanceConfig = binanceConfig;
        this.indicators = indicators;
    }

    @Override
    public void setIndicators(Indicators indicators) {
        this.indicators = indicators;
    }

    /**
     * Função usada para backtests
     */
    @Override
    public TradeSignal generateSignal(int candlePosition) {
        TradeSignal tradeDecision;

        try {
            List<Double> macdLine = indicators.getMACDLine();
            List<Double> signalLine = indicators.getMACDSignalLine();

            double macdCurrent = macdLine.get(candlePosition);
            double macdPrevious = macdLine.get(candlePosition - 1);
            double signalPrevious = signalLine.get(candlePosition - 1);
            double signalCurrent = signalLine.get(candlePosition);

            VortexStrategy vortexStrategy = new VortexStrategy(binanceConfig, indicators);

            if (macdPrevious < signalPrevious && macdCurrent > signalCurrent) {
                tradeDecision = TradeSignal.BUY;
            } else if (macdPrevious > signalPrevious && macdCurrent < signalCurrent) {
                tradeDecision = TradeSignal.SELL;
            } else {
                tradeDecision = TradeSignal.HOLD;
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao gerar sinal em MACDStrategy: ", e);
            return TradeSignal.HOLD;
        }

        return tradeDecision;
    }

    public TradeSignal getTradeDecision() {
        TradeSignal tradeDecision;

        try {
            List<Double> macdLine = indicators.getMACDLine();
            List<Double> signalLine = indicators.getMACDSignalLine();

            double macdPrevious = macdLine.get(macdLine.size() - 2);
            double macdCurrent = macdLine.getLast();
            double signalPrevious = signalLine.get(signalLine.size() - 2);
            double signalCurrent = signalLine.getLast();

            if (macdPrevious < signalPrevious && macdCurrent > signalCurrent) {
                tradeDecision = TradeSignal.BUY;
            } else if (macdPrevious > signalPrevious && macdCurrent < signalCurrent) {
                tradeDecision = TradeSignal.SELL;
            } else {
                tradeDecision = TradeSignal.HOLD;
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
                    tradeDecision.name());

            LOGGER.info("\n---------------------------------------------\n");
        } catch (Exception e) {
            LOGGER.error("Erro ao gerar sinal em MACDStrategy: ", e);
            return TradeSignal.HOLD;
        }

        return tradeDecision;
    }
}
