package com.bianca.AutomaticCryptoTrader.strategies;

import com.bianca.AutomaticCryptoTrader.config.BinanceConfig;
import com.bianca.AutomaticCryptoTrader.indicators.Indicators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class VortexStrategy {
    private final Logger LOGGER = LoggerFactory.getLogger(VortexStrategy.class);

    private final BinanceConfig binanceConfig;
    private final Indicators indicators;

    public VortexStrategy(BinanceConfig binanceConfig, Indicators indicators) {
        this.binanceConfig = binanceConfig;
        this.indicators = indicators;
    }

//    @Override
    public TradeSignal generateSignal() {
        List<Double> vortexViPlus = indicators.getVortexViPlus();
        List<Double> vortexViMinus = indicators.getVortexViMinus();

        double viPlus = vortexViPlus.getLast();
        double viPlusPrevious = vortexViPlus.get(vortexViPlus.size() - 2);
        double viMinus = vortexViMinus.getLast();
        double viMinusPrevious = vortexViMinus.get(vortexViMinus.size() - 2);

        TradeSignal tradeDecision;

        if (viPlus > viMinus) {
            tradeDecision = TradeSignal.BUY;
        } else if (viMinus > viPlus) {
            tradeDecision = TradeSignal.SELL;
        } else {
            tradeDecision = TradeSignal.HOLD;
        }

        // Calcula o gradiente (mudança) das linhas
        double viPlusGradient = viPlus - viPlusPrevious;
        double viMinusGradient = viMinus - viMinusPrevious;

        LOGGER.info("\n---------------------------------------------\n");
        LOGGER.info("Estratégia executada: Vortex");
        LOGGER.info("({})", binanceConfig.getOperationCode());
        LOGGER.info(" | VI+ Atual: {}", viPlus);
        LOGGER.info(" | VI+ Anterior: {}", viPlusPrevious);
        LOGGER.info(" | Gradiente VI+: {} ({})", viPlusGradient, (viPlusGradient > 0 ? "SUBINDO" : "DESCENDO"));
        LOGGER.info(" | VI- Atual: {}", viMinus);
        LOGGER.info(" | VI- Anterior: {}", viMinusPrevious);
        LOGGER.info(" | Gradiente VI-: {} ({})", viMinusGradient, (viMinusGradient > 0 ? "SUBINDO" : "DESCENDO"));
        LOGGER.info(" | Decisão: {}",
                tradeDecision.name());
        LOGGER.info("\n---------------------------------------------\n");

        return tradeDecision;
    }
}
