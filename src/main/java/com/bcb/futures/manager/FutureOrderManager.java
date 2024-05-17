package com.bcb.futures.manager;

import java.lang.reflect.Type;
import java.util.Date;
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
import com.bcb.transfer.PositionInfo;
import com.bcb.transfer.TickerInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

class FutureOrderManager extends ExceptionManager {
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
			createPosition(parameters, retry);

		} catch (BinanceConnectorException e) {
			handleConnectorException(e);
		} catch (BinanceClientException e) {
			String errorCode = String.valueOf(e.getErrorCode());
			if (Coins.ERROR_CODE_1111.equalsIgnoreCase(errorCode) && retry <= 5) {
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

	private void createPosition(Map<String, Object> parameters, int retry) {
		String result = client.createFutures().createFuturesPosition(parameters);
		Gson gson = new Gson();
		Type orderListType = new TypeToken<OpenOrderInfo>() {
		}.getType();
		OpenOrderInfo openOrderInfos = gson.fromJson(result, orderListType);
		FutureOrderScheduler.openOrders.add(openOrderInfos);
		FutureOrderScheduler.processed.add(String.valueOf(parameters.get("symbol")));
		System.out.println("Position Creation status for coin " + parameters + " Result: " + result);
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
		Map<String, Object> parameters = new LinkedHashMap<>();
		// parameters.put("timestamp",new Date().getTime()+"");
		String result = client.createFutures().getFuturesOpenOrders(parameters);
		Type orderListType = new TypeToken<List<OpenOrderInfo>>() {
		}.getType();
		List<OpenOrderInfo> openOrderInfos = gson.fromJson(result, orderListType);
		return openOrderInfos;
	}

	public void createFutureOpenLimitOrder(Map<String, Object> parameters, String coin, PositionInfo positionInfo, List<OpenOrderInfo> openOrders) {
		createFutureOpenOrder(coin, positionInfo, positionInfo.getEntryPrice(), 0,openOrders);
	}

	public void createFutureOpenOrder(String coin, PositionInfo positionInfo, double entryPrice, int retry, List<OpenOrderInfo> openOrders)
			throws BinanceConnectorException, BinanceClientException {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("symbol", coin);
		if(coin.endsWith("USDC"))
			parameters.put("side", CoinUtil.reverseSide(String.valueOf(parameters.get("side"))));
		else
			parameters.put("side", CoinUtil.reverseSide(CoinUtil.evaluateSide(positionInfo)));
		parameters.put("type", "STOP_MARKET");
		parameters.put("quantity", String.valueOf(Math.abs(positionInfo.getPositionAmount()-CoinUtil.openOrderQuantity(coin, openOrders))));
		parameters.put("stopPrice", CoinUtil.addOrReduceOneBasisPoint(entryPrice, false));
		parameters.put("timeInForce", Coins.TIME_IN_FORCE);
		parameters.put("closePosition", "false");
		parameters.put("newOrderRespType", "ACK");
		parameters.put("reduceOnly", "true");
		System.out.println("Creating Open Limit Order for : " + parameters);
		try {
			retry += 1;
			createPosition(parameters, 0);
		} catch (BinanceConnectorException e) {
			handleConnectorException(e);
		} catch (BinanceClientException e) {
			String errorCode = String.valueOf(e.getErrorCode());
			if (Coins.ERROR_CODE_1111.equalsIgnoreCase(errorCode) && retry <= 2) {
				reducePrecisionAndRetry(parameters, positionInfo, retry,openOrders);
			}
		} catch (Exception e) {
			handleGenericException(parameters, e);
		}
	}

	private void reducePrecisionAndRetry(Map<String, Object> parameters, PositionInfo positionInfo, int retry, List<OpenOrderInfo> openOrders) {
		double price = CoinUtil.reducePrecision(Double.parseDouble(String.valueOf(parameters.get("stopPrice"))));
		createFutureOpenOrder(String.valueOf(parameters.get("symbol")), positionInfo, price, retry,openOrders);
	}

}
