package com.bcb.futures.strategy.executor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.bcb.enums.MarketType;
import com.bcb.futures.manager.PMFutureOrderManager;
import com.bcb.futures.manager.PMOrderManager;
import com.bcb.futures.manager.PMPositionManager;
import com.bcb.futures.manager.PMWalletManager;
import com.bcb.trade.constants.Coins;
import com.bcb.trade.sentiment.MarketSentimentAnalyzer;
import com.bcb.trade.util.CoinUtil;
import com.bcb.trade.util.PositionCalculator;
import com.bcb.transfer.AccountInfo;
import com.bcb.transfer.OpenOrderInfo;
import com.bcb.transfer.Order;
import com.bcb.transfer.PositionInfo;
import com.bcb.transfer.TickerInfo;

public class PMStrategyExecutor {
	private static final String CRON_FINISHED_MESSAGE = "Cron Finished at ";
	private static final String TOTAL_TIME_MESSAGE = "Total Time taken to Execute The Job : ";

	private final PMPositionManager positionManager;
	private final PMFutureOrderManager futureOrderManager;
	private final PMWalletManager walletManager;
	private final PMOrderManager orderManager;

	private static List<String> processed = new ArrayList<>();
	private static List<String> errored = new ArrayList<>();
	private static boolean pauseNewOrderFor2Hrs = false;
	private static Date pauseTimefor2Hrs = null;
	List<PositionInfo> openPositions = null;
	List<PositionInfo> buyPositions = null;
	List<PositionInfo> sellPositions = null;
	List<OpenOrderInfo> openOrders = null;
	AccountInfo balanceInfo = null;
	private Set<String> symbols = new HashSet();
	List<PositionInfo> buyPositionsInProfit = null;
	Integer upMovement = 0;
	Integer downMovement = 0;

	public PMStrategyExecutor(PMPositionManager positionManager2, PMFutureOrderManager futureOrderManager2,
			PMWalletManager walletManager2, PMOrderManager orderManager2) {
		this.positionManager = positionManager2;
		this.futureOrderManager = futureOrderManager2;
		this.walletManager = walletManager2;
		this.orderManager = orderManager2;
	}

	public void executeStrategy() {
		if (isJobPaused()) {
			System.out.println("Job paused due to Futures Trading Quantitative Rules.");
			return;
		}
		Date startTime = new Date();
		resumeJobIfNeeded();

		initializePositionData();

		Map<String, TickerInfo> tickerMap = initializeTickerMap();
		buyPositionsInProfit = getProfitablePositions(buyPositions);

		printPositionSummary(buyPositionsInProfit);

		calculateMarketMovement(tickerMap);

		balanceInfo = walletManager.getAccountInfo();
		System.out.printf("BalanceInfo: %s, UpMovement: %d, DownMovement: %d%n", balanceInfo.getTotalAvailableBalance(),
				upMovement, downMovement);
		System.out.println("****************************************************************************************");
		processBuyPositions(tickerMap, openOrders);
		processSellPositions(tickerMap, openOrders);
		//processCoinMPositions(tickerMap);

		printResult(new ArrayList<>(tickerMap.keySet()), new ArrayList<>(), errored, startTime);
	}

	private void processCoinMPositions(Map<String, TickerInfo> tickerMap) {
		List<PositionInfo> coinMOpenOrder = positionManager.getCoinMOpenPositions("cm");
		coinMOpenOrder.forEach(System.out::println);		
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
		openOrders = futureOrderManager.getOpenOrders().stream()
				.filter(f -> "NEW".equalsIgnoreCase(f.getStrategyStatus())).collect(Collectors.toList());
		// List<Order> orders = futureOrderManager.getFutureAllOrders();
		openPositions = positionManager.getAllOpenPositions();
//		Set<String> usdcSymbols = orders.stream().filter(position -> {
//			return position.getSymbol().endsWith("C") ;
//		}).map(m -> {
//			return m.getSymbol();
//		}).collect(Collectors.toSet());
//		List<String> usdtSymbols = usdcSymbols.stream().map(symbol -> {
//			return symbol.replace("USDC", "USDT");
//		}).collect(Collectors.toList());
//		openPositions = openPositions.stream().filter(position -> {
//			return (usdtSymbols.contains(position.getSymbol()) || position.getSymbol().endsWith("USDC"))
//					&& !position.getSymbol().startsWith("1000");
//		}).collect(Collectors.toList());
//		openPositions = openPositions.stream().filter(f -> f.getPositionAmount() != 0.0).collect(Collectors.toList());
		symbols.addAll(Arrays.asList(Coins.FUTURE_OPPOSITE_COINS));

		buyPositions = CoinUtil.getOpenPositions(CoinUtil.openPosition(openPositions), Coins.BUY_SIDE);
		sellPositions = CoinUtil.getOpenPositions(CoinUtil.openPosition(openPositions), Coins.SELL_SIDE);

		double totalBuyAmount = buyPositions.stream().mapToDouble(CoinUtil::getPositionAmount).sum();
		double totalSellAmount = sellPositions.stream().mapToDouble(CoinUtil::getPositionAmount).sum();
		System.out.println("****************************************************************************************");
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

	private Map<String, TickerInfo> initializeTickerMap() {
		return MarketSentimentAnalyzer.getTickers(Coins.DESC, symbols.toArray(new String[0]));
	}

	private void processBuyPositions(Map<String, TickerInfo> tickerMap, List<OpenOrderInfo> openOrders) {
		// Fetch wallet balance for USDT
		balanceInfo = walletManager.getAccountInfo();

		for (Map.Entry<String, TickerInfo> entry : tickerMap.entrySet()) {
			String coin = entry.getKey();
			TickerInfo tickerInfo = entry.getValue();

			// Skip coins that don't end with "USDT" or are excluded
			if (shouldSkipCoin(coin)) {
				continue;
			}

			try {
				handleBuyPosition(coin, tickerInfo, openOrders);
			} catch (Exception e) {
				handleBuyPositionError(coin, e);
			}
		}
	}

	private boolean shouldSkipCoin(String coin) {
		return !coin.endsWith("USDT");// || coin.equalsIgnoreCase("XRPUSDT")
	}

	private void handleBuyPosition(String coin, TickerInfo tickerInfo, List<OpenOrderInfo> openOrders) {
		// Get position info for the coin
		PositionInfo positionInfo = CoinUtil.getPosition(CoinUtil.getOpenPosition(coin, openPositions));

		if (positionInfo != null) {
			processExistingBuyPosition(positionInfo, tickerInfo, openOrders);
		} else {
			processNewBuyPosition(coin, tickerInfo);
		}
	}

	private void handleBuyPositionError(String coin, Exception e) {
		errored.add(coin);
		e.printStackTrace();
		System.out.printf("Exception while processing Buy Order for coin: %s, ERROR: %s%n", coin, e.getMessage());
	}

	private void processExistingBuyPosition(PositionInfo positionInfo, TickerInfo tickerInfo,
			List<OpenOrderInfo> openOrders) {
		if (isProfitablePosition(positionInfo)) {
			handleProfitableBuy(positionInfo, tickerInfo, openOrders);
			return;
		} else if (isLossGT1000(positionInfo)) {
			createMinimumBuyPosition(positionInfo, tickerInfo);
			return;
		}

		handleMismatchedBuyPosition(positionInfo, tickerInfo);
	}

	private boolean isLossGT1000(PositionInfo positionInfo) {
		return positionInfo != null && positionInfo.getUnRealizedProfit() <= -1000;
	}

	private void handleMismatchedBuyPosition(PositionInfo positionInfo, TickerInfo tickerInfo) {
		String usdcCoin = positionInfo.getSymbol().replace("USDT", "USDC");
		PositionInfo usdcPositionInfo = getUsdcPositionInfo(usdcCoin);

		double usdcPositionAmount = usdcPositionInfo != null ? Math.abs(usdcPositionInfo.getPositionAmount()) : 0;
		double usdtPositionAmount = positionInfo.getPositionAmount();

		// Handle stabilization for mismatched positions
		if (shouldStabilizePosition(usdcPositionAmount, usdtPositionAmount)) {
			//stabilizeBuyPosition(positionInfo, tickerInfo, usdcPositionInfo,
			//usdcPositionAmount, usdtPositionAmount);
			return;
		}
	}

	private void processNewBuyPosition(String coin, TickerInfo tickerInfo) {
		if (downMovement <= 8) {// && ((buyPositions.size() >= 21 && buyPositionsInProfit.size() >= 11) ||
								// buyPositions.size() < 11)
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

		PositionInfo usdcPositionInfo = getUsdcPositionInfo(usdcCoin);
		double usdcAmount = usdcPositionInfo != null ? CoinUtil.getPositionAmount(usdcPositionInfo) : 0;

		logBuySellAmounts(usdtAmount, usdcAmount, profitPercentage);

		// Early exits for certain conditions
		if (shouldSkipProcessing(profitPercentage)) {
			return;
		}
		if (isBelowMinimumUsdtAmount(usdtAmount)) {
			createMinimumBuyPosition(positionInfo, tickerInfo);
			return;
		}
		// Handle highly profitable positions
		processHighlyProfitablePosition(positionInfo, tickerInfo, openOrders, usdcPositionInfo, profitPercentage,
				usdcAmount, usdtAmount);
	}

	private boolean isBelowMinimumUsdtAmount(double usdtAmount) {
		return usdtAmount <= 1.0;
	}

	private PositionInfo getUsdcPositionInfo(String usdcCoin) {
		return CoinUtil.getOpenPosition(usdcCoin, openPositions).stream().findFirst().orElse(null);
	}

	private void logBuySellAmounts(double usdtAmount, double usdcAmount, double profitPercentage) {
		System.out.printf("Buy usdtAmount: %.2f, Sell usdcAmount: %.2f, profitPercentage: %.2f%n", usdtAmount,
				usdcAmount, profitPercentage);
	}

	private boolean shouldSkipProcessing(double profitPercentage) {
		return downMovement > 6 || profitPercentage < 1;
	}

	private void createMinimumBuyPosition(PositionInfo positionInfo, TickerInfo tickerInfo) {
		Map<String, Object> params = createOrderParams(positionInfo.getSymbol(), Coins.BUY_SIDE, tickerInfo);
		params.put("quantity", String.valueOf(Math.abs(positionInfo.getPositionAmount())));
		futureOrderManager.createFuturePosition(params, 0);
		System.out.println("Made buy position for at least USDT 1.0 or more for params: " + params);
	}

	private void processHighlyProfitablePosition(PositionInfo positionInfo, TickerInfo tickerInfo,
			List<OpenOrderInfo> openOrders, PositionInfo usdcPositionInfo, double profitPercentage, double usdcAmount,
			double usdtAmount) {
		if (profitPercentage <= 25) {
			return; // Exit early if profit percentage is not high enough
		}

		// handleFutureOpenOrder(positionInfo, tickerInfo, openOrders, usdcPositionInfo,
		// profitPercentage, usdtAmount);

		double usdcPositionAmount = usdcPositionInfo != null ? Math.abs(usdcPositionInfo.getPositionAmount()) : 0;
		double usdtPositionAmount = positionInfo.getPositionAmount();

		if (shouldStabilizePosition(usdcPositionAmount, usdtPositionAmount)) {
			//stabilizeBuyPosition(positionInfo, tickerInfo, usdcPositionInfo,
			//usdcPositionAmount, usdtPositionAmount);
			return;
		}

		handleProfitScenarios(positionInfo, tickerInfo, usdcPositionInfo, profitPercentage, usdcAmount, usdtAmount);
	}

	private void handleFutureOpenOrder(PositionInfo positionInfo, TickerInfo tickerInfo, List<OpenOrderInfo> openOrders,
			PositionInfo usdcPositionInfo, double profitPercentage, double usdtAmount) {
		boolean hasNoOpenOrders = orderManager.getOpenOrder(positionInfo.getSymbol(), openOrders).isEmpty();
		boolean shouldCreateOrder = profitPercentage >= 33 && !hasNoOpenOrders;

		if (usdtAmount > 1.0 && (hasNoOpenOrders || shouldCreateOrder)) {
			orderManager.createFutureOpenOrder(positionInfo.getSymbol(), positionInfo, openOrders, usdcPositionInfo);
		}
	}

	private boolean shouldStabilizePosition(double usdcPositionAmount, double usdtPositionAmount) {
		return usdcPositionAmount >= usdtPositionAmount;
	}

	private void handleProfitScenarios(PositionInfo positionInfo, TickerInfo tickerInfo, PositionInfo usdcPositionInfo,
			double profitPercentage, double usdcAmount, double usdtAmount) {
		if (isModerateProfit(profitPercentage, usdtAmount)) {
			// createMinimumBuyPosition(positionInfo, tickerInfo);
		} else if (isHighProfit(profitPercentage)) {
			// closeHighlyProfitablePosition(positionInfo, usdcPositionInfo);
		} else if (isBetweenModerateAndHighProfit(profitPercentage)
				&& isUsdcPositionHigherOrEqual(usdcPositionInfo, positionInfo, usdcAmount, usdtAmount)) {
			handleNewBuyOrderEquivalentToOppositePosition(positionInfo, tickerInfo, usdcPositionInfo);
		}
	}

	private boolean isModerateProfit(double profitPercentage, double usdtAmount) {
		return profitPercentage > 50 && profitPercentage <= 66 && usdtAmount < 2;
	}

	private boolean isHighProfit(double profitPercentage) {
		return profitPercentage >= 75;
	}

	private boolean isBetweenModerateAndHighProfit(double profitPercentage) {
		return profitPercentage > 66 && profitPercentage < 75;
	}

	private void stabilizeBuyPosition(PositionInfo positionInfo, TickerInfo tickerInfo, PositionInfo usdcPositionInfo,
			double usdcPositionAmount, double usdtPositionAmount) {
		double quantity = calculateStabilizationQuantity(usdcPositionAmount, usdtPositionAmount);
		if (quantity <= 0.0) {
			System.out.println("No stabilization needed as quantity is zero or negative.");
			return;
		}

		Map<String, Object> params = prepareBuyOrderParams(positionInfo.getSymbol(), tickerInfo, quantity);

		executeBuyOrder(params);
	}

	private double calculateStabilizationQuantity(double usdcPositionAmount, double usdtPositionAmount) {
		double quantityDifference = usdcPositionAmount - usdtPositionAmount;
		return (quantityDifference > 0.0) ? quantityDifference : usdtPositionAmount;
	}

	private Map<String, Object> prepareBuyOrderParams(String symbol, TickerInfo tickerInfo, double quantity) {
		Map<String, Object> params = createOrderParams(symbol, Coins.BUY_SIDE, tickerInfo);
		params.put("quantity", String.format("%.2f", quantity));
		return params;
	}

	private void executeBuyOrder(Map<String, Object> params) {
		try {
			futureOrderManager.createFuturePosition(params, 0);
			System.out.println("Buy position stabilized for params: " + params);
		} catch (Exception e) {
			System.err.println("Error while stabilizing buy position: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void closeHighlyProfitablePosition(PositionInfo positionInfo, PositionInfo usdcPositionInfo) {
		try {
			positionManager.deleteFuturesOpenOrder(positionInfo.getSymbol());

			if (usdcPositionInfo != null) {
				double adjustedQuantity = calculateAdjustedQuantityForBuy(positionInfo, usdcPositionInfo);
				closePositionWithAdjustedQuantity(positionInfo, adjustedQuantity);
			} else {
				closePositionDirectly(positionInfo);
			}

			System.out.println("Closed highly profitable position for symbol: " + positionInfo.getSymbol());
		} catch (Exception e) {
			System.err.printf("Error while closing highly profitable position for symbol %s: %s%n",
					positionInfo.getSymbol(), e.getMessage());
			e.printStackTrace();
		}
	}

	private double calculateAdjustedQuantityForBuy(PositionInfo positionInfo, PositionInfo usdcPositionInfo) {
		return positionInfo.getPositionAmount() - Math.abs(usdcPositionInfo.getPositionAmount());
	}

	private void closePositionWithAdjustedQuantity(PositionInfo positionInfo, double quantity) {
		positionManager.closeFuturePosition(positionInfo.getSymbol(),
				CoinUtil.reverseSide(CoinUtil.evaluateSide(positionInfo)), String.valueOf(quantity));
	}

	private void closePositionDirectly(PositionInfo positionInfo) {
		positionManager.closeFuturePosition(positionInfo.getSymbol(), positionInfo);
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
		return usdcAmount * 2 >= usdtAmount || (usdcPositionInfo != null
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
	private void handleNewBuyOrderEquivalentToOppositePosition(PositionInfo positionInfo, TickerInfo tickerInfo,
			PositionInfo usdcPositionInfo) {
		Double quantity = determineQuantity(usdcPositionInfo);
		if (quantity == null) {
			return;
		}

		double totalPositionAmount = CoinUtil.getPositionAmount(positionInfo);
		double usdtAmount = calculateUsdtAmount(quantity, tickerInfo, positionInfo);

		Map<String, Object> params = prepareBuyOrderParams(positionInfo, tickerInfo, quantity);

		if (canCreateBuyOrder(usdtAmount, totalPositionAmount)) {
			futureOrderManager.createFuturePosition(params, 0);
			System.out.println("Doubled Buy order for params: " + params);
		}
	}

	private double calculateUsdtAmount(double quantity, TickerInfo tickerInfo, PositionInfo positionInfo) {
		return (quantity * tickerInfo.getLastPrice()) / positionInfo.getLeverage();
	}

	private Map<String, Object> prepareBuyOrderParams(PositionInfo positionInfo, TickerInfo tickerInfo,
			double quantity) {
		Map<String, Object> params = createOrderParams(positionInfo.getSymbol(), Coins.BUY_SIDE, tickerInfo);
		params.put("quantity", String.format("%.2f", quantity));
		return params;
	}

	private boolean canCreateBuyOrder(double usdtAmount, double totalPositionAmount) {
		double availableBalance = Double.valueOf(balanceInfo.getTotalAvailableBalance());
		int maxUsdtAmountDigits = calculateMaxUsdtAmountDigits(availableBalance);
		double maxUsdtAmount = calculateMaxUsdtAmount(maxUsdtAmountDigits);

		boolean isUsdtAmountConditionMet = usdtAmount > 0 && usdtAmount <= maxUsdtAmount
				&& totalPositionAmount <= maxUsdtAmount;

		return buyPositionsInProfit.size() >= 6 && availableBalance >= usdtAmount * 10 * 2 * 22
				&& isUsdtAmountConditionMet;
	}

	private int calculateMaxUsdtAmountDigits(double availableBalance) {
		int availableBalanceDigits = String.valueOf((int) availableBalance).length();
		return Math.max(availableBalanceDigits - 2, 1); // At least 1 digit
	}

//	private double calculateMaxUsdtAmount(int maxUsdtAmountDigits) {
//		return Math.pow(10, maxUsdtAmountDigits) - 1; // Max value with `maxUsdtAmountDigits` digits
//	}

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
		PositionInfo usdcPositionInfo = getUsdcPositionInfo(usdcCoin);

		// Calculate the quantity for the new Buy order
		Double quantity = determineBuyQuantity(coin, tickerInfo, usdcPositionInfo);

		if (quantity == null || quantity <= 0) {
			System.out.println("Unable to determine quantity for Buy order for coin: " + coin);
			return;
		}

		// Prepare the parameters for the Buy order
		Map<String, Object> params = prepareBuyOrderParams(coin, tickerInfo, quantity);

		// Calculate amounts for USDT and USDC
		Double usdcAmount = calculateUsdcAmount(usdcPositionInfo, quantity, tickerInfo);
		Double usdtAmount = calculateUsdtAmount(coin, quantity, tickerInfo);
		double buyAmount = PositionCalculator.calculateUsdtAmount(quantity, 65);

		// Execute the Buy order based on conditions
		if (canCreateBuyOrder(usdcPositionInfo, buyAmount, usdcAmount, usdtAmount)) {
			futureOrderManager.createFuturePosition(params, 0);
			System.out.println("Created new Buy order for coin: " + coin + " with quantity: " + quantity);
		} else {
			System.out.println("Conditions not met for Buy order for coin: " + coin);
		}
	}

	private Double determineBuyQuantity(String coin, TickerInfo tickerInfo, PositionInfo usdcPositionInfo) {
		Double quantity = calculateBuyQuantity(usdcPositionInfo);
		if (quantity == null) {
			quantity = Double.valueOf(CoinUtil.getQuantity(coin, tickerInfo.getLastPrice()));
		}
		return quantity;
	}

	private Map<String, Object> prepareBuyOrderParams(String coin, TickerInfo tickerInfo, Double quantity) {
		Map<String, Object> params = createOrderParams(coin, Coins.BUY_SIDE, tickerInfo);
		params.put("quantity", String.format("%.2f", quantity));
		return params;
	}

	private boolean canCreateBuyOrder(PositionInfo usdcPositionInfo, double buyAmount, Double usdcAmount,
			Double usdtAmount) {
		double availableBalance = Double.valueOf(balanceInfo.getTotalAvailableBalance());
		if (usdcPositionInfo != null && availableBalance > buyAmount) {
			return true;
		}
		return shouldCreateBuyOrder(usdcAmount, usdtAmount, buyAmount);
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
		double availableBalance = Double.valueOf(balanceInfo.getTotalAvailableBalance());
		int availableBalanceDigits = calculateNumberOfDigits(availableBalance);
		double maxUsdtAmount = calculateMaxUsdtAmount(availableBalanceDigits);

		boolean isUsdtAmountConditionMet = isUsdtAmountWithinRange(usdtAmount, maxUsdtAmount);
		boolean isBuyPositionConditionMet = isBuyPositionLimitMet();
		boolean isBalanceConditionMet = isBalanceSufficient(usdcAmount, usdtAmount, buyAmount, availableBalance);

		return isUsdtAmountConditionMet && isBuyPositionConditionMet && isBalanceConditionMet;
	}

	private int calculateNumberOfDigits(double value) {
		return String.valueOf((int) value).length();
	}

	private double calculateMaxUsdtAmount(int availableBalanceDigits) {
		if (availableBalanceDigits <= 2) {
			return 1.0; // Minimum max amount if digits are <= 2
		}
		int maxUsdtAmountDigits = availableBalanceDigits - 2;
		return Math.pow(10, maxUsdtAmountDigits) - 1; // Max value with `maxUsdtAmountDigits` digits
	}

	private boolean isUsdtAmountWithinRange(Double usdtAmount, double maxUsdtAmount) {
		return usdtAmount > 0 && usdtAmount <= maxUsdtAmount;
	}

	private boolean isBuyPositionLimitMet() {
		return (buyPositions.size() < 12 || (buyPositions.size() >= 12 && buyPositionsInProfit.size() > 6));
	}

	private boolean isBalanceSufficient(Double usdcAmount, Double usdtAmount, double buyAmount,
			double availableBalance) {
		double thresholdMultiplier = 10 * 2 * 22; // Avoiding magic numbers
		return usdcAmount * thresholdMultiplier < availableBalance
				&& usdtAmount * thresholdMultiplier < availableBalance
				&& buyAmount * thresholdMultiplier < availableBalance;
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
		balanceInfo = walletManager.getAccountInfo();

		for (Map.Entry<String, TickerInfo> entry : tickerMap.entrySet()) {
			String usdcCoin = entry.getKey();
			TickerInfo tickerInfo = entry.getValue();

			if (shouldSkipCoinProcessing(usdcCoin)) {
				continue;
			}

			PositionInfo usdcPositionInfo = CoinUtil.getPosition(CoinUtil.getOpenPosition(usdcCoin, openPositions));

			if (shouldClosePosition(usdcCoin, usdcPositionInfo, openOrders)) {
				closePosition(usdcCoin, usdcPositionInfo);
				continue;
			}

			String usdtCoin = usdcCoin.replace("USDC", "USDT");
			PositionInfo usdtPositionInfo = CoinUtil.getOppositeCoinPosition(usdtCoin, openPositions);

			if (usdtPositionInfo == null) {
				logMissingUsdtPosition(usdcCoin, usdtCoin);
				continue;
			}

			try {
				adjustSellPositions(usdcCoin, usdcPositionInfo, tickerInfo, usdtPositionInfo, openOrders);
			} catch (Exception e) {
				handleSellPositionException(usdcCoin, e);
			}
		}
	}

	private boolean shouldSkipCoinProcessing(String usdcCoin) {
		return !usdcCoin.endsWith("USDC");// || usdcCoin.equalsIgnoreCase("XRPUSDC")
	}

	private boolean shouldClosePosition(String usdcCoin, PositionInfo usdcPositionInfo,
			List<OpenOrderInfo> openOrders) {
		return usdcCoin.endsWith("USDC") && downMovement <= 6 && CoinUtil.openOrderExist(usdcCoin, openOrders)
				&& usdcPositionInfo != null && usdcPositionInfo.getUnRealizedProfit() > 0;
	}

	private void closePosition(String usdcCoin, PositionInfo usdcPositionInfo) {
		positionManager.deleteFuturesOpenOrder(usdcCoin);
		positionManager.closeFuturePosition(usdcCoin, usdcPositionInfo);
		System.out.println("Closed position for coin: " + usdcCoin);
	}

	private void logMissingUsdtPosition(String usdcCoin, String usdtCoin) {
		System.out.println("No action for coin: " + usdcCoin + ", Missing opposite position for USDT: " + usdtCoin);
	}

	private void handleSellPositionException(String usdcCoin, Exception e) {
		errored.add(usdcCoin);
		e.printStackTrace();
		System.out.println("Exception while processing Sell Order for coin: " + usdcCoin + " ERROR: " + e.getMessage());
	}

	private void adjustSellPositions(String coin, PositionInfo positionInfo, TickerInfo tickerInfo,
			PositionInfo usdtPositionInfo, List<OpenOrderInfo> openOrders) {
		adjustQuantityBasedOnDownMovement(coin, positionInfo, tickerInfo, usdtPositionInfo, openOrders);
	}

	private void adjustQuantityBasedOnDownMovement(String coin, PositionInfo positionInfo, TickerInfo tickerInfo,
			PositionInfo usdtPositionInfo, List<OpenOrderInfo> openOrders) {
		// double baseQuantity = Double.valueOf(CoinUtil.getQuantity(coin,
		// tickerInfo.getLastPrice()));
		double usdtAmount = CoinUtil.getPositionAmount(usdtPositionInfo);
		double usdcAmount = positionInfo != null ? CoinUtil.getPositionAmount(positionInfo) : 0;

		logPositionDetails(coin, usdtAmount, usdcAmount);

// Handle profitable USDC position
		if (usdcAmount > 0
				&& handleProfitableUsdcPosition(coin, positionInfo, usdcAmount, openOrders, usdtPositionInfo)) {
			return;
		}

// Ensure positions are not already stable
		if (isPositionStable(positionInfo, usdtPositionInfo)) {
			return;
		}

// Calculate adjusted quantity based on downMovement
		double quantity = calculateAdjustedQuantity(positionInfo, usdtPositionInfo);

		if (shouldCreateSellOrder(quantity, usdcAmount, usdtAmount)) {
			createSellOrder(coin, tickerInfo, quantity, usdtAmount, usdcAmount);
		}
	}

	private void logPositionDetails(String coin, double usdtAmount, double usdcAmount) {
		System.out.printf("Coin: %s, SELL usdtAmount: %.2f, usdcAmount: %.2f%n", coin, usdtAmount, usdcAmount);
	}

	private boolean shouldCreateSellOrder(double quantity, double usdcAmount, double usdtAmount) {
		return quantity > 0 && downMovement > 6 && usdcAmount < usdtAmount
				&& Double.valueOf(balanceInfo.getTotalAvailableBalance()) > usdtAmount;
	}

	private void createSellOrder(String coin, TickerInfo tickerInfo, double quantity, double usdtAmount,
			double usdcAmount) {
		Map<String, Object> params = createOrderParams(coin, Coins.SELL_SIDE, tickerInfo);
		params.put("quantity", String.format("%.2f", quantity));

		if (usdcAmount < usdtAmount) {
			futureOrderManager.createFuturePosition(params, 0);
			System.out.println("Adjusted Sell order for params: " + params);
		} else {
			System.out.println(
					"Position is already stable or insufficient balance. Skipping Sell order for params: " + params);
		}
	}

	private boolean handleProfitableUsdcPosition(String coin, PositionInfo positionInfo, double usdcAmount,
			List<OpenOrderInfo> openOrders, PositionInfo usdtPositionInfo) {
		double profitInPercentage = PositionCalculator.calculatePercentageProfit(positionInfo.getUnRealizedProfit(),
				usdcAmount);
		boolean hasSufficientBalance = Double.valueOf(balanceInfo.getTotalAvailableBalance()) >= usdcAmount * 22 * 2 * 10;
		boolean isMarketFavorable = upMovement > downMovement * 6;

		if (profitInPercentage > 100.0 && hasSufficientBalance) {
			// positionManager.deleteFuturesOpenOrder(coin);
			// positionManager.closeFuturePosition(coin, positionInfo);
			return true;
		} else if (profitInPercentage > 50.0 && hasSufficientBalance && isMarketFavorable) {
			orderManager.createFutureOpenOrder(positionInfo.getSymbol(), positionInfo, openOrders, usdtPositionInfo);
			return true;
		}
		return false;
	}

	private boolean isPositionStable(PositionInfo positionInfo, PositionInfo usdtPositionInfo) {
		return positionInfo != null
				&& usdtPositionInfo.getPositionAmount() <= Math.abs(positionInfo.getPositionAmount());
	}

	private double calculateAdjustedQuantity(PositionInfo positionInfo, PositionInfo usdtPositionInfo) {
		double usdtPositionAmount = Math.abs(usdtPositionInfo.getPositionAmount());
		double usdcPositionAmount = positionInfo != null ? Math.abs(positionInfo.getPositionAmount()) : 0;

		logPositionAmounts(usdtPositionAmount, usdcPositionAmount);

		if (isDownMovementInRange(22, 28)) {
			return calculateQuantityForRange(usdtPositionInfo, positionInfo, usdcPositionAmount, 4);
		} else if (isDownMovementInRange(28, 34)) {
			return calculateQuantityForRange(usdtPositionInfo, positionInfo, usdcPositionAmount, 2);
		} else if (downMovement > 34) {
			return calculateQuantityForHighDownMovement(positionInfo, usdtPositionInfo);
		}

		return 0;
	}

	private void logPositionAmounts(double usdtPositionAmount, double usdcPositionAmount) {
		System.out.printf("usdtPositionAmount: %.2f\nusdcPositionAmount: %.2f%n", usdtPositionAmount,
				usdcPositionAmount);
	}

	private boolean isDownMovementInRange(int lowerBound, int upperBound) {
		return downMovement > lowerBound && downMovement <= upperBound;
	}

	private double calculateQuantityForRange(PositionInfo usdtPositionInfo, PositionInfo positionInfo,
			double usdcPositionAmount, int divisor) {
		double usdtPositionAmount = Math.abs(usdtPositionInfo.getPositionAmount());
		if (usdtPositionAmount / divisor <= usdcPositionAmount) {
			return 0; // Skip adjustment
		}
		return positionInfo != null
				? Math.abs(
						(usdtPositionInfo.getPositionAmount() / divisor) - Math.abs(positionInfo.getPositionAmount()))
				: usdtPositionInfo.getPositionAmount() / divisor;
	}

	private double calculateQuantityForHighDownMovement(PositionInfo positionInfo, PositionInfo usdtPositionInfo) {
		return positionInfo != null
				? Math.max(usdtPositionInfo.getPositionAmount() - Math.abs(positionInfo.getPositionAmount()), 0)
				: usdtPositionInfo.getPositionAmount();
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
		System.out.println("-----------------------------------------------------------------------------------------");
		symbols.removeAll(errored);
		Date finishedTime = new Date();
		System.out.println(CRON_FINISHED_MESSAGE + finishedTime);
		System.out.println(TOTAL_TIME_MESSAGE + (finishedTime.getTime() - startTime.getTime()) / (60.0 * 1000.0)
				+ " minutes" + " \nProcessed Coins : " + processed);
		System.out.println("Errors: " + errors + "\nCoins that didn't get processed: " + errored);
		balanceInfo = walletManager.getAccountInfo();
		System.out.printf("BalanceInfo: %s, UpMovement: %d, DownMovement: %d%n", balanceInfo.getTotalAvailableBalance(),
				upMovement, downMovement);
		double totalBuyAmount = buyPositions.stream().mapToDouble(CoinUtil::getPositionAmount).sum();
		double totalSellAmount = sellPositions.stream().mapToDouble(CoinUtil::getPositionAmount).sum();
		System.out.println("Buy Amount: " + totalBuyAmount);
		System.out.println("Sell Amount: " + totalSellAmount);
		System.out.println("-----------------------------------------------------------------------------------------");
	}
}