package com.bianca.AutomaticCryptoTrader.service;

import com.bianca.AutomaticCryptoTrader.config.BinanceConfig;
import com.bianca.AutomaticCryptoTrader.indicators.ExponentialMovingAverage;
import com.bianca.AutomaticCryptoTrader.indicators.Indicators;
import com.bianca.AutomaticCryptoTrader.indicators.MovingAverageCalculator;
import com.bianca.AutomaticCryptoTrader.model.StockData;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IndicatorsService {
    private BinanceConfig binanceConfig;
    private Indicators indicators;

    @Autowired
    public IndicatorsService(Indicators indicators, BinanceConfig binanceConfig) {
        this.indicators = indicators;
        this.binanceConfig = binanceConfig;
    }

    public void calculateIndicators(ArrayList<StockData> stockData) {
        calculateMovingAverage(stockData, indicators);
        calculateRSI(stockData, indicators);
        calculateVolatily(stockData, indicators);
        calculateMACD(stockData, indicators);
        calculateVortex(stockData, indicators);
    }

    private void calculateVortex(ArrayList<StockData> stockData, Indicators indicators) {
    }

    public void calculateMACD(List<StockData> stockData, Indicators indicators) {
        List<Double> closePrices = stockData.stream().map(StockData::getClosePrice).toList();
        int fastWindowSize = binanceConfig.getFastWindowMACD();
        int slowWindowSize = binanceConfig.getSlowWindowMACD();
        int smoothingParameterMACD = binanceConfig.getSmoothingParameterMACD();

        if (closePrices.size() < slowWindowSize) {
            throw new RuntimeException("Not enough data for MACD calculation.");
        }

        ExponentialMovingAverage exponentialMovingAverage = new ExponentialMovingAverage();

        List<Double> ema12 = exponentialMovingAverage.calculateEMA(closePrices, fastWindowSize);
        List<Double> ema26 = exponentialMovingAverage.calculateEMA(closePrices, slowWindowSize);
        List<Double> macdLine = new ArrayList<>();
        List<Double> histogram = new ArrayList<>();

        int startIndex = Math.max(ema12.size(), ema26.size());
        for (int i = startIndex; i < ema12.size(); i++) {
            macdLine.add(ema12.get(i) - ema26.get(i));
        }

        List<Double> signalLine = new ArrayList<>(exponentialMovingAverage.calculateEMA(macdLine, smoothingParameterMACD));

        for (int i = 0; i < signalLine.size(); i++) {
            histogram.add(macdLine.get(i) - signalLine.get(i));
        }

        indicators.setMACDSignalLine(signalLine);
        indicators.setMACDLine(macdLine);
        indicators.setMACDHistogram(histogram);
    }

    /**
     * Calcula a Moving Volatily do ativo de acordo com o candle period configurado
     * e windowSize.
     */
    public void calculateVolatily(List<StockData> stockData, Indicators indicators) {
        List<Double> closePrices = stockData.stream().map(StockData::getClosePrice).toList();
        int windowSize = binanceConfig.getVolatilityWindow();

        DescriptiveStatistics stats = new DescriptiveStatistics();
        stats.setWindowSize(windowSize);

        ArrayList<Double> rollingStds = new ArrayList<>();

        for (Double price : closePrices) {
            stats.addValue(price);
            if (stats.getN() == windowSize) {
                rollingStds.add(stats.getStandardDeviation());
            }
        }

        indicators.setVolatility(rollingStds);
    }

    /**
     * Calcula o índice de força relativa (RSI) para uma série de preços
     */
    private void calculateRSI(List<StockData> stockData, Indicators indicators) {
        List<Double> series = stockData.stream().map(StockData::getClosePrice).toList();
        int window = binanceConfig.getRsiWindow();

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

        ExponentialMovingAverage exponentialMovingAverage = new ExponentialMovingAverage();

        // Médias suavizadas (EMA)
        List<Double> avgGain = exponentialMovingAverage.calculateEMA(gain, window);
        List<Double> avgLoss = exponentialMovingAverage.calculateEMA(loss, window);

        // Calcula RS e RSI
        List<Double> rsi = new ArrayList<>();
        for (int i = 0; i < avgGain.size(); i++) {
            double rs = avgGain.get(i) / avgLoss.get(i);
            rsi.add(100.0 - (100.0 / (1.0 + rs)));
        }

        indicators.setRsi(rsi);
    }

    private void calculateMovingAverage(ArrayList<StockData> stockData, Indicators indicators) {
        int fastWindow = binanceConfig.getMaFastWindow();
        int slowWindow = binanceConfig.getMaSlowWindow();

        // Get the close prices
        List<Double> closePrices = stockData.stream()
                .map(StockData::getClosePrice)
                .collect(Collectors.toList());

        MovingAverageCalculator movingAverageCalculator = new MovingAverageCalculator();

        // Calculate the fast moving average (short window)
        List<Double> maFast = movingAverageCalculator.calculateMovingAverage(closePrices, fastWindow);

        // Calculate the slow moving average (long window)
        List<Double> maSlow = movingAverageCalculator.calculateMovingAverage(closePrices, slowWindow);

        // Pega os indicadores
        double lastMaFast = maFast.getLast(); // Última Média Rápida
        double prevMaFast = maFast.get(maFast.size() - 3); // Penúltima Média Rápida
        double lastMaSlow = maSlow.getLast(); // Última Média Lenta
        double prevMaSlow = maSlow.get(maSlow.size() - 3); // Penúltima Média Lenta

        // Calcula o gradiente (mudança) das médias móveis
        double fastGradient = lastMaFast - prevMaFast;
        double slowGradient = lastMaSlow - prevMaSlow;

        indicators.setMaFast(maFast);
        indicators.setMaSlow(maSlow);
        indicators.setMaFastGradient(fastGradient);
        indicators.setMaSlowGradient(slowGradient);
    }
}
