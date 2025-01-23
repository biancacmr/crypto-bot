package com.bianca.AutomaticCryptoTrader.model;

import lombok.Data;
import org.json.JSONObject;

@Data
public class Order {
    private String symbol;
    private long orderId;
    private long orderListId;
    private String clientOrderId;
    private String price;
    private String origQty;
    private String executedQty;
    private String cumulativeQuoteQty;
    private String status;
    private String timeInForce;
    private String type;
    private String side;
    private String stopPrice;
    private String icebergQty;
    private long time;
    private long updateTime;
    private boolean isWorking;
    private long workingTime;
    private String origQuoteOrderQty;
    private String selfTradePreventionMode;
    private int preventedMatchId;
    private String preventedQuantity;

    public Order(JSONObject data) {
        int i = 0;
        this.symbol = data.getString("symbol");
        this.orderId = data.getLong("orderId");
        this.orderListId = data.getLong("orderListId");
        this.clientOrderId = data.getString("clientOrderId");
        this.price = data.getString("price");
        this.origQty = data.getString("origQty");
        this.executedQty = data.getString("executedQty");
        this.cumulativeQuoteQty = data.getString("cummulativeQuoteQty");
        this.status = data.getString("status");
        this.timeInForce = data.getString("timeInForce");
        this.type = data.getString("type");
        this.side = data.getString("side");
        this.stopPrice = data.getString("stopPrice");
        this.icebergQty = data.getString("icebergQty");
        this.time = data.getLong("time");
        this.updateTime = data.getLong("updateTime");
        this.isWorking = data.getBoolean("isWorking");
        this.workingTime = data.getLong("workingTime");
        this.origQuoteOrderQty = data.getString("origQuoteOrderQty");
        this.selfTradePreventionMode = data.getString("selfTradePreventionMode");
    }

    public String getIcebergQty() {
        return icebergQty;
    }

    public long getTime() {
        return time;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public long getWorkingTime() {
        return workingTime;
    }

    public String getPreventedQuantity() {
        return preventedQuantity;
    }

    public int getPreventedMatchId() {
        return preventedMatchId;
    }

    public String getSelfTradePreventionMode() {
        return selfTradePreventionMode;
    }

    public String getOrigQuoteOrderQty() {
        return origQuoteOrderQty;
    }

    public boolean isWorking() {
        return isWorking;
    }

    public String getStopPrice() {
        return stopPrice;
    }

    public String getSide() {
        return side;
    }

    public String getType() {
        return type;
    }

    public String getTimeInForce() {
        return timeInForce;
    }

    public String getStatus() {
        return status;
    }

    public String getCumulativeQuoteQty() {
        return cumulativeQuoteQty;
    }

    public String getExecutedQty() {
        return executedQty;
    }

    public String getOrigQty() {
        return origQty;
    }

    public String getPrice() {
        return price;
    }

    public String getClientOrderId() {
        return clientOrderId;
    }

    public long getOrderListId() {
        return orderListId;
    }

    public long getOrderId() {
        return orderId;
    }

    public String getSymbol() {
        return symbol;
    }
}
