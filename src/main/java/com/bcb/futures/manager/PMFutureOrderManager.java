package com.bcb.futures.manager;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bcb.client.SpotClient;
import com.bcb.exceptions.BinanceClientException;
import com.bcb.exceptions.BinanceConnectorException;
import com.bcb.trade.constants.Coins;
import com.bcb.trade.util.CoinUtil;
import com.bcb.transfer.OpenOrderInfo;
import com.bcb.transfer.Order;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class PMFutureOrderManager extends ExceptionManager {
	private static final String REDUCING_PRECISION_MESSAGE = "Position Retried by reducing precision for coin ";
	private static final String DOUBLING_QUANTITY_MESSAGE = "Position Retried by doubling quantity for coin ";
	private static final String INCREASING_QUANTITY_MESSAGE = "Position Retried by increasing quantity for coin ";
	private static final String HALVING_QUANTITY_MESSAGE = "Position Retried by halving quantity for coin ";

	private static PMFutureOrderManager instance;

	private final SpotClient client;

	PMFutureOrderManager(SpotClient client) {
		this.client = client;
	}

	public static PMFutureOrderManager getInstance(SpotClient client) {
		if (instance == null) {
			instance = new PMFutureOrderManager(client);
		}
		return instance;
	}

	public void createFuturePosition(Map<String, Object> parameters, int retry) {
		try {
			String result = client.createPortfolioMarginFuture().createFuturesPosition(parameters,true);
			Gson gson = new Gson();
			Type orderListType = new TypeToken<OpenOrderInfo>() {
			}.getType();
			OpenOrderInfo openOrderInfos = gson.fromJson(result, orderListType);
			FutureOrderSchedulerTAA.openOrders.add(openOrderInfos);
			FutureOrderSchedulerTAA.processed.add(String.valueOf(parameters.get("symbol")));
			System.out.println("Position Creation status for coin " + parameters + " Result: " + result);
		} catch (BinanceConnectorException e) {
			e.printStackTrace();
			System.out.println("Issues while createting position for params: "+parameters);
			handleConnectorException(e);
		} catch (BinanceClientException e) {
			String errorCode = String.valueOf(e.getErrorCode());
			if (Coins.ERROR_CODE_1111.equalsIgnoreCase(errorCode) && retry <= 5) {
				retryAndLog(parameters, retry, REDUCING_PRECISION_MESSAGE);
			} else if (Coins.ERROR_CODE_4164.equalsIgnoreCase(errorCode) && retry <= 4) {
				retryAndLog(parameters, retry, DOUBLING_QUANTITY_MESSAGE);
			} else if (Coins.ERROR_CODE_4003.equalsIgnoreCase(errorCode) && retry <= 2) {
				retryAndLog(parameters, retry, INCREASING_QUANTITY_MESSAGE);
			}else if (Coins.ERROR_CODE_2027.equalsIgnoreCase(errorCode) && retry <= 2) {
				reduceQunatityAndRetry(parameters,retry,HALVING_QUANTITY_MESSAGE);
			}
			else
				handleClientException(parameters, e, retry);
		} catch (Exception e) {
			handleGenericException(parameters, e);
		}
	}

	private void reduceQunatityAndRetry(Map<String, Object> parameters, int retry, String logMessage) {
		retry += 1;
		Double quantity = Double.valueOf(String.valueOf(parameters.get("quantity")));
		parameters.put("quantity", String.valueOf(quantity/2));	
		parameters.remove("timestamp");
		parameters.remove("signature");
		createFuturePosition(parameters, retry);
		System.out.println(logMessage + parameters);
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
			return client.createPortfolioMarginFuture().getFuturesOpenOrders(parameters, true);
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	public List<OpenOrderInfo> getFuturesOpenOrders(String coin) {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("symbol", coin);
		try {
			Gson gson = new Gson();
			String result = client.createPortfolioMarginFuture().getFuturesOpenOrders(parameters, true);
			Type orderListType = new TypeToken<List<OpenOrderInfo>>() {
			}.getType();
			return  gson.fromJson(result, orderListType);
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	public List<OpenOrderInfo> getOpenOrders() {
		Gson gson = new Gson();
		Map<String, Object> parameters = new LinkedHashMap<>();
		String result = client.createPortfolioMarginFuture().getFuturesOpenOrders(parameters, true);
		Type orderListType = new TypeToken<List<OpenOrderInfo>>() {
		}.getType();
		List<OpenOrderInfo> openOrderInfos = gson.fromJson(result, orderListType);
		return openOrderInfos;
	}

	public List<Order> getFutureAllOrders() {
		Gson gson = new Gson();
		Map<String, Object> parameters = new LinkedHashMap<>();
		String result = client.createPortfolioMarginFuture().getFuturesAllOrders(parameters, true);
		Type orderListType = new TypeToken<List<Order>>() {
		}.getType();
		List<Order> openOrderInfos = gson.fromJson(result, orderListType);
		return openOrderInfos;
	}

}
