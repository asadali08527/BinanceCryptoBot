package com.bcb.orders.futures.test.api;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.bcb.client.SpotClient;
import com.bcb.config.PrivateConfig;
import com.bcb.enums.MarketType;
import com.bcb.exceptions.BinanceClientException;
import com.bcb.exceptions.BinanceConnectorException;
import com.bcb.impl.SpotClientImpl;
import com.bcb.trade.constants.Coins;
import com.bcb.trade.sentiment.MarketSentimentAnalyzer;
import com.bcb.trade.util.CoinUtil;
import com.bcb.transfer.OpenOrderInfo;
import com.bcb.transfer.PositionInfo;
import com.bcb.transfer.Sentiment;
import com.bcb.transfer.TickerInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class FutureLimitOrder {
    private static final String REDUCING_PRECISION_MESSAGE = "Position created by reducing precision for coin ";
    private static final String DOUBLING_QUANTITY_MESSAGE = "Position Retrying by doubling quantity for coin ";
    private static final String INCREASING_QUANTITY_MESSAGE = "Position Retrying by increasing quantity for coin ";
    private static final String PAUSING_ORDER_MESSAGE = "ReduceOnly Order is getting rejected: Pausing new orders";

    private static final int EXECUTION_INTERVAL_MINUTES = 5;
    private static final MarketType MARKET_TYPE = MarketType.LIMIT;
    private static List<String> errored = new ArrayList<>();
    private static List<String> invalidSymbol = new ArrayList<>();
    private static List<String> processed = new ArrayList<>();

    List<String> symbols = null;

    private static boolean pauseNewOrderFor2Hrs = false;
    private static Date pauseTimefor2Hrs = null;
    private static boolean openOrderExist = false;
    private static boolean keepEitherOpenOrderOrOpenPosition = false;

    public FutureLimitOrder(){
        //this.symbols = getAllFutureCoinsByTypeAndCategory();
        this.symbols = getAllFutureCoins();
    }

    public static void main(String[] args) {
        FutureLimitOrder futureLimitOrder = new FutureLimitOrder();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(futureLimitOrder::takePositions, 0, EXECUTION_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }
    public void takePositions() {
        processed.clear();
        errored.clear();
        if (pauseNewOrderFor2Hrs && checkIfCoolingPeriodPassed(pauseTimefor2Hrs)) {
            pauseNewOrderFor2Hrs = false;
            pauseTimefor2Hrs = null;
        }else if(pauseNewOrderFor2Hrs){
            System.out.println("Got Futures Trading Quantitative Rules violated error: Job paused for next 2 hours...");
            return;
        }
        Map<String, TickerInfo> tickerMap = MarketSentimentAnalyzer.getTickers(null);
        Map<String, Object> parameters = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        Date startTime = new Date();
        System.out.println("**************************************************************************************************************************************************");
        System.out.println("Cron started at " + startTime);
        Iterator<String> iterator = symbols.iterator();
        while(iterator.hasNext())
        {
            String coin = iterator.next();
            openOrderExist= openOrderExist(coin);
            if(keepEitherOpenOrderOrOpenPosition)
                if(openOrderExist)
                    continue;
                parameters.put("symbol", coin);
                try {
                    takePositionForCoin(parameters, coin, tickerMap);
                } catch (BinanceConnectorException | BinanceClientException e) {
                    CoinUtil.handleException(errors, coin, e);
                } catch (Exception e) {
                    System.out.println("Exception: " + e.getMessage());
                } finally {
                    parameters.clear();
                }
            }
        printResult(symbols,errors,errored,startTime);
    }

    private boolean checkIfCoolingPeriodPassed(Date pauseTimefor2Hrs) {
        if(pauseTimefor2Hrs == null)
            return true;
        // Convert Date to LocalDateTime
        LocalDateTime inputDateTime = pauseTimefor2Hrs.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        // Add 2 hours to the input date time
        LocalDateTime datePlus2Hours = inputDateTime.plusHours(2);

        // Get the current date and time
        LocalDateTime currentDateTime = LocalDateTime.now();

        // Check if datePlus2Hours is after the current date and time
        if (datePlus2Hours.isAfter(currentDateTime)) {
            return false;
        } else {
            return true;
        }
    }

    private void printResult(List<String> symbols, List<String> errors, List<String> errored, Date startTime) {
        symbols.removeAll(errored);
        Date finishedTime = new Date();
        System.out.println("Cron Finished at " + finishedTime);
        System.out.println("Total Time taken to Execute The Job : " + (finishedTime.getTime()-startTime.getTime())/(60.0 * 1000.0) + " minutes" + " \nProcessed Coins : "+processed);
        System.out.println("Errors: "+errors+" | \nCoin didn't get processed : "+errored);
        System.out.println("**************************************************************************************************************************************************");
    }

    private void takePositionForCoin(Map<String, Object> parameters, String coin, Map<String, TickerInfo> tickerInfoMap)
            throws BinanceConnectorException, BinanceClientException {

        SpotClient client = new SpotClientImpl(PrivateConfig.TAA_API_KEY, PrivateConfig.TAA_SECRET_KEY, PrivateConfig.BASE_URLS[0]);
        String result = client.createFutures().getFuturesOpenPosition(parameters);
        Gson gson = new Gson();
        Type orderListType = new TypeToken<List<PositionInfo>>() {}.getType();
        List<PositionInfo> positionInfos = gson.fromJson(result, orderListType);
        PositionInfo positionInfo = positionInfos.get(0);
        parameters.clear();
        parameters = updateParameters(parameters, coin, tickerInfoMap);
        if(parameters == null) {
            return;
        }
        System.out.println("Current Position Info: "+positionInfo);
         if (positionInfo.getPositionAmount() == 0.0 && !openOrderExist) {
             if(!pauseNewOrderFor2Hrs) {
                 System.out.println("Creating Order for : " + parameters);
                 createFuturePosition(parameters, 0);
             }
        }
         else if (positionInfo.getPositionAmount() < 0.0) {
            System.out.println("Handling Existing Sell Order : "+positionInfo);
            handleNegativePosition(parameters, coin, positionInfo, client);
        } else if (positionInfo.getPositionAmount() > 0.0){
            System.out.println("Handling Existing Buy Order : "+positionInfo);
            handlePositivePosition(parameters, coin, positionInfo, client);
        }
    }

    private boolean openOrderExist(String symbol) {
        SpotClient client = new SpotClientImpl(PrivateConfig.TAA_API_KEY, PrivateConfig.TAA_SECRET_KEY, PrivateConfig.BASE_URLS[0]);
        try {
            Map<String, Object> params = new HashMap<>();
            Gson gson = new Gson();
            params.put("symbol", symbol);
            String result = client.createFutures().getFuturesOpenOrders(params);
            Type orderListType = new TypeToken<List<OpenOrderInfo>>() {}.getType();
            List<OpenOrderInfo> positionInfos = gson.fromJson(result, orderListType);
            return !positionInfos.isEmpty();
        } catch (Exception e) {
            errored.add(symbol);
            // Handle the exception (log, throw, or handle appropriately)
            e.printStackTrace(); // You can replace this with logging framework like SLF4J
            return false; // Indicate that there was an issue
        }
    }

    private void handleNegativePosition(Map<String, Object> parameters, String coin, PositionInfo positionInfo,
                                               SpotClient client) throws BinanceConnectorException, BinanceClientException {

        Double unRealizedProfit = positionInfo.getUnRealizedProfit();

        if (unRealizedProfit >= 0.0 && parameters !=null) {
            if (!pauseNewOrderFor2Hrs && isPositionAmountLT75Cent(coin, positionInfo)) {
                increasePositionAmount(parameters, Coins.SELL_SIDE, client);
                System.out.println("Position Increased for " + parameters);
            }else  if (unRealizedProfit >= 1.0 && parameters !=null) {
                closeAndCreatePosition(coin, positionInfo, client, parameters);
            }
        } else if (unRealizedProfit < 0) {
            handleNegativeUnrealizedProfitForSellOrder(parameters, coin, positionInfo, client);
        }
    }

    private static void closeFuturePosition(String coin, PositionInfo positionInfo, SpotClient client)
            throws BinanceConnectorException, BinanceClientException {
        Map<String, Object> parameters = null;
        try {
            System.out.println("Closing position for  " + coin);
            parameters = new HashMap<>();
            parameters.put("symbol", coin);
            parameters.put("side", CoinUtil.reverseSide(CoinUtil.evaluateSide(positionInfo)));
            parameters.put("type", "MARKET");
            parameters.put("quantity", String.valueOf(Math.abs(positionInfo.getPositionAmount())));
            String result = client.createFutures().createFuturesPosition(parameters);
            System.out.println("Position Closed status for coin " + coin + " Result: " + result);
        } catch (BinanceConnectorException e) {
            handleConnectorException(e);
        } catch (BinanceClientException e) {
            handleClientException(parameters, e, 0);
        } catch (Exception e) {
            handleGenericException(parameters, e);
        }
    }
    private void closeAndCreatePosition(String coin, PositionInfo positionInfo, SpotClient client, Map<String, Object> parameters)
            throws BinanceConnectorException, BinanceClientException {
        closeFuturePosition(coin, positionInfo, client);
        if(!pauseNewOrderFor2Hrs)
            createFuturePosition(parameters, 0);
    }
    private static boolean isPositionAmountLT75Cent(String coin, PositionInfo positionInfo) {
        double positionAmount = Math.abs(positionInfo.getPositionAmount()) * positionInfo.getEntryPrice()
                / positionInfo.getLeverage();
        return positionAmount <= 1.0;
    }
    private void handleNegativeUnrealizedProfitForSellOrder(Map<String, Object> parameters, String coin,
                                                                   PositionInfo positionInfo, SpotClient client)
            throws BinanceConnectorException, BinanceClientException {
        if (positionInfo.getUnRealizedProfit() >= 1.0 && parameters !=null) {
            //closeFuturePosition(coin, positionInfo, client);
            closeAndCreatePosition(coin, positionInfo, client, parameters);
        } else if(positionInfo.getUnRealizedProfit() <= -2.0){
            System.out.println("Closing SELL order: "+positionInfo);
            closeFuturePosition(coin, positionInfo, client);
        }else if (parameters !=null && CoinUtil.getPercentageGap(positionInfo.getEntryPrice(), positionInfo.getMarkPrice())
                >= Coins.PRICE_CHANGE_PERCENTAGE_THRESHOLD) {
            //closeAndCreatePosition(coin, positionInfo, client, parameters);
        }
    }

    private void handlePositivePosition(Map<String, Object> parameters, String coin, PositionInfo positionInfo,
                                               SpotClient client) throws BinanceConnectorException, BinanceClientException {

        Double unRealizedProfit = positionInfo.getUnRealizedProfit();

        if (unRealizedProfit >= 1.0 && parameters !=null) {
            if(pauseNewOrderFor2Hrs)
                closeFuturePosition(coin, positionInfo, client);
            else
                closeAndCreatePosition(coin, positionInfo, client, parameters);
        } else if(unRealizedProfit >= 1.0){
            closeFuturePosition(coin,positionInfo,client);
        }else if (!pauseNewOrderFor2Hrs && isPositionAmountLT75Cent(coin, positionInfo) && parameters !=null) {
            increasePositionAmount(parameters, Coins.BUY_SIDE, client);
            System.out.println("Position Increased for " + parameters);
        }
        else if (unRealizedProfit < 0) {
            handleNegativeUnrealizedProfitForBuyOrder(parameters, coin, positionInfo, client);
        }
    }
    private void increasePositionAmount(Map<String, Object> parameters, String side, SpotClient client)
            throws BinanceConnectorException, BinanceClientException {
        parameters.put("side", side);
        parameters.put("type","MARKET");
        parameters.remove("price");
        parameters.remove("timeInForce");
        parameters.remove("closePosition");
        parameters.remove("newOrderRespType");
        createFuturePosition(parameters, 0);
    }

    private void handleNegativeUnrealizedProfitForBuyOrder(Map<String, Object> parameters, String coin,
                                                                  PositionInfo positionInfo, SpotClient client)
            throws BinanceConnectorException, BinanceClientException {

        if (!pauseNewOrderFor2Hrs && isPositionAmountLT75Cent(coin, positionInfo) && parameters != null) {
            increasePositionAmount(parameters, Coins.BUY_SIDE, client);
            System.out.println("Position Increased for " + parameters);
        } else if (CoinUtil.getPercentageGap(positionInfo.getLiquidationPrice(), positionInfo.getMarkPrice())
                <= Coins.POSITION_CLOSE_THRESOLD_PERCENTAGE) {
            //closeFuturePosition(coin, positionInfo, client);
        }
    }
    private static void createFuturePosition(Map<String, Object> parameters, int retry) {
        try {
            SpotClient client = new SpotClientImpl(PrivateConfig.TAA_API_KEY, PrivateConfig.TAA_SECRET_KEY,PrivateConfig.BASE_URLS[0]);
            String result = client.createFutures().createFuturesPosition(parameters);
            processed.add(String.valueOf(parameters.get("symbol")));
            System.out.println("Position Creation status for coin "+parameters+" Result: "+result);
        }catch (BinanceConnectorException e) {
            handleConnectorException(e);
        } catch (BinanceClientException e) {
            handleClientException(parameters, e, retry);
        } catch (Exception e) {
            handleGenericException(parameters, e);
        }
//        catch (BinanceConnectorException e) {
//            System.err.println((String) String.format("fullErrMessage: %s", e.getMessage()));
//        } catch (BinanceClientException e) {
//            System.err.println((String) String.format("fullErrMessage: %s \nerrMessage: %s \nerrCode: %d \nHTTPStatusCode: %d",
//                    e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode()));
//            if(Coins.ERROR_CODE_1111.equalsIgnoreCase(String.valueOf(e.getErrorCode())) && retry <= 1 ){
//                retry += 1;
//                parameters.put("quantity",CoinUtil.adjustPrecision(String.valueOf(parameters.get("quantity"))));
//                parameters.remove("timestamp");
//                parameters.remove("signature");
//                createFuturePosition(parameters, retry);
//                System.out.println("Position created by reducing precision for coin "+parameters);
//            }else if(Coins.ERROR_CODE_4164.equalsIgnoreCase(String.valueOf(e.getErrorCode())) && retry <= 4){
//                retry += 1;
//                parameters.put("quantity",CoinUtil.doubleQuantity(String.valueOf(parameters.get("quantity"))));
//                parameters.remove("timestamp");
//                parameters.remove("signature");
//                System.out.println("Position Retrying by doubling quantity for coin "+parameters);
//                createFuturePosition(parameters, retry);
//                System.out.println("Position created by doubling quantity for coin "+parameters);
//            }
//            else if(Coins.ERROR_CODE_4003.equalsIgnoreCase(String.valueOf(e.getErrorCode())) && retry <= 2){
//                retry += 1;
//                parameters.put("quantity",CoinUtil.increaseByPoint1(String.valueOf(parameters.get("quantity"))));
//                parameters.remove("timestamp");
//                parameters.remove("signature");
//                System.out.println("Position Retrying by increasing quantity for coin "+parameters);
//                createFuturePosition(parameters, retry);
//                System.out.println("Position created by by increasing quantity for coin "+parameters);
//            }
//            else if(Coins.ERROR_CODE_4400.equalsIgnoreCase(String.valueOf(e.getErrorCode())) && retry <= 1 ){
//                //retry += 1;
//                //parameters.put("quantity",CoinUtil.increaseByPoint1(String.valueOf(parameters.get("quantity"))));
//                //parameters.remove("timestamp");
//                //parameters.remove("signature");
//                //parameters.put("reduceOnly","true");
//                System.out.println("Skipping due to error: Futures Trading Quantitative Rules violated, only reduceOnly order is allowed, please try again later. "+parameters);
//                pauseNewOrderFor2Hrs = true;
//                pauseTimefor2Hrs = new Date();
//                //createFuturePosition(parameters, retry);
//                return;
//            }else if(Coins.ERROR_CODE_1121.equalsIgnoreCase(String.valueOf(e.getErrorCode()))){
//                System.out.println("Invalid symbol "+parameters);
//                errored.add(String.valueOf(parameters.get("symbol")));
//                invalidSymbol.add(String.valueOf(parameters.get("symbol")));
//            }else if(Coins.ERROR_CODE_2022.equalsIgnoreCase(String.valueOf(e.getErrorCode()))){
//                System.out.println("ReduceOnly Order is getting rejected: Pausing new orders");
//                pauseNewOrderFor2Hrs = true;
//                errored.add(String.valueOf(parameters.get("symbol")));
//                return;
//            } else if(Coins.ERROR_CODE_4141.equalsIgnoreCase(String.valueOf(e.getErrorCode()))){
//                System.out.println("Symbol is closed. "+parameters);
//                errored.add(String.valueOf(parameters.get("symbol")));
//                return;
//            }else if(Coins.ERROR_CODE_1102.equalsIgnoreCase(String.valueOf(e.getErrorCode()))){
//                System.out.println("Mandatory parameter 'price' was not sent, was empty/null, or malformed. | "+parameters);
//                errored.add(String.valueOf(parameters.get("symbol")));
//                return;
//            }else if(Coins.ERROR_CODE_4014.equalsIgnoreCase(String.valueOf(e.getErrorCode()))){
//                System.out.println("Price not increased by tick size | "+parameters);
//                errored.add(String.valueOf(parameters.get("symbol")));
//                return;
//            }
//            else{
//                errored.add(String.valueOf(parameters.get("symbol")));
//            }
//        }catch (Exception e){
//            errored.add(String.valueOf(parameters.get("symbol")));
//            System.out.println("Error occured while processing "+parameters.get("symbol")+" | Error: "+e.getMessage());
//        }
    }

    public static List<String> getAllFutureCoins() {
        // return CoinUtil.getAllFutureCoinsByTypeAndCategory();
        return CoinUtil.getAllFutureCoinsByTypeAndCategory(Coins.FUTURE_USDT_COINS_IN_ACTION);
    }

    // Function to create a signature for the request
    private static String generateSignature(String data)
            throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(PrivateConfig.TAA_SECRET_KEY.getBytes(), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hmacData = mac.doFinal(data.getBytes());
        return new String(Base64.getEncoder().encode(hmacData));
    }
    private static Map<String, Object> updateParameters(Map<String, Object> params, String coin, Map<String, TickerInfo> tickerMap) {
        Sentiment sentiment = MarketSentimentAnalyzer.getSentiment(coin, MARKET_TYPE, tickerMap);
        if(sentiment!=null && (sentiment.getSide()==null ||Coins.HOLD_SIDE.equalsIgnoreCase(sentiment.getSide())))
            return null;
        params.put("symbol", sentiment.getSymbol());
        params.put("side", sentiment.getSide());
        params.put("type", sentiment.getType());
        params.put("quantity", CoinUtil.getQuantity(sentiment.getSymbol(),Double.valueOf(sentiment.getPrice())));
        params.put("price", sentiment.getPrice());
        params.put("timeInForce", sentiment.getTimeInForce());
        params.put("closePosition", sentiment.getClosePosition());
        params.put("newOrderRespType", sentiment.getNewOrderRespType());  // Set the response type (ACK, RESULT, FULL)
        //params.put("timestamp", System.currentTimeMillis());
        System.out.println("Parameters: " + params);
        return params;
    }

    // Function to place a future order with stop loss and take profit
    private String placeFutureOrder(Sentiment sentiment, SpotClient client)
            throws NoSuchAlgorithmException, InvalidKeyException, IOException {

        // Construct parameters
        Map<String, Object> params = new TreeMap<>();
        params.put("symbol", sentiment.getSymbol());
        params.put("side", sentiment.getSide());
        params.put("type", sentiment.getType());
        params.put("quantity", CoinUtil.getQuantity(sentiment.getSymbol(),Double.valueOf(sentiment.getPrice())));
        params.put("price", sentiment.getPrice());
        params.put("timeInForce", sentiment.getTimeInForce());
        params.put("closePosition", sentiment.getClosePosition());
        params.put("newOrderRespType", sentiment.getNewOrderRespType());  // Set the response type (ACK, RESULT, FULL)
        params.put("timestamp", System.currentTimeMillis());
        String result = client.createFutures().createFuturesPosition(params);
        return result;
    }
    private static void handleConnectorException(BinanceConnectorException e) {
        System.err.println(String.format("fullErrMessage: %s", e.getMessage()));
    }
    private static void printErrorMessage(String errorMessage, Map<String, Object> parameters) {
        errored.add(String.valueOf(parameters.get("symbol")));
        System.out.println(errorMessage + parameters);
    }
    private static void retryAndLog(Map<String, Object> parameters, int retry, String logMessage) {
        retry += 1;
        parameters.put("quantity", CoinUtil.adjustPrecision(String.valueOf(parameters.get("quantity"))));
        parameters.remove("timestamp");
        parameters.remove("signature");
        createFuturePosition(parameters, retry);
        System.out.println(logMessage + parameters);
    }

    private static void handleInvalidSymbol(Map<String, Object> parameters) {
        invalidSymbol.add(String.valueOf(parameters.get("symbol")));
    }

    private static void handleGenericException(Map<String, Object> parameters, Exception e) {
        errored.add(String.valueOf(parameters.get("symbol")));
        e.printStackTrace();
        System.out.println("Error occurred while processing " + parameters.get("symbol") + " | Error: " + e.getMessage());
    }
    private static void handleClientException(Map<String, Object> parameters, BinanceClientException e, int retry) {
        System.err.println(String.format("fullErrMessage: %s \nerrMessage: %s \nerrCode: %d \nHTTPStatusCode: %d",
                e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode()));

        String errorCode = String.valueOf(e.getErrorCode());

        if (Coins.ERROR_CODE_1111.equalsIgnoreCase(errorCode) && retry <= 1) {
            retryAndLog(parameters, retry, REDUCING_PRECISION_MESSAGE);
        } else if (Coins.ERROR_CODE_4164.equalsIgnoreCase(errorCode) && retry <= 4) {
            retryAndLog(parameters, retry, DOUBLING_QUANTITY_MESSAGE);
        } else if (Coins.ERROR_CODE_4003.equalsIgnoreCase(errorCode) && retry <= 2) {
            retryAndLog(parameters, retry, INCREASING_QUANTITY_MESSAGE);
        } else if (Coins.ERROR_CODE_4400.equalsIgnoreCase(errorCode) && retry <= 1) {
            pauseTimefor2Hrs = new Date();
            pauseNewOrderFor2Hrs = true;
            System.out.println("Skipping due to error: Futures Trading Quantitative Rules violated, only reduceOnly order is allowed, please try again later. " + parameters);
        } else if (Coins.ERROR_CODE_1121.equalsIgnoreCase(errorCode)) {
            printErrorMessage(e.getMessage(),parameters);
            handleInvalidSymbol(parameters);
        } else if (Coins.ERROR_CODE_2022.equalsIgnoreCase(errorCode)) {
            System.out.println(PAUSING_ORDER_MESSAGE);
            pauseNewOrderFor2Hrs = true;
            printErrorMessage(e.getMessage(),parameters);
        } else if (Coins.ERROR_CODE_4141.equalsIgnoreCase(errorCode)) {
            //System.out.println("Symbol is closed. " + parameters);
            printErrorMessage(e.getMessage(),parameters);
        } else if (Coins.ERROR_CODE_1102.equalsIgnoreCase(errorCode) || Coins.ERROR_CODE_4014.equalsIgnoreCase(errorCode)) {
            printErrorMessage(e.getMessage(),parameters);
        } else {
            printErrorMessage(e.getMessage(),parameters);
        }
    }
}
