package com.bianca.AutomaticCryptoTrader.indicators;

import java.util.ArrayList;
import java.util.List;

public class ExponentialMovingAverage {
    private double alpha;
    private Double oldValue;

    public ExponentialMovingAverage() {
    }

    /**
     * Calcula a média móvel exponencial (EMA) para uma lista de valores.
     *
     * @param series Lista de valores.
     * @param window Período da média móvel.
     * @return Lista de valores suavizados.
     */
    public List<Double> calculateEMA(List<Double> series, int window) {
        List<Double> ema = new ArrayList<>();
        alpha = 1.0 / (window);

        for (Double val : series) {
            ema.add(average(val));
        }

        return ema;
    }

    private double average(double value) {
        if (oldValue == null) {
            oldValue = value;
            return value;
        }

        double newValue = oldValue + alpha * (value - oldValue);
        oldValue = newValue;
        return newValue;
    }
}
