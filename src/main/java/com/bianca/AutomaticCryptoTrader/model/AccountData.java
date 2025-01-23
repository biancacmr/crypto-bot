package com.bianca.AutomaticCryptoTrader.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class AccountData {
    private Integer makerComission;
    private Integer takerCommission;
    private Integer buyerCommission;
    private Integer sellerCommission;
    private Boolean canTrade;
    private Boolean canWithdraw;
    private Boolean canDeposit;
    private Boolean brokered;
    private Boolean requireSelfTradePrevention;
    private Boolean preventSor;
    private double updateTime;
    private String accountType;
    private ArrayList<Balance> balances;
    private ArrayList<String> permissions;
    private Integer uid;

    public AccountData() {

    }

    public void updateDataFromJSONObject(JSONObject response) {
        this.makerComission = response.getInt("makerCommission");
        this.takerCommission = response.getInt("takerCommission");
        this.buyerCommission = response.getInt("buyerCommission");
        this.sellerCommission = response.getInt("sellerCommission");

        this.canTrade = response.getBoolean("canTrade");
        this.canWithdraw = response.getBoolean("canWithdraw");
        this.canDeposit = response.getBoolean("canDeposit");
        this.brokered = response.getBoolean("brokered");
        this.requireSelfTradePrevention = response.getBoolean("requireSelfTradePrevention");
        this.preventSor = response.getBoolean("preventSor");

        this.updateTime = response.getDouble("updateTime");
        this.accountType = response.getString("accountType");

        this.balances = getBalancesList(response.getJSONArray("balances"));
        this.permissions = jsonArrayToList(response.getJSONArray("permissions"));

        this.uid = response.getInt("uid");
    }

    private ArrayList<Balance> getBalancesList(JSONArray balances) {
        ArrayList<Balance> list = new ArrayList<>();
        for (int i = 0; i < balances.length(); i++) {
            JSONObject rawBalanceObj = balances.getJSONObject(i);
            Balance balance = new Balance(
                    rawBalanceObj.getString("asset"),
                    Double.parseDouble(rawBalanceObj.getString("free")),
                    Double.parseDouble(rawBalanceObj.getString("locked"))
            );
            list.add(balance);
        }
        return list;
    }

    // Helper method to convert JSONArray to ArrayList
    public static ArrayList<String> jsonArrayToList(JSONArray jsonArray) {
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            list.add(jsonArray.getString(i));
        }
        return list;
    }

    public Integer getMakerComission() {
        return makerComission;
    }

    public Integer getTakerCommission() {
        return takerCommission;
    }

    public Integer getBuyerCommission() {
        return buyerCommission;
    }

    public Integer getSellerCommission() {
        return sellerCommission;
    }

    public Boolean getCanDeposit() {
        return canDeposit;
    }

    public Boolean getBrokered() {
        return brokered;
    }

    public Boolean getCanTrade() {
        return canTrade;
    }

    public Boolean getCanWithdraw() {
        return canWithdraw;
    }

    public Boolean getRequireSelfTradePrevention() {
        return requireSelfTradePrevention;
    }

    public Boolean getPreventSor() {
        return preventSor;
    }

    public String getAccountType() {
        return accountType;
    }

    public ArrayList<Balance> getBalances() {
        return balances;
    }

    public double getUpdateTime() {
        return updateTime;
    }

    public ArrayList<String> getPermissions() {
        return permissions;
    }

    public Integer getUid() {
        return uid;
    }

    @Override
    public String toString() {
        return "AccountData{" +
                "makerComission=" + makerComission +
                ", takerCommission=" + takerCommission +
                ", buyerCommission=" + buyerCommission +
                ", sellerCommission=" + sellerCommission +
                ", canTrade=" + canTrade +
                ", canWithdraw=" + canWithdraw +
                ", canDeposit=" + canDeposit +
                ", brokered=" + brokered +
                ", requireSelfTradePrevention=" + requireSelfTradePrevention +
                ", preventSor=" + preventSor +
                ", updateTime=" + updateTime +
                ", accountType=" + accountType +
                ", balances=" + balances +
                ", permissions=" + permissions +
                ", uid=" + uid +
                '}';
    }
}
