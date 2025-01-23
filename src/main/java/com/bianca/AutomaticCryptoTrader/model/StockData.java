package com.bianca.AutomaticCryptoTrader.model;

import lombok.Getter;
import org.json.JSONArray;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Getter
public class StockData {
    private final Timestamp openTime;
    private final Double openPrice;
    private final Double highPrice;
    private final Double lowPrice;
    private final Double closePrice;
    private final Double volume;
    private final Timestamp closeTime;
    private final Double closeAssetVolume;
    private final Long numberOfTrades;
    private final Double takerBuyBaseAssetVolume;
    private final Double takerBuyQuoteAssetVolume;
    private final Double unknown;

    public StockData(JSONArray data) {
        int i = 0;
        this.openTime = convertToUtcMinus3(new Timestamp(data.getLong(i++)));
        this.openPrice = data.getDouble(i++);
        this.highPrice = data.getDouble(i++);
        this.lowPrice = data.getDouble(i++);
        this.closePrice = data.getDouble(i++);
        this.volume = data.getDouble(i++);
        this.closeTime = convertToUtcMinus3(new Timestamp(data.getLong(i++)));
        this.closeAssetVolume = data.getDouble(i++);
        this.numberOfTrades = data.getLong(i++);
        this.takerBuyBaseAssetVolume = data.getDouble(i++);
        this.takerBuyQuoteAssetVolume = data.getDouble(i++);
        this.unknown = data.getDouble(i++);
    }

    private Timestamp convertToUtcMinus3(Timestamp timestamp) {
        Instant instant = timestamp.toInstant();
        ZonedDateTime utcMinus3 = instant.atZone(ZoneId.of("UTC-3"));
        LocalDateTime localDateTime = utcMinus3.toLocalDateTime();
        return Timestamp.valueOf(localDateTime);
    }

    @Override
    public String toString() {
        return "StockData{" +
                "openTime=" + openTime +
                ", openPrice=" + openPrice +
                ", highPrice=" + highPrice +
                ", lowPrice=" + lowPrice +
                ", closePrice=" + closePrice +
                ", volume=" + volume +
                ", closeTime=" + closeTime +
                ", closeAssetVolume=" + closeAssetVolume +
                ", numberOfTrades=" + numberOfTrades +
                ", takerBuyBaseAssetVolume=" + takerBuyBaseAssetVolume +
                ", takerBuyQuoteAssetVolume=" + takerBuyQuoteAssetVolume +
                ", unknown=" + unknown +
                '}';
    }

    public Double getClosePrice() {
        return closePrice;
    }

    public Timestamp getCloseTime() {
        return closeTime;
    }
}
