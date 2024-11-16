package com.bcb.futures.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.bcb.client.SpotClient;
import com.bcb.exceptions.BinanceClientException;
import com.bcb.exceptions.BinanceConnectorException;
import com.bcb.trade.constants.Coins;
import com.bcb.trade.util.CoinUtil;
import com.bcb.transfer.OpenOrderInfo;
import com.bcb.transfer.PositionInfo;

public class OrderManager extends ExceptionManager {
	private static final String REDUCING_PRECISION_MESSAGE = "Position Retried by reducing precision for coin ";
	private static final String DOUBLING_QUANTITY_MESSAGE = "Position Retried by doubling quantity for coin ";
	private static final String INCREASING_QUANTITY_MESSAGE = "Position Retried by increasing quantity for coin ";

	private final SpotClient client;

	public OrderManager(SpotClient client) {
		this.client = client;
	}

	public void createFutureOpenOrder(String coin, PositionInfo positionInfo, List<OpenOrderInfo> openOrderInfoList)
			throws BinanceConnectorException, BinanceClientException {
		Map<String, Object> parameters = new HashMap<>();
		if(openOrderInfoList.size()!=0) {
			Double openOrderQuantity = openOrderInfoList.stream().mapToDouble(m->Double.valueOf(m.getOrigQty())).sum();
			Double quantity = Math.abs(positionInfo.getPositionAmount())-openOrderQuantity;
			if(quantity>=0)
				parameters.put("quantity", String.valueOf(quantity));
			else {
				return;
			}
		}
		else
			parameters.put("quantity", String.valueOf(Math.abs(positionInfo.getPositionAmount())));
		parameters.put("symbol", coin);
		parameters.put("side", CoinUtil.reverseSide(CoinUtil.evaluateSide(positionInfo)));
		parameters.put("type", "STOP_MARKET");
		parameters.put("stopPrice", CoinUtil.addOrReduceOneBasisPoint(positionInfo.getEntryPrice(), false));		
		//parameters.put("price", CoinUtil.addOrReduceOneBasisPoint(positionInfo.getEntryPrice(), false));
		parameters.put("timeInForce", Coins.TIME_IN_FORCE);
		parameters.put("closePosition", "false");
		parameters.put("newOrderRespType", "ACK");
		parameters.put("reduceOnly", "true");
		String result = createOrder(parameters, 0);
		System.out.println("Open Limit Order Result : " + result);

	}

	protected String retryAndLog(Map<String, Object> parameters, int retry, String logMessage) {
		retry += 1;
		parameters.put("stopPrice", CoinUtil.addOrReduceOneBasisPoint(
				Double.valueOf(CoinUtil.adjustPrecision(String.valueOf(parameters.get("stopPrice")))), false));
		parameters.remove("timestamp");
		parameters.remove("signature");
		return createOrder(parameters, retry);
	}

	private String createOrder(Map<String, Object> parameters, int retry) {
		System.out.println("Creating Open Limit Order for : " + parameters);
		try {
			return client.createFutures().createFuturesPosition(parameters);
		} catch (BinanceConnectorException e) {
			handleConnectorException(e);
		} catch (BinanceClientException e) {
			String errorCode = String.valueOf(e.getErrorCode());
			if (Coins.ERROR_CODE_1111.equalsIgnoreCase(errorCode) && retry <= 20) {
				return retryAndLog(parameters, retry, REDUCING_PRECISION_MESSAGE);
			} else if (Coins.ERROR_CODE_4164.equalsIgnoreCase(errorCode) && retry <= 4) {
				return retryAndLog(parameters, retry, DOUBLING_QUANTITY_MESSAGE);
			} else if (Coins.ERROR_CODE_4003.equalsIgnoreCase(errorCode) && retry <= 2) {
				return retryAndLog(parameters, retry, INCREASING_QUANTITY_MESSAGE);
			} else
				handleClientException(parameters, e, retry);
		} catch (Exception e) {
			handleGenericException(parameters, e);
		}
		return null;
	}

	public List<OpenOrderInfo> getOpenOrder(String coin, List<OpenOrderInfo> openOrders) {
		List<OpenOrderInfo> openOrderInfoList = openOrders.stream().filter(f -> f.getSymbol().equalsIgnoreCase(coin))
				.collect(Collectors.toList());
		return openOrderInfoList;
	}

}
