package com.bcb.futures.manager;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.bcb.client.SpotClient;
import com.bcb.config.PrivateConfig;
import com.bcb.enums.MarketType;
import com.bcb.exceptions.BinanceClientException;
import com.bcb.exceptions.BinanceConnectorException;
import com.bcb.impl.SpotClientImpl;
import com.bcb.trade.constants.Coins;
import com.bcb.trade.sentiment.MarketSentimentAnalyzer;
import com.bcb.trade.util.CoinUtil;
import com.bcb.transfer.OpenOrderInfo;
import com.bcb.transfer.PositionInfo;
import com.bcb.transfer.TickerInfo;

public class FutureOrderScheduler {
	private static final int EXECUTION_INTERVAL_MINUTES = 3;
	private static final String CRON_FINISHED_MESSAGE = "Cron Finished at ";
	private static final String TOTAL_TIME_MESSAGE = "Total Time taken to Execute The Job : ";

	private List<OpenOrderInfo> openOrders = new ArrayList<>();
	private List<String> errored = new ArrayList<>();
	private List<String> processed = new ArrayList<>();
	private boolean pauseNewOrderFor2Hrs = false;
	private Date pauseTimefor2Hrs = null;
	private boolean pauseCreateOrders = false;
	private int iteration = 0;
	private final PositionManager positionManager;
	private final FutureOrderManager futureOrderManager;
	private List<String> symbols;

	private SpotClient createSpotClient() {
		return new SpotClientImpl(PrivateConfig.TAA_API_KEY, PrivateConfig.TAA_SECRET_KEY, PrivateConfig.BASE_URLS[0]);
	}

	public FutureOrderScheduler() {
		this.symbols = CoinUtil.getAllFutureCoinsByTypeAndCategory();
		this.positionManager = new PositionManager(createSpotClient());
		this.futureOrderManager = new FutureOrderManager(createSpotClient());
	}

	public static void main(String[] args) {
		FutureOrderScheduler futureOrderScheduler = new FutureOrderScheduler();
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

		scheduler.scheduleAtFixedRate(futureOrderScheduler::takePositions, 0, EXECUTION_INTERVAL_MINUTES,
				TimeUnit.MINUTES);
		scheduler.scheduleAtFixedRate(futureOrderScheduler::takeUSDCUSDTPositionsSideBySide, 0, 40, TimeUnit.SECONDS);
		scheduler.scheduleAtFixedRate(futureOrderScheduler::takeOppositePositions, 0, 1, TimeUnit.MINUTES);
		scheduler.scheduleAtFixedRate(futureOrderScheduler::takePositionsForPrefixed1000, 0, 47, TimeUnit.SECONDS);
	}

	private void resetState() {
		processed.clear();
		errored.clear();
		symbols.clear();
	}

	private void resumeOrderExecutionIfCoolingPeriodPassed() {
		if (pauseNewOrderFor2Hrs && CoinUtil.checkIfCoolingPeriodPassed(pauseTimefor2Hrs)) {
			System.out.println("Cooling period passed, resuming order execution...");
			pauseNewOrderFor2Hrs = false;
			pauseTimefor2Hrs = null;
		} else if (pauseNewOrderFor2Hrs) {
			System.out.println("Got Futures Trading Quantitative Rules violated error: Job paused for next 2 hours...");
		}
	}

	public void takeUSDCUSDTPositions() {
		iteration++;
		resetState();

		resumeOrderExecutionIfCoolingPeriodPassed();
		if (pauseNewOrderFor2Hrs)
			return;

		List<OpenOrderInfo> openOrderList = futureOrderManager.getOpenOrders();
		List<PositionInfo> openPositionList = positionManager.getAllOpenPositions();

		List<String> usdcSymbols = extractUSDCPositions(openPositionList);
		List<String> usdtSymbols = convertToUSDT(usdcSymbols);

		List<PositionInfo> openPositions = filterOpenPositions(openPositionList, usdtSymbols);
		symbols.addAll(usdcSymbols);
		symbols.addAll(usdtSymbols);

		Integer aggLeverage = calculateAggregateLeverage(openPositionList);
		System.out.println(aggLeverage);

		Collections.shuffle(symbols);
		Map<String, TickerInfo> tickerMap = MarketSentimentAnalyzer.getTickers(Coins.DESC,
				symbols.toArray(new String[0]));
		Integer upMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_UP);
		Integer downMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_DOWN);

		printCronStartDetails(upMovement, downMovement);

		processCoins(tickerMap, upMovement, downMovement, openPositions, openOrderList);
		printCronEndDetails(new ArrayList<>(tickerMap.keySet()),;
	}

	private List<String> extractUSDCPositions(List<PositionInfo> openPositionList) {
		return openPositionList.stream()
				.filter(position -> position.getSymbol().endsWith("USDC") && !position.getSymbol().startsWith("1000"))
				.map(PositionInfo::getSymbol).collect(Collectors.toList());
	}

	private List<String> convertToUSDT(List<String> usdcSymbols) {
		return usdcSymbols.stream().map(symbol -> symbol.replace("USDC", "USDT")).collect(Collectors.toList());
	}

	private List<PositionInfo> filterOpenPositions(List<PositionInfo> openPositionList, List<String> usdtSymbols) {
		return openPositionList.stream().filter(
				position -> (usdtSymbols.contains(position.getSymbol()) || position.getSymbol().endsWith("USDC"))
						&& !position.getSymbol().startsWith("1000"))
				.collect(Collectors.toList());
	}

	private Integer calculateAggregateLeverage(List<PositionInfo> openPositionList) {
		return openPositionList.stream().mapToInt(PositionInfo::getLeverage).sum() / openPositionList.size();
	}

	private void printCronStartDetails(Integer upMovement, Integer downMovement) {
		Date startTime = new Date();
		System.out.println(
				"**************************************************************************************************************************************************");
		System.out.println("Cron started at " + startTime);
		System.out.println("UpMovement :" + upMovement);
		System.out.println("DownMovement :" + downMovement);
		System.out.println("Open Orders " + openOrders.size());
	}

	private void processCoins(Map<String, TickerInfo> tickerMap, Integer upMovement, Integer downMovement,
			List<PositionInfo> openPositions, List<OpenOrderInfo> openOrderList) {
		Set<String> keySets = tickerMap.keySet();
		Iterator<String> iterator = keySets.iterator();

		while (iterator.hasNext() && !pauseNewOrderFor2Hrs) {
			String coin = iterator.next();
			if (pauseNewOrderFor2Hrs) {
				System.out.println(
						"Got Futures Trading Quantitative Rules violated error: Job paused for next 2 hours...");
				return;
			}
			handleCoinPosition(coin, tickerMap, upMovement, downMovement, openPositions, openOrderList);
		}
	}

	private void handleCoinPosition(String coin, Map<String, TickerInfo> tickerMap, Integer upMovement,
			Integer downMovement, List<PositionInfo> openPositions, List<OpenOrderInfo> openOrderList) {
		Map<String, Object> params = new HashMap<>();
		params.put("type", MarketType.MARKET.toString());
		params.put("symbol", coin);

		boolean openOrderExist = CoinUtil.openOrderExist(coin, openOrderList);
		PositionInfo positionInfo = CoinUtil.getOpenPosition(coin, openPositions).stream().findFirst().orElse(null);
		boolean openPositionExist = positionInfo != null;

		if (Arrays.asList(Coins.SKIP_USDT_LIST).contains(coin))
			return;

		if (shouldBuy(upMovement, downMovement, coin)) {
			params.put("side", Coins.BUY_SIDE);
			params.put("quantity", CoinUtil.getQuantity(coin, tickerMap.get(coin).getLastPrice()));
		} else if (shouldSell(upMovement, downMovement, coin)) {
			handleSell(params, coin, openPositions);
		} else {
			return;
		}

		System.out.println("Parameters: " + params);
		if (!(openOrderExist && openPositionExist)) {
			executeOrder(params, coin, positionInfo);
		}
	}

	private boolean shouldBuy(Integer upMovement, Integer downMovement, String coin) {
		return upMovement > downMovement && coin.endsWith("USDT");
	}

	private boolean shouldSell(Integer upMovement, Integer downMovement, String coin) {
		return downMovement > upMovement && coin.endsWith("USDC");
	}

	private void handleSell(Map<String, Object> params, String coin, List<PositionInfo> openPositions) {
		params.put("side", Coins.SELL_SIDE);
		String usdcCoin = coin.replace("USDC", "USDT");
		PositionInfo usdcPositionInfo = CoinUtil.getOpenPosition(usdcCoin, openPositions).stream().findFirst()
				.orElse(null);
		if (usdcPositionInfo == null)
			return;
		Double quantity = usdcPositionInfo.getPositionAmount();
		if (coin.equalsIgnoreCase("XRPUSDC") && quantity > 65)
			quantity = quantity / 2;
		params.put("quantity", String.valueOf(new DecimalFormat("#.##").format(quantity)));
	}

	private void executeOrder(Map<String, Object> params, String coin, PositionInfo positionInfo) {
		try {
			if (positionInfo == null) {
				futureOrderManager.createFuturePosition(params, 0);
			} else if (positionInfo.getPositionAmount() < 0.0) {
				System.out.println("Handling Existing Sell Order : " + positionInfo);
				positionManager.handleNegativePosition(params, coin, positionInfo);
			} else if (positionInfo.getPositionAmount() > 0.0) {
				System.out.println("Handling Existing Buy Order : " + positionInfo);
				positionManager.handlePositivePosition(params, coin, positionInfo);
			}
		} catch (BinanceConnectorException | BinanceClientException e) {
			CoinUtil.handleException(errored, coin, e);
		} catch (Exception e) {
			System.out.println("Exception: " + e.getMessage());
		}
	}

	// Print details about cron end time, errors, and processed symbols
	private void printCronEndDetails(List<String> symbols, List<String> errors, List<String> errored, Date startTime) {
	    symbols.removeAll(errored);
	    Date finishedTime = new Date();
	    
	    System.out.println(CRON_FINISHED_MESSAGE + finishedTime);
	    System.out.println(TOTAL_TIME_MESSAGE + (finishedTime.getTime() - startTime.getTime()) / (60.0 * 1000.0)
	            + " minutes" + " \nProcessed Coins : " + processed);
	    System.out.println("Errors: " + errors + "\nCoins that didn't get processed: " + errored);
	    System.out.println(
	            "**************************************************************************************************************************************************");
	}

	// Take position for a specific coin, handling buy or sell positions based on current market conditions
	private void takePositionForCoin(String coin, Map<String, TickerInfo> tickerInfoMap, Integer upMovement,
	        Integer downMovement, List<PositionInfo> openPositions, List<OpenOrderInfo> openOrders)
	        throws BinanceConnectorException, BinanceClientException {
	    List<PositionInfo> positionInfos = CoinUtil.getOpenPosition(coin, openPositions);
	    PositionInfo positionInfo = positionInfos.isEmpty() ? null : positionInfos.get(0);
	    Map<String, Object> parameters = CoinUtil.updateParameters(coin, tickerInfoMap, MarketType.MARKET);
	    System.out.println("Current Position Info: " + positionInfo);

	    if (positionInfo == null && parameters != null) {
	        if (CoinUtil.shouldCreateOrder(openOrders, openPositions, downMovement, upMovement)) {
	            if (!pauseCreateOrders) {
	                System.out.println("Creating Order for : " + parameters);
	                futureOrderManager.createFuturePosition(parameters, 0);
	            }
	            return;
	        }
	    } else if (positionInfo != null) {
	        handleExistingPosition(coin, parameters, positionInfo);
	    }
	}

	// Handles an existing position, either buying or selling based on position data
	private void handleExistingPosition(String coin, Map<String, Object> parameters, PositionInfo positionInfo)
	        throws BinanceConnectorException, BinanceClientException {
	    if (positionInfo.getPositionAmount() < 0.0) {
	        System.out.println("Handling Existing Sell Order: " + positionInfo);
	        positionManager.handleNegativePosition(parameters, coin, positionInfo);
	    } else if (positionInfo.getPositionAmount() > 0.0) {
	        System.out.println("Handling Existing Buy Order: " + positionInfo);
	        positionManager.handlePositivePosition(parameters, coin, positionInfo);
	    }
	}
}