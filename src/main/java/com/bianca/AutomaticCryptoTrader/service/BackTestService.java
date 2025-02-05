package com.bianca.AutomaticCryptoTrader.service;

import com.bianca.AutomaticCryptoTrader.indicators.Indicators;
import com.bianca.AutomaticCryptoTrader.model.StockData;
import com.bianca.AutomaticCryptoTrader.strategies.Strategy;
import com.bianca.AutomaticCryptoTrader.strategies.TradeSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class BackTestService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackTestService.class);

    @Autowired
    private final BinanceService binanceService;
    @Autowired
    private final IndicatorsService indicatorsService;
    @Autowired
    private final Indicators indicators;

    public BackTestService(BinanceService binanceService, IndicatorsService indicatorsService, Indicators indicators) {
        this.binanceService = binanceService;
        this.indicatorsService = indicatorsService;
        this.indicators = indicators;
    }

    public void runBacktest(Strategy strategy, String operationCode, String candlePeriod) {
        try {
            LOGGER.info("Running BACKTESTS for: " + strategy.getClass().getName());
            LOGGER.info("ASSET: {} | CANDLE PERIOD: {}", operationCode, candlePeriod);

            // Obter os dados históricos (últimos 1000 candles)
            ArrayList<StockData> stockData = binanceService.updateStockData(operationCode, candlePeriod, 1000);

            // Calcular os indicadores
            indicatorsService.calculateIndicators(stockData);
            strategy.setIndicators(indicators);

            // Rodar o backtest para a estratégia
            double accountBalance = 1000; // Por exemplo, saldo inicial
            int tradesExecuted = 0;
            double assetsOwned = 0; // Quantidade de ativos comprados
            double buyPrice = 0; // Preço de compra para cálculo de venda (sempre que uma compra ocorre)

            int candleQuantity = 0;

            if (candlePeriod.equals("1h")) candleQuantity = 30 * 24; // 10 dias
            if (candlePeriod.equals("4h")) candleQuantity = 30 * (24 / 4); // 30 dias
            if (candlePeriod.equals("15m")) candleQuantity = 960; // 10 dias

            stockData = new ArrayList<>(stockData.subList(0, Math.min(stockData.size(), candleQuantity)));
            for (int i = 2; i < stockData.size(); i++) { // Começando do índice 2, para evitar problemas de índice
                TradeSignal signal = strategy.generateSignal(i);

                // Se o sinal for de compra
                if (signal.equals(TradeSignal.BUY) && accountBalance > 0) {
                    double priceAtBuy = stockData.get(i).getClosePrice();
                    double maxQuantity = accountBalance / priceAtBuy; // Quantidade de ativos que podemos comprar

                    // Comprar o máximo possível
                    assetsOwned += maxQuantity;
                    accountBalance -= maxQuantity * priceAtBuy; // Deduzir o valor gasto

                    buyPrice = priceAtBuy; // Armazenar o preço de compra
                    tradesExecuted++;
                }
                // Se o sinal for de venda e tivermos ativos para vender
                else if (signal.equals(TradeSignal.SELL) && assetsOwned > 0) {
                    double priceAtSell = stockData.get(i).getClosePrice();
                    // Vender todos os ativos
                    accountBalance += assetsOwned * priceAtSell;
                    assetsOwned = 0; // Resetando os ativos, pois todos foram vendidos
                    tradesExecuted++;
                }

                if (i == stockData.size() - 1) {
                    double priceAtSell = stockData.get(i).getClosePrice();
                    // Vender todos os ativos
                    accountBalance += assetsOwned * priceAtSell;
                    assetsOwned = 0; // Resetando os ativos, pois todos foram vendidos
                    tradesExecuted++;
                }
            }

            // Relatar resultados
            LOGGER.info("Initial Balance: 1000 USDT");
            LOGGER.info("Account Balance: " + accountBalance + " USDT");
            LOGGER.info("Trades Executed: " + tradesExecuted);
            LOGGER.info("----------------------------------------------");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
