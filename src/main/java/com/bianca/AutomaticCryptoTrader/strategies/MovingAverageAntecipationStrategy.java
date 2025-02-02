package com.bianca.AutomaticCryptoTrader.strategies;

import com.bianca.AutomaticCryptoTrader.config.BinanceConfig;
import com.bianca.AutomaticCryptoTrader.indicators.Indicators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovingAverageAntecipationStrategy {
    private final Logger LOGGER = LoggerFactory.getLogger(MovingAverageAntecipationStrategy.class);
    private final BinanceConfig binanceConfig;
    private final Indicators indicators;

    public MovingAverageAntecipationStrategy(BinanceConfig binanceConfig, Indicators indicators) {
        this.binanceConfig = binanceConfig;
        this.indicators = indicators;
    }

    public Boolean getTradeDecision() {
        // Pega os indicadores
        double lastMaFast = indicators.getMaFast().getLast(); // Última Média Rápida
        double prevMaFast = indicators.getMaFast().get(indicators.getMaFast().size() - 3); // Penúltima Média Rápida
        double lastMaSlow = indicators.getMaSlow().getLast(); // Última Média Lenta
        double prevMaSlow = indicators.getMaSlow().get(indicators.getMaSlow().size() - 3); // Penúltima Média Lenta

        // Última volatilidade
        double lastVolatility = indicators.getVolatility().get(indicators.getVolatility().size() - 2);

        // Calcula o gradiente (mudança) das médias móveis
        double fastGradient = lastMaFast - prevMaFast;
        double slowGradient = lastMaSlow - prevMaSlow;

        // Calcula a diferença atual entre as médias
        double currentDifference = Math.abs(lastMaFast - lastMaSlow);

        // Inicializa a decisão
        Boolean maTradeDecision = null;

        // Toma a decisão com base em volatilidade + gradiente
        if (currentDifference < (lastVolatility * binanceConfig.getVolatilityFactor())) {

            // Comprar se a média rápida está convergindo para cruzar de baixo para cima
            if (fastGradient > 0 && fastGradient > slowGradient) {
                maTradeDecision = true; // Comprar
            }

            // Vender se a média rápida está convergindo para cruzar de cima para baixo
            else if (fastGradient < 0 && fastGradient < slowGradient) {
                maTradeDecision = false; // Vender
            }
        }

        LOGGER.info(binanceConfig.getVolatilityFactor().toString());

        LOGGER.info("\n---------------------------------------------\n");
        LOGGER.info("Estratégia executada: Moving Average Antecipation");
        LOGGER.info("({})", binanceConfig.getOperationCode());
        LOGGER.info(" | Última Média Rápida: {}", lastMaFast);
        LOGGER.info(" | Última Média Lenta: {}", lastMaSlow);
        LOGGER.info(" | Última Volatilidade: {}", lastVolatility);
        LOGGER.info(" | Diferença Atual: {}", currentDifference);
        LOGGER.info(" | Diferença para antecipação: {}", binanceConfig.getVolatilityFactor() * lastVolatility);
        LOGGER.info(" | Gradiente Rápido: {} ({})", fastGradient, (fastGradient > 0 ? "SUBINDO" : "DESCENDO"));
        LOGGER.info(" | Gradiente Lento: {} ({})", slowGradient, (slowGradient > 0 ? "SUBINDO" : "DESCENDO"));
        LOGGER.info(" | Decisão: {}",
                maTradeDecision == null ? "NENHUMA" : maTradeDecision ? "COMPRAR" : "VENDER");
        LOGGER.info("\n---------------------------------------------\n");

        return maTradeDecision;
    }
}
