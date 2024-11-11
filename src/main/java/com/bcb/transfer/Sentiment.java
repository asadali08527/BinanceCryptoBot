package com.bcb.transfer;

public class Sentiment {
    private String symbol;
    private String side;
    private String positionSide;
    private String type;
    private String timeInForce;
    private String quantity;
    private String reduceOnly;
    private String price;
    private String newClientOrderId;
    private String stopPrice;
    private String closePosition;
    private String activationPrice;
    private String callbackRate;
    private String workingType;
    private String priceProtect;
    private String newOrderRespType;
    private String priceMatch;
    private String selfTradePreventionMode;
    private String goodTillDate;
    private String recvWindow;
    private String strategy;
    private Double lastPrice;

    public Double getLastPrice() {
        return lastPrice;
    }

    public void setLastPrice(Double lastPrice) {
        this.lastPrice = lastPrice;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTimeInForce() {
        return timeInForce;
    }

    public void setTimeInForce(String timeInForce) {
        this.timeInForce = timeInForce;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getReduceOnly() {
        return reduceOnly;
    }

    public void setReduceOnly(String reduceOnly) {
        this.reduceOnly = reduceOnly;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getNewClientOrderId() {
        return newClientOrderId;
    }

    public void setNewClientOrderId(String newClientOrderId) {
        this.newClientOrderId = newClientOrderId;
    }

    public String getStopPrice() {
        return stopPrice;
    }

    public void setStopPrice(String stopPrice) {
        this.stopPrice = stopPrice;
    }

    public String getClosePosition() {
        return closePosition;
    }

    public void setClosePosition(String closePosition) {
        this.closePosition = closePosition;
    }

    public String getActivationPrice() {
        return activationPrice;
    }

    public void setActivationPrice(String activationPrice) {
        this.activationPrice = activationPrice;
    }

    public String getCallbackRate() {
        return callbackRate;
    }

    public void setCallbackRate(String callbackRate) {
        this.callbackRate = callbackRate;
    }

    public String getWorkingType() {
        return workingType;
    }

    public void setWorkingType(String workingType) {
        this.workingType = workingType;
    }

    public String getPriceProtect() {
        return priceProtect;
    }

    public void setPriceProtect(String priceProtect) {
        this.priceProtect = priceProtect;
    }

    public String getNewOrderRespType() {
        return newOrderRespType;
    }

    public void setNewOrderRespType(String newOrderRespType) {
        this.newOrderRespType = newOrderRespType;
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

    public String getGoodTillDate() {
        return goodTillDate;
    }

    public void setGoodTillDate(String goodTillDate) {
        this.goodTillDate = goodTillDate;
    }

    public String getRecvWindow() {
        return recvWindow;
    }

    public void setRecvWindow(String recvWindow) {
        this.recvWindow = recvWindow;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return "Sentiment{" +
                "symbol='" + symbol + '\'' +
                ", side='" + side + '\'' +
                ", positionSide='" + positionSide + '\'' +
                ", type='" + type + '\'' +
                ", timeInForce='" + timeInForce + '\'' +
                ", quantity='" + quantity + '\'' +
                ", reduceOnly='" + reduceOnly + '\'' +
                ", price='" + price + '\'' +
                ", newClientOrderId='" + newClientOrderId + '\'' +
                ", stopPrice='" + stopPrice + '\'' +
                ", closePosition='" + closePosition + '\'' +
                ", activationPrice='" + activationPrice + '\'' +
                ", callbackRate='" + callbackRate + '\'' +
                ", workingType='" + workingType + '\'' +
                ", priceProtect='" + priceProtect + '\'' +
                ", newOrderRespType='" + newOrderRespType + '\'' +
                ", priceMatch='" + priceMatch + '\'' +
                ", selfTradePreventionMode='" + selfTradePreventionMode + '\'' +
                ", goodTillDate='" + goodTillDate + '\'' +
                ", recvWindow='" + recvWindow + '\'' +
                ", strategy='" + strategy + '\'' +
                ", lastPrice=" + lastPrice +
                '}';
    }
}
