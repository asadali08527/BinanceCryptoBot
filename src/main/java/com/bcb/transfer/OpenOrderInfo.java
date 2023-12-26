package com.bcb.transfer;

import com.google.gson.annotations.SerializedName;

public class OpenOrderInfo {
    @SerializedName("orderId")
    private long orderId;

    @SerializedName("symbol")
    private String symbol;

    @SerializedName("status")
    private String status;

    @SerializedName("clientOrderId")
    private String clientOrderId;

    @SerializedName("price")
    private String price;

    @SerializedName("avgPrice")
    private String avgPrice;

    @SerializedName("origQty")
    private String origQty;

    @SerializedName("executedQty")
    private String executedQty;

    @SerializedName("cumQuote")
    private String cumQuote;

    @SerializedName("timeInForce")
    private String timeInForce;

    @SerializedName("type")
    private String type;

    @SerializedName("reduceOnly")
    private boolean reduceOnly;

    @SerializedName("closePosition")
    private boolean closePosition;

    @SerializedName("side")
    private String side;

    @SerializedName("positionSide")
    private String positionSide;

    @SerializedName("stopPrice")
    private String stopPrice;

    @SerializedName("workingType")
    private String workingType;

    @SerializedName("priceProtect")
    private boolean priceProtect;

    @SerializedName("origType")
    private String origType;

    @SerializedName("priceMatch")
    private String priceMatch;

    @SerializedName("selfTradePreventionMode")
    private String selfTradePreventionMode;

    @SerializedName("goodTillDate")
    private long goodTillDate;

    @SerializedName("time")
    private long time;

    @SerializedName("updateTime")
    private long updateTime;

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getClientOrderId() {
        return clientOrderId;
    }

    public void setClientOrderId(String clientOrderId) {
        this.clientOrderId = clientOrderId;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getAvgPrice() {
        return avgPrice;
    }

    public void setAvgPrice(String avgPrice) {
        this.avgPrice = avgPrice;
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

    public String getCumQuote() {
        return cumQuote;
    }

    public void setCumQuote(String cumQuote) {
        this.cumQuote = cumQuote;
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

    public boolean isReduceOnly() {
        return reduceOnly;
    }

    public void setReduceOnly(boolean reduceOnly) {
        this.reduceOnly = reduceOnly;
    }

    public boolean isClosePosition() {
        return closePosition;
    }

    public void setClosePosition(boolean closePosition) {
        this.closePosition = closePosition;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getPositionSide() {
        return positionSide;
    }

    public void setPositionSide(String positionSide) {
        this.positionSide = positionSide;
    }

    public String getStopPrice() {
        return stopPrice;
    }

    public void setStopPrice(String stopPrice) {
        this.stopPrice = stopPrice;
    }

    public String getWorkingType() {
        return workingType;
    }

    public void setWorkingType(String workingType) {
        this.workingType = workingType;
    }

    public boolean isPriceProtect() {
        return priceProtect;
    }

    public void setPriceProtect(boolean priceProtect) {
        this.priceProtect = priceProtect;
    }

    public String getOrigType() {
        return origType;
    }

    public void setOrigType(String origType) {
        this.origType = origType;
    }

    public String getPriceMatch() {
        return priceMatch;
    }

    public void setPriceMatch(String priceMatch) {
        this.priceMatch = priceMatch;
    }

    public String getSelfTradePreventionMode() {
        return selfTradePreventionMode;
    }

    public void setSelfTradePreventionMode(String selfTradePreventionMode) {
        this.selfTradePreventionMode = selfTradePreventionMode;
    }

    public long getGoodTillDate() {
        return goodTillDate;
    }

    public void setGoodTillDate(long goodTillDate) {
        this.goodTillDate = goodTillDate;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "OrderDto{" +
                "orderId=" + orderId +
                ", symbol='" + symbol + '\'' +
                ", status='" + status + '\'' +
                ", clientOrderId='" + clientOrderId + '\'' +
                ", price='" + price + '\'' +
                ", avgPrice='" + avgPrice + '\'' +
                ", origQty='" + origQty + '\'' +
                ", executedQty='" + executedQty + '\'' +
                ", cumQuote='" + cumQuote + '\'' +
                ", timeInForce='" + timeInForce + '\'' +
                ", type='" + type + '\'' +
                ", reduceOnly=" + reduceOnly +
                ", closePosition=" + closePosition +
                ", side='" + side + '\'' +
                ", positionSide='" + positionSide + '\'' +
                ", stopPrice='" + stopPrice + '\'' +
                ", workingType='" + workingType + '\'' +
                ", priceProtect=" + priceProtect +
                ", origType='" + origType + '\'' +
                ", priceMatch='" + priceMatch + '\'' +
                ", selfTradePreventionMode='" + selfTradePreventionMode + '\'' +
                ", goodTillDate=" + goodTillDate +
                ", time=" + time +
                ", updateTime=" + updateTime +
                '}';
    }
}

