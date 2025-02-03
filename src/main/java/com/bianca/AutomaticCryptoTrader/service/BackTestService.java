package com.bianca.AutomaticCryptoTrader.service;

import com.bianca.AutomaticCryptoTrader.config.BinanceConfig;
import com.bianca.AutomaticCryptoTrader.indicators.Indicators;
import com.bianca.AutomaticCryptoTrader.model.StockData;
import com.bianca.AutomaticCryptoTrader.strategies.MACDStrategy;
import com.bianca.AutomaticCryptoTrader.strategies.Strategy;
import com.bianca.AutomaticCryptoTrader.strategies.TradeSignal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class BackTestService {

    @Autowired
    private final Indicators indicators;

    @Autowired
    private final BinanceConfig binanceConfig;

    private final BinanceService binanceService;
    private final IndicatorsService indicatorsService;

    public BackTestService(Indicators indicators, BinanceConfig binanceConfig, BinanceService binanceService, IndicatorsService indicatorsService) {
        this.indicators = indicators;
        this.binanceConfig = binanceConfig;
        this.binanceService = binanceService;
        this.indicatorsService = indicatorsService;
    }

    public void runBacktest(Strategy strategy) {
        // Obter os dados históricos (últimos 1000 candles)
        ArrayList<StockData> stockData = binanceService.updateStockData("SOLUSDT", "1h", 1000);

        // Calcular os indicadores
        indicatorsService.calculateIndicators(stockData);

        // Rodar o backtest para a estratégia
        double accountBalance = 1000; // Por exemplo, saldo inicial
        int tradesExecuted = 0;
        double assetsOwned = 0; // Quantidade de ativos comprados
        double buyPrice = 0; // Preço de compra para cálculo de venda (sempre que uma compra ocorre)

        for (int i = 2; i < stockData.size(); i++) { // Começando do índice 2, para evitar problemas de índice
            TradeSignal signal = strategy.generateSignal(i);

            // Se o sinal for de compra
            if (signal == TradeSignal.BUY && accountBalance > 0) {
                double priceAtBuy = stockData.get(i).getClosePrice();
                double maxQuantity = accountBalance / priceAtBuy; // Quantidade de ativos que podemos comprar

                // Comprar o máximo possível
                assetsOwned += maxQuantity;
                accountBalance -= maxQuantity * priceAtBuy; // Deduzir o valor gasto

                buyPrice = priceAtBuy; // Armazenar o preço de compra
                tradesExecuted++;
            }
            // Se o sinal for de venda e tivermos ativos para vender
            else if (signal == TradeSignal.SELL && assetsOwned > 0) {
                double priceAtSell = stockData.get(i).getClosePrice();

                // Vender todos os ativos
                accountBalance += assetsOwned * priceAtSell;
                assetsOwned = 0; // Resetando os ativos, pois todos foram vendidos
                tradesExecuted++;
            }
        }

        // Relatar resultados
        System.out.println("Initial Balance: 1000 USDT");
        System.out.println("Account Balance: " + accountBalance + " USDT");
        System.out.println("Trades Executed: " + tradesExecuted);
        System.out.println("Remaining Assets: " + assetsOwned + " SOL");
    }
}
