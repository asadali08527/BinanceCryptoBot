package com.bcb.futures.manager;

import java.util.HashMap;
import java.util.Map;

import com.bcb.client.SpotClient;
import com.bcb.config.PrivateConfig;
import com.bcb.exceptions.BinanceClientException;
import com.bcb.exceptions.BinanceConnectorException;
import com.bcb.impl.SpotClientImpl;
import com.bcb.trade.constants.Coins;
import com.bcb.trade.util.CoinUtil;
import com.bcb.transfer.PositionInfo;

public class OrderManager {
	private final static SpotClient client = new SpotClientImpl(PrivateConfig.TAA_API_KEY, PrivateConfig.TAA_SECRET_KEY, PrivateConfig.BASE_URLS[0]);

	public static void main(String[] args) {
	//	System.out.println(reducePrecision(80.732));
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("symbol", "XRPUSDT");
		parameters.put("side", "BUY");
		parameters.put("type", "MARKET");
		parameters.put("quantity", "1000");
		//parameters.put("stopPrice", CoinUtil.addOrReduceOneBasisPoint(80.7325, false));
		//parameters.put("timeInForce", Coins.TIME_IN_FORCE);
		parameters.put("closePosition", "false");
		parameters.put("newOrderRespType", "ACK");
		//parameters.put("reduceOnly", "true");
		
		System.out.println("Creating Open Limit Order for : " + parameters);
		System.out.println(createFuturesPosition(parameters));
		
	}
	
	private static double reducePrecision(double price) {
		int index=String.valueOf(price).indexOf(".");
		int precision = String.valueOf(price).length()-(index+1);
		double cons= Math.pow(10, precision-1);
	    return Math.round(price * cons) / cons;
	}
	
	public void createFutureOpenOrder(String coin, PositionInfo positionInfo)
			throws BinanceConnectorException, BinanceClientException {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("symbol", coin);
		parameters.put("side", CoinUtil.reverseSide(CoinUtil.evaluateSide(positionInfo)));
		parameters.put("type", "STOP_MARKET");
		parameters.put("quantity", String.valueOf(Math.abs(positionInfo.getPositionAmount())));
		parameters.put("price", CoinUtil.addOrReduceOneBasisPoint(positionInfo.getEntryPrice(), false));
		parameters.put("timeInForce", Coins.TIME_IN_FORCE);
		parameters.put("closePosition", "false");
		parameters.put("newOrderRespType", "ACK");
		parameters.put("reduceOnly", "true");
		
		System.out.println("Creating Open Limit Order for : " + parameters);
		createFuturesPosition(parameters);
	}
	public static String createFuturesPosition(Map<String, Object> parameters) {
		try {
			return client.createFutures().createFuturesPosition(parameters);
		} catch (Exception e) {
			System.err.println("Error in getFuturesOpenPosition: " + e.getMessage());
			return "";
		}
	}

}
