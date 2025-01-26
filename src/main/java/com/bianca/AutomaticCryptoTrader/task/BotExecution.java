package com.bianca.AutomaticCryptoTrader.task;

import com.bianca.AutomaticCryptoTrader.config.BinanceConfig;
import com.bianca.AutomaticCryptoTrader.service.BinanceService;
import com.bianca.AutomaticCryptoTrader.service.StrategyRunner;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            LOGGER.info("Robô iniciando...");
            LOGGER.info("---------------------------------------------\n");

            binanceService.updateAllData();

            LOGGER.info("\n---------------------------------------------\n");
            LOGGER.info("Executado {}", getCurrentDateTime());
            LOGGER.info("Posição atual: {}", binanceService.getActualTradePosition() ? "COMPRADO" : "VENDIDO");
            LOGGER.info("Balanço atual: {} ({})", binanceService.getLastStockAccountBalance(), binanceConfig.getStockCode());

            // Estratégias sentinelas de saída
            // Se perder mais que o panic sell aceitável, ele sai a mercado, independente.
            if (binanceService.stopLossTrigger()) {
                LOGGER.info("STOP LOSS executado - Saindo a preço de mercado...");
                scheduleTask(delay);
            }

            // Executar estratégias
            StrategyRunner strategyRunner = new StrategyRunner(binanceService, LOGGER, binanceConfig);
            Boolean tradeDecision = strategyRunner.getFinalDecision();
            binanceService.setLastTradeDecision(tradeDecision);

            if (tradeDecision != null) {
                // Cancela as ordens abertas, se houver alguma
                if (binanceService.hasOpenOrders("BUY") || binanceService.hasOpenOrders("SELL")) {
                    LOGGER.info("\n---------------------------------------------\n");
                    binanceService.cancelAllOrders();
                    Thread.sleep(2000);
                }

                /* Se a posição atual for VENDIDA e a decisão for de COMPRA, compra o ativo
                  Se a posição atual for COMPRADA e a decisão for de VENDA, vende o ativo
                  Demais casos, nada acontece **/
                if (binanceService.getLastTradeDecision() && !binanceService.getActualTradePosition()) {
                    LOGGER.info("Ação final: COMPRAR");
                    LOGGER.info("\n---------------------------------------------\n");

                    LOGGER.info("Carteira em {} [ANTES]:", binanceConfig.getStockCode());
                    binanceService.printStock(binanceConfig.getStockCode());

                    // Realiza a compra
//                    binanceService.buyLimitedOrder();
                    binanceService.buyLimitedOrderByValue(binanceConfig.getMaxBuyValue());
                    Thread.sleep(2000);

                    // Atualiza os dados da conta
                    binanceService.updateAllData();

                    LOGGER.info("Carteira em {} [DEPOIS]:", binanceConfig.getStockCode());
                    binanceService.printStock(binanceConfig.getStockCode());

                    delay *= 2;
                } else if (!binanceService.getLastTradeDecision() && binanceService.getActualTradePosition()) {
                    LOGGER.info("Ação final: VENDER");
                    LOGGER.info("\n---------------------------------------------\n");

                    LOGGER.info("Carteira em {} [ANTES]:", binanceConfig.getStockCode());
                    binanceService.printStock(binanceConfig.getStockCode());

                    // Realiza a venda
                    binanceService.sellLimitedOrder();
                    Thread.sleep(2000);

                    // Atualiza os dados da conta
                    binanceService.updateAllData();

                    LOGGER.info("Carteira em {} [DEPOIS]:", binanceConfig.getStockCode());
                    binanceService.printStock(binanceConfig.getStockCode());

                    delay *= 2;

                    delay *= 2;
                } else {
                    LOGGER.info("Ação final: MANTER POSIÇÃO");
                }
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

    private String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss:SSS");
        return now.format(formatter);
    }
}
