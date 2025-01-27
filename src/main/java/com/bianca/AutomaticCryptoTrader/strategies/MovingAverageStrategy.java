package com.bianca.AutomaticCryptoTrader.strategies;

import com.bianca.AutomaticCryptoTrader.config.BinanceConfig;
import com.bianca.AutomaticCryptoTrader.indicators.MovingAverageCalculator;
import com.bianca.AutomaticCryptoTrader.model.StockData;
import com.bianca.AutomaticCryptoTrader.service.BinanceService;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MovingAverageStrategy {
    private final MovingAverageCalculator movingAverageCalculator = new MovingAverageCalculator();
    private final BinanceConfig binanceConfig;
    private final BinanceService binanceService;
    private final Logger LOGGER;

    public MovingAverageStrategy(BinanceService binanceService, Logger logger, BinanceConfig binanceConfig) {
        this.binanceService = binanceService;
        this.LOGGER = logger;
        this.binanceConfig = binanceConfig;
    }

    public boolean getTradeDecision() {
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

        // Get the last values of each moving average
        double lastMaFast = maFast.getLast();
        double lastMaSlow = maSlow.getLast();

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
