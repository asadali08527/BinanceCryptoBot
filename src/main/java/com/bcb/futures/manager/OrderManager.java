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
import com.bcb.utils.PrecisionAdjuster;

public class OrderManager extends ExceptionManager {
	private static final String REDUCING_PRECISION_MESSAGE = "Position Retried by reducing precision for coin ";
	private static final String DOUBLING_QUANTITY_MESSAGE = "Position Retried by doubling quantity for coin ";
	private static final String INCREASING_QUANTITY_MESSAGE = "Position Retried by increasing quantity for coin ";

	private final SpotClient client;

	public OrderManager(SpotClient client) {
		this.client = client;
	}

	public void createFutureOpenOrder(String coin, PositionInfo positionInfo, List<OpenOrderInfo> openOrderInfoList,
			PositionInfo oppositePositionInfo) throws BinanceConnectorException, BinanceClientException {
		Map<String, Object> parameters = new HashMap<>();
		List<OpenOrderInfo> openOrders = openOrderInfoList.stream()
				.filter(order -> order.getSymbol().equalsIgnoreCase(coin)).collect(Collectors.toList());
		double quantity = calculateOrderQuantity(coin, positionInfo, openOrders, oppositePositionInfo);

		if (quantity <= 0) {
			System.out.printf("Skipping open order creation for coin: %s due to insufficient quantity (%.2f).%n", coin,
					quantity);
			return;
		}

		// Prepare order parameters
		parameters.put("symbol", coin);
		String side = CoinUtil.reverseSide(CoinUtil.evaluateSide(positionInfo));
		parameters.put("side", side);
		parameters.put("type", "STOP_MARKET");
		double stopPrice = CoinUtil.addOrReduceOneBasisPoint(positionInfo.getEntryPrice(), false);
		if (Coins.SELL_SIDE.equals(side)) {
			if (!openOrders.isEmpty()) {
				double openOrderQuantity = openOrders.stream().mapToDouble(order -> Double.parseDouble(order.getOrigQty()))
						.sum();
				quantity +=openOrderQuantity;
				double average = openOrders.stream()
                        .mapToDouble(order -> order.getStopPrice() != null ? Double.valueOf(order.getStopPrice()) : 0.0)
                        .average()
                        .orElse(0);
				stopPrice = stopPrice + (Math.abs(stopPrice - average) / 4);
				deleteFuturesOpenOrder(coin);
			} else if (openOrders.isEmpty()) {
				stopPrice = stopPrice + (stopPrice / 200);
			}
		} else if (Coins.BUY_SIDE.equals(side)) {
			if (!openOrders.isEmpty()) {
				double openOrderQuantity = openOrders.stream().mapToDouble(order -> Double.parseDouble(order.getOrigQty()))
						.sum();
				quantity +=openOrderQuantity;
				double average = openOrders.stream()
                        .mapToDouble(order -> order.getStopPrice() != null ? Double.valueOf(order.getStopPrice()) : 0.0)
                        .average()
                        .orElse(0);
				stopPrice = stopPrice - (Math.abs(stopPrice - average) / 4);
				deleteFuturesOpenOrder(coin);
			} else if (openOrders.isEmpty()) {
				stopPrice = stopPrice - (stopPrice / 200);
			}
		}
		parameters.put("quantity", String.format("%.2f", quantity));
		parameters.put("stopPrice", Double.parseDouble(String.valueOf(PrecisionAdjuster.adjustPrecision(stopPrice))));
		// String.format("%.3f",
		// CoinUtil.addOrReduceOneBasisPoint(positionInfo.getEntryPrice(), true)));
		parameters.put("timeInForce", Coins.TIME_IN_FORCE);
		parameters.put("closePosition", "false");
		parameters.put("newOrderRespType", "ACK");
		parameters.put("reduceOnly", "true");

		// Execute order creation
		String result = createOrder(parameters, 0);
		System.out.printf("Open Limit Order Result for coin %s: %s%n", coin, result);
	}

	public double calculateOrderQuantity(String coin, PositionInfo positionInfo, List<OpenOrderInfo> openOrders,
			PositionInfo oppositePositionInfo) {

		double quantity = 0;

		if (!openOrders.isEmpty()) {
			double openOrderQuantity = openOrders.stream().mapToDouble(order -> Double.parseDouble(order.getOrigQty()))
					.sum();

			quantity = Math.abs(positionInfo.getPositionAmount()) - Math.abs(openOrderQuantity);
			//System.out.printf("Quantity difference: %.2f, for open order coin: %s%n", quantity, coin);

			if (quantity < 1) {
				return quantity; // Insufficient quantity
			}

			// Adjust quantity for coins with "T" suffix
			if (positionInfo.getSymbol().endsWith("T") && oppositePositionInfo != null) {
				double oppositePositionAmount = Math.abs(oppositePositionInfo.getPositionAmount());
				if (quantity >= oppositePositionAmount) {
//					if (quantity <= oppositePositionAmount) {
//						return 0; // Skip order creation
//					}
					quantity = quantity - oppositePositionAmount;
				}
			}
		} else {
			quantity = Math.abs(positionInfo.getPositionAmount());

			// Adjust quantity for coins with "T" suffix
			if (positionInfo.getSymbol().endsWith("T") && oppositePositionInfo != null) {
				double oppositePositionAmount = Math.abs(oppositePositionInfo.getPositionAmount());
				if (quantity >= oppositePositionAmount) {
					quantity = quantity - oppositePositionAmount;
				}
			}
		}

		return quantity;
	}

	protected String retryAndLog(Map<String, Object> parameters, int retry, String logMessage) {
		// Adjust parameters based on the retry count
		if (retry % 2 == 0) {
			double price = CoinUtil
					.addOrReduceOneBasisPoint(Double.valueOf(String.valueOf(parameters.get("stopPrice"))), true);
			double stopPrice = Double.parseDouble(String.valueOf(PrecisionAdjuster.adjustPrecision(price)));
			parameters.put("stopPrice", stopPrice);
		} else {
			parameters.put("quantity", String.valueOf(Double.parseDouble((String) parameters.get("quantity"))));
		}

		// Remove unnecessary parameters
		parameters.remove("timestamp");
		parameters.remove("signature");

		// Log retry action
		System.out.printf("Retry #%d: %s%n", retry + 1, logMessage);

		// Increment retry count and create the order
		return createOrder(parameters, retry + 1);
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
				return doubleQuantityAndRetry(parameters, retry, DOUBLING_QUANTITY_MESSAGE);
			} else if (Coins.ERROR_CODE_4003.equalsIgnoreCase(errorCode) && retry <= 2) {
				return retryAndLog(parameters, retry, INCREASING_QUANTITY_MESSAGE);
			} else
				handleClientException(parameters, e, retry);
		} catch (Exception e) {
			handleGenericException(parameters, e);
		}
		return null;
	}

	protected String doubleQuantityAndRetry(Map<String, Object> parameters, int retry, String logMessage) {
		retry += 1;
		double quantity = Double.valueOf(String.valueOf(parameters.get("quantity")));
		quantity += 1;
		parameters.put("quantity", CoinUtil.adjustPrecision(String.valueOf(quantity)));
		parameters.remove("timestamp");
		parameters.remove("signature");
		System.out.println(logMessage + parameters);
		return createOrder(parameters, retry + 1);
	}

	public List<OpenOrderInfo> getOpenOrder(String coin, List<OpenOrderInfo> openOrders) {
		List<OpenOrderInfo> openOrderInfoList = openOrders.stream().filter(f -> f.getSymbol().equalsIgnoreCase(coin))
				.collect(Collectors.toList());
		return openOrderInfoList;
	}
	/**
	 * HANDLE 
	 * Creating Open Limit Order for : {closePosition=false, symbol=DOGEUSDC,
	 * side=BUY, stopPrice=0.391781, quantity=62.0, reduceOnly=true,
	 * newOrderRespType=ACK, type=STOP_MARKET, timeInForce=GTC} fullErrMessage:
	 * {"code":-2021,"msg":"Order would immediately trigger."} errMessage: Order
	 * would immediately trigger. errCode: -2021 HTTPStatusCode: 400
	 * {"code":-2021,"msg":"Order would immediately trigger."}{closePosition=false,
	 * symbol=DOGEUSDC, side=BUY, stopPrice=0.391781, quantity=62.0,
	 * reduceOnly=true,
	 * signature=8008fe45a6411b741bd5c26566127921a273606f163779d10f9fc5ba5b3eadd7,
	 * newOrderRespType=ACK, type=STOP_MARKET, timeInForce=GTC,
	 * timestamp=1737196736466}
	 */
	public void deleteFuturesOpenOrder(String coin) {
		List<OpenOrderInfo>  openOrderInfos = FutureOrderManager.getInstance(this.client).getFuturesOpenOrders(coin);
		openOrderInfos.forEach(f->{
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("symbol", coin);
			parameters.put("orderId", f.getOrderId());
			try {
				 this.client.createFutures().deleteFuturesOpenOrder(parameters);
			} catch (Exception e) {
				System.err.println("Error in getFuturesOpenPosition: " + e.getMessage());
			}
		});
	}
}
