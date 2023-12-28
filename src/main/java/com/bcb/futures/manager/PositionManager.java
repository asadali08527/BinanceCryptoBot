package com.bcb.futures.manager;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bcb.client.SpotClient;
import com.bcb.exceptions.BinanceClientException;
import com.bcb.exceptions.BinanceConnectorException;
import com.bcb.trade.constants.Coins;
import com.bcb.trade.util.CoinUtil;
import com.bcb.transfer.OpenOrderInfo;
import com.bcb.transfer.PositionInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class PositionManager {
	
	private static final String POSITION_INCREASED_MESSAGE = "Position Increased for ";

    private final SpotClient client;

    public PositionManager(SpotClient client) {
        this.client = client;
    }

    public boolean openOrderExist(String symbol) {
        try {
            Map<String, Object> params = new HashMap<>();
            Gson gson = new Gson();
            params.put("symbol", symbol);
            String result = client.createFutures().getFuturesOpenOrders(params);
            Type orderListType = new TypeToken<List<OpenOrderInfo>>() {}.getType();
            List<OpenOrderInfo> positionInfos = gson.fromJson(result, orderListType);
            return !positionInfos.isEmpty();
        } catch (Exception e) {
            e.printStackTrace(); 
            return false; 
        }
    }
    
    public String getFuturesOpenPosition(Map<String, Object> parameters) {
        try {
            return client.createFutures().getFuturesOpenPosition(parameters);
        } catch (Exception e) {
            System.err.println("Error in getFuturesOpenPosition: " + e.getMessage());
            return "";
        }
    }
    
    public void handleNegativePosition(Map<String, Object> parameters, String coin, PositionInfo positionInfo)
            throws BinanceConnectorException, BinanceClientException {
        if (parameters == null) {
            return;
        }

        Double unRealizedProfit = positionInfo.getUnRealizedProfit();

        if (unRealizedProfit >= 0.0) {
            if (isPositionAmountLT75Cent(coin, positionInfo) && parameters != null) {
                increasePositionAmount(parameters, Coins.SELL_SIDE);
                System.out.println(POSITION_INCREASED_MESSAGE + parameters);
            } else if (unRealizedProfit >= 1.0 && parameters != null) {
                closeAndCreatePosition(coin, positionInfo, parameters);
            }
        } else if (unRealizedProfit < 0) {
            handleNegativeUnrealizedProfitForSellOrder(parameters, coin, positionInfo);
        }
    }

    public void handlePositivePosition(Map<String, Object> parameters, String coin, PositionInfo positionInfo)
            throws BinanceConnectorException, BinanceClientException {
        Double unRealizedProfit = positionInfo.getUnRealizedProfit();

        if (unRealizedProfit >= 1.0 && parameters != null) {
            closeAndCreatePosition(coin, positionInfo, parameters);
        } else if (unRealizedProfit >= 1.0) {
            closeFuturePosition(coin, positionInfo);
        } else if (!FutureOrderScheduler.pauseNewOrderFor2Hrs && unRealizedProfit >= 0.0 && isPositionAmountLT75Cent(coin, positionInfo) && parameters != null) {
            increasePositionAmount(parameters, Coins.BUY_SIDE);
            System.out.println(POSITION_INCREASED_MESSAGE + parameters);
        } else if (unRealizedProfit < 0) {
            handleNegativeUnrealizedProfitForBuyOrder(parameters, coin, positionInfo);
        }
    }

    private void closeAndCreatePosition(String coin, PositionInfo positionInfo, Map<String, Object> parameters)
            throws BinanceConnectorException, BinanceClientException {
        closeFuturePosition(coin, positionInfo);
        FutureOrderManager.getInstance(this.client).createFuturePosition(parameters, 0);
    }

    private void increasePositionAmount(Map<String, Object> parameters, String side)
            throws BinanceConnectorException, BinanceClientException {
        parameters.put("side", side);
        FutureOrderManager.getInstance(this.client).createFuturePosition(parameters, 0);
    }

    private void handleNegativeUnrealizedProfitForSellOrder(Map<String, Object> parameters, String coin,
                                                            PositionInfo positionInfo) throws BinanceConnectorException, BinanceClientException {
        if (positionInfo.getUnRealizedProfit() >= 1.0 && parameters != null) {
            closeAndCreatePosition(coin, positionInfo, parameters);
        } else if (positionInfo.getUnRealizedProfit() <= -2.0) {
        	System.out.println("Closing SELL order for coin " + coin + ": " + positionInfo);
            closeFuturePosition(coin, positionInfo);
        } else if (parameters != null && CoinUtil.getPercentageGap(positionInfo.getEntryPrice(), positionInfo.getMarkPrice())
                >= Coins.PRICE_CHANGE_PERCENTAGE_THRESHOLD) {
            closeAndCreatePosition(coin, positionInfo, parameters);
        }
    }

    private void handleNegativeUnrealizedProfitForBuyOrder(Map<String, Object> parameters, String coin,
                                                           PositionInfo positionInfo) throws BinanceConnectorException, BinanceClientException {
        if (!FutureOrderScheduler.pauseNewOrderFor2Hrs && isPositionAmountLT75Cent(coin, positionInfo) && parameters != null) {
            increasePositionAmount(parameters, Coins.BUY_SIDE);
            System.out.println("Position Increased for " + parameters);
        } else if (CoinUtil.getPercentageGap(positionInfo.getLiquidationPrice(), positionInfo.getMarkPrice())
                <= Coins.POSITION_CLOSE_THRESOLD_PERCENTAGE) {
            closeFuturePosition(coin, positionInfo);
        }
    }

    private boolean isPositionAmountLT75Cent(String coin, PositionInfo positionInfo) {
        double positionAmount = Math.abs(positionInfo.getPositionAmount()) * positionInfo.getEntryPrice()
                / positionInfo.getLeverage();
        return positionAmount <= 0.75;
    }
    
    public void closeFuturePosition(String coin, PositionInfo positionInfo)
            throws BinanceConnectorException, BinanceClientException {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("symbol", coin);
        parameters.put("side", CoinUtil.reverseSide(CoinUtil.evaluateSide(positionInfo)));
        parameters.put("type", "MARKET");
        parameters.put("quantity", String.valueOf(Math.abs(positionInfo.getPositionAmount())));
        FutureOrderManager.getInstance(client).createFuturePosition(parameters,0);
    }

}