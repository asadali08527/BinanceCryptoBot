package com.bcb.futures.strategy.executor;

import com.bcb.service.MarketService;
import com.bcb.service.OrderService;
import com.bcb.service.PositionService;
import com.bcb.service.WalletService;
import com.bcb.trade.constants.Coins;
import com.bcb.trade.util.CoinUtil;
import com.bcb.trade.util.PositionCalculator;
import com.bcb.transfer.BalanceInfo;
import com.bcb.transfer.OpenOrderInfo;
import com.bcb.transfer.PositionInfo;
import com.bcb.transfer.TickerInfo;
import java.util.*;
import java.util.stream.Collectors;

public class StrategyExecutorV3 {
	private static final String CRON_FINISHED_MESSAGE = "Cron Finished at ";
	private static final String TOTAL_TIME_MESSAGE = "Total Time taken to Execute The Job : ";

	private final PositionService positionService;
	private final OrderService orderService;
	private final MarketService marketService;
	private final WalletService walletService;

	private static List<String> processed = new ArrayList<>();
	private static List<String> errored = new ArrayList<>();
	private static boolean pauseNewOrderFor2Hrs = false;
	private static Date pauseTimefor2Hrs = null;

	private List<PositionInfo> openPositions;
	private List<PositionInfo> buyPositions;
	private List<PositionInfo> sellPositions;
	private BalanceInfo balanceInfo;
	private List<String> symbols = new ArrayList<>();
	private List<PositionInfo> buyPositionsInProfit;
	private Integer upMovement = 0;
	private Integer downMovement = 0;

	public StrategyExecutorV3(PositionService positionService, OrderService orderService, MarketService marketService,
			WalletService walletService) {
		this.positionService = positionService;
		this.orderService = orderService;
		this.marketService = marketService;
		this.walletService = walletService;
	}

	public void executeStrategy() {
		if (isJobPaused()) {
			System.out.println("Job paused due to Futures Trading Quantitative Rules.");
			return;
		}
		Date startTime = new Date();
		resumeJobIfNeeded();

		initializePositionData();

		Map<String, TickerInfo> tickerMap = marketService.getTickers(symbols.toArray(new String[0]));
		List<OpenOrderInfo> openOrders = orderService.getOpenOrders();

		printPositionSummary(buyPositionsInProfit);

		calculateMarketMovement(tickerMap);

		balanceInfo = walletService.getFutureWalletBalance("USDT");
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
        openPositions = positionService.getAllOpenPositions();
        List<String> usdcSymbols = openPositions.stream()
                .filter(position -> position.getSymbol().endsWith("USDC") && !position.getSymbol().startsWith("1000"))
                .map(PositionInfo::getSymbol)
                .collect(Collectors.toList());
        List<String> usdtSymbols = usdcSymbols.stream()
                .map(symbol -> symbol.replace("USDC", "USDT"))
                .collect(Collectors.toList());
        openPositions = openPositions.stream()
                .filter(position -> {
                	return usdtSymbols.contains(position.getSymbol()) || position.getSymbol().endsWith("USDC");
                	})
                .filter(position -> {
                	return position.getPositionAmount() != 0.0;
                	}
                ).filter(position -> {
                	return !position.getSymbol().startsWith("1000");
                	}
                ).collect(Collectors.toList());
        symbols.addAll(usdcSymbols);
        symbols.addAll(usdtSymbols);
    	symbols.remove("IPUSDC");
		symbols.remove("IPUSDT");
        buyPositions = positionService.filterPositions(openPositions, "BUY");
        sellPositions = positionService.filterPositions(openPositions, "SELL");

        double totalBuyAmount = buyPositions.stream().mapToDouble(CoinUtil::getPositionAmount).sum();
        double totalSellAmount = sellPositions.stream().mapToDouble(CoinUtil::getPositionAmount).sum();
        System.out.println("****************************************************************************************");
        System.out.println("Buy Amount: " + totalBuyAmount);
        System.out.println("Sell Amount: " + totalSellAmount);
        buyPositionsInProfit = positionService.getProfitablePositions(buyPositions);
        int buyAggLeverage = positionService.calculateAggregateLeverage(buyPositions);
        int sellAggLeverage = positionService.calculateAggregateLeverage(sellPositions);
        System.out.println("Buy Positions Aggregate Leverage: " + buyAggLeverage);
        System.out.println("Sell Positions Aggregate Leverage: " + sellAggLeverage);
    }

	private void printPositionSummary(List<PositionInfo> buyPositionsInProfit) {
		System.out.printf("Buy Positions in Profit: %d%n", buyPositionsInProfit.size());
	}

	private void calculateMarketMovement(Map<String, TickerInfo> tickerMap) {
		upMovement = marketService.calculateMarketMovement(tickerMap, "UP");
		downMovement = marketService.calculateMarketMovement(tickerMap, "DOWN");
	}

	private void processBuyPositions(Map<String, TickerInfo> tickerMap, List<OpenOrderInfo> openOrders) {
		// Fetch wallet balance for USDT
		balanceInfo = walletService.getFutureWalletBalance("USDT");

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
		return !coin.endsWith("USDT");
	}

	private void handleBuyPosition(String coin, TickerInfo tickerInfo, List<OpenOrderInfo> openOrders) {
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
		}
	}

	private void processNewBuyPosition(String coin, TickerInfo tickerInfo) {
		if (downMovement <= Coins.INITIAL_SELL_ORDER_THRESHOLD && ((buyPositions.size() >= Coins.MAX_BUY_ORDER
				&& buyPositionsInProfit.size() >= Coins.BUY_ORDER_IN_PROFIT_COUNT)
				|| buyPositions.size() < Coins.MAX_BUY_ORDER)) {
			createNewBuyPosition(coin, tickerInfo);
		} else if (downMovement <= Coins.INITIAL_SELL_ORDER_THRESHOLD) {
			String usdcCoin = coin.replace("USDT", "USDC");
			PositionInfo usdcPositionInfo = getUsdcPositionInfo(usdcCoin);
			if (usdcPositionInfo != null) {
				createNewBuyPosition(coin, tickerInfo);
			}
		}
	}

	private boolean isProfitablePosition(PositionInfo positionInfo) {
		return positionInfo != null && positionInfo.getUnRealizedProfit() > 0;
	}

	private void handleProfitableBuy(PositionInfo positionInfo, TickerInfo tickerInfo, List<OpenOrderInfo> openOrders) {
		String usdcCoin = positionInfo.getSymbol().replace("USDT", "USDC");
		double usdtAmount = CoinUtil.getPositionAmount(positionInfo);
		double profitPercentage = PositionCalculator.calculatePercentageProfit(positionInfo.getUnRealizedProfit(),
				usdtAmount);
		System.out.println("Coin: " + positionInfo.getSymbol() + " , Profit %: " + profitPercentage);

		PositionInfo usdcPositionInfo = getUsdcPositionInfo(usdcCoin);
		double usdcAmount = usdcPositionInfo != null ? CoinUtil.getPositionAmount(usdcPositionInfo) : 0;

		logBuySellAmounts(usdtAmount, usdcAmount, profitPercentage);

		if (shouldSkipProcessing(profitPercentage)) {
			return;
		}
		if (isBelowMinimumUsdtAmount(usdtAmount)) {
			createMinimumBuyPosition(positionInfo, tickerInfo);
			return;
		}
		processHighlyProfitablePosition(positionInfo, tickerInfo, openOrders, usdcPositionInfo, profitPercentage,
				usdcAmount, usdtAmount);
	}

	private boolean isBelowMinimumUsdtAmount(double usdtAmount) {
		return usdtAmount <= 1;
	}

	private PositionInfo getUsdcPositionInfo(String usdcCoin) {
		return CoinUtil.getOpenPosition(usdcCoin, openPositions).stream().findFirst().orElse(null);
	}

	private void logBuySellAmounts(double usdtAmount, double usdcAmount, double profitPercentage) {
		System.out.printf("Buy usdtAmount: %.2f, Sell usdcAmount: %.2f, profitPercentage: %.2f%n", usdtAmount,
				usdcAmount, profitPercentage);
	}

	private boolean shouldSkipProcessing(double profitPercentage) {
		return downMovement > Coins.INITIAL_SELL_ORDER_THRESHOLD || profitPercentage < 5;
	}

	private void createMinimumBuyPosition(PositionInfo positionInfo, TickerInfo tickerInfo) {
		Map<String, Object> params = createOrderParams(positionInfo.getSymbol(), "BUY", tickerInfo);
		orderService.createFuturePosition(params, 0);
		System.out.println("Made buy position for at least USDT 1.0 or more for params: " + params);
	}

	private void processHighlyProfitablePosition(PositionInfo positionInfo, TickerInfo tickerInfo,
			List<OpenOrderInfo> openOrders, PositionInfo usdcPositionInfo, double profitPercentage, double usdcAmount,
			double usdtAmount) {
		double usdcPositionAmount = usdcPositionInfo != null ? Math.abs(usdcPositionInfo.getPositionAmount()) : 0;
		double usdtPositionAmount = positionInfo.getPositionAmount();

		if (profitPercentage <= Coins.OPEN_ORDER_THRESHOLD_FOR_BUY) {
			return;
		}

		handleFutureOpenOrder(positionInfo, tickerInfo, openOrders, usdcPositionInfo, profitPercentage, usdtAmount);

		if (shouldStabilizePosition(usdcPositionAmount, usdtPositionAmount)) {
			stabilizeBuyPosition(positionInfo, tickerInfo, usdcPositionInfo, usdcPositionAmount, usdtPositionAmount);
			return;
		}

		handleProfitScenarios(positionInfo, tickerInfo, usdcPositionInfo, profitPercentage, usdcAmount, usdtAmount,
				openOrders);
	}

	private void handleFutureOpenOrder(PositionInfo positionInfo, TickerInfo tickerInfo, List<OpenOrderInfo> openOrders,
			PositionInfo usdcPositionInfo, double profitPercentage, double usdtAmount) {
		if (usdtAmount > 1.0) {
			orderService.createFutureOpenOrder(positionInfo.getSymbol(), positionInfo, openOrders, usdcPositionInfo);
		}
	}

	private boolean shouldStabilizePosition(double usdcPositionAmount, double usdtPositionAmount) {
		return usdcPositionAmount >= usdtPositionAmount;
	}

	private void handleProfitScenarios(PositionInfo positionInfo, TickerInfo tickerInfo, PositionInfo usdcPositionInfo,
			double profitPercentage, double usdcAmount, double usdtAmount, List<OpenOrderInfo> openOrders) {
		if (isModerateProfit(profitPercentage, usdtAmount) && orderService
				.calculateOrderQuantity(positionInfo.getSymbol(), positionInfo, orderService.filterOpenOrders(positionInfo.getSymbol(),openOrders), usdcPositionInfo) <= 0.0) {
			createMinimumBuyPosition(positionInfo, tickerInfo);
		} else if (isHighProfit(profitPercentage)) {
			if (Arrays.asList(Coins.SKIP_USDT_LIST).contains(positionInfo.getSymbol()))
				return;
			closeHighlyProfitablePosition(positionInfo, usdcPositionInfo);
		} else if (isBetweenModerateAndHighProfit(profitPercentage)
				&& isUsdcPositionHigherOrEqual(usdcPositionInfo, positionInfo, usdcAmount, usdtAmount)) {
			handleNewBuyOrderEquivalentToOppositePosition(positionInfo, tickerInfo, usdcPositionInfo);
		}
	}

	private boolean isModerateProfit(double profitPercentage, double usdtAmount) {
		return profitPercentage > Coins.BUY_NEW_CREATE_ORDER_THRESHOLD && profitPercentage < 550 && usdtAmount < Coins.MAX_BUY_USDT_AMOUNT;
	}

	private boolean isHighProfit(double profitPercentage) {
		return profitPercentage >= 550;
	}

	private boolean isBetweenModerateAndHighProfit(double profitPercentage) {
		return profitPercentage > 0 && profitPercentage < 150;
	}

	private void stabilizeBuyPosition(PositionInfo positionInfo, TickerInfo tickerInfo, PositionInfo usdcPositionInfo,
			double usdcPositionAmount, double usdtPositionAmount) {
		double availableBalance = Double.valueOf(balanceInfo.getAvailableBalance());
		double quantity = calculateStabilizationQuantity(usdcPositionAmount, usdtPositionAmount);
		Double usdtAmount = calculateUsdtAmount(positionInfo.getSymbol(), quantity, tickerInfo);

		if (quantity <= 0.0 || availableBalance < usdtAmount) {
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
		Map<String, Object> params = createOrderParams(symbol, "BUY", tickerInfo);
		params.put("quantity", String.format("%.2f", quantity));
		return params;
	}

	private void executeBuyOrder(Map<String, Object> params) {
		try {
			orderService.createFuturePosition(params, 0);
			System.out.println("Buy position stabilized for params: " + params);
		} catch (Exception e) {
			System.err.println("Error while stabilizing buy position: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void closeHighlyProfitablePosition(PositionInfo positionInfo, PositionInfo usdcPositionInfo) {
		try {
			positionService.closePosition(positionInfo.getSymbol(), positionInfo);

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
		positionService.closePosition(positionInfo.getSymbol(), positionInfo);
	}

	private void closePositionDirectly(PositionInfo positionInfo) {
		positionService.closePosition(positionInfo.getSymbol(), positionInfo);
	}

	private boolean isUsdcPositionHigherOrEqual(PositionInfo usdcPositionInfo, PositionInfo positionInfo,
			Double usdcAmount, Double usdtAmount) {
		return usdcAmount * 2 >= usdtAmount || (usdcPositionInfo != null
				&& Math.abs(usdcPositionInfo.getPositionAmount()) >= Math.abs(positionInfo.getPositionAmount()));
	}

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
			orderService.createFuturePosition(params, 0);
			System.out.println("Doubled Buy order for params: " + params);
		}
	}

	private double calculateUsdtAmount(double quantity, TickerInfo tickerInfo, PositionInfo positionInfo) {
		return (quantity * tickerInfo.getLastPrice()) / positionInfo.getLeverage();
	}

	private Map<String, Object> prepareBuyOrderParams(PositionInfo positionInfo, TickerInfo tickerInfo,
			double quantity) {
		Map<String, Object> params = createOrderParams(positionInfo.getSymbol(), "BUY", tickerInfo);
		params.put("quantity", String.format("%.2f", quantity));
		return params;
	}

	private boolean canCreateBuyOrder(double usdtAmount, double totalPositionAmount) {
		double availableBalance = Double.valueOf(balanceInfo.getAvailableBalance());
		int maxUsdtAmountDigits = calculateMaxUsdtAmountDigits(availableBalance);
		double maxUsdtAmount = calculateMaxUsdtAmount(maxUsdtAmountDigits);

		boolean isUsdtAmountConditionMet = usdtAmount > 0 && usdtAmount <= maxUsdtAmount
				&& totalPositionAmount <= maxUsdtAmount;

		return buyPositionsInProfit.size() >= 6 && availableBalance >= usdtAmount * Coins.MULTIPLIER_LEVERAGE
				&& isUsdtAmountConditionMet;
	}

	private int calculateMaxUsdtAmountDigits(double availableBalance) {
		int availableBalanceDigits = String.valueOf((int) availableBalance).length();
		return Math.max(availableBalanceDigits - 2, 1);
	}

	private Double determineQuantity(PositionInfo usdcPositionInfo) {
		if (usdcPositionInfo == null) {
			return null;
		}

		Double quantity = Math.abs(usdcPositionInfo.getPositionAmount());
		if (CoinUtil.getPositionAmount(usdcPositionInfo) <= 1
				&& buyPositionsInProfit.size() > Coins.BUY_ORDER_IN_PROFIT_COUNT) {
			quantity *= 2;
		}
		return quantity;
	}

	private void createNewBuyPosition(String coin, TickerInfo tickerInfo) {
		String usdcCoin = coin.replace("USDT", "USDC");
		PositionInfo usdcPositionInfo = getUsdcPositionInfo(usdcCoin);

		Double quantity = determineBuyQuantity(coin, tickerInfo, usdcPositionInfo);

		if (quantity == null || quantity <= 0) {
			System.out.println("Unable to determine quantity for Buy order for coin: " + coin);
			return;
		}

		Map<String, Object> params = prepareBuyOrderParams(coin, tickerInfo, quantity);

		Double usdcAmount = calculateUsdcAmount(usdcPositionInfo, quantity, tickerInfo);
		Double usdtAmount = calculateUsdtAmount(coin, quantity, tickerInfo);
		double buyAmount = PositionCalculator.calculateUsdtAmount(quantity, 65);

		if (canCreateBuyOrder(usdcPositionInfo, buyAmount, usdcAmount, usdtAmount)) {
			orderService.createFuturePosition(params, 0);
			initializePositionData();
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
		Map<String, Object> params = createOrderParams(coin, "BUY", tickerInfo);
		params.put("quantity", String.format("%.2f", quantity));
		return params;
	}

	private boolean canCreateBuyOrder(PositionInfo usdcPositionInfo, double buyAmount, Double usdcAmount,
			Double usdtAmount) {
		double availableBalance = Double.valueOf(balanceInfo.getAvailableBalance());
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
		double availableBalance = Double.valueOf(balanceInfo.getAvailableBalance());
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
			return 1.0;
		}
		int maxUsdtAmountDigits = availableBalanceDigits - 2;
		return Math.pow(10, maxUsdtAmountDigits) - 1;
	}

	private boolean isUsdtAmountWithinRange(Double usdtAmount, double maxUsdtAmount) {
		return usdtAmount > 0 && usdtAmount <= maxUsdtAmount;
	}

	private boolean isBuyPositionLimitMet() {
		return (buyPositions.size() < Coins.MAX_BUY_ORDER || (buyPositions.size() >= Coins.MAX_BUY_ORDER
				&& buyPositionsInProfit.size() >= Coins.BUY_ORDER_IN_PROFIT_COUNT));
	}

	private boolean isBalanceSufficient(Double usdcAmount, Double usdtAmount, double buyAmount,
			double availableBalance) {
		double thresholdMultiplier = Coins.MULTIPLIER_LEVERAGE;
		return usdcAmount * thresholdMultiplier < availableBalance
				&& usdtAmount * thresholdMultiplier < availableBalance
				&& buyAmount * thresholdMultiplier < availableBalance;
	}

	private Double calculateBuyQuantity(PositionInfo usdcPositionInfo) {
		if (usdcPositionInfo == null) {
			return null;
		}
		return Math.abs(usdcPositionInfo.getPositionAmount());
	}

	private void processSellPositions(Map<String, TickerInfo> tickerMap, List<OpenOrderInfo> openOrders) {
		balanceInfo = walletService.getFutureWalletBalance("USDT");

		for (Map.Entry<String, TickerInfo> entry : tickerMap.entrySet()) {
			String usdcCoin = entry.getKey();
			TickerInfo tickerInfo = entry.getValue();

			if (shouldSkipCoinProcessing(usdcCoin)) {
				continue;
			}

			PositionInfo usdcPositionInfo = CoinUtil.getPosition(CoinUtil.getOpenPosition(usdcCoin, openPositions));

			if (shouldClosePosition(usdcCoin, usdcPositionInfo)) {
				positionService.closePosition(usdcCoin, usdcPositionInfo);
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
		return !usdcCoin.endsWith("USDC");
	}

	private boolean shouldClosePosition(String usdcCoin, PositionInfo usdcPositionInfo) {
		return usdcCoin.endsWith("USDC") && downMovement <= Coins.INITIAL_SELL_ORDER_THRESHOLD && usdcPositionInfo != null
				&& usdcPositionInfo.getUnRealizedProfit() > 0;
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
		double usdtAmount = CoinUtil.getPositionAmount(usdtPositionInfo);
		double usdcAmount = positionInfo != null ? CoinUtil.getPositionAmount(positionInfo) : 0;

		logPositionDetails(coin, usdtAmount, usdcAmount);

		if (usdcAmount > 0
				&& handleProfitableUsdcPosition(coin, positionInfo, usdcAmount, openOrders, usdtPositionInfo)) {
			return;
		} else if (downMovement > Coins.INITIAL_SELL_ORDER_THRESHOLD && usdtPositionInfo.getUnRealizedProfit() > 0) {
			closeHighlyProfitablePosition(usdtPositionInfo, positionInfo);
			return;
		}

		double profitInPercentage = positionInfo != null
				? PositionCalculator.calculatePercentageProfit(positionInfo.getUnRealizedProfit(),
						CoinUtil.getPositionAmount(positionInfo))
				: 0;
		double quantity = calculateAdjustedQuantity(coin, positionInfo, usdtPositionInfo, openOrders);

		if (shouldCreateSellOrder(quantity, usdcAmount, usdtAmount)) {
			createSellOrder(coin, tickerInfo, quantity, usdtAmount, usdcAmount);
		}
		if (profitInPercentage >= Coins.SELL_PROFIT_PERCENTAGE_CUTOFF * 3 || (profitInPercentage >= 0 && downMovement <= Coins.INITIAL_SELL_ORDER_THRESHOLD )) {
			positionService.closePosition(coin, positionInfo);
		}
	}

	private void logPositionDetails(String coin, double usdtAmount, double usdcAmount) {
		System.out.printf("Coin: %s, SELL usdtAmount: %.2f, usdcAmount: %.2f%n", coin, usdtAmount, usdcAmount);
	}

	private boolean shouldCreateSellOrder(double quantity, double usdcAmount, double usdtAmount) {
		return quantity > 0 && downMovement > Coins.INITIAL_SELL_ORDER_THRESHOLD
				&& Double.valueOf(balanceInfo.getAvailableBalance()) > usdtAmount;
	}

	private void createSellOrder(String coin, TickerInfo tickerInfo, double quantity, double usdtAmount,
			double usdcAmount) {
		Map<String, Object> params = createOrderParams(coin, "SELL", tickerInfo);
		params.put("quantity", String.format("%.2f", quantity));

		if (usdcAmount < usdtAmount * 3) {
			orderService.createFuturePosition(params, 0);
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
		if (profitInPercentage <= 0)
			return false;
		boolean hasSufficientBalance = Double.valueOf(balanceInfo.getAvailableBalance()) >= usdcAmount
				* Coins.MULTIPLIER_LEVERAGE;
		boolean isMarketFavorable = upMovement > downMovement * 4;

		if (profitInPercentage > 0.0 && hasSufficientBalance && isMarketFavorable) {
			positionService.closePosition(coin, positionInfo);
			return true;
		} else if (profitInPercentage > Coins.SELL_PROFIT_PERCENTAGE_CUTOFF) {
			orderService.createFutureOpenOrder(positionInfo.getSymbol(), positionInfo, openOrders, usdtPositionInfo);
			return false;
		}
		return false;
	}

	private double calculateAdjustedQuantity(String coin, PositionInfo positionInfo, PositionInfo usdtPositionInfo,
			List<OpenOrderInfo> openOrders) {
		double usdtPositionAmount = Math.abs(usdtPositionInfo.getPositionAmount());
		double usdcPositionAmount = (positionInfo != null) ? Math.abs(positionInfo.getPositionAmount()) : 0;

		logPositionAmounts(usdtPositionAmount, usdcPositionAmount);

		double profitInPercentage = calculateProfitPercentage(positionInfo);

		if (shouldCreateUsdcPosition(positionInfo, coin, openOrders, usdtPositionInfo, profitInPercentage)) {
			return usdtPositionInfo.getPositionAmount();
		}

		return 0;
	}

	private double calculateProfitPercentage(PositionInfo positionInfo) {
		return (positionInfo != null)
				? PositionCalculator.calculatePercentageProfit(positionInfo.getUnRealizedProfit(),
						CoinUtil.getPositionAmount(positionInfo))
				: 0;
	}

	private boolean shouldCreateUsdcPosition(PositionInfo positionInfo, String coin, List<OpenOrderInfo> openOrders,
			PositionInfo usdtPositionInfo, double profitInPercentage) {

		return (downMovement > Coins.INITIAL_SELL_ORDER_THRESHOLD)
				&& (positionInfo == null || (profitInPercentage >= Coins.SELL_PROFIT_PERCENTAGE_CUTOFF * 2
						&& orderService.calculateOrderQuantity(coin, positionInfo, orderService.filterOpenOrders(positionInfo.getSymbol(),openOrders), usdtPositionInfo) <= 0));
	}

	private void logPositionAmounts(double usdtPositionAmount, double usdcPositionAmount) {
		System.out.printf("usdtPositionAmount: %.2f\nusdcPositionAmount: %.2f%n", usdtPositionAmount,
				usdcPositionAmount);
	}

	private Map<String, Object> createOrderParams(String coin, String side, TickerInfo tickerInfo) {
		Map<String, Object> params = new HashMap<>();
		params.put("type", "MARKET");
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
		balanceInfo = walletService.getFutureWalletBalance("USDT");
		System.out.printf("BalanceInfo: %s, UpMovement: %d, DownMovement: %d%n", balanceInfo.getAvailableBalance(),
				upMovement, downMovement);
		double totalBuyAmount = buyPositions.stream().mapToDouble(CoinUtil::getPositionAmount).sum();
		double totalSellAmount = sellPositions.stream().mapToDouble(CoinUtil::getPositionAmount).sum();
		System.out.println("Buy Amount: " + totalBuyAmount);
		System.out.println("Sell Amount: " + totalSellAmount);
		System.out.println("-----------------------------------------------------------------------------------------");
	}
}