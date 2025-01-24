package com.bianca.AutomaticCryptoTrader.task;

import com.bianca.AutomaticCryptoTrader.config.BinanceConfig;
import com.bianca.AutomaticCryptoTrader.model.MovingAverageResult;
import com.bianca.AutomaticCryptoTrader.model.MovingAverageStrategy;
import com.bianca.AutomaticCryptoTrader.service.BinanceService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledExecutorService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class BotExecution {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Logger LOGGER = LoggerFactory.getLogger(BotExecution.class);
    private static final int BASE_DELAY = 15;
    private final BinanceService binanceService;
    private final BinanceConfig binanceConfig;

    @PostConstruct
    public void initialize() {
        scheduleTask(0);
    }

    private void scheduleTask(int delay) {
        scheduler.schedule(this::execute, delay, TimeUnit.MINUTES);
    }

    public BotExecution(BinanceService binanceService, BinanceConfig binanceConfig) {
        this.binanceService = binanceService;
        this.binanceConfig = binanceConfig;
    }

    public void execute() {
        int delay = BASE_DELAY;

        try {

            LOGGER.info("---------------------------------------------");
            LOGGER.info("INIT AutomaticCryptoTrader...");
            LOGGER.info("---------------------------------------------");

            binanceService.updateAllData();

            LOGGER.info("---------------------------------------------");
            LOGGER.info("Executado {}", getCurrentDateTime());
            LOGGER.info("Posição atual: {}", binanceService.getActualTradePosition() ? "COMPRADO" : "VENDIDO");
            LOGGER.info("Balanço atual: {} ({})", binanceService.getLastStockAccountBalance(), binanceConfig.getStockCode());

            // Estratégias sentinelas de saída
            // Se perder mais que o panic sell aceitável, ele sai à mercado, independente.
            if (binanceService.stopLossTrigger()) {
                LOGGER.info("STOP LOSS executado - Saindo a preço de mercado...");
                scheduleTask(delay);
            }

            // Executar estratégias
            MovingAverageStrategy movingAverageStrategy = new MovingAverageStrategy(binanceService, LOGGER);
            MovingAverageResult movingAverageResult = movingAverageStrategy.executeMovingAverageTradeStrategy();
            boolean tradeDecision = movingAverageResult.getTradeDecision();
            binanceService.setLastTradeDecision(tradeDecision);

            LOGGER.info("---------------------------------------");
            LOGGER.info("Estratégia executada: Moving Average");
            LOGGER.info("({})", binanceConfig.getOperationCode());
            LOGGER.info(" | Última Média Rápida = " + movingAverageResult.getLastMaFast());
            LOGGER.info(" | Última Média Lenta = " + movingAverageResult.getLastMaSlow());
            LOGGER.info(" | Decisão = " + (tradeDecision ? "COMPRAR" : "VENDER"));
            LOGGER.info("---------------------------------------");
            LOGGER.info("Decisão Final: " + (tradeDecision ? "COMPRAR" : "VENDER"));

            /* Se a posição atual for VENDIDA e a decisão for de COMPRA, compra o ativo
              Se a posição atual for COMPRADA e a decisão for de VENDA, vende o ativo
              Demais casos, nada acontece **/
            if (binanceService.getLastTradeDecision() && !binanceService.getActualTradePosition()) {
                LOGGER.info("Ação final: COMPRAR");

                binanceService.printStock(binanceConfig.getStockCode());
                binanceService.printStock("USDT");
                binanceService.cancelAllOrders();

                Thread.sleep(2000);
                binanceService.buyLimitedOrder();
                Thread.sleep(2000);

                binanceService.updateAllData();
                binanceService.printStock(binanceConfig.getStockCode());
                binanceService.printStock("USDT");

                delay *= 2;
            } else if (!binanceService.getLastTradeDecision() && binanceService.getActualTradePosition()) {
                LOGGER.info("Ação final: VENDER");

                binanceService.printStock(binanceConfig.getStockCode());
                binanceService.printStock("USDT");
                binanceService.cancelAllOrders();

                Thread.sleep(2000);
                binanceService.sellLimitedOrder();
                Thread.sleep(2000);

                binanceService.updateAllData();
                binanceService.printStock(binanceConfig.getStockCode());
                binanceService.printStock("USDT");

                delay *= 2;
            } else {
                LOGGER.info("Ação final: MANTER POSIÇÃO");

            }

            LOGGER.info("---------------------------------------------");
            scheduleTask(delay);
        } catch (Exception e) {
            LOGGER.error("Erro ao executar tarefa agendada: ", e);
            scheduleTask(delay);
        }
    }

    private String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss:SSS");
        return now.format(formatter);
    }
}
