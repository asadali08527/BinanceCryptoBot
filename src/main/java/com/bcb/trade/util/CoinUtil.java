package com.bcb.trade.util;

import com.bcb.enums.MarketType;
import com.bcb.trade.constants.Coins;
import com.bcb.trade.sentiment.MarketSentimentAnalyzer;
import com.bcb.transfer.OpenOrderInfo;
import com.bcb.transfer.PositionInfo;
import com.bcb.transfer.Sentiment;
import com.bcb.transfer.TickerInfo;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CoinUtil {

	public static List<String> getAllFutureCoinsByTypeAndCategory(String[]... coinArrays) {
		return Stream.of(coinArrays).flatMap(Arrays::stream).collect(Collectors.toList());
	}

	public static String getQuantity(String coin, Double lastPrice) {
		double result = Coins.BASE_LEVERAGE_12 / lastPrice;
		DecimalFormat decimalFormat = null;
		if (Arrays.asList(Coins.ZERO_DIGIT_FUTURE_USDT_COINS).contains(coin)) {
			return String.valueOf((int) result);
		}
		if (Arrays.asList(Coins.ONE_DIGIT_FUTURE_USDT_COINS).contains(coin)) {
			decimalFormat = new DecimalFormat("#.#");
		} else if (Arrays.asList(Coins.TWO_DIGIT_FUTURE_USDT_COINS).contains(coin)) {
			decimalFormat = new DecimalFormat("#.##");
		} else
			decimalFormat = new DecimalFormat("#.###");
		return nonZeroQuantity(decimalFormat.format(result));
	}

	private static String nonZeroQuantity(String quantity) {
		if (quantity.equalsIgnoreCase("0") || quantity.equalsIgnoreCase("0.") || quantity.equalsIgnoreCase("0.0")) {
			return "0.01";
		} else
			return quantity;
	}

	public static String adjustPrecision(String qty) {
		String quantity = qty.contains(".") ? qty.substring(0, qty.length() - 1)
				: String.valueOf(Double.valueOf(qty) * 2);
		quantity = quantity.equalsIgnoreCase("0") || quantity.equalsIgnoreCase("0.") || quantity.equalsIgnoreCase("0.0") || quantity.equalsIgnoreCase("0.00")? String.valueOf(Double.valueOf(qty) * 2) : quantity;
		return nonZeroQuantity(quantity);
	}

	public static void handleException(List<String> errors, String coin, Exception exception) {
		System.err.println((String) String.format("fullErrMessage: %s", exception.getMessage()));
		errors.add(coin + " : FullErrMsg : " + exception.getMessage());
	}

	public static String increaseByPoint1(String quantity) {
		if (quantity.equalsIgnoreCase("0.0"))
			return "0.25";
		return "0.5";
	}

	public static String doubleQuantity(String quantity) {
		return String.valueOf(Double.valueOf(quantity) * 2);
	}

	public static String reverseSide(String side) {
		return Coins.BUY_SIDE.equalsIgnoreCase(side) ? Coins.SELL_SIDE : Coins.BUY_SIDE;
	}

	public static List<String> getAllFutureCoinsByTypeAndCategory() {
		return getAllFutureCoinsByTypeAndCategory(Coins.FUTURE_USDT_COINS_FAVOURITE,
				Coins.FUTURE_USDT_COINS_BY_METAVERSE_AND_GAMING_COINS, Coins.FUTURE_USDT_COINS_BY_PRICE_AND_MARKETCAP,
				Coins.FUTURE_USDT_COINS_BY_TYPE_AND_GROWTH, Coins.FUTURE_USDT_COINS_BY_LAYER1_BLOCKCHAIN,
				Coins.FUTURE_USDT_COINS_BY_PRIVACY_COINS, Coins.FUTURE_USDT_COINS_BY_MEME_COINS,
				Coins.FUTURE_USDT_COINS_BY_EXCHANGE_ISSUED_COINS, Coins.FUTURE_USDT_COINS_BY_STABLE_COINS);
	}

	public static String evaluateSide(PositionInfo positionInfo) {
		return positionInfo.getPositionAmount() < 0 ? Coins.SELL_SIDE : Coins.BUY_SIDE;
	}

	public static double getPercentageGap(Double price1, Double price2) {
		return 100 - (price1 * 100 / price2);
	}

	public static Map<String, Object> updateParameters(String coin, Map<String, TickerInfo> tickerMap) {
		Map<String, Object> params = new HashMap<>();
		if (tickerMap==null || tickerMap.isEmpty()) {
			params.put("symbol", coin);
			params.put("type", MarketType.MARKET.toString());
			params.put("quantity", CoinUtil.getQuantity(coin, MarketSentimentAnalyzer.getTicker(coin).getLastPrice()));
			System.out.println("Parameters: " + params);
			return params;
		} else {
			Sentiment sentiment = MarketSentimentAnalyzer.getSentiment(coin, MarketType.LIMIT, tickerMap);
			if (sentiment == null || sentiment.getSide() == null
					|| Coins.HOLD_SIDE.equalsIgnoreCase(sentiment.getSide())) {
				return null;
			}
			params.put("symbol", sentiment.getSymbol());
			params.put("side", sentiment.getSide());
			params.put("type", sentiment.getType());
			params.put("quantity", CoinUtil.getQuantity(sentiment.getSymbol(), Double.valueOf(sentiment.getPrice())));
			params.put("price", sentiment.getPrice());
			params.put("timeInForce", sentiment.getTimeInForce());
			params.put("closePosition", sentiment.getClosePosition());
			params.put("newOrderRespType", sentiment.getNewOrderRespType()); // Set the response type (ACK, RESULT,
																				// FULL)
			// params.put("timestamp", System.currentTimeMillis());
			System.out.println("Parameters: " + params);
			return params;
		}
	}

	public static boolean checkIfCoolingPeriodPassed(Date pauseTimefor2Hrs) {
		if (pauseTimefor2Hrs == null)
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

	public static List<PositionInfo> getOpenPosition(String coin, List<PositionInfo> openPositions) {
		return openPositions.stream().filter(f -> f.getSymbol().equalsIgnoreCase(coin)).collect(Collectors.toList());
	}

	public static Map<String, TickerInfo> sortByPriceChangePercent(Map<String, TickerInfo> tickerInfoMap,
			boolean ascending) {
		Comparator<Map.Entry<String, TickerInfo>> comparator = Comparator
				.comparingDouble(entry -> entry.getValue().getPriceChangePercent());

		if (!ascending) {
			comparator = comparator.reversed();
		}

		return tickerInfoMap.entrySet().stream().sorted(comparator)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	public static Map<String, TickerInfo> keepAlternatingEntries(Map<String, TickerInfo> tickerInfoMap) {
		List<Map.Entry<String, TickerInfo>> sortedEntries = new ArrayList<>(tickerInfoMap.entrySet());
		sortedEntries.sort(Comparator.comparingDouble(entry -> entry.getValue().getPriceChangePercent()));

		Map<String, TickerInfo> alternatingEntriesMap = new LinkedHashMap<String, TickerInfo>();

		int halfSize = sortedEntries.size() / 2;
		for (int i = 0; i < halfSize; i++) {
			alternatingEntriesMap.put(sortedEntries.get(i).getKey(), sortedEntries.get(i).getValue());
			alternatingEntriesMap.put(sortedEntries.get(sortedEntries.size() - 1 - i).getKey(),
					sortedEntries.get(sortedEntries.size() - 1 - i).getValue());
		}

		return alternatingEntriesMap;
	}

	public static boolean openOrderExist(String coin, List<OpenOrderInfo> openOrders) {
		List<OpenOrderInfo> openOrderInfo = openOrders.stream().filter(f -> f.getSymbol().equalsIgnoreCase(coin))
				.collect(Collectors.toList());
		return !openOrderInfo.isEmpty();
	}

	private static int getMaxThreshold(Integer downMovement, Integer upMovement) {
		return downMovement >= upMovement ? Coins.MAX_OPEN_ORDER_AND_OPEN_POSITION_THRESHOLD * 2
				: Coins.MAX_OPEN_ORDER_AND_OPEN_POSITION_THRESHOLD;
	}

	public static boolean shouldCreateOrder(List<OpenOrderInfo> openOrders, List<PositionInfo> openPositions,
			Integer downMovement, Integer upMovement) {
		int maxThreshold = getMaxThreshold(downMovement, upMovement);
		int totalOrdersAndPositions = openOrders.size() + openPositions.size();
		return (openOrders.size() <= Coins.OPEN_ORDER_THRESHOLD
				|| openPositions.size() <= Coins.OPEN_POSITION_THRESHOLD) && totalOrdersAndPositions < maxThreshold;
	}

	public static boolean openPositionExist(String coin, List<PositionInfo> openPositions) {
		List<PositionInfo> positionInfos = getOpenPosition(coin, openPositions);
		return positionInfos.isEmpty() ? false : true;
	}
}
