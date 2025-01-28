package com.bianca.AutomaticCryptoTrader.model;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.json.JSONArray;

public class OrderResponseFull {
    private String symbol;
    private long orderId;
    private long orderListId;
    private String clientOrderId;
    private long transactTime;
    private String price;
    private String origQty;
    private String executedQty;
    private String cumulativeQuoteQty;
    private String status;
    private String timeInForce;
    private String type;
    private String side;
    private long strategyId;
    private long strategyType;
    private long workingTime;
    private String selfTradePreventionMode;
    private List<Fill> fills;

    public OrderResponseFull(JSONObject json) {
        this.symbol = (String) json.get("symbol");
        this.orderId = (long) json.get("orderId");
        this.orderListId = (long) json.get("orderListId");
        this.clientOrderId = (String) json.get("clientOrderId");
        this.transactTime = (long) json.get("transactTime");
        this.price = (String) json.get("price");
        this.origQty = (String) json.get("origQty");
        this.executedQty = (String) json.get("executedQty");
        this.cumulativeQuoteQty = (String) json.get("cumulativeQuoteQty");
        this.status = (String) json.get("status");
        this.timeInForce = (String) json.get("timeInForce");
        this.type = (String) json.get("type");
        this.side = (String) json.get("side");
        this.strategyId = (long) json.get("strategyId");
        this.strategyType = (long) json.get("strategyType");
        this.workingTime = (long) json.get("workingTime");
        this.selfTradePreventionMode = (String) json.get("selfTradePreventionMode");

        // Parse "fills" array
        JSONArray fillsArray = (JSONArray) json.get("fills");
        this.fills = new ArrayList<>();
        if (fillsArray != null) {
            for (Object obj : fillsArray) {
                JSONObject fillJson = (JSONObject) obj;
                this.fills.add(new Fill(fillJson));
            }
        }
    }

    // Classe interna para representar "fills"
    public static class Fill {
        private String price;
        private String qty;
        private String commission;
        private String commissionAsset;

        public Fill(JSONObject json) {
            this.price = (String) json.get("price");
            this.qty = (String) json.get("qty");
            this.commission = (String) json.get("commission");
            this.commissionAsset = (String) json.get("commissionAsset");
        }

        public String getPrice() {
            return price;
        }

        public void setPrice(String price) {
            this.price = price;
        }

        public String getQty() {
            return qty;
        }

        public void setQty(String qty) {
            this.qty = qty;
        }

        public String getCommission() {
            return commission;
        }

        public void setCommission(String commission) {
            this.commission = commission;
        }

        public String getCommissionAsset() {
            return commissionAsset;
        }

        public void setCommissionAsset(String commissionAsset) {
            this.commissionAsset = commissionAsset;
        }
    }

    // Getters e Setters para OrderResponseFull
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public long getOrderListId() {
        return orderListId;
    }

    public void setOrderListId(long orderListId) {
        this.orderListId = orderListId;
    }

    public String getClientOrderId() {
        return clientOrderId;
    }

    public void setClientOrderId(String clientOrderId) {
        this.clientOrderId = clientOrderId;
    }

    public long getTransactTime() {
        return transactTime;
    }

    public void setTransactTime(long transactTime) {
        this.transactTime = transactTime;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getOrigQty() {
        return origQty;
    }

    public void setOrigQty(String origQty) {
        this.origQty = origQty;
    }

    public String getExecutedQty() {
        return executedQty;
    }

    public void setExecutedQty(String executedQty) {
        this.executedQty = executedQty;
    }

    public String getCumulativeQuoteQty() {
        return cumulativeQuoteQty;
    }

    public void setCumulativeQuoteQty(String cumulativeQuoteQty) {
        this.cumulativeQuoteQty = cumulativeQuoteQty;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimeInForce() {
        return timeInForce;
    }

    public void setTimeInForce(String timeInForce) {
        this.timeInForce = timeInForce;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public long getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(long strategyId) {
        this.strategyId = strategyId;
    }

    public long getStrategyType() {
        return strategyType;
    }

    public void setStrategyType(long strategyType) {
        this.strategyType = strategyType;
    }

    public long getWorkingTime() {
        return workingTime;
    }

    public void setWorkingTime(long workingTime) {
        this.workingTime = workingTime;
    }

    public String getSelfTradePreventionMode() {
        return selfTradePreventionMode;
    }

    public void setSelfTradePreventionMode(String selfTradePreventionMode) {
        this.selfTradePreventionMode = selfTradePreventionMode;
    }

    public List<Fill> getFills() {
        return fills;
    }

    public void setFills(List<Fill> fills) {
        this.fills = fills;
    }
}
