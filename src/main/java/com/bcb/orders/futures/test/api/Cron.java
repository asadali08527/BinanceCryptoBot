package com.bcb.orders.futures.test.api;

import com.bcb.client.SpotClient;
import com.bcb.config.PrivateConfig;
import com.bcb.exceptions.BinanceClientException;
import com.bcb.exceptions.BinanceConnectorException;
import com.bcb.impl.SpotClientImpl;
import com.bcb.impl.spot.Market;
import com.bcb.transfer.Order;
import com.bcb.transfer.PositionInfo;
import com.bcb.transfer.Sentiment;
import com.bcb.transfer.TickerInfo;
import com.bcb.trade.constants.Coins;
import com.bcb.trade.util.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Cron {
    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        // Schedule the task to run every 5 minutes
        scheduler.scheduleAtFixedRate(Cron::getOpenOrder, 0, 5, TimeUnit.MINUTES);
    }
    public static List<String> getAllFutureCoins(){
        return Arrays.asList(Coins.FUTURE_USDT_COINS_IN_ACTION);
    }
    public static void getOpenOrder(){
        Map<String, Object> parameters = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> symbols = getAllFutureCoins();
        SpotClient client = new SpotClientImpl(PrivateConfig.TAA_API_KEY, PrivateConfig.TAA_SECRET_KEY, PrivateConfig.BASE_URLS[0]);
        System.out.println("Cron started at "+ new Date());
        symbols.stream().forEach(coin-> {
            parameters.put("symbol", coin);
            try {
                String result = client.createFutures().getFuturesOpenPosition(parameters);
                parameters.clear();
                updateParameters(parameters,coin);
                Gson gson = new Gson();
                Type orderListType = new TypeToken<List<PositionInfo>>() {}.getType();
                List<PositionInfo> positionInfos = gson.fromJson(result, orderListType);
                PositionInfo positionInfo = positionInfos.get(0);
                System.out.println(positionInfo);
                if(positionInfo.getPositionAmount()==0.0){
                    createFuturePosition(parameters, client,0);
                }else if (positionInfo.getPositionAmount() < 0.0) {
                    Double unRealizedProfit = positionInfo.getUnRealizedProfit();
                    if(unRealizedProfit >= 1.0 ){
                        closeFuturePosition(coin,positionInfo, client);
                        createFuturePosition(parameters, client,0);
                    }
                    else if(unRealizedProfit < 0 ) {
                        if(isPositionAmountLT75Cent(coin,positionInfo)){
                                increasePositionAmount(parameters,Coins.SELL_SIDE, client);
                                System.out.println("Position Increased for "+parameters);
                        }
                        else if (getPercentageGap(positionInfo.getEntryPrice(), positionInfo.getMarkPrice()) >= Coins.PRICE_CHANGE_PERCENTAGE_THRESHOLD) {
                            closeFuturePosition(coin, positionInfo, client);
                            createFuturePosition(parameters, client,0);
                        }
                    }
                }else{
                    Double unRealizedProfit = positionInfo.getUnRealizedProfit();
                    if(unRealizedProfit >= 1.0 ){
                        closeFuturePosition(coin,positionInfo, client);
                        createFuturePosition(parameters, client,0);
                    }
                    else if(unRealizedProfit < 0) {
                        if(isPositionAmountLT75Cent(coin,positionInfo)){
                            increasePositionAmount(parameters,Coins.BUY_SIDE, client);
                            System.out.println("Position Increased for "+parameters);
                        } else if(getPercentageGap(positionInfo.getLiquidationPrice(),positionInfo.getMarkPrice())<=Coins.POSITION_CLOSE_THRESOLD_PERCENTAGE ){
                            closeFuturePosition(coin,positionInfo, client);
                        }
                    }
                }
                parameters.clear();
            } catch (BinanceConnectorException e) {
                System.err.println((String) String.format("fullErrMessage: %s", e.getMessage()));
                errors.add(coin+" : FullErrMsg : "+ e.getMessage());
                //errors.add(String.valueOf(parameters.get("symbol")));
                parameters.clear();
            } catch (BinanceClientException e) {
                System.err.println((String) String.format("fullErrMessage: %s \nerrMessage: %s \nerrCode: %d \nHTTPStatusCode: %d",
                        e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode()));
                errors.add(coin+" : FullErrMsg : "+ e.getMessage()+" | ErrMsg : "+ e.getErrMsg()+ " | ErrorCode : "+e.getErrorCode()+" | HttpStatusCode : "+e.getHttpStatusCode() );
                //errors.add(String.valueOf(parameters.get("symbol")));
                parameters.clear();
            }
        });
        System.out.println("Cron Finished at "+ new Date());
        System.out.println(errors);
    }

    private static void increasePositionAmount(Map<String, Object> parameters, String side, SpotClient client) {
        parameters.put("side", side);
        createFuturePosition(parameters, client,0);
    }

    private static boolean isPositionAmountLT75Cent(String coin, PositionInfo positionInfo) {
        if((Math.abs(positionInfo.getPositionAmount())* positionInfo.getEntryPrice())/positionInfo.getLeverage() <= 0.75)
            return true;
        return false;
    }

    private static Map<String, TickerInfo> getTickerMap(ArrayList<String> symbols) {
        List<TickerInfo> tickerInfos = getTickers(symbols);
        Map<String, TickerInfo> tickerMap =tickerInfos.stream()
                .collect(Collectors.toMap(TickerInfo::getSymbol, tickerInfo -> tickerInfo));
        return tickerMap;
    }

    private static void closeFuturePosition(String coin, PositionInfo positionInfo, SpotClient client) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("symbol", coin);
        parameters.put("side", reverseSide(evaluateSide(positionInfo)));
        parameters.put("type", "MARKET");
        parameters.put("quantity", String.valueOf(Math.abs(positionInfo.getPositionAmount())));
        String result = client.createFutures().createFuturesPosition(parameters);
        System.out.println("Position Closed status for coin "+coin+" Result: "+result);
    }

    private static String reverseSide(String side) {
        if(Coins.BUY_SIDE.equalsIgnoreCase(side))
            return Coins.SELL_SIDE;
        else
            return Coins.BUY_SIDE;
    }

    private static String evaluateSide(PositionInfo positionInfo) {
        if(positionInfo.getPositionAmount() < 0)
            return Coins.SELL_SIDE;
        else
            return Coins.BUY_SIDE;
    }

    private static String getSide(PositionInfo positionInfo){
        if(positionInfo.getMarkPrice()-positionInfo.getEntryPrice()>0 && positionInfo.getUnRealizedProfit()>0){
            return Coins.BUY_SIDE;
        }else
            return Coins.SELL_SIDE;
    }
    private static double getPercentageGap(Double price1, Double price2) {
        return (100-(price1*100/price2));
    }

    private static Order createFuturePosition(Map<String, Object> parameters, SpotClient client, int retry) {
        Order order = null;
        try {
            String result = client.createFutures().createFuturesPosition(parameters);
            Gson gson= new Gson();
            order = gson.fromJson(result, Order.class);
            System.out.println("Position Creation status for coin "+parameters+" Result: "+result);
            return order;
        }catch (BinanceConnectorException e) {
            System.err.println((String) String.format("fullErrMessage: %s", e.getMessage()));
        } catch (BinanceClientException e) {
            System.err.println((String) String.format("fullErrMessage: %s \nerrMessage: %s \nerrCode: %d \nHTTPStatusCode: %d",
                    e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode()));
            if(Coins.ERROR_CODE_1111.equalsIgnoreCase(String.valueOf(e.getErrorCode())) && retry <= 1 ){
                retry += 1;
                parameters.put("quantity",reducePrecision(String.valueOf(parameters.get("quantity"))));
                order = createFuturePosition(parameters,client, retry);
                System.out.println("Position created by reducing precision for coin "+parameters+" Result: "+order);
                return order;
            }else if(Coins.ERROR_CODE_4164.equalsIgnoreCase(String.valueOf(e.getErrorCode())) && retry <= 4){
                retry += 1;
                parameters.put("quantity",doubleQuantity(String.valueOf(parameters.get("quantity"))));
                System.out.println("Position Retrying by doubling quantity for coin "+parameters+" Result: "+order);
                order = createFuturePosition(parameters,client, retry);
                System.out.println("Position created by doubling quantity for coin "+parameters+" Result: "+order);
                return order;
            }
        }
        return  order;
    }

    private static String doubleQuantity(String quantity) {
        return String.valueOf(Double.valueOf(quantity)*2);
    }

    private static Map<String, Object> updateParameters(Map<String, Object> parameters,String coin) {
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

    private static Sentiment getSentiment(String coin) {
        TickerInfo tickerInfo = getTicker(coin);
        Sentiment sentiment = new Sentiment();
        sentiment.setType(Coins.TYPE_MARKET);
        sentiment.setQuantity(getQuantity(coin,tickerInfo.getLastPrice()));
        sentiment.setSide(Coins.BUY_SIDE);
        if(tickerInfo.getPriceChangePercent() < 0.0 && Math.abs(tickerInfo.getPriceChangePercent())>=Coins.PRICE_CHANGE_PERCENTAGE_THRESHOLD)
            sentiment.setSide(Coins.SELL_SIDE);
        if(Coins.BUY_SIDE.equalsIgnoreCase(sentiment.getSide()))
                sentiment.setStopPrice(String.valueOf(tickerInfo.getLowPrice()));
        else
                sentiment.setStopPrice(String.valueOf(tickerInfo.getHighPrice()));
        sentiment.setPrice(String.valueOf(tickerInfo.getLastPrice()));
        sentiment.setTimeInForce(Coins.TIME_IN_FORCE);
        return  sentiment;
    }

    private static String getQuantity(String coin, Double lastPrice) {
        double result = 25.0 / lastPrice;
        DecimalFormat decimalFormat = null;
        if(Arrays.asList(Coins.ZERO_DIGIT_FUTURE_USDT_COINS).contains(coin)){
            return String.valueOf((int) result);
        }
        if(Arrays.asList(Coins.ONE_DIGIT_FUTURE_USDT_COINS).contains(coin)){
            decimalFormat = new DecimalFormat("#.#");
        }
        else if(Arrays.asList(Coins.TWO_DIGIT_FUTURE_USDT_COINS).contains(coin)){
            decimalFormat = new DecimalFormat("#.##");
        }else
            decimalFormat = new DecimalFormat("#.###");
        return decimalFormat.format(result);
    }
    private static String reducePrecision(String price) {
        return price.substring(0, price.length() - 1);
    }
    private static List<TickerInfo>  getTickers(List<String> symbols) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        SpotClient client = new SpotClientImpl(PrivateConfig.TAA_API_KEY, PrivateConfig.TAA_SECRET_KEY);
        Market market = client.createMarket();
        parameters.put("symbols", symbols);// Max 100 Symbols
        String result = market.ticker(parameters);
        System.out.println(result);
        Type orderListType = new TypeToken<List<TickerInfo>>() {}.getType();
        Gson gson = new Gson();
        List<TickerInfo> tickerInfo = gson.fromJson(result, orderListType);
        return tickerInfo;
    }

    private static TickerInfo  getTicker(String symbol) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        SpotClient client = new SpotClientImpl(PrivateConfig.TAA_API_KEY, PrivateConfig.TAA_SECRET_KEY);
        Market market = client.createMarket();
        parameters.put("symbol", symbol);// Max 100 Symbols
        String result = market.ticker(parameters);
        System.out.println(result);
        Gson gson = new Gson();
        TickerInfo tickerInfo = gson.fromJson(result, TickerInfo.class);
        return tickerInfo;
    }
}
