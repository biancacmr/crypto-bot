package com.bianca.AutomaticCryptoTrader.indicators;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.ArrayList;
import java.util.List;

public class MovingAverageCalculator {
    public MovingAverageCalculator() {

    }

    public List<Double> calculateMovingAverage(List<Double> data, int windowSize) {
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
