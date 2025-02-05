package com.bianca.AutomaticCryptoTrader.strategies;

import com.bianca.AutomaticCryptoTrader.indicators.Indicators;

public interface Strategy {
    TradeSignal generateSignal(int candlePosition);
    void setIndicators(Indicators indicators);
}
