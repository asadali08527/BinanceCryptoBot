package com.bcb.orders.futures.test.api;

import com.bcb.trade.constants.Coins;
import com.bcb.trade.sentiment.MarketSentimentAnalyzer;
import com.bcb.trade.util.CoinUtil;
import com.bcb.client.SpotClient;
import com.bcb.config.PrivateConfig;
import com.bcb.enums.MarketType;
import com.bcb.exceptions.BinanceClientException;
import com.bcb.exceptions.BinanceConnectorException;
import com.bcb.impl.SpotClientImpl;
import com.bcb.impl.spot.Market;
import com.bcb.transfer.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FutureOrderExecutor {

    private static boolean pauseNewOrder = false;
    static Map<String, TickerInfo> tickerMap = null;
    private static List<String> errored = new ArrayList<>();

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) {
        scheduler.scheduleAtFixedRate(FutureOrderExecutor::executeCronJob, 0, 300, TimeUnit.SECONDS);
    }

    public static List<String> getAllFutureCoins() {
        return Arrays.asList(Coins.FUTURE_USDT_COINS_IN_ACTION);
    }

    public static void executeCronJob() {
        pauseNewOrder = false;
        tickerMap = MarketSentimentAnalyzer.getTickers(null);
        Map<String, Object> parameters = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> symbols = CoinUtil.getAllFutureCoinsByTypeAndCategory();
        Date startTime = new Date();
        System.out.println("*******************************************************************************************************************************************");
        System.out.println("Cron started at " + startTime);
        symbols.forEach(coin -> {
            if(!pauseNewOrder) {
                if (!openOrderExist(coin)) {
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
                            printResult(symbols, errors, startTime, errored);
                            pauseNewOrder = true;
                        }
                        handleException(errors, coin, parameters, e);
                    } finally {
                        parameters.clear();
                    }
                }
            }else{
                System.out.println("Unable to process coin: "+coin+" | Order paused status : " + pauseNewOrder);
            }
        });
        printResult(symbols, errors, startTime,errored);
    }

    private static void printResult(List<String> symbols, List<String> errors, Date startTime, List<String> errored) {
        symbols.removeAll(errored);
        Date finishedTime = new Date();
        System.out.println("Cron Finished at " + finishedTime);
        System.out.println("Total Time taken to Execute The Job : " + (finishedTime.getTime()-startTime.getTime())/(60.0 * 1000.0) + " minutes");
        System.out.println("*******************************************************************************************************************************************");
        System.out.println(errors);
        System.out.println("Errored : "+errored);
    }

    private static boolean openOrderExist(String symbol) {
        SpotClient client = new SpotClientImpl(PrivateConfig.TAA_API_KEY, PrivateConfig.TAA_SECRET_KEY, PrivateConfig.BASE_URLS[0]);
        try {
            Map<String, Object> params = new HashMap<>();
            Gson gson = new Gson();
            params.put("symbol", symbol);
            String result = client.createFutures().getFuturesOpenOrders(params);
            Type orderListType = new TypeToken<List<OpenOrderInfo>>() {}.getType();
            List<OpenOrderInfo> positionInfos = gson.fromJson(result, orderListType);
            if(positionInfos.isEmpty()){
                return false;
            }else{
                System.out.println("Open Order already exists: "+ positionInfos);
                return true;
            }
        } catch (Exception e) {
            //errored.add(symbol);
            e.printStackTrace();
            return false;
        }
    }
    private static void processCronJobForSymbol(Map<String, Object> parameters, String coin)
            throws BinanceConnectorException, BinanceClientException {
        SpotClient client = new SpotClientImpl(PrivateConfig.TAA_API_KEY, PrivateConfig.TAA_SECRET_KEY, PrivateConfig.BASE_URLS[0]);
        String result = client.createFutures().getFuturesOpenPosition(parameters);
        Gson gson = new Gson();
        Type orderListType = new TypeToken<List<PositionInfo>>() {}.getType();
        List<PositionInfo> positionInfos = gson.fromJson(result, orderListType);
        PositionInfo positionInfo = positionInfos.get(0);
        parameters.clear();
        parameters = updateParameters(parameters, coin);
        if(parameters == null)
            return;
        //System.out.println(positionInfo);
        if (positionInfo.getPositionAmount() == 0.0) {
            if( !pauseNewOrder)
                createFuturePosition(parameters, client, 0);
//            if(Coins.SELL_SIDE.equalsIgnoreCase(String.valueOf(parameters.get("side")))) {
//                System.out.println("Creating SELL Order for : "+coin);
//                createFuturePosition(parameters, client, 0);
//         }
        } else if (positionInfo.getPositionAmount() < 0.0) {
            handleNegativePosition(parameters, coin, positionInfo, client);
        } else {
            handlePositivePosition(parameters, coin, positionInfo, client);
        }
    }
    private static void createFuturePosition(Map<String, Object> parameters, SpotClient client, int retry) {
        try {
            String result = client.createFutures().createFuturesPosition(parameters);
            System.out.println("Position Creation status for coin "+parameters+" Result: "+result);
        }catch (BinanceConnectorException e) {
            System.err.println((String) String.format("fullErrMessage: %s", e.getMessage()));
        } catch (BinanceClientException e) {
            System.err.println((String) String.format("fullErrMessage: %s \nerrMessage: %s \nerrCode: %d \nHTTPStatusCode: %d",
                    e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode()));
            if(Coins.ERROR_CODE_1111.equalsIgnoreCase(String.valueOf(e.getErrorCode())) && retry <= 1 ){
                retry += 1;
                parameters.put("quantity",reducePrecision(String.valueOf(parameters.get("quantity"))));
                parameters.remove("timestamp");
                parameters.remove("signature");
                System.out.println("Position retrying by reducing precision for coin "+parameters);
                createFuturePosition(parameters,client, retry);
            }else if(Coins.ERROR_CODE_4164.equalsIgnoreCase(String.valueOf(e.getErrorCode())) && retry <= 4){
                retry += 1;
                parameters.put("quantity",doubleQuantity(String.valueOf(parameters.get("quantity"))));
                parameters.remove("timestamp");
                parameters.remove("signature");
                System.out.println("Position Retrying by doubling quantity for coin "+parameters);
                createFuturePosition(parameters,client, retry);
            }
            else if(Coins.ERROR_CODE_4003.equalsIgnoreCase(String.valueOf(e.getErrorCode())) && retry <= 2){
                retry += 1;
                parameters.put("quantity",increaseByPoint1(String.valueOf(parameters.get("quantity"))));
                parameters.remove("timestamp");
                parameters.remove("signature");
                System.out.println("Position Retrying by increasing quantity for coin "+parameters);
                createFuturePosition(parameters,client, retry);
            }else if(Coins.ERROR_CODE_4400.equalsIgnoreCase(String.valueOf(e.getErrorCode())) && retry <= 1 ){
                retry += 1;
                parameters.remove("timestamp");
                parameters.remove("signature");
                parameters.put("reduceOnly","true");
                System.out.println("Position Retrying with reduceOnly parameters "+parameters);
                createFuturePosition(parameters,client, retry);
            }else if(Coins.ERROR_CODE_2022.equalsIgnoreCase(String.valueOf(e.getErrorCode()))){
                System.out.println("ReduceOnly Order is getting rejected: Pausing new orders");
                errored.add(String.valueOf(parameters.get("symbol")));
                pauseNewOrder = true;
                return;
            } else if(Coins.ERROR_CODE_4141.equalsIgnoreCase(String.valueOf(e.getErrorCode()))){
                System.out.println("Symbol is closed. "+parameters);
                errored.add(String.valueOf(parameters.get("symbol")));
            }else{
                errored.add(String.valueOf(parameters.get("symbol")));
            }
        }
    }

    private static String increaseByPoint1(String quantity) {
        if(quantity.equalsIgnoreCase("0.0"))
            return "0.25";
        return "0.5";
    }

    private static void handleNegativePosition(Map<String, Object> parameters, String coin, PositionInfo positionInfo,
                                               SpotClient client) throws BinanceConnectorException, BinanceClientException {

        Double unRealizedProfit = positionInfo.getUnRealizedProfit();

        if (unRealizedProfit >= 1.0) {
            closeAndCreatePosition(coin, positionInfo, client, parameters);
        } else if (unRealizedProfit < 0) {
            handleNegativeUnrealizedProfitForSellOrder(parameters, coin, positionInfo, client);
        }
    }

    private static void handleNegativeUnrealizedProfitForSellOrder(Map<String, Object> parameters, String coin,
                                                       PositionInfo positionInfo, SpotClient client)
            throws BinanceConnectorException, BinanceClientException {

//        if (isPositionAmountLT75Cent(coin, positionInfo)) {
//            increasePositionAmount(parameters, Coins.SELL_SIDE, client);
//            System.out.println("Position Increased for " + parameters);
//        } else
        if(positionInfo.getUnRealizedProfit() <= -1.0){
            System.out.println("Closing SELL order: "+positionInfo);
            closeFuturePosition(coin, positionInfo, client);
        }else if (getPercentageGap(positionInfo.getEntryPrice(), positionInfo.getMarkPrice())
                >= Coins.PRICE_CHANGE_PERCENTAGE_THRESHOLD) {
            closeFuturePosition(coin, positionInfo, client);
            //closeAndCreatePosition(coin, positionInfo, client, parameters);
        }
    }

    private static void handlePositivePosition(Map<String, Object> parameters, String coin, PositionInfo positionInfo,
                                               SpotClient client) throws BinanceConnectorException, BinanceClientException {

        Double unRealizedProfit = positionInfo.getUnRealizedProfit();

        if (unRealizedProfit >= 1.0) {
            //closeFuturePosition(coin, positionInfo, client);
            closeAndCreatePosition(coin, positionInfo, client, parameters);
        }else if (!pauseNewOrder && isPositionAmountLT75Cent(coin, positionInfo)) {
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
        if(!pauseNewOrder)
            createFuturePosition(parameters, client, 0);
    }

    private static void handleException(List<String> errors, String coin, Map<String, Object> parameters,
                                        Exception exception) {
        System.err.println((String) String.format("fullErrMessage: %s", exception.getMessage()));
        errors.add(coin + " : FullErrMsg : " + exception.getMessage());
        parameters.clear();
    }

    private static Map<String, Object> updateParameters(Map<String, Object> parameters, String coin) {
        Sentiment sentiment = MarketSentimentAnalyzer.getSentiment(coin, MarketType.MARKET, tickerMap);
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
        return positionAmount <= 0.75;
    }

    private static void increasePositionAmount(Map<String, Object> parameters, String side, SpotClient client)
            throws BinanceConnectorException, BinanceClientException {
        if(!pauseNewOrder) {
            parameters.put("side", side);
            createFuturePosition(parameters, client, 0);
        }
    }

    private static double getPercentageGap(Double price1, Double price2) {
        return 100 - (price1 * 100 / price2);
    }

    private static Sentiment getSentiment(String coin) {
        TickerInfo tickerInfo = getTicker(coin);
        Sentiment sentiment = new Sentiment();
        sentiment.setType(Coins.TYPE_MARKET);
        sentiment.setQuantity(getQuantity(coin, tickerInfo.getLastPrice()));
        sentiment.setSide(Coins.BUY_SIDE);
//        if (tickerInfo.getPriceChangePercent() < 0.0)
//            sentiment.setSide(Coins.SELL_SIDE);
        if (tickerInfo.getPriceChangePercent() < 0.0 && Math.abs(tickerInfo.getPriceChangePercent())
                >= Coins.PRICE_CHANGE_PERCENTAGE_THRESHOLD_10)
            sentiment.setSide(Coins.SELL_SIDE);
        if (Coins.BUY_SIDE.equalsIgnoreCase(sentiment.getSide()))
            sentiment.setStopPrice(String.valueOf(tickerInfo.getLowPrice()));
        else
            sentiment.setStopPrice(String.valueOf(tickerInfo.getHighPrice()));
        sentiment.setPrice(String.valueOf(tickerInfo.getLastPrice()));
        sentiment.setTimeInForce(Coins.TIME_IN_FORCE);
        return sentiment;
    }

    private static String getQuantity(String coin, Double lastPrice) {
        double result = Coins.BASE_LEVERAGE_25 / lastPrice;
        DecimalFormat decimalFormat = null;
        if (Arrays.asList(Coins.ZERO_DIGIT_FUTURE_USDT_COINS).contains(coin)) {
            return String.valueOf((int) result);
        }
        if (Arrays.asList(Coins.ONE_DIGIT_FUTURE_USDT_COINS).contains(coin)) {
            decimalFormat = new DecimalFormat("#.#");
        } else if (Arrays.asList(Coins.TWO_DIGIT_FUTURE_USDT_COINS).contains(coin)) {
            decimalFormat = new DecimalFormat("#.##");
        } else
            decimalFormat = new DecimalFormat("#.###");
        return nonZeroQuantity(decimalFormat.format(result));
    }

    private static String reducePrecision(String price) {
        String quantity = price.substring(0, price.length() - 1);
        return nonZeroQuantity(quantity);

    }

    private static String nonZeroQuantity(String quantity) {
        if(quantity.equalsIgnoreCase("0")|| quantity.equalsIgnoreCase("0.")||quantity.equalsIgnoreCase("0.0")){
            return "1";
        }
        else
            return quantity;
    }

    private static String doubleQuantity(String quantity) {
        return String.valueOf(Double.valueOf(quantity)*2);
    }

    private static List<TickerInfo> getTickers(List<String> symbols) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        SpotClient client = new SpotClientImpl(PrivateConfig.TEE_API_KEY, PrivateConfig.TAA_SECRET_KEY);
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
        SpotClient client = new SpotClientImpl(PrivateConfig.TAA_API_KEY, PrivateConfig.TAA_SECRET_KEY);
        Market market = client.createMarket();
        parameters.put("symbol", symbol);
        String result = market.ticker(parameters);
        System.out.println(result);
        Gson gson = new Gson();
        return gson.fromJson(result, TickerInfo.class);
    }

}

