package com.bianca.AutomaticCryptoTrader.model;

import lombok.Getter;

@Getter
public class Balance {
    private String asset;
    private Double free;
    private Double locked;

    public Double getFree() {
        return free;
    }

    public String getAsset() {
        return asset;
    }

    public Double getLocked() {
        return locked;
    }

    public Balance(String asset, Double free, Double locked) {
        this.free = free;
        this.asset = asset;
        this.locked = locked;
    }

    @Override
    public String toString() {
        return "Balance{" +
                "asset='" + asset + '\'' +
                ", free=" + free +
                ", locked=" + locked +
                '}';
    }
}
