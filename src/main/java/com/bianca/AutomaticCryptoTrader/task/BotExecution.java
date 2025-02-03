package com.bianca.AutomaticCryptoTrader.task;

import com.bianca.AutomaticCryptoTrader.config.BinanceConfig;
import com.bianca.AutomaticCryptoTrader.model.OrderResponse;
import com.bianca.AutomaticCryptoTrader.service.BinanceService;
import com.bianca.AutomaticCryptoTrader.service.EmailService;
import com.bianca.AutomaticCryptoTrader.service.IndicatorsService;
import com.bianca.AutomaticCryptoTrader.service.StrategiesService;
import com.bianca.AutomaticCryptoTrader.strategies.TradeSignal;
import com.binance.connector.client.impl.spot.Trade;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledExecutorService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class BotExecution {
    private static final Logger LOGGER = LoggerFactory.getLogger(BotExecution.class);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final int BASE_DELAY = 15;
    private final StrategiesService strategiesService;
    private final BinanceService binanceService;
    private final IndicatorsService indicatorsCalculator;
    private final BinanceConfig binanceConfig;
    private final EmailService emailService;

    @Autowired
    public BotExecution(StrategiesService strategiesService, BinanceService binanceService, BinanceConfig binanceConfig, IndicatorsService indicatorsCalculator, EmailService emailService) {
        this.strategiesService = strategiesService;
        this.binanceService = binanceService;
        this.binanceConfig = binanceConfig;
        this.indicatorsCalculator = indicatorsCalculator;
        this.emailService = emailService;
    }

    @PostConstruct
    public void initialize() {
        scheduleTask(0);
    }

    private void scheduleTask(int delay) {
        scheduler.schedule(this::execute, delay, TimeUnit.MINUTES);
    }

    public void execute() {
        int delay = BASE_DELAY;

        try {
            LOGGER.info("---------------------------------------------");
            LOGGER.info("Robô iniciando...");
            LOGGER.info("---------------------------------------------\n");

            binanceService.updateAllData();

            LOGGER.info("\n---------------------------------------------\n");
            LOGGER.info("Executado {}", getCurrentDateTime());
            LOGGER.info("Posição atual: {}", binanceService.getActualTradePosition() ? "COMPRADO" : "VENDIDO");
            LOGGER.info("Balanço atual: {} ({})", binanceService.getLastStockAccountBalance(), binanceConfig.getStockCode());

            // Estratégias sentinelas de saída
            // Se perder mais que o panic sell aceitável, ele sai a mercado.
            if (binanceService.stopLossTrigger()) {
                LOGGER.info("STOP LOSS executado - Saindo a preço de mercado...");
                scheduleTask(delay);
            }

            // Calcular indicadores
            indicatorsCalculator.calculateIndicators(binanceService.getStockData());

            // Executar estratégias
            TradeSignal tradeDecision = strategiesService.getFinalDecision();
            binanceService.setLastTradeDecision(tradeDecision);

            if (!tradeDecision.equals(TradeSignal.HOLD)) {
                handleTradeDecision(tradeDecision);
                delay *= 2;
            } else {
                LOGGER.info("\n---------------------------------------------\n");
                LOGGER.info("Decisão Final: INCONCLUSIVA (considere ativar a estratégia de fallback!)");
            }

            LOGGER.info("\n---------------------------------------------\n");
            scheduleTask(delay);
        } catch (Exception e) {
            LOGGER.error("Erro ao executar tarefa agendada: ", e);
            scheduleTask(delay);
        }
    }

    private void handleTradeDecision(TradeSignal tradeDecision) throws Exception {
        if (tradeDecision.equals(TradeSignal.BUY) && !binanceService.getActualTradePosition()) {
            executeBuyOrder();
        } else if (tradeDecision.equals(TradeSignal.SELL) && binanceService.getActualTradePosition()) {
            executeSellOrder();
        } else {
            LOGGER.info("Ação final: MANTER POSIÇÃO");
        }
    }

    private void executeBuyOrder() throws Exception {
        LOGGER.info("Ação final: COMPRAR");
        LOGGER.info("\n---------------------------------------------\n");

        LOGGER.debug("Carteira em {} [ANTES]:", binanceConfig.getStockCode());
        binanceService.printStock(binanceConfig.getStockCode());

        OrderResponse orderResponse = binanceService.buyLimitedOrderByValue(binanceConfig.getMaxBuyValue());
        emailService.sendEmailOrder(binanceConfig.getEmailReceiverList(), orderResponse);
        binanceService.updateAllData();

        LOGGER.debug("Carteira em {} [DEPOIS]:", binanceConfig.getStockCode());
        binanceService.printStock(binanceConfig.getStockCode());
    }

    private void executeSellOrder() throws Exception {
        LOGGER.info("Ação final: VENDER");
        LOGGER.info("\n---------------------------------------------\n");

        LOGGER.debug("Carteira em {} [ANTES]:", binanceConfig.getStockCode());
        binanceService.printStock(binanceConfig.getStockCode());

        OrderResponse orderResponse = binanceService.sellLimitedOrder();
        emailService.sendEmailOrder(binanceConfig.getEmailReceiverList(), orderResponse);
        binanceService.updateAllData();

        LOGGER.debug("Carteira em {} [DEPOIS]:", binanceConfig.getStockCode());
        binanceService.printStock(binanceConfig.getStockCode());
    }

    private String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss:SSS");
        return now.format(formatter);
    }
}
