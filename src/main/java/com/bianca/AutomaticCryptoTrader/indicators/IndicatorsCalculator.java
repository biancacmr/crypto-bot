package com.bianca.AutomaticCryptoTrader.indicators;

import com.bianca.AutomaticCryptoTrader.model.StockData;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class IndicatorsCalculator {
    private List<Double> movingAverageFast;

    public IndicatorsCalculator() {

    }

    public Indicators calculate(StockData stockData) {
        Indicators indicators = new Indicators();

        double movingAverage = calculateMovingAverage(stockData);
        double rsi = calculateRSI(stockData);
        // Outros cálculos...

        return new Indicators(movingAverage, rsi);
    }

    private double calculateMovingAverage(StockData stockData) {
        // Implementação do cálculo
        return 0; // Exemplo
    }

    private double calculateRSI(StockData stockData) {
        // Implementação do cálculo
        return 0; // Exemplo
    }

    private void calculeMovingAverages(ArrayList<StockData> stockData) {
        int fastWindow = 7;
        int slowWindow = 40;

        // Get the close prices
        List<Double> closePrices = stockData.stream()
                .map(StockData::getClosePrice)
                .collect(Collectors.toList());

        MovingAverageCalculator movingAverageCalculator = new MovingAverageCalculator();

        // Calculate the fast moving average (short window)
        List<Double> maFast = movingAverageCalculator.calculateMovingAverage(closePrices, fastWindow);

        // Calculate the slow moving average (long window)
        List<Double> maSlow = movingAverageCalculator.calculateMovingAverage(closePrices, slowWindow);
    }
}
