package com.bianca.AutomaticCryptoTrader.indicators;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component // Isso torna a classe Indicators um bean gerenciado pelo Spring
public class Indicators {
    private List<Double> maFast;
    private List<Double> maSlow;
    private Double maFastGradient;
    private Double maSlowGradient;
    private ArrayList<Double> volatility;
    private List<Double> rsi;
    private List<Double> MACDLine;
    private List<Double> MACDSignalLine;
    private List<Double> MACDHistogram;

    public Indicators() {
    }

    public List<Double> getRsi() {
        return rsi;
    }

    public void setRsi(List<Double> rsi) {
        this.rsi = rsi;
    }

    public ArrayList<Double> getVolatility() {
        return volatility;
    }

    public void setVolatility(ArrayList<Double> volatility) {
        this.volatility = volatility;
    }

    public Double getMaSlowGradient() {
        return maSlowGradient;
    }

    public void setMaSlowGradient(Double maSlowGradient) {
        this.maSlowGradient = maSlowGradient;
    }

    public Double getMaFastGradient() {
        return maFastGradient;
    }

    public void setMaFastGradient(Double maFastGradient) {
        this.maFastGradient = maFastGradient;
    }

    public List<Double> getMaSlow() {
        return maSlow;
    }

    public void setMaSlow(List<Double> maSlow) {
        this.maSlow = maSlow;
    }

    public List<Double> getMaFast() {
        return maFast;
    }

    public void setMaFast(List<Double> maFast) {
        this.maFast = maFast;
    }

    public void setMACDHistogram(List<Double> MACDHistogram) {
        this.MACDHistogram = MACDHistogram;
    }

    public void setMACDLine(List<Double> MACDLine) {
        this.MACDLine = MACDLine;
    }

    public void setMACDSignalLine(List<Double> MACDSignalLine) {
        this.MACDSignalLine = MACDSignalLine;
    }

    public List<Double> getMACDHistogram() {
        return MACDHistogram;
    }

    public List<Double> getMACDLine() {
        return MACDLine;
    }

    public List<Double> getMACDSignalLine() {
        return MACDSignalLine;
    }
}
