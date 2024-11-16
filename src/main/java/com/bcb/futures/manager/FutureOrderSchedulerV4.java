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

public class FutureOrderSchedulerV4 {
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

	public FutureOrderSchedulerV4() {
		this.symbols = CoinUtil.getAllFutureCoinsByTypeAndCategory();
		this.positionManager = new PositionManager(createSpotClient());
		this.futureOrderManager = new FutureOrderManager(createSpotClient());
	}

	public static void main(String[] args) {
		FutureOrderSchedulerV4 futureOrderScheduler = new FutureOrderSchedulerV4();
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

		scheduler.scheduleAtFixedRate(futureOrderScheduler::takePositions, 0, EXECUTION_INTERVAL_MINUTES,
				TimeUnit.MINUTES);
		
		scheduler.scheduleAtFixedRate(futureOrderScheduler::takeOppositePositions, 0, EXECUTION_INTERVAL_MINUTES,
				TimeUnit.MINUTES);
		
		scheduler.scheduleAtFixedRate(futureOrderScheduler::takePositionsForPrefixed1000, 0, EXECUTION_INTERVAL_MINUTES,
				TimeUnit.MINUTES);
		
		scheduler.scheduleAtFixedRate(futureOrderScheduler::takeUSDCUSDTPositionsSideBySide, 0, EXECUTION_INTERVAL_MINUTES,
				TimeUnit.MINUTES);
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
        if (pauseNewOrderFor2Hrs && CoinUtil.checkIfCoolingPeriodPassed(pauseTimefor2Hrs)) {
            System.out.println("Cooling period passed, resuming order execution...");
            pauseNewOrderFor2Hrs = false;
            pauseTimefor2Hrs = null;
        } else if (pauseNewOrderFor2Hrs) {
            System.out.println("Got Futures Trading Quantitative Rules violated error: Job paused for next 2 hours...");
            return;
        }

        List<OpenOrderInfo> openOrderList = futureOrderManager.getOpenOrders();
        List<PositionInfo> openPositionList = positionManager.getAllOpenPositions();
        List<String> usdcSymbols = extractUSDCPositions(openPositionList);
        List<String> usdtSymbols = convertToUSDT(usdcSymbols);

        List<PositionInfo> openPositions = filterOpenPositions(openPositionList, usdcSymbols, usdtSymbols);
        symbols.addAll(usdcSymbols);
        symbols.addAll(usdtSymbols);
        Integer aggLeverage = calculateAggregateLeverage(openPositionList);
        System.out.println("Aggregate Leverage: " + aggLeverage);

        Collections.shuffle(symbols);
        Map<String, TickerInfo> tickerMap = MarketSentimentAnalyzer.getTickers(Coins.DESC, symbols.toArray(new String[0]));
        Integer upMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_UP);
        Integer downMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_DOWN);

        Date startTime = new Date();
        System.out.println(
                "**************************************************************************************************************************************************");
        System.out.println("Cron started at " + startTime);
        System.out.println("UpMovement: " + upMovement);
        System.out.println("DownMovement: " + downMovement);

        System.out.println("Open Orders " + openOrders.size());
        List<PositionInfo> buyPositions = CoinUtil.getOpenPositions(CoinUtil.openPosition(openPositions), Coins.BUY_SIDE);
        List<PositionInfo> sellPositions = CoinUtil.getOpenPositions(CoinUtil.openPosition(openPositions), Coins.SELL_SIDE);
        System.out.println("Open Positions: " + openPositions.size());
        System.out.println("Sell Count: " + sellPositions.size() + "\n Sell Positions: " + sellPositions);
        System.out.println("Buy Count: " + buyPositions.size() + "\n Buy Positions: " + buyPositions);

        List<String> errors = new ArrayList<>();
        Set<String> keySets = tickerMap.keySet();
        Iterator<String> iterator = keySets.iterator();

        while (iterator.hasNext() && !pauseNewOrderFor2Hrs) {
            String coin = iterator.next();
            if (pauseNewOrderFor2Hrs) {
                System.out.println("Got Futures Trading Quantitative Rules violated error: Job paused for next 2 hours...");
                return;
            }
            Map<String, Object> params = prepareParameters(coin, tickerMap, openOrderList, openPositions, upMovement, downMovement);
            if (params == null) continue;

            try {
                handleOrderCreation(params, coin, openOrderList, openPositions);
            } catch (BinanceConnectorException | BinanceClientException e) {
                CoinUtil.handleException(errors, coin, e);
            } catch (Exception e) {
                System.out.println("Exception: " + e.getMessage());
            }
        }
        System.out.println(
                "**************************************************************************************************************************************************");
        printResult(new ArrayList<>(keySets), errors, errored, startTime);
    }
	private List<String> extractUSDCPositions(List<PositionInfo> openPositionList) {
        return openPositionList.stream().filter(position ->
                position.getSymbol().endsWith("USDC") && !position.getSymbol().startsWith("1000"))
                .map(PositionInfo::getSymbol)
                .collect(Collectors.toList());
    }

    private List<String> convertToUSDT(List<String> usdcSymbols) {
        return usdcSymbols.stream()
                .map(symbol -> symbol.replace("USDC", "USDT"))
                .collect(Collectors.toList());
    }

    private List<PositionInfo> filterOpenPositions(List<PositionInfo> openPositionList, List<String> usdcSymbols, List<String> usdtSymbols) {
        return openPositionList.stream().filter(position ->
                (usdtSymbols.contains(position.getSymbol()) || position.getSymbol().endsWith("USDC"))
                        && !position.getSymbol().startsWith("1000") && position.getPositionAmount() != 0.0)
                .collect(Collectors.toList());
    }

    private Integer calculateAggregateLeverage(List<PositionInfo> openPositionList) {
        return openPositionList.stream().map(PositionInfo::getLeverage).mapToInt(Short::shortValue).sum() / openPositionList.size();
    }
	private Map<String, Object> prepareParameters(String coin, Map<String, TickerInfo> tickerMap, List<OpenOrderInfo> openOrderList, List<PositionInfo> openPositions, Integer upMovement, Integer downMovement) {
        Map<String, Object> params = new HashMap<>();
        params.put("type", MarketType.MARKET.toString());
        params.put("symbol", coin);
        boolean openOrderExist = CoinUtil.openOrderExist(coin, openOrderList);
        List<PositionInfo> positionInfoList = CoinUtil.getOpenPosition(coin, openPositions);
        PositionInfo positionInfo = positionInfoList.isEmpty() ? null : positionInfoList.get(0);
        boolean openPositionExist = positionInfo != null;

        if (Arrays.asList(Coins.SKIP_USDT_LIST).contains(coin)) return null;

        if (upMovement > downMovement && coin.endsWith("USDT")) {
            params.put("side", Coins.BUY_SIDE);
            params.put("quantity", CoinUtil.getQuantity(coin, tickerMap.get(coin).getLastPrice()));
        } else if (downMovement > upMovement && coin.endsWith("USDC")) {
            params.put("side", Coins.SELL_SIDE);
            String usdcCoin = coin.replace("USDC", "USDT");
            List<PositionInfo> usdcPositionInfoList = CoinUtil.getOpenPosition(usdcCoin, openPositions);
            PositionInfo usdcPositionInfo = usdcPositionInfoList.isEmpty() ? null : usdcPositionInfoList.get(0);
            if (usdcPositionInfo == null) return null;
            Double quantity = usdcPositionInfo.getPositionAmount();
            if (coin.equalsIgnoreCase("XRPUSDC") && quantity > 65) quantity /= 2;
            params.put("quantity", String.valueOf(new DecimalFormat("#.##").format(quantity)));
        } else {
            return null;
        }
        return params;
    }

    private void handleOrderCreation(Map<String, Object> params, String coin, List<OpenOrderInfo> openOrderList, List<PositionInfo> openPositions) throws BinanceConnectorException, BinanceClientException {
        boolean openOrderExist = CoinUtil.openOrderExist(coin, openOrderList);
        List<PositionInfo> positionInfoList = CoinUtil.getOpenPosition(coin, openPositions);
        PositionInfo positionInfo = positionInfoList.isEmpty() ? null : positionInfoList.get(0);
        boolean openPositionExist = positionInfo != null;

        if (!(openOrderExist && openPositionExist)) {
            if (!openPositionExist) {
                futureOrderManager.createFuturePosition(params, 0);
            } else if (positionInfo.getPositionAmount() < 0.0) {
                System.out.println("Handling Existing Sell Order : " + positionInfo);
                positionManager.handleNegativePosition(params, coin, positionInfo);
            } else if (positionInfo.getPositionAmount() > 0.0) {
                System.out.println("Handling Existing Buy Order : " + positionInfo);
                positionManager.handlePositivePosition(params, coin, positionInfo);
            }
        }
    }
	public void takePositions() {
		iteration++;
		resetState();

		resumeOrderExecutionIfCoolingPeriodPassed();
		if (pauseNewOrderFor2Hrs)
			return;

		openOrders = futureOrderManager.getOpenOrders().stream().filter(f -> !f.getSymbol().startsWith("1000"))
				.collect(Collectors.toList());

		List<PositionInfo> openPositionList = positionManager.getAllOpenPositions().stream()
				.filter(f -> !f.getSymbol().startsWith("1000")).collect(Collectors.toList());

		List<String> coinsNotToProcess = Arrays.asList(Coins.FUTURE_SYMBOLS_NOT_TO_BE_PROCESSED);
		List<String> invalidSymbolList = Arrays.asList(Coins.FUTURE_INVALID_SYMBOLS_FOR_TICKERS);
		openPositionList = openPositionList.stream()
				.filter(f -> !coinsNotToProcess.contains(f.getSymbol()) && !invalidSymbolList.contains(f.getSymbol()))
				.collect(Collectors.toList());

		List<PositionInfo> openPositions = openPositionList.stream().filter(f -> f.getPositionAmount() != 0.0)
				.collect(Collectors.toList());

		symbols = openPositionList.stream().map(PositionInfo::getSymbol).collect(Collectors.toList());
		Collections.shuffle(symbols);

		Map<String, TickerInfo> tickerMap = MarketSentimentAnalyzer.getTickers(Coins.DESC,
				symbols.toArray(new String[0]));
		Integer upMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_UP);
		Integer downMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_DOWN);

		printCronStartDetails(upMovement, downMovement);
		processCoins(tickerMap, upMovement, downMovement, openPositions);
		printCronEndDetails(new ArrayList<>(tickerMap.keySet()));
	}

	public void takeOppositePositions() {
		resetState();
		resumeOrderExecutionIfCoolingPeriodPassed();
		if (pauseNewOrderFor2Hrs)
			return;

		List<OpenOrderInfo> openOrderList = futureOrderManager.getOpenOrders();
		List<PositionInfo> openPositionList = positionManager.getAllOpenPositions().stream()
				.filter(f -> f.getPositionAmount() != 0.0).collect(Collectors.toList());

		System.out.println("open Orders " + openOrderList.size());
		System.out.println("open Positions " + openPositionList.size());

		List<String> oppositeSymbols = Arrays.asList(Coins.FUTURE_OPPOSITE_SYMBOLS);
		Map<String, TickerInfo> tickerMap = MarketSentimentAnalyzer.getTickers(null, Coins.FUTURE_OPPOSITE_COINS);
		Integer upMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_UP);
		Integer downMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_DOWN);

		System.out.println("UpCount: " + upMovement);
		System.out.println("DownCount: " + downMovement);

		List<String> errors = new ArrayList<>();
		Date startTime = new Date();
		System.out.println(
				"===================================================================================================================================");
		System.out.println("Cron Job for Opposite Coins started at " + startTime);

		String[] coins = { Coins.USDC, Coins.USDT };
		for (String cc : oppositeSymbols) {
			for (String symbol : coins) {
				String coin = cc + symbol;
				if (Arrays.asList(Coins.SKIP_LIST).contains(coin))
					continue;

				Map<String, Object> parameters = CoinUtil.updateParameters(coin, tickerMap, MarketType.MARKET);
				if (Coins.USDT.equalsIgnoreCase(symbol)) {
					parameters.put("side", Coins.BUY_SIDE);
				} else {
					parameters.put("side", Coins.SELL_SIDE);
					if (upMovement > downMovement)
						continue;
				}
				if (pauseNewOrderFor2Hrs) {
					System.out.println(
							"Got Futures Trading Quantitative Rules violated error: Job paused for next 2 hours...");
					return;
				}

				boolean openOrderExist = CoinUtil.openOrderExist(coin, openOrderList);
				PositionInfo positionInfo = CoinUtil.getOpenPosition(coin, openPositionList).stream().findFirst()
						.orElse(null);
				boolean openPositionExist = positionInfo != null;

				if (!openOrderExist || !openPositionExist) {
					try {
						if (!openPositionExist) {
							futureOrderManager.createFuturePosition(parameters, 0);
						} else if (positionInfo.getPositionAmount() < 0.0) {
							System.out.println("Handling Existing Sell Order : " + positionInfo);
							positionManager.handleNegativePosition(parameters, coin, positionInfo);
						} else if (positionInfo.getPositionAmount() > 0.0) {
							System.out.println("Handling Existing Buy Order : " + positionInfo);
							positionManager.handlePositivePosition(parameters, coin, positionInfo);
						}
					} catch (BinanceConnectorException | BinanceClientException e) {
						CoinUtil.handleException(errors, coin, e);
					} catch (Exception e) {
						System.out.println("Exception: " + e.getMessage());
					}
				}
			}
		}
		System.out.println(
				"===================================================================================================================================");
		printResult(new ArrayList<>(oppositeSymbols), errors, errored, startTime);
	}

	private void printResult(List<String> symbols, List<String> errors, List<String> errored, Date startTime) {
		symbols.removeAll(errored);
		Date finishedTime = new Date();
		System.out.println(CRON_FINISHED_MESSAGE + finishedTime);
		System.out.println(TOTAL_TIME_MESSAGE + (finishedTime.getTime() - startTime.getTime()) / (60.0 * 1000.0)
				+ " minutes" + " \nProcessed Coins : " + processed);
		System.out.println("Errors: " + errors + "\nCoin didn't get processed : " + errored);
		System.out.println(
				"**************************************************************************************************************************************************");
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
			List<PositionInfo> openPositions) {
		Set<String> keySets = tickerMap.keySet();
		Iterator<String> iterator = keySets.iterator();

		while (iterator.hasNext() && !pauseNewOrderFor2Hrs) {
			String coin = iterator.next();
			if (pauseNewOrderFor2Hrs) {
				System.out.println(
						"Got Futures Trading Quantitative Rules violated error: Job paused for next 2 hours...");
				return;
			}
			handleCoinPosition(coin, tickerMap, upMovement, downMovement, openPositions);
		}
	}

	public void takePositionsForPrefixed1000() {
		resetState();
		resumeOrderExecutionIfCoolingPeriodPassed();
		if (pauseNewOrderFor2Hrs)
			return;

		List<OpenOrderInfo> openOrderList = futureOrderManager.getOpenOrders();
		List<PositionInfo> openPositionList = positionManager.getAllOpenPositions().stream()
				.filter(f -> f.getPositionAmount() != 0.0).collect(Collectors.toList());

		System.out.println("open Orders " + openOrderList.size());
		System.out.println("open Positions " + openPositionList.size());

		List<String> setOf1000 = Arrays.asList(Coins.FUTURE_SYMBOLS_WITH_PREFIX_1000_COIN_NAME);
		Map<String, TickerInfo> tickerMap = MarketSentimentAnalyzer.getTickers(Coins.DESC,
				Coins.FUTURE_SYMBOLS_WITH_PREFIX_1000_COIN_NAME);
		Integer upMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_UP);
		Integer downMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_DOWN);

		System.out.println("UpCount: " + upMovement);
		System.out.println("DownCount: " + downMovement);

		List<String> errors = new ArrayList<>();
		Date startTime = new Date();
		System.out.println(
				"===================================================================================================================================");
		System.out.println("Cron Job for Set of 1000 Coins started at " + startTime);

		for (String symbol : setOf1000) {
			String coin = "1000" + symbol;
			if (Arrays.asList(Coins.SKIP_LIST).contains(coin))
				continue;

			Map<String, Object> parameters = CoinUtil.updateParameters(symbol, tickerMap, MarketType.MARKET);
			parameters.put("symbol", coin);
			parameters.put("side", upMovement > downMovement ? Coins.BUY_SIDE : Coins.SELL_SIDE);

			if (pauseNewOrderFor2Hrs) {
				System.out.println(
						"Got Futures Trading Quantitative Rules violated error: Job paused for next 2 hours...");
				return;
			}

			boolean openOrderExist = CoinUtil.openOrderExist(coin, openOrderList);
			PositionInfo positionInfo = CoinUtil.getOpenPosition(coin, openPositionList).stream().findFirst()
					.orElse(null);
			boolean openPositionExist = positionInfo != null;

			if (!openOrderExist || !openPositionExist) {
				try {
					handlePosition(parameters, coin, positionInfo);
				} catch (BinanceConnectorException | BinanceClientException e) {
					CoinUtil.handleException(errors, coin, e);
				} catch (Exception e) {
					System.out.println("Exception: " + e.getMessage());
				}
			}
		}
		System.out.println(
				"===================================================================================================================================");
		printResult(new ArrayList<>(setOf1000), errors, errored, startTime);
	}

	private void handlePosition(Map<String, Object> parameters, String coin, PositionInfo positionInfo)
			throws BinanceConnectorException, BinanceClientException {
		if (positionInfo == null) {
			futureOrderManager.createFuturePosition(parameters, 0);
		} else if (positionInfo.getPositionAmount() < 0.0) {
			System.out.println("Handling Existing Sell Order : " + positionInfo);
			positionManager.handleNegativePosition(parameters, coin, positionInfo);
		} else if (positionInfo.getPositionAmount() > 0.0) {
			System.out.println("Handling Existing Buy Order : " + positionInfo);
			positionManager.handlePositivePosition(parameters, coin, positionInfo);
		}
	}

	private void handleCoinPosition(String coin, Map<String, TickerInfo> tickerMap, Integer upMovement,
			Integer downMovement, List<PositionInfo> openPositions) {
		Map<String, Object> params = new HashMap<>();
		params.put("type", MarketType.MARKET.toString());
		params.put("symbol", coin);

		boolean openOrderExist = CoinUtil.openOrderExist(coin, openOrders);
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

	public void takeUSDCUSDTPositionsSideBySide() {
		boolean keepEitherOpenOrderOrOpenPosition = false;
		iteration++;
		resetState();
		resumeOrderExecutionIfCoolingPeriodPassed();
		if (pauseNewOrderFor2Hrs)
			return;

		List<OpenOrderInfo> openOrderList = futureOrderManager.getOpenOrders();
		List<PositionInfo> openPositionList = positionManager.getAllOpenPositions();

		List<String> usdcSymbols = openPositionList.stream()
				.filter(position -> position.getSymbol().endsWith("USDC") && !position.getSymbol().startsWith("1000"))
				.map(PositionInfo::getSymbol).collect(Collectors.toList());

		List<String> usdtSymbols = usdcSymbols.stream().map(symbol -> symbol.replace("USDC", "USDT"))
				.collect(Collectors.toList());

		List<PositionInfo> openPositions = openPositionList.stream().filter(
				position -> (usdtSymbols.contains(position.getSymbol()) || position.getSymbol().endsWith("USDC"))
						&& !position.getSymbol().startsWith("1000") && position.getPositionAmount() != 0.0)
				.collect(Collectors.toList());

		symbols = new ArrayList<>();
		symbols.addAll(usdcSymbols);
		symbols.addAll(usdtSymbols);
		Collections.shuffle(symbols);

		Map<String, TickerInfo> tickerMap = MarketSentimentAnalyzer.getTickers(Coins.DESC,
				symbols.toArray(new String[0]));
		Integer upMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_UP);
		Integer downMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_DOWN);

		Date startTime = new Date();
		System.out.println(
				"**************************************************************************************************************************************************");
		System.out.println("Cron started at " + startTime);
		System.out.println("UpMovement : " + upMovement);
		System.out.println("DownMovement : " + downMovement);

		System.out.println("Open Orders " + openOrders.size());
		List<PositionInfo> buyPositions = CoinUtil.getOpenPositions(CoinUtil.openPosition(openPositions),
				Coins.BUY_SIDE);
		List<PositionInfo> sellPositions = CoinUtil.getOpenPositions(CoinUtil.openPosition(openPositions),
				Coins.SELL_SIDE);

		double totalBuyAmount = buyPositions.stream().mapToDouble(CoinUtil::getPositionAmount).sum();
		double totalSellAmount = sellPositions.stream().mapToDouble(CoinUtil::getPositionAmount).sum();
		int buyAggLeverage = buyPositions.stream().mapToInt(PositionInfo::getLeverage).sum()
				/ Math.max(1, buyPositions.size());
		int sellAggLeverage = sellPositions.stream().mapToInt(PositionInfo::getLeverage).sum()
				/ Math.max(1, sellPositions.size());

		System.out.println("Buy Positions Aggregate Leverage: " + buyAggLeverage);
		System.out.println("Sell Positions Aggregate Leverage: " + sellAggLeverage);
		System.out.println("Open Positions " + openPositions.size());
		System.out.println("Buy Amount: " + totalBuyAmount);
		System.out.println("Sell Amount: " + totalSellAmount);
		System.out.println("Sell Count: " + sellPositions.size() + "\n Sell Positions: " + sellPositions);
		System.out.println("Buy Count: " + buyPositions.size() + "\n Buy Positions: " + buyPositions);

		List<String> errors = new ArrayList<>();
		for (String coin : tickerMap.keySet()) {
			if (pauseNewOrderFor2Hrs) {
				System.out.println(
						"Got Futures Trading Quantitative Rules violated error: Job paused for next 2 hours...");
				return;
			}

			Map<String, Object> params = new HashMap<>();
			params.put("type", MarketType.MARKET.toString());
			params.put("symbol", coin);

			boolean openOrderExist = CoinUtil.openOrderExist(coin, openOrderList);
			PositionInfo positionInfo = CoinUtil.getOpenPosition(coin, openPositions).stream().findFirst().orElse(null);
			boolean openPositionExist = positionInfo != null;

			if (Arrays.asList(Coins.SKIP_USDT_LIST).contains(coin))
				continue;

			if (coin.endsWith("T")) {
				params.put("side", Coins.BUY_SIDE);
				String usdcCoin = coin.replace("USDT", "USDC");
				PositionInfo usdcPositionInfo = CoinUtil.getOpenPosition(usdcCoin, openPositions).stream().findFirst()
						.orElse(null);
				if (usdcPositionInfo != null) {
					double quantity = Math.abs(usdcPositionInfo.getPositionAmount()) * 2;
					params.put("quantity", String.valueOf(new DecimalFormat("#.##").format(quantity)));
				} else {
					params.put("quantity", CoinUtil.getQuantity(coin, tickerMap.get(coin).getLastPrice()));
				}
			} else if (coin.endsWith("USDC")) {
				params.put("side", Coins.SELL_SIDE);
				String usdtCoin = coin.replace("USDC", "USDT");
				PositionInfo usdtPositionInfo = CoinUtil.getOpenPosition(usdtCoin, openPositions).stream().findFirst()
						.orElse(null);
				if (usdtPositionInfo == null)
					continue;
				double quantity = Math.abs(usdtPositionInfo.getPositionAmount());
				if (CoinUtil.getPositionAmount(usdtPositionInfo) > 65
						&& (Arrays.asList(Coins.SKIP_USDC_LIST).contains(coin) || downMovement < upMovement)) {
					quantity /= 2;
				} else if (downMovement < upMovement) {
					quantity /= 2;
				}
				params.put("quantity", String.valueOf(new DecimalFormat("#.##").format(quantity)));
			} else {
				continue;
			}

			System.out.println("Parameters: " + params);

			if (!keepEitherOpenOrderOrOpenPosition || !openOrderExist) {
				try {
					if (!openPositionExist) {
						futureOrderManager.createFuturePosition(params, 0);
					} else if (positionInfo.getPositionAmount() < 0.0) {
						System.out.println("Handling Existing Sell Order : " + positionInfo);
						positionManager.handleNegativePosition(params, coin, positionInfo);
					} else if (positionInfo.getPositionAmount() > 0.0) {
						System.out.println("Handling Existing Buy Order : " + positionInfo);
						positionManager.handlePositivePosition(params, coin, positionInfo);
					}
				} catch (BinanceConnectorException | BinanceClientException e) {
					CoinUtil.handleException(errors, coin, e);
				} catch (Exception e) {
					System.out.println("Exception: " + e.getMessage());
				}
			}
		}
		System.out.println(
				"**************************************************************************************************************************************************");
		printResult(new ArrayList<>(tickerMap.keySet()), errors, errored, startTime);
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

	private void printCronEndDetails(List<String> symbols) {
		symbols.removeAll(errored);
		Date finishedTime = new Date();
		System.out.println(CRON_FINISHED_MESSAGE + finishedTime);
		System.out.println(TOTAL_TIME_MESSAGE + (finishedTime.getTime() - finishedTime.getTime()) / (60.0 * 1000.0)
				+ " minutes" + " \nProcessed Coins : " + processed);
		System.out.println("Errors: Coins that didn't get processed: " + errored);
		System.out.println(
				"**************************************************************************************************************************************************");
	}
}
