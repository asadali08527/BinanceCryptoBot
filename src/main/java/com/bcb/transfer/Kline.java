package com.bcb.transfer;

import com.google.gson.annotations.SerializedName;

public class Kline {

    @SerializedName("openTime")
    private long openTime;

    @SerializedName("openPrice")
    private String openPrice;

    @SerializedName("highPrice")
    private String highPrice;

    @SerializedName("lowPrice")
    private String lowPrice;

    @SerializedName("closePrice")
    private String closePrice;

    @SerializedName("volume")
    private String volume;

    @SerializedName("closeTime")
    private long closeTime;

    @SerializedName("quoteAssetVolume")
    private String quoteAssetVolume;

    @SerializedName("numberOfTrades")
    private int numberOfTrades;

    @SerializedName("takerBuyBaseAssetVolume")
    private String takerBuyBaseAssetVolume;

    @SerializedName("takerBuyQuoteAssetVolume")
    private String takerBuyQuoteAssetVolume;

    // Unused field, ignored.
    private String unusedField;

    // Getters and setters

    public long getOpenTime() {
        return openTime;
    }

    public void setOpenTime(long openTime) {
        this.openTime = openTime;
    }

    public String getOpenPrice() {
        return openPrice;
    }

    public void setOpenPrice(String openPrice) {
        this.openPrice = openPrice;
    }

    public String getHighPrice() {
        return highPrice;
    }

    public void setHighPrice(String highPrice) {
        this.highPrice = highPrice;
    }

    public String getLowPrice() {
        return lowPrice;
    }

    public void setLowPrice(String lowPrice) {
        this.lowPrice = lowPrice;
    }

    public String getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(String closePrice) {
        this.closePrice = closePrice;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public long getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(long closeTime) {
        this.closeTime = closeTime;
    }

    public String getQuoteAssetVolume() {
        return quoteAssetVolume;
    }

    public void setQuoteAssetVolume(String quoteAssetVolume) {
        this.quoteAssetVolume = quoteAssetVolume;
    }

    public int getNumberOfTrades() {
        return numberOfTrades;
    }

    public void setNumberOfTrades(int numberOfTrades) {
        this.numberOfTrades = numberOfTrades;
    }

    public String getTakerBuyBaseAssetVolume() {
        return takerBuyBaseAssetVolume;
    }

    public void setTakerBuyBaseAssetVolume(String takerBuyBaseAssetVolume) {
        this.takerBuyBaseAssetVolume = takerBuyBaseAssetVolume;
    }

    public String getTakerBuyQuoteAssetVolume() {
        return takerBuyQuoteAssetVolume;
    }

    public void setTakerBuyQuoteAssetVolume(String takerBuyQuoteAssetVolume) {
        this.takerBuyQuoteAssetVolume = takerBuyQuoteAssetVolume;
    }

    public String getUnusedField() {
        return unusedField;
    }

    public void setUnusedField(String unusedField) {
        this.unusedField = unusedField;
    }

    @Override
    public String toString() {
        return "KlineData{" +
                "openTime=" + openTime +
                ", openPrice='" + openPrice + '\'' +
                ", highPrice='" + highPrice + '\'' +
                ", lowPrice='" + lowPrice + '\'' +
                ", closePrice='" + closePrice + '\'' +
                ", volume='" + volume + '\'' +
                ", closeTime=" + closeTime +
                ", quoteAssetVolume='" + quoteAssetVolume + '\'' +
                ", numberOfTrades=" + numberOfTrades +
                ", takerBuyBaseAssetVolume='" + takerBuyBaseAssetVolume + '\'' +
                ", takerBuyQuoteAssetVolume='" + takerBuyQuoteAssetVolume + '\'' +
                ", unusedField='" + unusedField + '\'' +
                '}';
    }

    public Kline(long openTime, String openPrice, String highPrice, String lowPrice, String closePrice, String volume, long closeTime, String quoteAssetVolume, int numberOfTrades, String takerBuyBaseAssetVolume, String takerBuyQuoteAssetVolume, String unusedField) {
        this.openTime = openTime;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
        this.closeTime = closeTime;
        this.quoteAssetVolume = quoteAssetVolume;
        this.numberOfTrades = numberOfTrades;
        this.takerBuyBaseAssetVolume = takerBuyBaseAssetVolume;
        this.takerBuyQuoteAssetVolume = takerBuyQuoteAssetVolume;
        this.unusedField = unusedField;
    }
}
