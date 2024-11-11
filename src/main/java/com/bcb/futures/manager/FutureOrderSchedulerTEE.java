package com.bcb.futures.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

public class FutureOrderSchedulerTEE {
	private static final int EXECUTION_INTERVAL_MINUTES = 3;
	private static final String CRON_FINISHED_MESSAGE = "Cron Finished at ";
	private static final String TOTAL_TIME_MESSAGE = "Total Time taken to Execute The Job : ";
	public static List<OpenOrderInfo> openOrders = new ArrayList<>();
	static List<String> errored = new ArrayList<>();
	static List<String> invalidSymbol = new ArrayList<>();
	static List<String> processed = new ArrayList<>();
	static boolean pauseNewOrderFor2Hrs = false;
	public static Date pauseTimefor2Hrs = null;
	public static boolean pauseCreateOrders = false;

	private final PositionManager positionManager;
	private final FutureOrderManager futureOrderManager;
	private List<String> symbols = null;

	private SpotClient createSpotClient() {
		return new SpotClientImpl(PrivateConfig.TEE_API_KEY, PrivateConfig.TEE_SECRET_KEY, PrivateConfig.BASE_URLS[0]);
	}

	public FutureOrderSchedulerTEE() {
		this.symbols = CoinUtil.getAllFutureCoinsByTypeAndCategory();
		this.positionManager = new PositionManager(createSpotClient());
		this.futureOrderManager = new FutureOrderManager(createSpotClient());
	}

	public static void main(String[] args) {
		FutureOrderSchedulerTEE futureOrderScheduler = new FutureOrderSchedulerTEE();
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//		scheduler.scheduleAtFixedRate(futureOrderScheduler::takePositions, 0, EXECUTION_INTERVAL_MINUTES,
//				TimeUnit.MINUTES);
		scheduler.scheduleAtFixedRate(futureOrderScheduler::takeOppositePositions, 0, 1, TimeUnit.MINUTES);
		scheduler.scheduleAtFixedRate(futureOrderScheduler::takePositionsForPrefixed1000, 0, 47, TimeUnit.SECONDS);

	}

	public void takePositions() {
		boolean keepEitherOpenOrderOrOpenPosition = false;
		boolean openOrderExist = false;
		Integer upMovement = null;
		Integer downMovement = null;
		processed.clear();
		errored.clear();
		List<PositionInfo> openPositions = new ArrayList<>();
		Map<String, TickerInfo> tickerMap = new HashMap<>();
		if (pauseNewOrderFor2Hrs && CoinUtil.checkIfCoolingPeriodPassed(pauseTimefor2Hrs)) {
			System.out.println("Cooling period passed, resuming order execution...");
			pauseNewOrderFor2Hrs = false;
			pauseTimefor2Hrs = null;
		} else if (pauseNewOrderFor2Hrs) {
			System.out.println("Got Futures Trading Quantitative Rules violated error: Job paused for next 2 hours...");
			return;
		}
		openOrders = futureOrderManager.getOpenOrders();
		openOrders = openOrders.stream()
				.filter(f -> !(Arrays.asList(Coins.FUTURE_SYMBOLS_WITH_PREFIX_1000).contains(f.getSymbol())
						|| f.getSymbol().startsWith("1000")))
				.collect(Collectors.toList());
		List<PositionInfo> openPositionList = positionManager.getAllOpenPositions();
		openPositionList = openPositionList.stream()
				.filter(f -> !(Arrays.asList(Coins.FUTURE_SYMBOLS_WITH_PREFIX_1000).contains(f.getSymbol())
						|| f.getSymbol().startsWith("1000")))
				.collect(Collectors.toList());
		List<String> coinsNotToProcess = Arrays.asList(Coins.FUTURE_SYMBOLS_NOT_TO_BE_PROCESSED);
		List<String> invalidSymbolList = Arrays.asList(Coins.FUTURE_INVALID_SYMBOLS_FOR_TICKERS);
		openPositionList = openPositionList.stream().filter(f -> {
			return !(coinsNotToProcess.contains(f.getSymbol()) || invalidSymbolList.contains(f.getSymbol()));
		}).collect(Collectors.toList());
		openPositions = openPositionList.stream().filter(f -> f.getPositionAmount() != 0.0)
				.collect(Collectors.toList());
//		openPositions = openPositions.stream()
//				.filter(f -> !(f.getSymbol().equalsIgnoreCase("XRPUSDC") || f.getSymbol().equalsIgnoreCase("XRPUSDT")))
//				.collect(Collectors.toList());

		symbols = openPositionList.stream().map(m -> m.getSymbol()).collect(Collectors.toList());
//		Integer aggLeverage = openPositionList.stream().map(m -> m.getLeverage()).mapToInt(Integer::valueOf).sum()
//				/ openPositionList.size();
		// System.out.println(aggLeverage);
		// symbols.removeAll(Arrays.asList(Coins.FUTURE_INVALID_SYMBOLS_FOR_TICKERS));
		// symbols.removeAll(Arrays.asList(Coins.FUTURE_SYMBOLS_NOT_TO_BE_PROCESSED));
		Collections.shuffle(symbols);
		tickerMap = MarketSentimentAnalyzer.getTickers(Coins.DESC, symbols.toArray(new String[0]));
		upMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_UP);
		downMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_DOWN);

		Date startTime = new Date();
		System.out.println(
				"**************************************************************************************************************************************************");
		System.out.println("Cron started at " + startTime);
		System.out.println("open Orders " + openOrders.size());
		List<PositionInfo> buyPositions = CoinUtil.getOpenPositions(CoinUtil.openPosition(openPositions),
				Coins.BUY_SIDE);
		List<PositionInfo> sellPositions = CoinUtil.getOpenPositions(CoinUtil.openPosition(openPositions),
				Coins.SELL_SIDE);
		System.out.println("open Positions " + openPositions.size());
		System.out.println("SELL Count: " + sellPositions.size() + "\n Sell Positions: " + sellPositions);

		System.out.println("BUY Count: " + buyPositions.size() + "\n Buy Positions: " + buyPositions);
		List<String> errors = new ArrayList<>();
		Set<String> keySets = tickerMap.keySet();
		Iterator<String> iterator = keySets.iterator();

		while (iterator.hasNext() && !pauseNewOrderFor2Hrs) {
			String coin = iterator.next();
			if (pauseNewOrderFor2Hrs) {
				System.out.println(
						"Got Futures Trading Quantitative Rules violated error: Job paused for next 2 hours...");
				return;
			}
			// String coin = iterator.next();
			// openOrderExist = positionManager.openOrderExist(coin);
			openOrderExist = CoinUtil.openOrderExist(coin, openOrders);
			if (!(keepEitherOpenOrderOrOpenPosition && openOrderExist && coin.endsWith("USDC"))) {
				try {
					takePositionForCoin(coin, tickerMap, upMovement, downMovement, openPositions, openOrders);
				} catch (BinanceConnectorException | BinanceClientException e) {
					CoinUtil.handleException(errors, coin, e);
				} catch (Exception e) {
					System.out.println("Exception: " + e.getMessage());
				}
			}
		}
		System.out.println(
				"**************************************************************************************************************************************************");
		printResult(new ArrayList<>(keySets), errors, errored, startTime);
	}

	public void takeOppositePositions() {
		boolean openOrderExist = false;
		List<PositionInfo> openPositions = new ArrayList<>();
		List<OpenOrderInfo> openOrderList = new ArrayList<>();
		processed.clear();
		errored.clear();
		boolean keepEitherOpenOrderOrOpenPosition = false;
		if (pauseNewOrderFor2Hrs && CoinUtil.checkIfCoolingPeriodPassed(pauseTimefor2Hrs)) {
			System.out.println("Cooling period passed, resuming order execution...");
			pauseNewOrderFor2Hrs = false;
			pauseTimefor2Hrs = null;
		} else if (pauseNewOrderFor2Hrs) {
			System.out.println("Got Futures Trading Quantitative Rules violated error: Job paused for next 2 hours...");
			return;
		}
		openOrderList = futureOrderManager.getOpenOrders();
		List<PositionInfo> openPositionList = positionManager.getAllOpenPositions();
		openPositions = openPositionList.stream().filter(f -> f.getPositionAmount() != 0.0)
				.collect(Collectors.toList());
		System.out.println("open Orders " + openOrderList.size());
		System.out.println("open Positions " + openPositions.size());
		List<String> oppositeSymols = Arrays.asList(Coins.FUTURE_OPPOSITE_SYMBOLS);
		Map<String, TickerInfo> tickerMap = MarketSentimentAnalyzer.getTickers(null, Coins.FUTURE_OPPOSITE_COINS);
		Integer upMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_UP);
		Integer downMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_DOWN);
		System.out.println(" UpCount: " + upMovement);
		System.out.println(" DownCount: " + downMovement);
		List<String> errors = new ArrayList<>();
		Date startTime = new Date();
		System.out.println(
				"===================================================================================================================================");
		System.out.println("Cron Job for Opposite Coins started at " + startTime);
		// Set<String> keySets = tickerMap.keySet();
		Iterator<String> iterator = oppositeSymols.iterator();
		String[] coins = { Coins.USDC, Coins.USDT };
		while (iterator.hasNext() && !pauseNewOrderFor2Hrs) {
			String cc = iterator.next();
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

				// String coin = iterator.next();
				// openOrderExist = positionManager.openOrderExist(coin);
				openOrderExist = CoinUtil.openOrderExist(coin, openOrderList);
				List<PositionInfo> positionInfoList = CoinUtil.getOpenPosition(coin, openPositions);
				PositionInfo positionInfo = positionInfoList.size() != 0 ? positionInfoList.get(0) : null;
				boolean openPositionExist = positionInfo != null ? true : false;
				if (!(keepEitherOpenOrderOrOpenPosition && openOrderExist)) {
					try {

						if (!openPositionExist) {
							futureOrderManager.createFuturePosition(parameters, 0);
						} else if (positionInfo.getPositionAmount() < 0.0) {
							System.out.println("Handling Existing Sell Order : " + positionInfo);
							positionManager.handleNegativePosition(parameters, coin, positionInfo, tickerMap.get(coin));
						} else if (positionInfo.getPositionAmount() > 0.0) {
							// continue;
							System.out.println("Handling Existing Buy Order : " + positionInfo);
							positionManager.handlePositivePosition(parameters, coin, positionInfo, tickerMap.get(coin));
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
		printResult(new ArrayList<>(oppositeSymols), errors, errored, startTime);
	}

	public void takePositionsForPrefixed1000() {
		boolean openOrderExist = false;
		List<PositionInfo> openPositions = new ArrayList<>();
		List<OpenOrderInfo> openOrderList = new ArrayList<>();
		processed.clear();
		errored.clear();
		boolean keepEitherOpenOrderOrOpenPosition = false;
		if (pauseNewOrderFor2Hrs && CoinUtil.checkIfCoolingPeriodPassed(pauseTimefor2Hrs)) {
			System.out.println("Cooling period passed, resuming order execution...");
			pauseNewOrderFor2Hrs = false;
			pauseTimefor2Hrs = null;
		} else if (pauseNewOrderFor2Hrs) {
			System.out.println("Got Futures Trading Quantitative Rules violated error: Job paused for next 2 hours...");
			return;
		}
		openOrderList = futureOrderManager.getOpenOrders();
		List<PositionInfo> openPositionList = positionManager.getAllOpenPositions();
		openPositions = openPositionList.stream().filter(f -> f.getPositionAmount() != 0.0)
				.collect(Collectors.toList());
		System.out.println("open Orders " + openOrderList.size());
		System.out.println("open Positions " + openPositions.size());
		List<String> setOf1000 = Arrays.asList(Coins.FUTURE_SYMBOLS_WITH_PREFIX_1000_COIN_NAME);
		Map<String, TickerInfo> tickerMap = MarketSentimentAnalyzer.getTickers(Coins.DESC,
				Coins.FUTURE_SYMBOLS_WITH_PREFIX_1000_COIN_NAME);
		Integer upMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_UP);
		Integer downMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_DOWN);
		System.out.println(" UpCount: " + upMovement);
		System.out.println(" DownCount: " + downMovement);
		List<String> errors = new ArrayList<>();
		Date startTime = new Date();
		System.out.println(
				"===================================================================================================================================");
		System.out.println("Cron Job for Set of 1000 Coins started at " + startTime);
		// Set<String> keySets = tickerMap.keySet();
		Iterator<String> iterator = setOf1000.iterator();
		while (iterator.hasNext() && !pauseNewOrderFor2Hrs) {
			String symbol = iterator.next();
			String coin = "1000" + symbol;

			if (Arrays.asList(Coins.SKIP_LIST).contains(coin))
				continue;
			Map<String, Object> parameters = CoinUtil.updateParameters(symbol, tickerMap, MarketType.MARKET);
			parameters.put("symbol", coin);
			if (upMovement > downMovement) {
				parameters.put("side", Coins.BUY_SIDE);
			} else {
				parameters.put("side", Coins.SELL_SIDE);
			}

			if (pauseNewOrderFor2Hrs) {
				System.out.println(
						"Got Futures Trading Quantitative Rules violated error: Job paused for next 2 hours...");
				return;
			}

			// String coin = iterator.next();
			// openOrderExist = positionManager.openOrderExist(coin);
			openOrderExist = CoinUtil.openOrderExist(coin, openOrderList);
			List<PositionInfo> positionInfoList = CoinUtil.getOpenPosition(coin, openPositions);
			PositionInfo positionInfo = positionInfoList.size() != 0 ? positionInfoList.get(0) : null;
			boolean openPositionExist = positionInfo != null ? true : false;
			if (!(keepEitherOpenOrderOrOpenPosition && openOrderExist)) {
				try {

					if (!openPositionExist) {
						futureOrderManager.createFuturePosition(parameters, 0);
					} else if (positionInfo.getPositionAmount() < 0.0) {
						System.out.println("Handling Existing Sell Order : " + positionInfo);
						positionManager.handleNegativePosition(parameters, coin, positionInfo, tickerMap.get(symbol));
					} else if (positionInfo.getPositionAmount() > 0.0) {
						// continue;
						System.out.println("Handling Existing Buy Order : " + positionInfo);
						positionManager.handlePositivePosition(parameters, coin, positionInfo, tickerMap.get(symbol));
					}
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

	private void takePositionForCoin(String coin, Map<String, TickerInfo> tickerInfoMap, Integer upMovement,
			Integer downMovement, List<PositionInfo> openPositions, List<OpenOrderInfo> openOrders)
			throws BinanceConnectorException, BinanceClientException {
		List<PositionInfo> positionInfos = CoinUtil.getOpenPosition(coin, openPositions);
		PositionInfo positionInfo = positionInfos.isEmpty() ? null : positionInfos.get(0);
		Map<String, Object> parameters = CoinUtil.updateParameters(coin, tickerInfoMap, MarketType.MARKET);
//		if (positionInfo != null && parameters == null) {
//			return;
//		}
		System.out.println("Current Position Info: " + positionInfo);
		if (positionInfo == null && parameters != null) {
			if (CoinUtil.shouldCreateOrder(openOrders, openPositions, downMovement, upMovement)) {
				if (!pauseCreateOrders) {
					System.out.println("Creating Order for : " + parameters);
					futureOrderManager.createFuturePosition(parameters, 0);
				}
				return;
			}
		} else if (positionInfo.getPositionAmount() < 0.0) {
			System.out.println("Handling Existing Sell Order : " + positionInfo);
			positionManager.handleNegativePosition(parameters, coin, positionInfo, tickerInfoMap.get(coin));
		} else if (positionInfo.getPositionAmount() > 0.0) {
			System.out.println("Handling Existing Buy Order : " + positionInfo);
			positionManager.handlePositivePosition(parameters, coin, positionInfo, tickerInfoMap.get(coin));
		}
	}

	public static List<String> getAllFutureCoins() {
		return CoinUtil.getAllFutureCoinsByTypeAndCategory(Coins.FUTURE_USDT_COINS_IN_ACTION);
	}

}

//    private boolean openOrderExist(String symbol) {
//        SpotClient client = new SpotClientImpl(PrivateConfig.TAA_API_KEY, PrivateConfig.TAA_SECRET_KEY,
//                PrivateConfig.BASE_URLS[0]);
//
//        try {
//            Map<String, Object> params = new HashMap<>();
//            Gson gson = new Gson();
//            params.put("symbol", symbol);
//            String result = client.createFutures().getFuturesOpenOrders(params);
//            Type orderListType = new TypeToken<List<OpenOrderInfo>>() {
//            }.getType();
//            List<OpenOrderInfo> positionInfos = gson.fromJson(result, orderListType);
//            return !positionInfos.isEmpty();
//        } catch (Exception e) {
//            erroredCoins.add(symbol);
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    private void handleNegativePosition(Map<String, Object> parameters, String coin, PositionInfo positionInfo,
//                                        SpotClient client) throws BinanceConnectorException, BinanceClientException {
//
//        Double unRealizedProfit = positionInfo.getUnRealizedProfit();
//
//        if (unRealizedProfit >= 0.0 && parameters != null) {
//            if (isPositionAmountLT75Cent(coin, positionInfo)) {
//                increasePositionAmount(parameters, Coins.SELL_SIDE, client);
//                System.out.println("Position Increased for " + parameters);
//            } else if (unRealizedProfit >= 1.0 && parameters != null) {
//                closeAndCreatePosition(coin, positionInfo, client, parameters);
//            }
//        } else if (unRealizedProfit < 0) {
//            handleNegativeUnrealizedProfitForSellOrder(parameters, coin, positionInfo, client);
//        }
//    }
//
//    private static void closeFuturePosition(String coin, PositionInfo positionInfo, SpotClient client)
//            throws BinanceConnectorException, BinanceClientException {
//        Map<String, Object> parameters = new HashMap<>();
//        parameters.put("symbol", coin);
//        parameters.put("side", CoinUtil.reverseSide(CoinUtil.evaluateSide(positionInfo)));
//        parameters.put("type", "MARKET");
//        parameters.put("quantity", String.valueOf(Math.abs(positionInfo.getPositionAmount())));
//        String result = client.createFutures().createFuturesPosition(parameters);
//        System.out.println("Position Closed status for coin " + coin + " Result: " + result);
//    }
//
//    private void closeAndCreatePosition(String coin, PositionInfo positionInfo, SpotClient client,
//                                        Map<String, Object> parameters) throws BinanceConnectorException, BinanceClientException {
//        closeFuturePosition(coin, positionInfo, client);
//        if (!pauseNewOrder)
//            createFuturePosition(parameters, 0);
//    }
//
//    private static boolean isPositionAmountLT75Cent(String coin, PositionInfo positionInfo) {
//        double positionAmount = Math.abs(positionInfo.getPositionAmount()) * positionInfo.getEntryPrice()
//                / positionInfo.getLeverage();
//        return positionAmount <= 0.75;
//    }
//
//    private void handleNegativeUnrealizedProfitForSellOrder(Map<String, Object> parameters, String coin,
//                                                            PositionInfo positionInfo, SpotClient client) throws BinanceConnectorException, BinanceClientException {
//        if (positionInfo.getUnRealizedProfit() >= 1.0 && parameters != null) {
//            closeAndCreatePosition(coin, positionInfo, client, parameters);
//        } else if (positionInfo.getUnRealizedProfit() <= -2.0) {
//            System.out.println("Closing SELL order: " + positionInfo);
//            closeFuturePosition(coin, positionInfo, client);
//        } else if (parameters != null && CoinUtil.getPercentageGap(positionInfo.getEntryPrice(),
//                positionInfo.getMarkPrice()) >= Coins.PRICE_CHANGE_PERCENTAGE_THRESHOLD) {
//            closeAndCreatePosition(coin, positionInfo, client, parameters);
//        }
//    }
//
//    private void handlePositivePosition(Map<String, Object> parameters, String coin, PositionInfo positionInfo,
//                                        SpotClient client) throws BinanceConnectorException, BinanceClientException {
//
//        Double unRealizedProfit = positionInfo.getUnRealizedProfit();
//
//        if (unRealizedProfit >= 1.0 && parameters != null) {
//            closeAndCreatePosition(coin, positionInfo, client, parameters);
//        } else if (unRealizedProfit >= 0.0 && isPositionAmountLT75Cent(coin, positionInfo) && parameters != null) {
//            increasePositionAmount(parameters, Coins.BUY_SIDE, client);
//            System.out.println("Position Increased for " + parameters);
//        } else if (unRealizedProfit >= 1.0) {
//            closeFuturePosition(coin, positionInfo, client);
//        } else if (unRealizedProfit < 0) {
//            handleNegativeUnrealizedProfitForBuyOrder(parameters, coin, positionInfo, client);
//        }
//    }
//
//    private void increasePositionAmount(Map<String, Object> parameters, String side, SpotClient client)
//            throws BinanceConnectorException, BinanceClientException {
//        parameters.put("side", side);
//        createFuturePosition(parameters, 0);
//    }
//
//    private void handleNegativeUnrealizedProfitForBuyOrder(Map<String, Object> parameters, String coin,
//                                                           PositionInfo positionInfo, SpotClient client) throws BinanceConnectorException, BinanceClientException {
//
//        if (isPositionAmountLT75Cent(coin, positionInfo) && parameters != null) {
//            increasePositionAmount(parameters, Coins.BUY_SIDE, client);
//            System.out.println("Position Increased for " + parameters);
//        } else if (CoinUtil.getPercentageGap(positionInfo.getLiquidationPrice(), positionInfo.getMarkPrice())
//                <= Coins.POSITION_CLOSE_THRESOLD_PERCENTAGE) {
//            closeFuturePosition(coin, positionInfo, client);
//        }
//    }
//
//    private void createFuturePosition(Map<String, Object> parameters, int retry) {
//        try {
//            SpotClient client = new SpotClientImpl(PrivateConfig.TAA_API_KEY, PrivateConfig.TAA_SECRET_KEY,
//                    PrivateConfig.BASE_URLS[0]);
//            String result = client.createFutures().createFuturesPosition(parameters);
//            processedCoins.add(String.valueOf(parameters.get("symbol")));
//            System.out.println("Position Creation status for coin " + parameters + " Result: " + result);
//        } catch (BinanceConnectorException e) {
//            System.err.println((String) String.format("fullErrMessage: %s", e.getMessage()));
//        } catch (BinanceClientException e) {
//            handleBinanceClientException(parameters, e, retry);
//        } catch (Exception e) {
//            //handleGenericException(parameters, e);
//        }
//    }
//
//    private void handleBinanceClientException(Map<String, Object> parameters, BinanceClientException e, int retry) {
//        System.err.println((String) String.format("fullErrMessage: %s \nerrMessage: %s \nerrCode: %d \nHTTPStatusCode: %d",
//                e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode()));
//
//        if (Coins.ERROR_CODE_1111.equalsIgnoreCase(String.valueOf(e.getErrorCode())) && retry <= 1) {
//            retry += 1;
//            parameters.put("quantity", CoinUtil.adjustPrecision(String.valueOf(parameters.get("quantity"))));
//            parameters.remove("timestamp");
//            parameters.remove("signature");
//            createFuturePosition(parameters, retry);
//            System.out.println("Position created by reducing precision for coin " + parameters);
//        } else if (Coins.ERROR_CODE_4164.equalsIgnoreCase(String.valueOf(e.getErrorCode())) && retry <= 4) {
//            retry += 1;
//            parameters.put("quantity", CoinUtil.doubleQuantity(String.valueOf(parameters.get("quantity"))));
//            parameters.remove("timestamp");
//            parameters.remove("signature");
//            System.out.println("Position Retrying by doubling quantity for coin " + parameters);
//            createFuturePosition(parameters, retry);
//            System.out.println("Position created");
//        }
//    }
//
//    private static Map<String, Object> updateParameters(Map<String, Object> params, String coin, Map<String, TickerInfo> tickerMap) {
//        Sentiment sentiment = MarketSentimentAnalyzer.getSentiment(coin, MARKET_TYPE, tickerMap);
//        if (sentiment != null && (sentiment.getSide() == null || Coins.HOLD_SIDE.equalsIgnoreCase(sentiment.getSide())))
//            return null;
//        params.put("symbol", sentiment.getSymbol());
//        params.put("side", sentiment.getSide());
//        params.put("type", sentiment.getType());
//        params.put("quantity", CoinUtil.getQuantity(sentiment.getSymbol(), Double.valueOf(sentiment.getPrice())));
//        params.put("price", sentiment.getPrice());
//        params.put("timeInForce", sentiment.getTimeInForce());
//        params.put("closePosition", sentiment.getClosePosition());
//        params.put("newOrderRespType", sentiment.getNewOrderRespType());
//        System.out.println("Parameters: " + params);
//        return params;
//    }
//	public void quickOrder() {
//		Map<String, Object> params = new HashMap<>();
//		//Sentiment sentiment = MarketSentimentAnalyzer.getSentiment(coin, marketType, tickerMap);
//			params.put("symbol", "XRPUSDC");
//			params.put("side", "BUY");
//			params.put("type", MarketType.MARKET.toString());
//			params.put("quantity", "250");
//			System.out.println("Parameters: " + params);
//
//			futureOrderManager.createFuturePosition(params, 0);
//	}
//}
