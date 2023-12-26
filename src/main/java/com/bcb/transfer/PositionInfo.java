package com.bcb.transfer;

import com.google.gson.annotations.SerializedName;

public class PositionInfo {
    private String symbol;
    @SerializedName("positionAmt")
    private Double positionAmount;
    private Double entryPrice;
    private Double breakEvenPrice;
    private Double markPrice;
    private Double unRealizedProfit;
    private Double liquidationPrice;
    private short leverage;
    private String maxNotionalValue;
    private String marginType;
    private Double isolatedMargin;
    private boolean isAutoAddMargin;
    private String positionSide;
    private String notional;
    private String isolatedWallet;
    private long updateTime;
    private boolean isolated;
    private int adlQuantile;

        public String getSymbol () {
            return symbol;
        }

        public void setSymbol (String symbol){
            this.symbol = symbol;
        }

    public Double getPositionAmount() {
        return positionAmount;
    }

    public void setPositionAmount(Double positionAmount) {
        this.positionAmount = positionAmount;
    }

    public Double getEntryPrice() {
        return entryPrice;
    }

    public void setEntryPrice(Double entryPrice) {
        this.entryPrice = entryPrice;
    }

    public Double getBreakEvenPrice() {
        return breakEvenPrice;
    }

    public void setBreakEvenPrice(Double breakEvenPrice) {
        this.breakEvenPrice = breakEvenPrice;
    }

    public Double getMarkPrice() {
        return markPrice;
    }

    public void setMarkPrice(Double markPrice) {
        this.markPrice = markPrice;
    }

    public Double getUnRealizedProfit() {
        return unRealizedProfit;
    }

    public void setUnRealizedProfit(Double unRealizedProfit) {
        this.unRealizedProfit = unRealizedProfit;
    }

    public Double getLiquidationPrice() {
        return liquidationPrice;
    }

    public void setLiquidationPrice(Double liquidationPrice) {
        this.liquidationPrice = liquidationPrice;
    }

    public short getLeverage() {
        return leverage;
    }

    public void setLeverage(short leverage) {
        this.leverage = leverage;
    }

    public String getMaxNotionalValue() {
        return maxNotionalValue;
    }

    public void setMaxNotionalValue(String maxNotionalValue) {
        this.maxNotionalValue = maxNotionalValue;
    }

    public String getMarginType() {
        return marginType;
    }

    public void setMarginType(String marginType) {
        this.marginType = marginType;
    }

    public Double getIsolatedMargin() {
        return isolatedMargin;
    }

    public void setIsolatedMargin(Double isolatedMargin) {
        this.isolatedMargin = isolatedMargin;
    }

    public boolean isAutoAddMargin() {
        return isAutoAddMargin;
    }

    public void setAutoAddMargin(boolean autoAddMargin) {
        isAutoAddMargin = autoAddMargin;
    }

    public String getPositionSide() {
        return positionSide;
    }

    public void setPositionSide(String positionSide) {
        this.positionSide = positionSide;
    }

    public String getNotional() {
        return notional;
    }

    public void setNotional(String notional) {
        this.notional = notional;
    }

    public String getIsolatedWallet() {
        return isolatedWallet;
    }

    public void setIsolatedWallet(String isolatedWallet) {
        this.isolatedWallet = isolatedWallet;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public boolean isIsolated() {
        return isolated;
    }

    public void setIsolated(boolean isolated) {
        this.isolated = isolated;
    }

    public int getAdlQuantile() {
        return adlQuantile;
    }

    public void setAdlQuantile(int adlQuantile) {
        this.adlQuantile = adlQuantile;
    }

    @Override
    public String toString() {
        return "PositionInfo{" +
                "symbol='" + symbol + '\'' +
                ", positionAmount='" + positionAmount + '\'' +
                ", entryPrice='" + entryPrice + '\'' +
                ", breakEvenPrice='" + breakEvenPrice + '\'' +
                ", markPrice='" + markPrice + '\'' +
                ", unRealizedProfit='" + unRealizedProfit + '\'' +
                ", liquidationPrice='" + liquidationPrice + '\'' +
                ", leverage='" + leverage + '\'' +
                ", maxNotionalValue='" + maxNotionalValue + '\'' +
                ", marginType='" + marginType + '\'' +
                ", isolatedMargin='" + isolatedMargin + '\'' +
                ", isAutoAddMargin=" + isAutoAddMargin +
                ", positionSide='" + positionSide + '\'' +
                ", notional='" + notional + '\'' +
                ", isolatedWallet='" + isolatedWallet + '\'' +
                ", updateTime=" + updateTime +
                ", isolated=" + isolated +
                ", adlQuantile=" + adlQuantile +
                '}';
    }
}