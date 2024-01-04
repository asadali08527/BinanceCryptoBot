package com.bcb.futures.manager;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bcb.client.SpotClient;
import com.bcb.exceptions.BinanceClientException;
import com.bcb.exceptions.BinanceConnectorException;
import com.bcb.trade.constants.Coins;
import com.bcb.trade.util.CoinUtil;
import com.bcb.transfer.OpenOrderInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

class FutureOrderManager extends ExceptionManager{
	private static final String REDUCING_PRECISION_MESSAGE = "Position Retried by reducing precision for coin ";
	private static final String DOUBLING_QUANTITY_MESSAGE = "Position Retried by doubling quantity for coin ";
	private static final String INCREASING_QUANTITY_MESSAGE = "Position Retried by increasing quantity for coin ";
	
	
	private static FutureOrderManager instance;

    private final SpotClient client;

    FutureOrderManager(SpotClient client) {
        this.client = client;
    }

    public static FutureOrderManager getInstance(SpotClient client) {
        if (instance == null) {
            instance = new FutureOrderManager(client);
        }
        return instance;
    }

    public void createFuturePosition(Map<String, Object> parameters, int retry) {
        try {
            String result = client.createFutures().createFuturesPosition(parameters);
            FutureOrderScheduler.processed.add(String.valueOf(parameters.get("symbol")));
            System.out.println("Position Creation status for coin " + parameters + " Result: " + result);
        } catch (BinanceConnectorException e) {
            handleConnectorException(e);
        } catch (BinanceClientException e) {
            String errorCode = String.valueOf(e.getErrorCode());
        	if (Coins.ERROR_CODE_1111.equalsIgnoreCase(errorCode) && retry <= 1) {
                retryAndLog(parameters, retry, REDUCING_PRECISION_MESSAGE);
            } else if (Coins.ERROR_CODE_4164.equalsIgnoreCase(errorCode) && retry <= 4) {
                retryAndLog(parameters, retry, DOUBLING_QUANTITY_MESSAGE);
            } else if (Coins.ERROR_CODE_4003.equalsIgnoreCase(errorCode) && retry <= 2) {
                retryAndLog(parameters, retry, INCREASING_QUANTITY_MESSAGE);
            } else
            	handleClientException(parameters, e, retry);
        } catch (Exception e) {
            handleGenericException(parameters, e);
        }
    }
    protected void retryAndLog(Map<String, Object> parameters, int retry, String logMessage) {
        retry += 1;
        parameters.put("quantity", CoinUtil.adjustPrecision(String.valueOf(parameters.get("quantity"))));
        parameters.remove("timestamp");
        parameters.remove("signature");
        createFuturePosition(parameters, retry);
        System.out.println(logMessage + parameters);
    }


	public String getFuturesOpenOrders(Map<String, Object> parameters) {
		try {
			return client.createFutures().getFuturesOpenOrders(parameters);
		} catch (Exception e) {
			e.printStackTrace(); 
			return ""; 
		}
	}

	public List<OpenOrderInfo> getOpenOrders() {
        Gson gson = new Gson();
        Map<String,Object> parameters = new LinkedHashMap<>();
        String result = client.createFutures().getFuturesOpenOrders(parameters);
        Type orderListType = new TypeToken<List<OpenOrderInfo>>() {}.getType();
        List<OpenOrderInfo> openOrderInfos = gson.fromJson(result, orderListType);
        return openOrderInfos;
    }

}
