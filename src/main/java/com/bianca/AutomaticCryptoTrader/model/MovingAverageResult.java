package com.bianca.AutomaticCryptoTrader.model;

public class MovingAverageResult {
    private final boolean tradeDecision; // true for Buy, false for Sell
    private final double lastMaFast;
    private final double lastMaSlow;

    public MovingAverageResult(boolean tradeDecision, double lastMaFast, double lastMaSlow) {
        this.tradeDecision = tradeDecision;
        this.lastMaFast = lastMaFast;
        this.lastMaSlow = lastMaSlow;
    }

    public boolean getTradeDecision() {
        return tradeDecision;
    }

    public double getLastMaSlow() {
        return lastMaSlow;
    }

    public double getLastMaFast() {
        return lastMaFast;
    }
}