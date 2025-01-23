package com.bianca.AutomaticCryptoTrader.model;

import com.bianca.AutomaticCryptoTrader.service.BinanceService;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MovingAverageStrategy {
    private final BinanceService binanceService;
    private final Logger LOGGER;

    public MovingAverageStrategy(BinanceService binanceService, Logger logger) {
        this.binanceService = binanceService;
        this.LOGGER = logger;
    }

    public MovingAverageResult executeMovingAverageTradeStrategy() {
        int fastWindow = 7;
        int slowWindow = 40;

        ArrayList<StockData> stockData = binanceService.getStockData();

        // Get the close prices
        List<Double> closePrices = stockData.stream()
                .map(StockData::getClosePrice)
                .collect(Collectors.toList());

        // Calculate the fast moving average (short window)
        List<Double> maFast = calculateMovingAverage(closePrices, fastWindow);

        // Calculate the slow moving average (long window)
        List<Double> maSlow = calculateMovingAverage(closePrices, slowWindow);

        // Get the last values of each moving average
        double lastMaFast = maFast.getLast();
        double lastMaSlow = maSlow.getLast();

        // Determine trade decision
        // Rápida > Lenta = COMPRAR | Lenta > Rápida = VENDER
        boolean tradeDecision = lastMaFast > lastMaSlow;

        return new MovingAverageResult(tradeDecision, lastMaFast, lastMaSlow);
    }

    private List<Double> calculateMovingAverage(List<Double> data, int windowSize) {
        if (windowSize <= 0 || data == null || data.size() < windowSize) {
            throw new IllegalArgumentException("Invalid window size or data list is too small.");
        }

        DescriptiveStatistics stats = new DescriptiveStatistics();
        stats.setWindowSize(windowSize);

        List<Double> movingAverages = new ArrayList<>();

        // Add data points to the stats object and calculate the moving average
        for (Double value : data) {
            stats.addValue(value);
            if (stats.getN() == windowSize) { // Ensure the window is full before adding averages
                movingAverages.add(stats.getMean());
            }
        }

        return movingAverages;
    }
}
