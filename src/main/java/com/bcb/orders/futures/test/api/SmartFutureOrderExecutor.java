package com.bcb.orders.futures.test.api;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.bcb.client.SpotClient;
import com.bcb.config.PrivateConfig;
import com.bcb.enums.MarketType;
import com.bcb.enums.TimeInterval;
import com.bcb.exceptions.BinanceClientException;
import com.bcb.exceptions.BinanceConnectorException;
import com.bcb.impl.SpotClientImpl;
import com.bcb.impl.spot.Market;
import com.bcb.trade.constants.Coins;
import com.bcb.trade.sentiment.MarketSentimentAnalyzer;
import com.bcb.trade.util.CoinUtil;
import com.bcb.transfer.ErrorResponse;
import com.bcb.transfer.OpenOrderInfo;
import com.bcb.transfer.Order;
import com.bcb.transfer.PositionInfo;
import com.bcb.transfer.Sentiment;
import com.bcb.transfer.TickerInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class SmartFutureOrderExecutor {

    static Map<String, TickerInfo> tickerMap = null;
    private static List<String> errored = new ArrayList<>();
    private static List<String> processed = new ArrayList<>();
    static List<String> symbols = CoinUtil.getAllFutureCoinsByTypeAndCategory();
    static SpotClient client = new SpotClientImpl(PrivateConfig.TEE_API_KEY, PrivateConfig.TEE_SECRET_KEY);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static boolean keepeitherOpenOrderOrPosition = true;
    private static boolean pauseNewOrder = false;
    private static boolean openOrderExist = false;

    public static void main(String[] args) {
        scheduler.scheduleAtFixedRate(SmartFutureOrderExecutor::executeCronJob, 0, 5, TimeUnit.MINUTES);
    }

    public static List<String> getAllFutureCoins() {
        return Arrays.asList(Coins.FUTURE_USDT_COINS_FAVOURITE);
    }

    public static void executeCronJob() {
        pauseNewOrder = false;
        processed.clear();
        errored.clear();
        tickerMap = MarketSentimentAnalyzer.getTickers();
        Map<String, Object> parameters = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        Date startTime = new Date();
        System.out.println("*******************************************************************************************************************************************");
        System.out.println("Cron started at " + startTime);
        symbols.forEach(coin -> {
            openOrderExist= openOrderExist(coin);
            if(keepeitherOpenOrderOrPosition)
                if(openOrderExist)
                    return;
                parameters.put("symbol", coin);
                try {
                    processCronJobForSymbol(parameters, coin);
                } catch (BinanceConnectorException | BinanceClientException e) {
                    Gson gson = new Gson();
                    Type orderListType = new TypeToken<ErrorResponse>() {
                    }.getType();
                    ErrorResponse errorResponse = gson.fromJson(e.getMessage(), orderListType);
                    if (Coins.ERROR_CODE_1003.equalsIgnoreCase(String.valueOf(errorResponse.getCode()))) {
                        System.out.println("Pausing execution for next 5 minutes, Error: " + errorResponse);
                        printResult(symbols, errors, startTime,errored);
                        pauseNewOrder = true;
                    }
                    CoinUtil.handleException(errors, coin, parameters, e);
                } catch (Exception e) {
                    System.out.println("Exception: " + e.getMessage());
                } finally {
                    parameters.clear();
                }
        });
        printResult(symbols, errors, startTime,errored);

    }

    private static void printResult(List<String> symbols, List<String> errors, Date startTime, List<String> errored) {
        symbols.removeAll(errored);
        Date finishedTime = new Date();
        System.out.println("Errors: "+errors);
        System.out.println("Errored : "+errored);
        System.out.println("Processed : "+processed);
        System.out.println("Cron Finished at " + finishedTime);
        System.out.println("Total Time taken to Execute The Job : " + (finishedTime.getTime()-startTime.getTime())/(60.0 * 1000.0) + " minutes");
        System.out.println("*******************************************************************************************************************************************");

    }

    private static void processCronJobForSymbol(Map<String, Object> parameters, String coin)
            throws BinanceConnectorException, BinanceClientException {
        SpotClient client = new SpotClientImpl(PrivateConfig.TEE_API_KEY, PrivateConfig.TEE_SECRET_KEY, PrivateConfig.BASE_URLS[0]);
        String result = client.createFutures().getFuturesOpenPosition(parameters);
        parameters.clear();
        Gson gson = new Gson();
        Type orderListType = new TypeToken<List<PositionInfo>>() {}.getType();
        List<PositionInfo> positionInfos = gson.fromJson(result, orderListType);
        PositionInfo positionInfo = positionInfos.get(0);
        System.out.println("Current Position Info: "+positionInfo);
        parameters = updateParameters(parameters, coin);
        if(parameters == null)
            return;
        if (positionInfo.getPositionAmount() == 0.0 && !openOrderExist && !pauseNewOrder) {
            System.out.println("Creating Order for : "+parameters);
            createFuturePosition(parameters, 0);
        } else if (positionInfo.getPositionAmount() < 0.0) {
            System.out.println("Handling Existing Sell Order : "+positionInfo);
            handleNegativePosition(parameters, coin, positionInfo, client);
        } else if(positionInfo.getPositionAmount() > 0.0){
            System.out.println("Handling Existing Buy Order : "+positionInfo);
            handlePositivePosition(parameters, coin, positionInfo, client);
        }
    }
    private static Order createFuturePosition(Map<String, Object> parameters,  int retry) {
        Order order = null;
        try {
            SpotClient client = new SpotClientImpl(PrivateConfig.TEE_API_KEY, PrivateConfig.TEE_SECRET_KEY, PrivateConfig.BASE_URLS[0]);
            String result = client.createFutures().createFuturesPosition(parameters);
            Gson gson= new Gson();
            order = gson.fromJson(result, Order.class);
            System.out.println("Position Creation status for coin "+parameters+" Result: "+result);
            processed.add(String.valueOf(parameters.get("symbol")));
            return order;
        }catch (BinanceConnectorException e) {
            System.err.println((String) String.format("fullErrMessage: %s", e.getMessage()));
        } catch (BinanceClientException e) {
            System.err.println((String) String.format("fullErrMessage: %s \nerrMessage: %s \nerrCode: %d \nHTTPStatusCode: %d",
                    e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode()));
            if(Coins.ERROR_CODE_1111.equalsIgnoreCase(String.valueOf(e.getErrorCode())) && retry <= 1 ){
                retry += 1;
                parameters.put("quantity",CoinUtil.adjustPrecision(String.valueOf(parameters.get("quantity"))));
                parameters.remove("timestamp");
                parameters.remove("signature");
                order = createFuturePosition(parameters, retry);
                System.out.println("Position created by reducing precision for coin "+parameters+" Result: "+order);
                return order;
            }else if(Coins.ERROR_CODE_4164.equalsIgnoreCase(String.valueOf(e.getErrorCode())) && retry <= 4){
                retry += 1;
                parameters.put("quantity",doubleQuantity(String.valueOf(parameters.get("quantity"))));
                parameters.remove("timestamp");
                parameters.remove("signature");
                System.out.println("Position Retrying by doubling quantity for coin "+parameters);
                order = createFuturePosition(parameters, retry);
                System.out.println("Position created by doubling quantity for coin "+parameters+" Result: "+order);
                return order;
            }
            else if(Coins.ERROR_CODE_4003.equalsIgnoreCase(String.valueOf(e.getErrorCode())) && retry <= 2){
                retry += 1;
                parameters.put("quantity",increaseByPoint1(String.valueOf(parameters.get("quantity"))));
                parameters.remove("timestamp");
                parameters.remove("signature");
                System.out.println("Position Retrying by increasing quantity for coin "+parameters);
                order = createFuturePosition(parameters, retry);
                System.out.println("Position created by by increasing quantity for coin "+parameters+" Result: "+order);
                return order;
            }else if(Coins.ERROR_CODE_4400.equalsIgnoreCase(String.valueOf(e.getErrorCode())) && retry <= 1 ){
                retry += 1;
                parameters.remove("timestamp");
                parameters.remove("signature");
                parameters.put("reduceOnly","true");
                System.out.println("Position Retrying with reduceOnly parameters "+parameters);
                createFuturePosition(parameters, retry);
            }else if(Coins.ERROR_CODE_2022.equalsIgnoreCase(String.valueOf(e.getErrorCode()))){
                System.out.println("ReduceOnly Order is getting rejected: Pausing new orders");
                errored.add(String.valueOf(parameters.get("symbol")));
                pauseNewOrder = true;
            } else if(Coins.ERROR_CODE_4141.equalsIgnoreCase(String.valueOf(e.getErrorCode()))){
                System.out.println("Symbol is closed. "+parameters);
                errored.add(String.valueOf(parameters.get("symbol")));
            }else{
                errored.add(String.valueOf(parameters.get("symbol")));
            }
        }
        return  order;
    }

    private static String increaseByPoint1(String quantity) {
        if(quantity.equalsIgnoreCase("0.0"))
            return "0.25";
        return "0.5";
    }

    private static void handleNegativePosition(Map<String, Object> parameters, String coin, PositionInfo positionInfo,
                                               SpotClient client) throws BinanceConnectorException, BinanceClientException {

        Double unRealizedProfit = positionInfo.getUnRealizedProfit();

        if (unRealizedProfit >= 0.0) {
            if (!pauseNewOrder && isPositionAmountLT75Cent(coin, positionInfo)) {
                increasePositionAmount(parameters, Coins.SELL_SIDE, client);
                System.out.println("Position Increased for " + parameters);
            }else  if (unRealizedProfit >= 1.0 && !pauseNewOrder) {
                closeAndCreatePosition(coin, positionInfo, client, parameters);
            }else if(unRealizedProfit >= 1.0) {
                closeFuturePosition(coin,positionInfo,client);
            }
        } else if (unRealizedProfit < 0) {
            handleNegativeUnrealizedProfitForSellOrder(parameters, coin, positionInfo, client);
        }
    }

    private static void handleNegativeUnrealizedProfitForSellOrder(Map<String, Object> parameters, String coin,
                                                       PositionInfo positionInfo, SpotClient client)
            throws BinanceConnectorException, BinanceClientException {

        if(positionInfo.getUnRealizedProfit() <= -2.0){
            System.out.println("Closing SELL order: "+positionInfo);
            closeFuturePosition(coin, positionInfo, client);
        }else if (getPercentageGap(positionInfo.getEntryPrice(), positionInfo.getMarkPrice())
                >= Coins.PRICE_CHANGE_PERCENTAGE_THRESHOLD) {
            closeAndCreatePosition(coin, positionInfo, client, parameters);
        }
    }

    private static void handlePositivePosition(Map<String, Object> parameters, String coin, PositionInfo positionInfo,
                                               SpotClient client) throws BinanceConnectorException, BinanceClientException {

        Double unRealizedProfit = positionInfo.getUnRealizedProfit();

        if (unRealizedProfit >= 1.0 && !pauseNewOrder) {
            //closeFuturePosition(coin, positionInfo, client);
            closeAndCreatePosition(coin, positionInfo, client, parameters);
        }if (unRealizedProfit >= 1.0 ) {
            closeFuturePosition(coin, positionInfo, client);
            //closeAndCreatePosition(coin, positionInfo, client, parameters);
        } else if (!pauseNewOrder && isPositionAmountLT75Cent(coin, positionInfo)) {
            increasePositionAmount(parameters, Coins.BUY_SIDE, client);
            System.out.println("Position Increased for " + parameters);
        }else if (unRealizedProfit < 0) {
            handleNegativeUnrealizedProfitForBuyOrder(parameters, coin, positionInfo, client);
        }
    }

    private static void handleNegativeUnrealizedProfitForBuyOrder(Map<String, Object> parameters, String coin,
                                                       PositionInfo positionInfo, SpotClient client)
            throws BinanceConnectorException, BinanceClientException {

        if (!pauseNewOrder && isPositionAmountLT75Cent(coin, positionInfo)) {
            increasePositionAmount(parameters, Coins.BUY_SIDE, client);
            System.out.println("Position Increased for " + parameters);
        } else if (getPercentageGap(positionInfo.getLiquidationPrice(), positionInfo.getMarkPrice())
                <= Coins.POSITION_CLOSE_THRESOLD_PERCENTAGE) {
            closeFuturePosition(coin, positionInfo, client);
        }
    }
    private static Map<String, Object> updateParametersUnused(Map<String, Object> parameters,String coin) {
        Sentiment sentiment = getSentiment(coin);
        parameters.put("symbol", coin);
        if(parameters.get("side") == null)
            parameters.put("side", sentiment.getSide());
        parameters.put("type", sentiment.getType());
//        if (sentiment.getTimeInForce() != null)
//            parameters.put("timeInForce", sentiment.getTimeInForce());
//        if (sentiment.getPrice() != null)
//            parameters.put("stopPrice", sentiment.getStopPrice());
//        parameters.put("price", sentiment.getPrice());
        parameters.put("quantity", sentiment.getQuantity());
        System.out.println("Parameters: "+parameters);
        return parameters;
    }
    private static void closeAndCreatePosition(String coin, PositionInfo positionInfo, SpotClient client, Map<String, Object> parameters)
            throws BinanceConnectorException, BinanceClientException {
        closeFuturePosition(coin, positionInfo, client);
        createFuturePosition(parameters, 0);
    }



    private static Map<String, Object> updateParameters(Map<String, Object> parameters, String coin) {
        Sentiment sentiment = MarketSentimentAnalyzer.getSentiment(coin, MarketType.LIMIT,tickerMap);
                //getSentimentWithMovingAverage(coin);
        if(sentiment!=null && (sentiment.getSide()==null ||Coins.HOLD_SIDE.equalsIgnoreCase(sentiment.getSide())))
            return null;
        parameters.put("symbol", coin);
        if (parameters.get("side") == null) parameters.put("side", sentiment.getSide());
        parameters.put("quantity", CoinUtil.getQuantity(sentiment.getSymbol(),Double.valueOf(sentiment.getPrice())));
        parameters.put("type", sentiment.getType());
        if(MarketType.LIMIT.toString().equalsIgnoreCase(sentiment.getType())) {
            parameters.put("price", sentiment.getPrice());
            parameters.put("timeInForce", sentiment.getTimeInForce());
            parameters.put("closePosition", sentiment.getClosePosition());
            parameters.put("newOrderRespType", sentiment.getNewOrderRespType());
        }
        System.out.println("Parameters: " + parameters);
        return parameters;
    }

    private static void closeFuturePosition(String coin, PositionInfo positionInfo, SpotClient client)
            throws BinanceConnectorException, BinanceClientException {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("symbol", coin);
        parameters.put("side", reverseSide(evaluateSide(positionInfo)));
        parameters.put("type", "MARKET");
        parameters.put("quantity", String.valueOf(Math.abs(positionInfo.getPositionAmount())));
        String result = client.createFutures().createFuturesPosition(parameters);
        System.out.println("Position Closed status for coin " + coin + " Result: " + result);
    }

    private static String reverseSide(String side) {
        return Coins.BUY_SIDE.equalsIgnoreCase(side) ? Coins.SELL_SIDE : Coins.BUY_SIDE;
    }

    private static String evaluateSide(PositionInfo positionInfo) {
        return positionInfo.getPositionAmount() < 0 ? Coins.SELL_SIDE : Coins.BUY_SIDE;
    }

    private static boolean isPositionAmountLT75Cent(String coin, PositionInfo positionInfo) {
        double positionAmount = Math.abs(positionInfo.getPositionAmount()) * positionInfo.getEntryPrice()
                / positionInfo.getLeverage();
        return positionAmount <= 1.0;
    }

    private static void increasePositionAmount(Map<String, Object> parameters, String side, SpotClient client)
            throws BinanceConnectorException, BinanceClientException {
        parameters.put("side", side);
        parameters.put("type","MARKET");
        parameters.remove("price");
        parameters.remove("timeInForce");
        parameters.remove("closePosition");
        parameters.remove("newOrderRespType");
        createFuturePosition(parameters, 0);
    }

    private static double getPercentageGap(Double price1, Double price2) {
        return 100 - (price1 * 100 / price2);
    }

    private static Sentiment getSentiment(String coin) {
        TickerInfo tickerInfo = getTicker(coin);
        Sentiment sentiment = new Sentiment();
        sentiment.setType(Coins.TYPE_MARKET);
        sentiment.setQuantity(CoinUtil.getQuantity(coin, tickerInfo.getLastPrice()));
        sentiment.setSide(Coins.BUY_SIDE);
        if (tickerInfo.getPriceChangePercent() < 0.0 && Math.abs(tickerInfo.getPriceChangePercent())
                >= Coins.PRICE_CHANGE_PERCENTAGE_THRESHOLD)
            sentiment.setSide(Coins.SELL_SIDE);
        if (Coins.BUY_SIDE.equalsIgnoreCase(sentiment.getSide()))
            sentiment.setStopPrice(String.valueOf(tickerInfo.getLowPrice()));
        else
            sentiment.setStopPrice(String.valueOf(tickerInfo.getHighPrice()));
        sentiment.setPrice(String.valueOf(tickerInfo.getLastPrice()));
        sentiment.setTimeInForce(Coins.TIME_IN_FORCE);
        return sentiment;
    }






    private static String doubleQuantity(String quantity) {
        return String.valueOf(Double.valueOf(quantity)*2);
    }

    private static List<TickerInfo> getTickers(List<String> symbols) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        SpotClient client = new SpotClientImpl(PrivateConfig.TEE_API_KEY, PrivateConfig.TEE_SECRET_KEY);
        Market market = client.createMarket();
        parameters.put("symbols", symbols);
        String result = market.ticker(parameters);
        System.out.println(result);
        Type orderListType = new TypeToken<List<TickerInfo>>() {}.getType();
        Gson gson = new Gson();
        return gson.fromJson(result, orderListType);
    }

    private static TickerInfo getTicker(String symbol) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        Market market = client.createMarket();
        parameters.put("symbol", symbol);
        String result = market.ticker(parameters);
        System.out.println(result);
        Gson gson = new Gson();
        return gson.fromJson(result, TickerInfo.class);
    }

    private static Sentiment getSentimentWithMovingAverage(String coin) {
        TickerInfo tickerInfo = getTicker(coin);
        double longTermMA = calculateMovingAverage(coin, Coins.LONG_TERM_PERIOD);
        double shortTermMA = calculateMovingAverage(coin, Coins.SHORT_TERM_PERIOD);
        System.out.println("longTermMA: "+longTermMA);
        System.out.println("shortTermMA: "+shortTermMA);
        Sentiment sentiment = new Sentiment();
        sentiment.setType(Coins.TYPE_MARKET);
        sentiment.setQuantity(CoinUtil.getQuantity(coin, tickerInfo.getLastPrice()));

        // Smart Entry Strategy
        if (shortTermMA > longTermMA && tickerInfo.getPriceChangePercent() > 0) {
            sentiment.setSide(Coins.BUY_SIDE);
        } else if (shortTermMA < longTermMA && tickerInfo.getPriceChangePercent() < 0) {
            sentiment.setSide(Coins.SELL_SIDE);
        }else if (shortTermMA > longTermMA && tickerInfo.getPriceChangePercent() > Coins.ENTRY_THRESHOLD_PERCENT) {
            sentiment.setSide(Coins.BUY_SIDE);
        } else if (shortTermMA < longTermMA && tickerInfo.getPriceChangePercent() < -Coins.PRICE_CHANGE_PERCENTAGE_THRESHOLD_10) {
            sentiment.setSide(Coins.SELL_SIDE);
        }else{
            sentiment.setSide(Coins.BUY_SIDE);
        }

        // Smart Exit Strategy
//        if (sentiment.getSide().equals(Coins.BUY_SIDE) && tickerInfo.getPriceChangePercent() < -Coins.EXIT_THRESHOLD_PERCENT) {
//            sentiment.setSide(Coins.SELL_SIDE);
//        } else if (sentiment.getSide().equals(Coins.SELL_SIDE) && tickerInfo.getPriceChangePercent() > Coins.EXIT_THRESHOLD_PERCENT) {
//            sentiment.setSide(Coins.BUY_SIDE);
//        }

        sentiment.setPrice(String.valueOf(tickerInfo.getLastPrice()));
        sentiment.setTimeInForce(Coins.TIME_IN_FORCE);

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

    private static List<Double> getHistoricalPrices(String coin, int period) {
        // Implement logic to fetch historical price data from Binance API
        // Use the specified period to get the required number of data points
        // Return a list of historical prices

        TimeInterval timeInterval =  period==Coins.SHORT_TERM_PERIOD ? TimeInterval.TEN_MINUTES:TimeInterval.ONE_HOUR;
        List<Double> historicalPrices = new LinkedList<>();
        // Replace the following with actual data fetching logic
        return HistoricalDataFetcher.getHistoricalPrices(coin,timeInterval).stream().map(m->Double.valueOf(m)).collect(Collectors.toList());
    }

    private static double getPriceForDate(String coin, Date date) {
        // Implement logic to fetch the price for the specified coin on the given date
        // Use Binance API or other data source
        // Replace the following with actual data fetching logic
        return 0.0;
    }

    private static Date calculateDateForIndex(int index) {
        // Implement logic to calculate the date for the given index
        // This can be based on the current date and the desired interval
        // Replace the following with actual date calculation logic
        return new Date();
    }
    private static boolean openOrderExist(String symbol) {
        SpotClient client = new SpotClientImpl(PrivateConfig.TEE_API_KEY, PrivateConfig.TEE_SECRET_KEY, PrivateConfig.BASE_URLS[0]);
        try {
            Map<String, Object> params = new HashMap<>();
            Gson gson = new Gson();
            params.put("symbol", symbol);
            String result = client.createFutures().getFuturesOpenOrders(params);
            Type orderListType = new TypeToken<List<OpenOrderInfo>>() {}.getType();
            List<OpenOrderInfo> positionInfos = gson.fromJson(result, orderListType);
            return !positionInfos.isEmpty();
        } catch (Exception e) {
            //errored.add(symbol);
            e.printStackTrace();
            return false;
        }
    }
}

