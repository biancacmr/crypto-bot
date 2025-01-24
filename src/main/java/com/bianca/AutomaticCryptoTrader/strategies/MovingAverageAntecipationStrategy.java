package com.bianca.AutomaticCryptoTrader.strategies;

import com.bianca.AutomaticCryptoTrader.config.BinanceConfig;
import com.bianca.AutomaticCryptoTrader.model.StockData;
import com.bianca.AutomaticCryptoTrader.service.BinanceService;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MovingAverageAntecipationStrategy {
    private final MovingAverageCalculator movingAverageCalculator = new MovingAverageCalculator();
    private final BinanceConfig binanceConfig = new BinanceConfig();
    private final BinanceService binanceService;
    private final Logger LOGGER;

    public MovingAverageAntecipationStrategy(BinanceService binanceService, Logger logger) {
        this.binanceService = binanceService;
        this.LOGGER = logger;
    }

    public Boolean getTradeDecision() {
        int fastWindow = 7;
        int slowWindow = 40;

        ArrayList<StockData> stockData = binanceService.getStockData();

        // Get the close prices
        List<Double> closePrices = stockData.stream()
                .map(StockData::getClosePrice)
                .collect(Collectors.toList());

        // Calculate the fast moving average (short window)
        List<Double> maFast = movingAverageCalculator.calculateMovingAverage(closePrices, fastWindow);

        // Calculate the slow moving average (long window)
        List<Double> maSlow = movingAverageCalculator.calculateMovingAverage(closePrices, slowWindow);

        // Pega os indicadores
        double lastMaFast = maFast.getLast(); // Última Média Rápida
        double prevMaFast = maFast.get(maFast.size() - 3); // Penúltima Média Rápida
        double lastMaSlow = maSlow.getLast(); // Última Média Lenta
        double prevMaSlow = maSlow.get(maFast.size() - 3); // Penúltima Média Lenta

        // Última volatilidade
        ArrayList<Double> volatilitys = binanceService.getRollingVolatility();
        double lastVolatility = volatilitys.get(volatilitys.size() - 2);

        // Calcula o gradiente (mudança) das médias móveis
        double fastGradient = lastMaFast - prevMaFast;
        double slowGradient = lastMaSlow - prevMaSlow;

        // Calcula a diferença atual entre as médias
        double currentDifference = Math.abs(lastMaFast - lastMaSlow);

        // Inicializa a decisão
        Boolean maTradeDecision = null;

        // Toma a decisão com base em volatilidade + gradiente
        if (currentDifference < lastVolatility * binanceConfig.getVolatilityFactor()) {

            // Comprar se a média rápida está convergindo para cruzar de baixo para cima
            if (fastGradient > 0 && fastGradient > slowGradient) {
                maTradeDecision = true; // Comprar
            }

            // Vender se a média rápida está convergindo para cruzar de cima para baixo
            else if (fastGradient < 0 && fastGradient < slowGradient) {
                maTradeDecision = false; // Vender
            }
        }

        LOGGER.info("---------------------------------------");
        LOGGER.info("Estratégia executada: Moving Average Antecipation");
        LOGGER.info("({})", binanceConfig.getOperationCode());
        LOGGER.info(" | Última Média Rápida: {}", lastMaFast);
        LOGGER.info(" | Última Média Lenta: {}", lastMaSlow);
        LOGGER.info(" | Última Volatilidade: {}", lastVolatility);
        LOGGER.info(" | Diferença Atual: {}", currentDifference);
        LOGGER.info(" | Diferença para antecipação: {}", binanceConfig.getVolatilityFactor() * lastVolatility);
        LOGGER.info(" | Gradiente Rápido: {} ({})", fastGradient, (fastGradient > 0 ? "SUBINDO" : "DESCENDO"));
        LOGGER.info(" | Gradiente Lento: {} ({})", slowGradient, (slowGradient > 0 ? "SUBINDO" : "DESCENDO"));
        LOGGER.info(" | Decisão: {}}",
                maTradeDecision == null ? "NENHUMA" : maTradeDecision ? "COMPRAR" : "VENDER");
        LOGGER.info("---------------------------------------");

        return maTradeDecision;
    }
}
