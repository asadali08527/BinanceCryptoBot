package com.bcb.transfer;

import java.util.List;

public class ExchangeInfos {
    private List<SymbolInf> symbols;

    public List<SymbolInf> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<SymbolInf> symbols) {
        this.symbols = symbols;
    }

    @Override
    public String toString() {
        return "ExchangeInfo{" +
                symbols +
                '}';
    }
}

class SymbolInfo {
    private String symbol;
    private String status;
    private String baseAsset;
    private int baseAssetPrecision;
    private String quoteAsset;
    private int quotePrecision;
    private int quoteAssetPrecision;
    private int baseCommissionPrecision;
    private int quoteCommissionPrecision;
    private List<String> orderTypes;
    private boolean icebergAllowed;
    private boolean ocoAllowed;
    private boolean quoteOrderQtyMarketAllowed;
    private boolean allowTrailingStop;
    private boolean cancelReplaceAllowed;
    private boolean isSpotTradingAllowed;
    private boolean isMarginTradingAllowed;
    private List<FilterInfo> filters;
    private List<String> permissions;
    private String defaultSelfTradePreventionMode;
    private List<String> allowedSelfTradePreventionModes;

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

    public String getBaseAsset() {
        return baseAsset;
    }

    public void setBaseAsset(String baseAsset) {
        this.baseAsset = baseAsset;
    }

    public int getBaseAssetPrecision() {
        return baseAssetPrecision;
    }

    public void setBaseAssetPrecision(int baseAssetPrecision) {
        this.baseAssetPrecision = baseAssetPrecision;
    }

    public String getQuoteAsset() {
        return quoteAsset;
    }

    public void setQuoteAsset(String quoteAsset) {
        this.quoteAsset = quoteAsset;
    }

    public int getQuotePrecision() {
        return quotePrecision;
    }

    public void setQuotePrecision(int quotePrecision) {
        this.quotePrecision = quotePrecision;
    }

    public int getQuoteAssetPrecision() {
        return quoteAssetPrecision;
    }

    public void setQuoteAssetPrecision(int quoteAssetPrecision) {
        this.quoteAssetPrecision = quoteAssetPrecision;
    }

    public int getBaseCommissionPrecision() {
        return baseCommissionPrecision;
    }

    public void setBaseCommissionPrecision(int baseCommissionPrecision) {
        this.baseCommissionPrecision = baseCommissionPrecision;
    }

    public int getQuoteCommissionPrecision() {
        return quoteCommissionPrecision;
    }

    public void setQuoteCommissionPrecision(int quoteCommissionPrecision) {
        this.quoteCommissionPrecision = quoteCommissionPrecision;
    }

    public List<String> getOrderTypes() {
        return orderTypes;
    }

    public void setOrderTypes(List<String> orderTypes) {
        this.orderTypes = orderTypes;
    }

    public boolean isIcebergAllowed() {
        return icebergAllowed;
    }

    public void setIcebergAllowed(boolean icebergAllowed) {
        this.icebergAllowed = icebergAllowed;
    }

    public boolean isOcoAllowed() {
        return ocoAllowed;
    }

    public void setOcoAllowed(boolean ocoAllowed) {
        this.ocoAllowed = ocoAllowed;
    }

    public boolean isQuoteOrderQtyMarketAllowed() {
        return quoteOrderQtyMarketAllowed;
    }

    public void setQuoteOrderQtyMarketAllowed(boolean quoteOrderQtyMarketAllowed) {
        this.quoteOrderQtyMarketAllowed = quoteOrderQtyMarketAllowed;
    }

    public boolean isAllowTrailingStop() {
        return allowTrailingStop;
    }

    public void setAllowTrailingStop(boolean allowTrailingStop) {
        this.allowTrailingStop = allowTrailingStop;
    }

    public boolean isCancelReplaceAllowed() {
        return cancelReplaceAllowed;
    }

    public void setCancelReplaceAllowed(boolean cancelReplaceAllowed) {
        this.cancelReplaceAllowed = cancelReplaceAllowed;
    }

    public boolean isSpotTradingAllowed() {
        return isSpotTradingAllowed;
    }

    public void setSpotTradingAllowed(boolean spotTradingAllowed) {
        isSpotTradingAllowed = spotTradingAllowed;
    }

    public boolean isMarginTradingAllowed() {
        return isMarginTradingAllowed;
    }

    public void setMarginTradingAllowed(boolean marginTradingAllowed) {
        isMarginTradingAllowed = marginTradingAllowed;
    }

    public List<FilterInfo> getFilters() {
        return filters;
    }

    public void setFilters(List<FilterInfo> filters) {
        this.filters = filters;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public String getDefaultSelfTradePreventionMode() {
        return defaultSelfTradePreventionMode;
    }

    public void setDefaultSelfTradePreventionMode(String defaultSelfTradePreventionMode) {
        this.defaultSelfTradePreventionMode = defaultSelfTradePreventionMode;
    }

    public List<String> getAllowedSelfTradePreventionModes() {
        return allowedSelfTradePreventionModes;
    }

    public void setAllowedSelfTradePreventionModes(List<String> allowedSelfTradePreventionModes) {
        this.allowedSelfTradePreventionModes = allowedSelfTradePreventionModes;
    }

    @Override
    public String toString() {
        return "SymbolInfo{" +
                "symbol='" + symbol + '\'' +
                ", status='" + status + '\'' +
                ", baseAsset='" + baseAsset + '\'' +
                ", baseAssetPrecision=" + baseAssetPrecision +
                ", quoteAsset='" + quoteAsset + '\'' +
                ", quotePrecision=" + quotePrecision +
                ", quoteAssetPrecision=" + quoteAssetPrecision +
                ", baseCommissionPrecision=" + baseCommissionPrecision +
                ", quoteCommissionPrecision=" + quoteCommissionPrecision +
                ", orderTypes=" + orderTypes +
                ", icebergAllowed=" + icebergAllowed +
                ", ocoAllowed=" + ocoAllowed +
                ", quoteOrderQtyMarketAllowed=" + quoteOrderQtyMarketAllowed +
                ", allowTrailingStop=" + allowTrailingStop +
                ", cancelReplaceAllowed=" + cancelReplaceAllowed +
                ", isSpotTradingAllowed=" + isSpotTradingAllowed +
                ", isMarginTradingAllowed=" + isMarginTradingAllowed +
                ", filters=" + filters +
                ", permissions=" + permissions +
                ", defaultSelfTradePreventionMode='" + defaultSelfTradePreventionMode + '\'' +
                ", allowedSelfTradePreventionModes=" + allowedSelfTradePreventionModes +
                '}';
    }
}

class FilterInfo {
    private String filterType;
    private String minPrice;
    private String maxPrice;
    private String tickSize;
    private String minQty;
    private String maxQty;
    private String stepSize;
    private int limit;
    private int minTrailingAboveDelta;
    private int maxTrailingAboveDelta;
    private int minTrailingBelowDelta;
    private int maxTrailingBelowDelta;
    private String bidMultiplierUp;
    private String bidMultiplierDown;
    private String askMultiplierUp;
    private String askMultiplierDown;
    private int avgPriceMins;
    private String minNotional;
    private boolean applyMinToMarket;
    private String maxNotional;
    private boolean applyMaxToMarket;
    private int maxNumOrders;
    private int maxNumAlgoOrders;

    @Override
    public String toString() {
        return "FilterInfo{" +
                "filterType='" + filterType + '\'' +
                ", minPrice='" + minPrice + '\'' +
                ", maxPrice='" + maxPrice + '\'' +
                ", tickSize='" + tickSize + '\'' +
                ", minQty='" + minQty + '\'' +
                ", maxQty='" + maxQty + '\'' +
                ", stepSize='" + stepSize + '\'' +
                ", limit=" + limit +
                ", minTrailingAboveDelta=" + minTrailingAboveDelta +
                ", maxTrailingAboveDelta=" + maxTrailingAboveDelta +
                ", minTrailingBelowDelta=" + minTrailingBelowDelta +
                ", maxTrailingBelowDelta=" + maxTrailingBelowDelta +
                ", bidMultiplierUp='" + bidMultiplierUp + '\'' +
                ", bidMultiplierDown='" + bidMultiplierDown + '\'' +
                ", askMultiplierUp='" + askMultiplierUp + '\'' +
                ", askMultiplierDown='" + askMultiplierDown + '\'' +
                ", avgPriceMins=" + avgPriceMins +
                ", minNotional='" + minNotional + '\'' +
                ", applyMinToMarket=" + applyMinToMarket +
                ", maxNotional='" + maxNotional + '\'' +
                ", applyMaxToMarket=" + applyMaxToMarket +
                ", maxNumOrders=" + maxNumOrders +
                ", maxNumAlgoOrders=" + maxNumAlgoOrders +
                '}';
    }

}

