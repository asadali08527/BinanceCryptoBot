package com.bcb.trade.sentiment;

import com.bcb.client.SpotClient;
import com.bcb.config.PrivateConfig;
import com.bcb.enums.MarketType;
import com.bcb.enums.TimeInterval;
import com.bcb.impl.SpotClientImpl;
import com.bcb.impl.spot.Market;
import com.bcb.orders.HistoricalDataFetcher;
import com.bcb.transfer.Sentiment;
import com.bcb.transfer.TickerInfo;
import com.bcb.trade.constants.Coins;
import com.bcb.trade.util.*;
import com.google.gson.Gson;

import java.util.*;
import java.util.stream.Collectors;

public class MarketSentimentAnalyzer {

    static SpotClient client = new SpotClientImpl(PrivateConfig.TAA_API_KEY, PrivateConfig.TAA_SECRET_KEY);


    public static Sentiment getSentiment(String coin, MarketType type, Map<String,TickerInfo> tickerMap) {
        TickerInfo tickerInfo = tickerMap.get(coin);
        Map<String, List<TickerInfo>> marketMovement = tickerMap.entrySet().stream()
                .collect(Collectors.groupingBy(entry ->
                                entry.getValue().getPriceChangePercent() < 0 && Math.abs(entry.getValue().getPriceChangePercent()) >= Coins.PRICE_CHANGE_PERCENTAGE_THRESHOLD ? "DOWN" : "UP",
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        int upCount = marketMovement.getOrDefault("UP",Collections.EMPTY_LIST).size();
        int downCount = marketMovement.getOrDefault("DOWN", Collections.EMPTY_LIST).size();
        double longTermMA = calculateMovingAverage(coin, Coins.LONG_TERM_PERIOD);
        double shortTermMA = calculateMovingAverage(coin, Coins.SHORT_TERM_PERIOD);
        System.out.println("longTermMA: "+longTermMA+" | UpCount: "+upCount);
        System.out.println("shortTermMA: "+shortTermMA+" | DownCount: "+downCount);
        Sentiment sentiment = new Sentiment();
        if(type==null)
            sentiment.setType(MarketType.LIMIT.toString());
        else
            sentiment.setType(type.toString());

        // Smart Entry Strategy
        if (upCount > downCount) {
            if (shortTermMA > longTermMA && (tickerInfo.getPriceChangePercent() > 0 && tickerInfo.getPriceChangePercent() < Coins.PRICE_CHANGE_PERCENTAGE_THRESHOLD)) {
                sentiment.setSide(Coins.BUY_SIDE);
            } else if (upCount > downCount * 4) {
                if (shortTermMA < longTermMA && tickerInfo.getPriceChangePercent() > 0) {
                    sentiment.setSide(Coins.BUY_SIDE);
                } else if (tickerInfo.getPriceChangePercent() > 0) {
                    sentiment.setSide(Coins.BUY_SIDE);
                } else if(upCount > downCount * 10){
                    sentiment.setSide(Coins.BUY_SIDE);
                }
            } else {
                sentiment.setSide(Coins.HOLD_SIDE);
            }
        } else if (upCount < downCount) {
            if (shortTermMA < longTermMA) {
                if (tickerInfo.getPriceChangePercent() < -Coins.PRICE_CHANGE_PERCENTAGE_THRESHOLD_10) {
                    sentiment.setSide(Coins.BUY_SIDE);
                } else if (upCount * 4 < downCount && tickerInfo.getPriceChangePercent() < 0 && tickerInfo.getPriceChangePercent() > -Coins.PRICE_CHANGE_PERCENTAGE_THRESHOLD) {
                    sentiment.setSide(Coins.SELL_SIDE);
                } else {
                    sentiment.setSide(Coins.HOLD_SIDE);
                }
            } else {
                sentiment.setSide(Coins.HOLD_SIDE);
            }
        } else {
            sentiment.setSide(Coins.HOLD_SIDE);
        }
        if(MarketType.LIMIT==type ){
            if(Coins.BUY_SIDE.equalsIgnoreCase(sentiment.getSide()))
                sentiment.setPrice(String.valueOf(tickerInfo.getLowPrice()));
            else
                sentiment.setPrice(String.valueOf(tickerInfo.getLastPrice()));
        }else{
            sentiment.setPrice(String.valueOf(tickerInfo.getLastPrice()));
        }
        sentiment.setTimeInForce(Coins.TIME_IN_FORCE);
        sentiment.setSymbol(coin);
        sentiment.setClosePosition("false");
        sentiment.setNewOrderRespType("ACK");
        System.out.println("Ticker Info: "+tickerInfo);
        System.out.println("Sentiment: "+sentiment);
        return sentiment;
    }


    private static double calculateMovingAverage(String coin, int period) {
        // Fetch historical price data for the specified coin
        List<Double> historicalPrices = getHistoricalPrices(coin, period);
        // Calculate the simple moving average
        double sum = 0.0;
        for (Double price : historicalPrices) {
            sum += price;
        }
        return sum / historicalPrices.size();
    }

    /** Implement logic to fetch historical price data from Binance API
        Use the specified period to get the required number of data points
       Return a list of historical prices*/
    private static List<Double> getHistoricalPrices(String coin, int period) {
        TimeInterval timeInterval =  period==Coins.SHORT_TERM_PERIOD ? TimeInterval.TEN_MINUTES:TimeInterval.ONE_HOUR;
        return HistoricalDataFetcher.getHistoricalPrices(coin,timeInterval).stream().map(m->Double.valueOf(m)).collect(Collectors.toList());
    }

    private static TickerInfo getTicker(String symbol) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        Market market = client.createMarket();
        parameters.put("symbol", symbol);
        String result = market.ticker(parameters);
        //System.out.println(result);
        Gson gson = new Gson();
        return gson.fromJson(result, TickerInfo.class);
    }

    public static Map<String,TickerInfo> getTickers() {
        Map<String, Object> parameters = new LinkedHashMap<>();
        Market market = client.createMarket();
        String[] allFutureCoins = Coins.FUTURE_USDT_COINS_IN_ACTION;
        int batchSize = 99;
        Map<String,TickerInfo> tickerInfoMap = new HashMap<>();

        // Iterate through batches
        for (int startIndex = 0; startIndex < allFutureCoins.length; startIndex += batchSize+1) {
            int endIndex = Math.min(startIndex + batchSize, allFutureCoins.length);
            if(endIndex==allFutureCoins.length)
                endIndex-=1;
            //System.out.println("Processing batch: " + startIndex + " to " + endIndex);

            // Get symbols for the current batch
            ArrayList<String> symbols = new ArrayList<>(Arrays.asList(getCoinSubset(allFutureCoins, startIndex, endIndex)));
            parameters.put("symbols", symbols);

            // Fetch ticker info for the current batch
            String result = market.ticker(parameters);
            //System.out.println(result);

            // Parse JSON into TickerInfo array
            // Parse JSON into TickerInfo array
            TickerInfo[] tickerInfoArray = new Gson().fromJson(result, TickerInfo[].class);

            // Convert TickerInfo array to Map and add to the existing map
            Map<String, TickerInfo> batchMap = Arrays.stream(tickerInfoArray)
                    .collect(Collectors.toMap(TickerInfo::getSymbol, tickerInfo -> tickerInfo));

            tickerInfoMap.putAll(batchMap);
        }
        return tickerInfoMap;
    }

    private static String[] getCoinSubset(String[] allCoins, int startIndex, int endIndex) {
        // Check if indices are within bounds
        if (startIndex < 0 || endIndex >= allCoins.length || startIndex > endIndex) {
            throw new IllegalArgumentException("Invalid start or end indices");
        }

        // Calculate the size of the subset
        int subsetSize = endIndex - startIndex + 1;

        // Create a new array for the subset
        String[] subset = new String[subsetSize];

        // Copy elements from the original array to the subset
        System.arraycopy(allCoins, startIndex, subset, 0, subsetSize);

        return subset;
    }

    public static void main(String[] args) {
        getSentiment("DOGEUSDT",MarketType.LIMIT,getTickers());
    }
}
