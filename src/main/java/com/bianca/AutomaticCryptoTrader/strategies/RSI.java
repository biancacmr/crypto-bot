package com.bianca.AutomaticCryptoTrader.strategies;

import java.util.ArrayList;
import java.util.List;

public class RSI {

    /**
     * Calcula o índice de força relativa (RSI) para uma série de preços.
     *
     * @param series   Lista de preços.
     * @param window   Período da média móvel.
     * @param lastOnly Indica se deve retornar apenas o último valor do RSI.
     * @return O RSI calculado, como um valor único (se lastOnly for true) ou uma lista de valores (se lastOnly for false).
     */
    public static Object calculateRSI(List<Double> series, int window, boolean lastOnly) {
        if (series.size() < window) {
            throw new IllegalArgumentException("A série de preços deve ter pelo menos o tamanho da janela.");
        }

        List<Double> delta = new ArrayList<>();
        List<Double> gain = new ArrayList<>();
        List<Double> loss = new ArrayList<>(); // Valor absoluto das perdas

        // Calcula a diferença entre os preços
        for (int i = 0; i < series.size(); i++) {
            if (i == 0) {
                delta.add(0.0);
            } else {
                delta.add(series.get(i) - series.get(i - 1));
            }
        }

        // Calcula ganhos e perdas
        gain = delta.stream().map(value -> {
            if (value > 0.0) return value;
            else return 0.0;
        }).toList();

        loss = delta.stream().map(value -> {
            if (value < 0.0) return Math.abs(value);
            else return 0.0;
        }).toList();

        // Médias suavizadas (EMA)
        List<Double> avgGain = calculateEMA(gain, window);
        List<Double> avgLoss = calculateEMA(loss, window);

        // Calcula RS e RSI
        List<Double> rsi = new ArrayList<>();
        for (int i = 0; i < avgGain.size(); i++) {
            double rs = avgGain.get(i) / avgLoss.get(i);
            rsi.add(100.0 - (100.0 / (1.0 + rs)));
        }

        // Retorna apenas o último valor ou a série inteira
        if (lastOnly) {
            return rsi.get(rsi.size() - 1);
        } else {
            return rsi;
        }
    }

    /**
     * Calcula a média móvel exponencial (EMA) para uma lista de valores.
     *
     * @param series Lista de valores.
     * @param window Período da média móvel.
     * @return Lista de valores suavizados.
     */
    private static List<Double> calculateEMA(List<Double> series, int window) {
        List<Double> ema = new ArrayList<>();
        double alpha = 1.0 / (window);

        ExponentialMovingAverage EMACalculator = new ExponentialMovingAverage(alpha);

        for (Double val : series) {
            ema.add(EMACalculator.average(val));
        }

        return ema;
    }

    public static void main(String[] args) {
        List<Double> prices = List.of(44.34, 44.09, 44.15, 43.61, 44.33, 44.83, 45.10, 45.42, 45.84, 46.08, 45.89, 46.03, 45.61, 46.28);
        int window = 14;
        boolean lastOnly = false;

        Object result = calculateRSI(prices, window, lastOnly);

        if (lastOnly) {
            System.out.println("Último RSI: " + result);
        } else {
            System.out.println("RSI: " + result);
        }
    }
}
