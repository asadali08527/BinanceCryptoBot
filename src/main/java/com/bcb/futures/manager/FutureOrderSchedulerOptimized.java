package com.bcb.futures.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.bcb.client.SpotClient;
import com.bcb.config.PrivateConfig;
import com.bcb.enums.MarketType;
import com.bcb.impl.SpotClientImpl;
import com.bcb.trade.constants.Coins;
import com.bcb.trade.sentiment.MarketSentimentAnalyzer;
import com.bcb.trade.util.CoinUtil;
import com.bcb.trade.util.PositionCalculator;
import com.bcb.transfer.BalanceInfo;
import com.bcb.transfer.OpenOrderInfo;
import com.bcb.transfer.PositionInfo;
import com.bcb.transfer.TickerInfo;

public class FutureOrderSchedulerOptimized {
	private static final int EXECUTION_INTERVAL_SECONDS = 40;
	private static final String CRON_FINISHED_MESSAGE = "Cron Finished at ";
	private static final String TOTAL_TIME_MESSAGE = "Total Time taken to Execute The Job : ";

	private final PositionManager positionManager;
	private final FutureOrderManager futureOrderManager;
	private final WalletManager walletManager;
	private final OrderManager orderManager;

	private static List<String> processed = new ArrayList<>();
	private static List<String> errored = new ArrayList<>();
	private static boolean pauseNewOrderFor2Hrs = false;
	private static Date pauseTimefor2Hrs = null;
	List<PositionInfo> openPositions = null;
	List<PositionInfo> buyPositions = null;
	List<PositionInfo> sellPositions = null;
	BalanceInfo balanceInfo = null;
	private List<String> symbols = new ArrayList<>();
	List<PositionInfo> buyPositionsInProfit = null;
	Integer upMovement = 0;
	Integer downMovement = 0;

	public FutureOrderSchedulerOptimized() {
		SpotClient client = new SpotClientImpl(PrivateConfig.TEE_API_KEY, PrivateConfig.TEE_SECRET_KEY,
				PrivateConfig.BASE_URLS[0]);
		this.positionManager = new PositionManager(client);
		this.futureOrderManager = new FutureOrderManager(client);
		this.walletManager = new WalletManager(client);
		this.orderManager = new OrderManager(client);
	}

	public static void main(String[] args) {
		FutureOrderSchedulerOptimized scheduler = new FutureOrderSchedulerOptimized();
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(scheduler::executeOppositeStrategy, 0, EXECUTION_INTERVAL_SECONDS,
				TimeUnit.SECONDS);
	}

	public void executeOppositeStrategy() {
		if (isJobPaused()) {
			System.out.println("Job paused due to Futures Trading Quantitative Rules.");
			return;
		}

		resumeJobIfNeeded();

		initializePositionData();

		Map<String, TickerInfo> tickerMap = initializeTickerMap(openPositions);
		List<OpenOrderInfo> openOrders = futureOrderManager.getOpenOrders();
		buyPositionsInProfit = getProfitablePositions(buyPositions);

		printPositionSummary(buyPositionsInProfit);

		calculateMarketMovement(tickerMap);

		balanceInfo = walletManager.getFutureWalletBalance("USDT");
		System.out.printf("BalanceInfo: %s, UpMovement: %d, DownMovement: %d%n", balanceInfo.getAvailableBalance(),
				upMovement, downMovement);

		processBuyPositions(tickerMap, openOrders);
		processSellPositions(tickerMap, openOrders);

		printResult(new ArrayList<>(tickerMap.keySet()), new ArrayList<>(), errored, new Date());
	}

	private boolean isJobPaused() {
		return pauseNewOrderFor2Hrs && !CoinUtil.checkIfCoolingPeriodPassed(pauseTimefor2Hrs);
	}

	private void resumeJobIfNeeded() {
		if (pauseNewOrderFor2Hrs) {
			pauseNewOrderFor2Hrs = false;
			pauseTimefor2Hrs = null;
			System.out.println("Resuming job execution after cooling period.");
		}
	}

	private void initializePositionData() {
		symbols.clear();
		openPositions = positionManager.getAllOpenPositions();
		List<String> usdcSymbols = openPositions.stream().filter(position -> {
			return position.getSymbol().endsWith("USDC") && !position.getSymbol().startsWith("1000");
		}).map(m -> {
			return m.getSymbol();
		}).collect(Collectors.toList());
		List<String> usdtSymbols = usdcSymbols.stream().map(symbol -> {
			return symbol.replace("USDC", "USDT");
		}).collect(Collectors.toList());
		openPositions = openPositions.stream().filter(position -> {
			return (usdtSymbols.contains(position.getSymbol()) || position.getSymbol().endsWith("USDC"))
					&& !position.getSymbol().startsWith("1000");
		}).collect(Collectors.toList());
		openPositions = openPositions.stream().filter(f -> f.getPositionAmount() != 0.0).collect(Collectors.toList());
		symbols.addAll(usdcSymbols);
		symbols.addAll(usdtSymbols);
		buyPositions = CoinUtil.getOpenPositions(CoinUtil.openPosition(openPositions), Coins.BUY_SIDE);
		sellPositions = CoinUtil.getOpenPositions(CoinUtil.openPosition(openPositions), Coins.SELL_SIDE);

		double totalBuyAmount = buyPositions.stream().mapToDouble(CoinUtil::getPositionAmount).sum();
		double totalSellAmount = sellPositions.stream().mapToDouble(CoinUtil::getPositionAmount).sum();
		System.out.println("Buy Amount: " + totalBuyAmount);
		System.out.println("Sell Amount: " + totalSellAmount);

		int buyAggLeverage = calculateAggregateLeverage(buyPositions);
		int sellAggLeverage = calculateAggregateLeverage(sellPositions);
		System.out.println("Buy Positions Aggregiate Levarage: " + buyAggLeverage);
		System.out.println("Sell Positions Aggregiate Levarage: " + sellAggLeverage);
	}

	private List<PositionInfo> getProfitablePositions(List<PositionInfo> positions) {
		return positions.stream().filter(position -> position.getUnRealizedProfit() > 0).collect(Collectors.toList());
	}

	private void printPositionSummary(List<PositionInfo> buyPositionsInProfit) {
		System.out.printf("Buy Positions in Profit: %d%n", buyPositionsInProfit.size());
	}

	private void calculateMarketMovement(Map<String, TickerInfo> tickerMap) {
		upMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_UP);
		downMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_DOWN);
	}

	private int calculateAggregateLeverage(List<PositionInfo> positions) {
		return positions.isEmpty() ? 0
				: positions.stream().mapToInt(PositionInfo::getLeverage).sum() / positions.size();
	}

	private Map<String, TickerInfo> initializeTickerMap(List<PositionInfo> openPositions) {

//		List<String> symbols = openPositions.stream().map(PositionInfo::getSymbol).distinct()
//				.collect(Collectors.toList());
		return MarketSentimentAnalyzer.getTickers(Coins.DESC, symbols.toArray(new String[0]));
	}

	private void processBuyPositions(Map<String, TickerInfo> tickerMap, List<OpenOrderInfo> openOrders) {
		// Fetch wallet balance for USDT
		balanceInfo = walletManager.getFutureWalletBalance("USDT");

		for (Map.Entry<String, TickerInfo> entry : tickerMap.entrySet()) {
			String coin = entry.getKey();
			TickerInfo tickerInfo = entry.getValue();

			// Skip coins that don't end with "USDT"
			if (!coin.endsWith("USDT")) {
				continue;
			}

			// Get position info for the coin
			PositionInfo positionInfo = CoinUtil.getPosition(CoinUtil.getOpenPosition(coin, openPositions));

			if (positionInfo != null) {
				processExistingBuyPosition(positionInfo, tickerInfo, openOrders);
			} else {
				processNewBuyPosition(coin, tickerInfo);
			}
		}
	}

	private void processExistingBuyPosition(PositionInfo positionInfo, TickerInfo tickerInfo,
			List<OpenOrderInfo> openOrders) {
		if (isProfitablePosition(positionInfo)) {
			handleProfitableBuy(positionInfo, tickerInfo, openOrders);
		}
	}

	private void processNewBuyPosition(String coin, TickerInfo tickerInfo) {
		if (downMovement <= 2) {
			createNewBuyPosition(coin, tickerInfo);
		}
	}

	/**
	 * Checks if a position is profitable.
	 *
	 * @param positionInfo The position to check.
	 * @return True if the position is profitable, otherwise false.
	 */
	private boolean isProfitablePosition(PositionInfo positionInfo) {
		return positionInfo != null && positionInfo.getUnRealizedProfit() > 0;
	}

	private void handleProfitableBuy(PositionInfo positionInfo, TickerInfo tickerInfo, List<OpenOrderInfo> openOrders) {
		// Get USDC equivalent coin and associated position info
		String usdcCoin = positionInfo.getSymbol().replace("USDT", "USDC");
		double usdtAmount = CoinUtil.getPositionAmount(positionInfo);
		double profitPercentage = PositionCalculator.calculatePercentageProfit(positionInfo.getUnRealizedProfit(),
				usdtAmount);
		PositionInfo usdcPositionInfo = CoinUtil.getOpenPosition(usdcCoin, openPositions).stream().findFirst()
				.orElse(null);
		double usdcAmount = usdcPositionInfo != null ? CoinUtil.getPositionAmount(usdcPositionInfo) : 0;

		System.out.printf("Buy usdtAmount: %.2f, Sell usdcAmount: %.2f%n", usdtAmount, usdcAmount);

		// Skip processing if downMovement is greater than 2 or profit percentage is too
		// low
		if (downMovement > 2 || profitPercentage <= 10) {
			return;
		}

		// Handle highly profitable positions
		if (profitPercentage > 50) {
			if (usdcPositionInfo == null) {
				processNewBuyOrder(positionInfo, tickerInfo, openOrders);
			} else if (isUsdcPositionHigherOrEqual(usdcPositionInfo, positionInfo, usdcAmount, usdtAmount)) {
				handleDoubledBuyOrder(positionInfo, tickerInfo, openOrders, usdcPositionInfo);
			}
		}
	}

	private void processNewBuyOrder(PositionInfo positionInfo, TickerInfo tickerInfo, List<OpenOrderInfo> openOrders) {
		orderManager.createFutureOpenOrder(positionInfo.getSymbol(), positionInfo, openOrders, null);
		createNewBuyPosition(positionInfo.getSymbol(), tickerInfo);
		System.out.println("Processed new buy order for symbol: " + positionInfo.getSymbol());
	}

	/**
	 * Determines if USDC position amount is greater than or equal to USDT position
	 * amount.
	 *
	 * @param usdcPositionInfo USDC position info.
	 * @param positionInfo     USDT position info.
	 * @param usdcAmount       Amount of USDC position.
	 * @param usdtAmount       Amount of USDT position.
	 * @return True if USDC position amount is greater than or equal to USDT
	 *         position amount.
	 */
	private boolean isUsdcPositionHigherOrEqual(PositionInfo usdcPositionInfo, PositionInfo positionInfo,
			Double usdcAmount, Double usdtAmount) {
		return usdcAmount >= usdtAmount || (usdcPositionInfo != null
				&& Math.abs(usdcPositionInfo.getPositionAmount()) >= Math.abs(positionInfo.getPositionAmount()));
	}

	/**
	 * Handles the creation of a doubled Buy order based on the specified
	 * conditions.
	 *
	 * @param positionInfo     The profitable position.
	 * @param tickerInfo       Ticker info for the coin.
	 * @param openOrders       List of open orders.
	 * @param usdcPositionInfo USDC position info.
	 * @param usdcAmount       Amount of USDC position.
	 */
	private void handleDoubledBuyOrder(PositionInfo positionInfo, TickerInfo tickerInfo, List<OpenOrderInfo> openOrders,
			PositionInfo usdcPositionInfo) {
		Double quantity = determineQuantity(usdcPositionInfo);
		if (quantity == null) {
			return;
		}

		// Prepare parameters and create Buy order
		Map<String, Object> params = createOrderParams(positionInfo.getSymbol(), Coins.BUY_SIDE, tickerInfo);
		params.put("quantity", String.format("%.2f", quantity));

		orderManager.createFutureOpenOrder(positionInfo.getSymbol(), positionInfo, openOrders, usdcPositionInfo);

		if (buyPositionsInProfit.size() > 6 && Double.valueOf(balanceInfo
				.getAvailableBalance()) >= ((quantity * tickerInfo.getLastPrice()) / positionInfo.getLeverage())) {
			futureOrderManager.createFuturePosition(params, 0);
			System.out.println("Doubled Buy order for param: " + params);
		}
	}

	/**
	 * Determines the quantity for the doubled Buy order based on conditions.
	 *
	 * @param usdcPositionInfo USDC position info.
	 * @return Calculated quantity, or null if no quantity is determined.
	 */
	private Double determineQuantity(PositionInfo usdcPositionInfo) {
		if (usdcPositionInfo == null) {
			return null;
		}

		Double quantity = Math.abs(usdcPositionInfo.getPositionAmount());
		if (CoinUtil.getPositionAmount(usdcPositionInfo) <= 1 && buyPositionsInProfit.size() > 6) {
			quantity *= 2;
		}
		return quantity;
	}

	private void createNewBuyPosition(String coin, TickerInfo tickerInfo) {
		// Retrieve the corresponding USDC position
		String usdcCoin = coin.replace("USDT", "USDC");
		PositionInfo usdcPositionInfo = CoinUtil.getOpenPosition(usdcCoin, openPositions).stream().findFirst()
				.orElse(null);

		// Calculate quantity for the new Buy order
		Double quantity = calculateBuyQuantity(usdcPositionInfo);
		if (quantity == null) {
			quantity = Double.valueOf(CoinUtil.getQuantity(coin, tickerInfo.getLastPrice()));
		}

		if (quantity == null || quantity <= 0) {
			System.out.println("Unable to determine quantity for Buy order for coin: " + coin);
			return;
		}

		// Prepare the parameters for the Buy order
		Map<String, Object> params = createOrderParams(coin, Coins.BUY_SIDE, tickerInfo);
		params.put("quantity", String.format("%.2f", quantity));

		// Calculate amounts for USDT and USDC
		Double usdcAmount = calculateUsdcAmount(usdcPositionInfo, quantity, tickerInfo);
		Double usdtAmount = calculateUsdtAmount(coin, quantity, tickerInfo);
		double buyAmount = PositionCalculator.calculateUsdtAmount(quantity, 65);

		// Execute the Buy order based on conditions
		if (usdcPositionInfo != null) {
			futureOrderManager.createFuturePosition(params, 0);
			System.out.println("Created new Buy order for coin: " + coin + " with quantity: " + quantity);
		} else if (shouldCreateBuyOrder(usdcAmount, usdtAmount, buyAmount)) {
			futureOrderManager.createFuturePosition(params, 0);
			System.out.println("Created new Buy order for coin: " + coin + " with quantity: " + quantity);
		} else {
			System.out.println("Conditions not met for Buy order for coin: " + coin);
		}
	}

	private Double calculateUsdcAmount(PositionInfo usdcPositionInfo, Double quantity, TickerInfo tickerInfo) {
		if (usdcPositionInfo != null) {
			return PositionCalculator.calculateUsdtAmount(Double.valueOf(usdcPositionInfo.getNotional()),
					usdcPositionInfo.getLeverage());
		}
		return (quantity * tickerInfo.getLastPrice()) / 65;
	}

	private Double calculateUsdtAmount(String coin, Double quantity, TickerInfo tickerInfo) {
		PositionInfo currentPositionInfo = CoinUtil.getOpenPosition(coin, openPositions).stream().findFirst()
				.orElse(null);
		if (currentPositionInfo != null) {
			return PositionCalculator.calculateUsdtAmount(Double.valueOf(currentPositionInfo.getNotional()),
					currentPositionInfo.getLeverage());
		}
		return (quantity * tickerInfo.getLastPrice()) / 65;
	}

	private boolean shouldCreateBuyOrder(Double usdcAmount, Double usdtAmount, double buyAmount) {
		double availableBalance = Double.valueOf(balanceInfo.getAvailableBalance());
		boolean isBuyPositionConditionMet = (buyPositions.size() < 12
				|| (buyPositions.size() >= 12 && buyPositionsInProfit.size() > 6));

		return isBuyPositionConditionMet && usdcAmount * 10 * 2 < availableBalance
				&& usdtAmount * 10 * 2  < availableBalance && buyAmount * 10 * 2  < availableBalance;
	}

	/**
	 * Calculates the Buy quantity based on the corresponding USDC position.
	 *
	 * @param usdcPositionInfo The USDC position information.
	 * @return The calculated quantity, or null if no valid quantity can be
	 *         determined.
	 */
	private Double calculateBuyQuantity(PositionInfo usdcPositionInfo) {
		if (usdcPositionInfo == null) {
			return null;
		}

		return Math.abs(usdcPositionInfo.getPositionAmount());

	}

	private void processSellPositions(Map<String, TickerInfo> tickerMap, List<OpenOrderInfo> openOrders) {
		balanceInfo = walletManager.getFutureWalletBalance("USDT");
		for (Map.Entry<String, TickerInfo> entry : tickerMap.entrySet()) {
			String usdcCoin = entry.getKey();
			TickerInfo tickerInfo = entry.getValue();

			// Skip coins that don't end with "USDC" or if downMovement is 0
			if (!usdcCoin.endsWith("USDC") || downMovement <= 2) {
				continue;
			}

			// Retrieve USDC position
			PositionInfo usdcPositionInfo = CoinUtil.getPosition(CoinUtil.getOpenPosition(usdcCoin, openPositions));

			// Retrieve corresponding USDT position
			String usdtCoin = usdcCoin.replace("USDC", "USDT");
			PositionInfo usdtPositionInfo = CoinUtil.getOppositeCoinPosition(usdtCoin, openPositions);

			// Validate USDT position
			if (usdtPositionInfo == null) {
				System.out.println(
						"No action for coin: " + usdcCoin + ", Missing opposite position for USDT: " + usdtCoin);
				continue;
			}

			// Adjust sell positions
			adjustSellPositions(usdcCoin, usdcPositionInfo, tickerInfo, usdtPositionInfo, openOrders);
		}
	}

	private void adjustSellPositions(String coin, PositionInfo positionInfo, TickerInfo tickerInfo,
			PositionInfo usdtPositionInfo, List<OpenOrderInfo> openOrders) {
		adjustQuantityBasedOnDownMovement(coin, positionInfo, tickerInfo, usdtPositionInfo, openOrders);
	}

	private void adjustQuantityBasedOnDownMovement(String coin, PositionInfo positionInfo, TickerInfo tickerInfo,
			PositionInfo usdtPositionInfo, List<OpenOrderInfo> openOrders) {
		double baseQuantity = Math.abs(usdtPositionInfo.getPositionAmount());
		double quantity = baseQuantity;

		double usdtAmount = CoinUtil.getPositionAmount(usdtPositionInfo);
		boolean openPositionExist = positionInfo != null ? true : false;
		double usdcAmount = positionInfo != null ? CoinUtil.getPositionAmount(positionInfo) : 0;
		System.out.println("SELL usdtAmount: " + usdtAmount + " , usdcAmount: " + usdcAmount);

		// Check if USDC position is profitable and create open order if conditions are
		// met
		if (usdcAmount > 0) {
			Double profitInPercentage = PositionCalculator.calculatePercentageProfit(positionInfo.getUnRealizedProfit(),
					usdcAmount);
			boolean hasSufficientBalance = Double.valueOf(balanceInfo.getAvailableBalance()) >= usdcAmount;
			boolean isMarketFavorable = upMovement > downMovement * 10;

			if (profitInPercentage > 25.0 && hasSufficientBalance && isMarketFavorable) {
				orderManager.createFutureOpenOrder(positionInfo.getSymbol(), positionInfo, openOrders,
						usdtPositionInfo);
			}
		}
		// Determine quantity adjustments based on downMovement
		if (downMovement >= 2 && downMovement < 6 && usdcAmount < usdtAmount) {
			if (openPositionExist)
				return;
			quantity = usdcAmount != 0
					? Math.max(usdtPositionInfo.getPositionAmount() - positionInfo.getPositionAmount(), 0)
					: baseQuantity / 4;
		} else if (downMovement >= 6 && downMovement < 12) {
			if (openPositionExist)
				return;
			quantity = usdcAmount != 0
					? Math.max(usdtPositionInfo.getPositionAmount() - Math.abs(positionInfo.getPositionAmount()), 0)
					: baseQuantity / 2;
		} else if (downMovement >= 12 && usdtAmount > usdcAmount) {
			quantity = Math.max(usdtPositionInfo.getPositionAmount() - Math.abs(positionInfo.getPositionAmount()), 0);
		}

		// Prepare parameters and create sell position
		Map<String, Object> params = createOrderParams(coin, Coins.SELL_SIDE, tickerInfo);
		params.put("quantity", String.format("%.2f", quantity));

		if (usdcAmount < usdtAmount && Double.valueOf(balanceInfo.getAvailableBalance()) > usdtAmount) {
			futureOrderManager.createFuturePosition(params, 0);
			System.out.println("Adjusted Sell order for params: " + params);
		} else {
			System.out.println(
					"Either position is already stable or No sufficient balance, Skipping Sell order for params: "
							+ params);
		}
	}

	private Map<String, Object> createOrderParams(String coin, String side, TickerInfo tickerInfo) {
		Map<String, Object> params = new HashMap<>();
		params.put("type", MarketType.MARKET.toString());
		params.put("symbol", coin);
		params.put("side", side);
		params.put("quantity", CoinUtil.getQuantity(coin, tickerInfo.getLastPrice()));
		return params;
	}

	private void printResult(List<String> symbols, List<String> errors, List<String> errored, Date startTime) {
		symbols.removeAll(errored);
		Date finishedTime = new Date();
		System.out.println(CRON_FINISHED_MESSAGE + finishedTime);
		System.out.println(TOTAL_TIME_MESSAGE + (finishedTime.getTime() - startTime.getTime()) / (60.0 * 1000.0)
				+ " minutes" + " \nProcessed Coins : " + processed);
		System.out.println("Errors: " + errors + "\nCoins that didn't get processed: " + errored);
	}
}
