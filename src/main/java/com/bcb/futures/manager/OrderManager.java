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
		List<OpenOrderInfo> openOrders = filterOpenOrders(coin, openOrderInfoList);
		double quantity = calculateOrderQuantity(coin, positionInfo, openOrders, oppositePositionInfo);

		if (quantity <= Coins.MINIMUM_OPEN_ORDER_QUANTITY) {
			System.out.printf("Skipping open order creation for coin: %s due to insufficient quantity (%.2f).%n", coin,
					quantity);
			return;
		}

		parameters.put("symbol", coin);
		String side = CoinUtil.reverseSide(CoinUtil.evaluateSide(positionInfo));
		parameters.put("side", side);
		parameters.put("type", "STOP_MARKET");

		double stopPrice = CoinUtil.addOrReduceOneBasisPoint(positionInfo.getEntryPrice(), false);
		stopPrice = adjustStopPrice(side, openOrders, stopPrice);

		if (!openOrders.isEmpty()) {
			quantity += calculateTotalOpenOrderQuantity(openOrders);
			deleteFuturesOpenOrder(coin);
		}

		parameters.put("quantity", String.format("%.2f", quantity));
		parameters.put("stopPrice", Double.parseDouble(String.valueOf(PrecisionAdjuster.adjustPrecision(stopPrice))));
		parameters.put("timeInForce", Coins.TIME_IN_FORCE);
		parameters.put("closePosition", "false");
		parameters.put("newOrderRespType", "ACK");
		parameters.put("reduceOnly", "true");

		String result = createOrder(parameters, 0);
		System.out.printf("Open Limit Order Result for coin %s: %s%n", coin, result);
	}

	public List<OpenOrderInfo> filterOpenOrders(String coin, List<OpenOrderInfo> openOrderInfoList) {
		return openOrderInfoList.stream().filter(order -> order.getSymbol().equalsIgnoreCase(coin))
				.collect(Collectors.toList());
	}

	private double calculateTotalOpenOrderQuantity(List<OpenOrderInfo> openOrders) {
		return openOrders.stream().mapToDouble(order -> Double.parseDouble(order.getOrigQty())).sum();
	}

	private double getAverageStopPrice(List<OpenOrderInfo> openOrders) {
		return openOrders.stream()
				.mapToDouble(order -> order.getStopPrice() != null ? Double.valueOf(order.getStopPrice()) : 0.0)
				.average().orElse(0);
	}

	private double adjustStopPrice(String side, List<OpenOrderInfo> openOrders, double stopPrice) {
		if (Coins.SELL_SIDE.equals(side)) {
			return modifyStopPrice(stopPrice, openOrders, 1);
		} else if (Coins.BUY_SIDE.equals(side)) {
			return modifyStopPrice(stopPrice, openOrders, -1);
		}
		return stopPrice;
	}

	private double modifyStopPrice(double stopPrice, List<OpenOrderInfo> openOrders, int direction) {
		if (!openOrders.isEmpty()) {
			double averageStopPrice = getAverageStopPrice(openOrders);
			return stopPrice + direction * (Math.abs(stopPrice - averageStopPrice)/2 );
		} else {
			return stopPrice + direction * (stopPrice / 200);
		}
	}

	public double calculateOrderQuantity(String coin, PositionInfo positionInfo, List<OpenOrderInfo> openOrders,
			PositionInfo oppositePositionInfo) {

		double openOrderQuantity = getTotalOpenOrderQuantity(openOrders);
		double quantity = Double.valueOf(String.format("%.3f", (Math.abs(positionInfo.getPositionAmount()) - openOrderQuantity)));

		if (quantity <= Coins.MINIMUM_OPEN_ORDER_QUANTITY) {
			return quantity; // Insufficient quantity
		}

		return adjustForTSuffix(quantity, positionInfo, oppositePositionInfo);
	}

	private double getTotalOpenOrderQuantity(List<OpenOrderInfo> openOrders) {
		return openOrders.isEmpty() ? 0
				: openOrders.stream().mapToDouble(order -> Double.parseDouble(order.getOrigQty())).sum();
	}

	private double adjustForTSuffix(double quantity, PositionInfo positionInfo, PositionInfo oppositePositionInfo) {
		if (positionInfo.getSymbol().endsWith("USDT") && oppositePositionInfo != null) {
			double oppositePositionAmount = Math.abs(oppositePositionInfo.getPositionAmount());
			if (quantity >= oppositePositionAmount) {
				quantity -= oppositePositionAmount;
			}
		}
		return quantity;
	}

	protected String retryAndLog(Map<String, Object> parameters, int retry, String logMessage) {
	    if (retry % 2 == 0) {
	        updateStopPrice(parameters);
	    } else {
	        updateQuantity(parameters);
	    }

	    cleanOrderParameters(parameters);
	    logRetry(retry, logMessage);

	    return createOrder(parameters, retry + 1);
	}

	private void updateStopPrice(Map<String, Object> parameters) {
	    double price = CoinUtil.addOrReduceOneBasisPoint(
	            Double.parseDouble(String.valueOf(parameters.get("stopPrice"))), true);
	    parameters.put("stopPrice", PrecisionAdjuster.adjustPrecision(price));
	}

	private void updateQuantity(Map<String, Object> parameters) {
	    parameters.put("quantity", String.valueOf(Double.parseDouble((String) parameters.get("quantity"))));
	}

	private void cleanOrderParameters(Map<String, Object> parameters) {
	    parameters.remove("timestamp");
	    parameters.remove("signature");
	}

	private void logRetry(int retry, String logMessage) {
	    System.out.printf("Retry #%d: %s%n", retry + 1, logMessage);
	}

	private String createOrder(Map<String, Object> parameters, int retry) {
	    System.out.println("Creating Open Limit Order for: " + parameters);
	    try {
	        return client.createFutures().createFuturesPosition(parameters);
	    } catch (BinanceConnectorException e) {
	        handleConnectorException(e);
	    } catch (BinanceClientException e) {
	        return handleBinanceClientException(parameters, e, retry);
	    } catch (Exception e) {
	        handleGenericException(parameters, e);
	    }
	    return null;
	}

	private String handleBinanceClientException(Map<String, Object> parameters, BinanceClientException e, int retry) {
	    String errorCode = String.valueOf(e.getErrorCode());

	    if (Coins.ERROR_CODE_1111.equalsIgnoreCase(errorCode) && retry <= 20) {
	        return retryAndLog(parameters, retry, REDUCING_PRECISION_MESSAGE);
	    } else if (Coins.ERROR_CODE_4164.equalsIgnoreCase(errorCode) && retry <= 4) {
	        return doubleQuantityAndRetry(parameters, retry, DOUBLING_QUANTITY_MESSAGE);
	    } else if (Coins.ERROR_CODE_4003.equalsIgnoreCase(errorCode) && retry <= 2) {
	        return retryAndLog(parameters, retry, INCREASING_QUANTITY_MESSAGE);
	    } else {
	        handleClientException(parameters, e, retry);
	    }
	    return null;
	}

	protected String doubleQuantityAndRetry(Map<String, Object> parameters, int retry, String logMessage) {
	    retry++;
	    double quantity = Double.parseDouble(String.valueOf(parameters.get("quantity")));
	    quantity += 1;

	    parameters.put("quantity", CoinUtil.adjustPrecision(String.valueOf(quantity)));
	    cleanOrderParameters(parameters);

	    System.out.println(logMessage + parameters);
	    return createOrder(parameters, retry + 1);
	}


	public List<OpenOrderInfo> getOpenOrder(String coin, List<OpenOrderInfo> openOrders) {
		List<OpenOrderInfo> openOrderInfoList = openOrders.stream().filter(f -> f.getSymbol().equalsIgnoreCase(coin))
				.collect(Collectors.toList());
		return openOrderInfoList;
	}

	/**
	 * HANDLE Creating Open Limit Order for : {closePosition=false, symbol=DOGEUSDC,
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
		List<OpenOrderInfo> openOrderInfos = FutureOrderManager.getInstance(this.client).getFuturesOpenOrders(coin);
		openOrderInfos.forEach(f -> {
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
