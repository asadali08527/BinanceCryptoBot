package com.bcb.service;

import com.bcb.trade.constants.Coins;
import com.bcb.trade.sentiment.MarketSentimentAnalyzer;
import com.bcb.transfer.TickerInfo;
import java.util.Map;

public class MarketService {

    public Map<String, TickerInfo> getTickers(String[] symbols) {
        return MarketSentimentAnalyzer.getTickers(Coins.ALT,symbols);
    }

    public int calculateMarketMovement(Map<String, TickerInfo> tickerMap, String movementType) {
        return MarketSentimentAnalyzer.marketMovement(tickerMap, movementType);
    }
}