package com.bianca.AutomaticCryptoTrader.model;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.json.JSONArray;

public class OrderResponseFull {
    private String symbol;
    private int orderId;
    private int orderListId;
    private String clientOrderId;
    private int transactTime;
    private String price;
    private String origQty;
    private String executedQty;
    private String cumulativeQuoteQty;
    private String status;
    private String timeInForce;
    private String type;
    private String side;
    private int strategyId;
    private int strategyType;
    private int workingTime;
    private String selfTradePreventionMode;
    private List<Fill> fills;

    public OrderResponseFull(JSONObject json) {
        this.symbol = json.getString("symbol");
        this.orderId = json.getInt("orderId");
        this.orderListId = json.getInt("orderListId");
        this.clientOrderId = json.getString("clientOrderId");
        this.transactTime = json.getInt("transactTime");
        this.price = json.getString("price");
        this.origQty = json.getString("origQty");
        this.executedQty = json.getString("executedQty");
        this.cumulativeQuoteQty = json.getString("cummulativeQuoteQty");
        this.status = json.getString("status");
        this.timeInForce = json.getString("timeInForce");
        this.type = json.getString("type");
        this.side = json.getString("side");
        this.strategyId = json.optInt("strategyId", 0);  // 0 é o valor padrão caso não exista
        this.strategyType = json.optInt("strategyType", 0);  // 0 é o valor padrão caso não exista
        this.workingTime = json.getInt("workingTime");
        this.selfTradePreventionMode = json.getString("selfTradePreventionMode");

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
            this.price = json.getString("price");
            this.qty = json.getString("qty");
            this.commission = json.getString("commission");
            this.commissionAsset = json.getString("commissionAsset");
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

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getOrderListId() {
        return orderListId;
    }

    public void setOrderListId(int orderListId) {
        this.orderListId = orderListId;
    }

    public String getClientOrderId() {
        return clientOrderId;
    }

    public void setClientOrderId(String clientOrderId) {
        this.clientOrderId = clientOrderId;
    }

    public int getTransactTime() {
        return transactTime;
    }

    public void setTransactTime(int transactTime) {
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

    public int getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(int strategyId) {
        this.strategyId = strategyId;
    }

    public int getStrategyType() {
        return strategyType;
    }

    public void setStrategyType(int strategyType) {
        this.strategyType = strategyType;
    }

    public int getWorkingTime() {
        return workingTime;
    }

    public void setWorkingTime(int workingTime) {
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
