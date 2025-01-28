package com.bianca.AutomaticCryptoTrader.service;

import org.json.JSONObject;
import org.slf4j.Logger;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class LogService {
    private final Logger LOGGER;

    public LogService(Logger logger) {
        this.LOGGER = logger;
    }

    public void createLogOrder(JSONObject order) {
        // Extracting necessary information
        String side = order.getString("side");
        String type = order.getString("type");
        double quantity = order.getDouble("executedQty");
        String asset = order.getString("symbol");
        double totalValue = order.getDouble("cummulativeQuoteQty");
        long timestamp = order.getLong("transactTime");
        String status = order.getString("status");

        // Handling optional fields
        String pricePerUnit = "-";
        String currency = "-";

        if (order.has("fills") && !order.getJSONArray("fills").isEmpty()) {
            JSONObject fill = order.getJSONArray("fills").getJSONObject(0);
            pricePerUnit = fill.optString("price", "-");
            currency = fill.optString("commissionAsset", "-");
        }

        // Convert timestamp to human-readable date/time
        LocalDateTime dateTimeTransact = Instant.ofEpochMilli(timestamp)
                .atOffset(ZoneOffset.UTC)
                .toLocalDateTime();
        String formattedDate = dateTimeTransact.format(DateTimeFormatter.ofPattern("HH:mm:ss yyyy-MM-dd"));

        LOGGER.info("---------------------------------------------");
        LOGGER.info("ORDEM ENVIADA:");
        LOGGER.info(" | Status: {}", getOrderStatus(status));
        LOGGER.info(" | Side: {}", side);
        LOGGER.info(" | Ativo: {}", asset);
        LOGGER.info(" | Quantidade: {}", quantity);
        LOGGER.info(" | Valor na venda: {}", pricePerUnit);
        LOGGER.info(" | Moeda: {}", currency);
        LOGGER.info(" | Valor em {}: {}", currency, totalValue);
        LOGGER.info(" | Type: {}", type);
        LOGGER.info(" | Data/Hora: {}", formattedDate);
        LOGGER.info(" | Complete_order: {}", order);
        LOGGER.info("---------------------------------------------");
    }

    private static String getOrderStatus(String status) {
        return switch (status) {
            case "NEW" -> "Order created";
            case "PENDING_NEW" -> "Pending order";
            case "PARTIALLY_FILLED" -> "Order partially completed";
            case "FILLED" -> "Completed";
            case "CANCELED" -> "Canceled by user";
            case "REJECTED" -> "Order rejected by client";
            case "EXPIRED" -> "Order canceled by client";
            case "EXPIRED_IN_MATCH" -> "Order expired";
            default -> "Unknown";
        };
    }

}
