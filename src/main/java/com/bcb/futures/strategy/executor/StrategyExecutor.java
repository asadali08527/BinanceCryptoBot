package com.bcb.futures.strategy.executor;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.bcb.enums.MarketType;
import com.bcb.futures.manager.FutureOrderManager;
import com.bcb.futures.manager.OrderManager;
import com.bcb.futures.manager.PositionManager;
import com.bcb.futures.manager.WalletManager;
import com.bcb.trade.constants.Coins;
import com.bcb.trade.sentiment.MarketSentimentAnalyzer;
import com.bcb.trade.util.CoinUtil;
import com.bcb.trade.util.PositionCalculator;
import com.bcb.transfer.BalanceInfo;
import com.bcb.transfer.OpenOrderInfo;
import com.bcb.transfer.PositionInfo;
import com.bcb.transfer.TickerInfo;

public class StrategyExecutor {
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

	public StrategyExecutor(PositionManager positionManager, FutureOrderManager futureOrderManager,
			WalletManager walletManager, OrderManager orderManager) {
		this.positionManager = positionManager;
		this.futureOrderManager = futureOrderManager;
		this.walletManager = walletManager;
		this.orderManager = orderManager;
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
		List<OpenOrderInfo> openOrders = futureOrderManager.getOpenOrders();
		buyPositionsInProfit = getProfitablePositions(buyPositions);

		printPositionSummary(buyPositionsInProfit);

		calculateMarketMovement(tickerMap);

		balanceInfo = walletManager.getFutureWalletBalance("USDT");
		System.out.printf("BalanceInfo: %s, UpMovement: %d, DownMovement: %d%n", balanceInfo.getAvailableBalance(),
				upMovement, downMovement);
		System.out.println("****************************************************************************************");
		processBuyPositions(tickerMap, openOrders);
		processSellPositions(tickerMap, openOrders);

		printResult(new ArrayList<>(tickerMap.keySet()), new ArrayList<>(), errored, startTime);
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
				processNewBuyPosition(coin, positionInfo, tickerInfo);
			}
		}
	}

	private void processExistingBuyPosition(PositionInfo positionInfo, TickerInfo tickerInfo,
			List<OpenOrderInfo> openOrders) {
		if (isProfitablePosition(positionInfo)) {
			handleProfitableBuy(positionInfo, tickerInfo, openOrders);
		}
	}

	private void processNewBuyPosition(String coin, PositionInfo positionInfo, TickerInfo tickerInfo) {
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

		PositionInfo usdcPositionInfo = getUsdcPositionInfo(usdcCoin);
		double usdcAmount = usdcPositionInfo != null ? CoinUtil.getPositionAmount(usdcPositionInfo) : 0;

		logBuySellAmounts(usdtAmount, usdcAmount);

		// Early exits for certain conditions
		if (shouldSkipProcessing(profitPercentage)) {
			return;
		}
		if (usdtAmount <= 2.0) {
			createMinimumBuyPosition(positionInfo, tickerInfo);
			return;
		}
		// Handle highly profitable positions
		processHighlyProfitablePosition(positionInfo, tickerInfo, openOrders, usdcPositionInfo, profitPercentage,
				usdcAmount, usdtAmount);
	}

	private PositionInfo getUsdcPositionInfo(String usdcCoin) {
		return CoinUtil.getOpenPosition(usdcCoin, openPositions).stream().findFirst().orElse(null);
	}

	private void logBuySellAmounts(double usdtAmount, double usdcAmount) {
		System.out.printf("Buy usdtAmount: %.2f, Sell usdcAmount: %.2f%n", usdtAmount, usdcAmount);
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
		// Create future open order for the current position
		if (usdtAmount > 2.0)
			orderManager.createFutureOpenOrder(positionInfo.getSymbol(), positionInfo, openOrders, usdcPositionInfo);

		double usdcPositionAmount = usdcPositionInfo != null ? Math.abs(usdcPositionInfo.getPositionAmount()) : 0;
		double usdtPositionAmount = positionInfo.getPositionAmount();

		// Handle stabilization for mismatched positions
		if (usdcPositionAmount >= usdtPositionAmount) {
			stabilizeBuyPosition(positionInfo, tickerInfo, usdcPositionInfo, usdcPositionAmount, usdtPositionAmount);
			return;
		}
		if (profitPercentage > 33 && profitPercentage <= 66 && usdtAmount <= 20.0) {
			createMinimumBuyPosition(positionInfo, tickerInfo);
		} else if (profitPercentage > 66 && profitPercentage < 75
				&& isUsdcPositionHigherOrEqual(usdcPositionInfo, positionInfo, usdcAmount, usdtAmount)) {
			handleNewBuyOrderEquivalentToOppositePosition(positionInfo, tickerInfo, usdcPositionInfo);
		} else if (profitPercentage >= 75) {
			closeHighlyProfitablePosition(positionInfo, usdcPositionInfo);
			// createNewBuyPosition(positionInfo.getSymbol(), tickerInfo);
		}
	}

	private void stabilizeBuyPosition(PositionInfo positionInfo, TickerInfo tickerInfo, PositionInfo usdcPositionInfo,
			double usdcPositionAmount, double usdtPositionAmount) {
		double quantity = usdcPositionAmount - usdtPositionAmount;
		if (quantity <= 0.0) {
			quantity = usdtPositionAmount;
		}
		// Prepare the parameters for the Buy order
		Map<String, Object> params = createOrderParams(positionInfo.getSymbol(), Coins.BUY_SIDE, tickerInfo);
		params.put("quantity", String.format("%.2f", quantity));

		futureOrderManager.createFuturePosition(params, 0);
		System.out.println("Buy position stabilized for params: " + params);
	}

	private void closeHighlyProfitablePosition(PositionInfo positionInfo, PositionInfo usdcPositionInfo) {
		if (usdcPositionInfo != null) {
			positionManager.deleteFuturesOpenOrder(positionInfo.getSymbol());
			double quantity = positionInfo.getPositionAmount() - Math.abs(usdcPositionInfo.getPositionAmount());
			positionManager.closeFuturePosition(positionInfo.getSymbol(),
					CoinUtil.reverseSide(CoinUtil.evaluateSide(positionInfo)), String.valueOf(quantity));
		} else {
			positionManager.deleteFuturesOpenOrder(positionInfo.getSymbol());
			positionManager.closeFuturePosition(positionInfo.getSymbol(), positionInfo);
		}
		System.out.println("Closed highly profitable position for symbol: " + positionInfo.getSymbol());
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
		return usdcAmount * 20 >= usdtAmount || (usdcPositionInfo != null
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
		// Prepare parameters and create Buy order
		Map<String, Object> params = createOrderParams(positionInfo.getSymbol(), Coins.BUY_SIDE, tickerInfo);
		params.put("quantity", String.format("%.2f", quantity));

		double availableBalance = Double.valueOf(balanceInfo.getAvailableBalance());
		// Determine the number of digits in the available balance
		int availableBalanceDigits = String.valueOf((int) availableBalance).length();

		// Calculate the maximum allowed digits for usdtAmount
		int maxUsdtAmountDigits = availableBalanceDigits - 2; // One less than the number of digits in balance
		double maxUsdtAmount = 0.0;
		if (maxUsdtAmountDigits <= 0) {
			maxUsdtAmount = 1.0;
		} else {
			maxUsdtAmount = Math.pow(10, maxUsdtAmountDigits) - 1; // Max value with `maxUsdtAmountDigits` digits
		}
		double usdtAmount = (quantity * tickerInfo.getLastPrice()) / positionInfo.getLeverage();
		// Check if the usdtAmount is within the allowed range
		boolean isUsdtAmountConditionMet = usdtAmount > 0 && usdtAmount <= maxUsdtAmount
				&& totalPositionAmount <= maxUsdtAmount;

		if (buyPositionsInProfit.size() >= 6
				&& Double.valueOf(balanceInfo.getAvailableBalance()) >= usdtAmount * 10 * 2 * 22
				&& isUsdtAmountConditionMet) {
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
		} else {
			double usdcAmount = CoinUtil.getPositionAmount(usdcPositionInfo);
			if (usdcAmount <= 10.0)
				quantity = quantity * 2;
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
		}
//		else if(positionInfo!=null) {
//			double existingPositionUsdtAmount = CoinUtil.getPositionAmount(positionInfo);
//			double profitInPercentage = PositionCalculator.calculatePercentageProfit(positionInfo.getUnRealizedProfit(),
//					usdtAmount);	
//			if(profitInPercentage>=100 && shouldCreateBuyOrder(usdcAmount, existingPositionUsdtAmount*2, buyAmount*2)) {
//				params.put("quantity", String.format("%.2f", positionInfo.));
//			}
//		} 
		else if (shouldCreateBuyOrder(usdcAmount, usdtAmount, buyAmount)) {
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
		// Determine the number of digits in the available balance
		int availableBalanceDigits = String.valueOf((int) availableBalance).length();

		// Calculate the maximum allowed digits for usdtAmount
		int maxUsdtAmountDigits = availableBalanceDigits - 2; // One less than the number of digits in balance
		double maxUsdtAmount = 0.0;
		if (maxUsdtAmountDigits <= 0) {
			maxUsdtAmount = 1.0;
		} else {
			maxUsdtAmount = Math.pow(10, maxUsdtAmountDigits) - 1; // Max value with `maxUsdtAmountDigits` digits
		}

		// Check if the usdtAmount is within the allowed range
		boolean isUsdtAmountConditionMet = usdtAmount > 0 && usdtAmount <= maxUsdtAmount;

		boolean isBuyPositionConditionMet = (buyPositions.size() < 12
				|| (buyPositions.size() >= 12 && buyPositionsInProfit.size() > 6));

		return isUsdtAmountConditionMet && isBuyPositionConditionMet && usdcAmount * 10 * 2 * 22 < availableBalance
				&& usdtAmount * 10 * 2 * 22 < availableBalance && buyAmount * 10 * 2 * 22 < availableBalance;
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
		double usdtAmount = CoinUtil.getPositionAmount(usdtPositionInfo);
		double usdcAmount = positionInfo != null ? CoinUtil.getPositionAmount(positionInfo) : 0;
		//boolean openPositionExists = positionInfo != null;

		System.out.printf("Coin: "+coin+", SELL usdtAmount: %.2f, usdcAmount: %.2f%n", usdtAmount, usdcAmount);

		// Handle profitable USDC position
		if (usdcAmount > 0
				&& handleProfitableUsdcPosition(coin, positionInfo, usdcAmount, openOrders, usdtPositionInfo)) {
			return;
		}

		// Ensure positions are not already stable
		if (isPositionStable(positionInfo, usdtPositionInfo)) {
			return;
		}

		// Adjust quantity based on downMovement
		double quantity = calculateAdjustedQuantity(positionInfo, usdtPositionInfo, baseQuantity, usdcAmount);
		if (quantity > 0) {
			// Prepare and execute the Sell position
			Map<String, Object> params = createOrderParams(coin, Coins.SELL_SIDE, tickerInfo);
			params.put("quantity", String.format("%.2f", quantity));

			if (usdcAmount < usdtAmount && Double.valueOf(balanceInfo.getAvailableBalance()) > usdtAmount) {
				futureOrderManager.createFuturePosition(params, 0);
				System.out.println("Adjusted Sell order for params: " + params);
			} else {
				System.out
						.println("Position is already stable or insufficient balance. Skipping Sell order for params: "
								+ params);
			}
		}
	}

	private boolean handleProfitableUsdcPosition(String coin, PositionInfo positionInfo, double usdcAmount,
			List<OpenOrderInfo> openOrders, PositionInfo usdtPositionInfo) {
		double profitInPercentage = PositionCalculator.calculatePercentageProfit(positionInfo.getUnRealizedProfit(),
				usdcAmount);
		boolean hasSufficientBalance = Double.valueOf(balanceInfo.getAvailableBalance()) >= usdcAmount * 22 * 2;
		boolean isMarketFavorable = upMovement > downMovement * 6;

		if (profitInPercentage > 100.0 && hasSufficientBalance) {
			positionManager.deleteFuturesOpenOrder(coin);
			positionManager.closeFuturePosition(coin, positionInfo);
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

	private double calculateAdjustedQuantity(PositionInfo positionInfo, PositionInfo usdtPositionInfo,
			double baseQuantity, double usdcAmount) {
		double usdtPositionAmount = Math.abs(usdtPositionInfo.getPositionAmount());
		double usdcPositionAmount = positionInfo != null ? Math.abs(positionInfo.getPositionAmount()) : 0;
		System.out.println("usdtPositionAmount: " + usdtPositionAmount + "\nusdcPositionAmount: " + usdcPositionAmount);

		if (downMovement > 2 && downMovement < 6) {
			if (usdtPositionAmount / 4 <= usdcPositionAmount) {
				return 0; // Skip adjustment
			}
			return positionInfo != null
					? Math.abs((usdtPositionInfo.getPositionAmount()/4) - Math.abs(positionInfo.getPositionAmount()))
					: baseQuantity / 4;
		} else if (downMovement >= 6 && downMovement < 12) {
			if (usdtPositionAmount / 2 <= usdcPositionAmount) {
				return 0; // Skip adjustment
			}
			return positionInfo != null
					? Math.abs((usdtPositionInfo.getPositionAmount()/2) - Math.abs(positionInfo.getPositionAmount()))
					: baseQuantity / 2;
		} else if (downMovement >= 12) {
			return positionInfo != null
					? Math.max(usdtPositionInfo.getPositionAmount() - Math.abs(positionInfo.getPositionAmount()), 0)
					: usdtPositionInfo.getPositionAmount();
		}
		return baseQuantity; // Default to base quantity
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
		balanceInfo = walletManager.getFutureWalletBalance("USDT");
		System.out.printf("BalanceInfo: %s, UpMovement: %d, DownMovement: %d%n", balanceInfo.getAvailableBalance(),
				upMovement, downMovement);
		System.out.println("-----------------------------------------------------------------------------------------");	
	}
}