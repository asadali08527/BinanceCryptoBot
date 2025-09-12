package com.bcb.futures.manager;

import java.text.DecimalFormat;
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
import com.bcb.trade.util.PositionCalculator;
import com.bcb.transfer.BalanceInfo;
import com.bcb.transfer.OpenOrderInfo;
import com.bcb.transfer.PositionInfo;
import com.bcb.transfer.TickerInfo;

public class FutureOrderSchedulerTAA {
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
	public int iteration = 0;
	private final PositionManager positionManager;
	private final FutureOrderManager futureOrderManager;
	private final WalletManager walletManager;
	private final OrderManager orderManager;

	private List<String> symbols = null;

	private SpotClient createSpotClient() {
		return new SpotClientImpl(PrivateConfig.TAA_API_KEY, PrivateConfig.TAA_SECRET_KEY, PrivateConfig.BASE_URLS[0]);
	}

	public FutureOrderSchedulerTAA() {
		this.symbols = CoinUtil.getAllFutureCoinsByTypeAndCategory();
		SpotClient client = createSpotClient();
		this.positionManager = new PositionManager(client);
		this.futureOrderManager = new FutureOrderManager(client);
		this.walletManager = new WalletManager(client);
		this.orderManager = new OrderManager(client);
	}

	public static void main(String[] args) {
		FutureOrderSchedulerTAA futureOrderScheduler = new FutureOrderSchedulerTAA();
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

		scheduler.scheduleAtFixedRate(futureOrderScheduler::takeUSDCUSDTPositionsSideBySide, 0, 40, TimeUnit.SECONDS);
		// scheduler.scheduleAtFixedRate(futureOrderScheduler::takePositions, 0,
		// EXECUTION_INTERVAL_MINUTES,
		// TimeUnit.MINUTES);
//		scheduler.scheduleAtFixedRate(futureOrderScheduler::takeOppositePositions, 0, 1, TimeUnit.MINUTES);
//		scheduler.scheduleAtFixedRate(futureOrderScheduler::takePositionsForPrefixed1000, 0, 47, TimeUnit.SECONDS);
//		scheduler.scheduleAtFixedRate(futureOrderScheduler::takeUSDCUSDTPositions, 0, 40, TimeUnit.SECONDS);

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

	public static List<String> getAllFutureCoins() {
		return CoinUtil.getAllFutureCoinsByTypeAndCategory(Coins.FUTURE_USDT_COINS_IN_ACTION);
	}

	public void takeUSDCUSDTPositions() {
		iteration = iteration + 1;
		boolean keepEitherOpenOrderOrOpenPosition = false;
		boolean openOrderExist = false;
		Integer upMovement = null;
		Integer downMovement = null;
		processed.clear();
		errored.clear();
		symbols.clear();
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
		List<OpenOrderInfo> openOrderList = futureOrderManager.getOpenOrders();
		List<PositionInfo> openPositionList = positionManager.getAllOpenPositions();
		List<String> usdcSymbols = openPositionList.stream().filter(position -> {
			return position.getSymbol().endsWith("USDC") && !position.getSymbol().startsWith("1000");
		}).map(m -> {
			return m.getSymbol();
		}).collect(Collectors.toList());
		List<String> usdtSymbols = usdcSymbols.stream().map(symbol -> {
			return symbol.replace("USDC", "USDT");
		}).collect(Collectors.toList());

		openPositions = openPositionList.stream().filter(position -> {
			return (usdtSymbols.contains(position.getSymbol()) || position.getSymbol().endsWith("USDC"))
					&& !position.getSymbol().startsWith("1000");
		}).collect(Collectors.toList());
		openPositions = openPositionList.stream().filter(f -> f.getPositionAmount() != 0.0)
				.collect(Collectors.toList());
		symbols.addAll(usdcSymbols);
		symbols.addAll(usdtSymbols);
		Integer aggLeverage = openPositionList.stream().map(m -> m.getLeverage()).mapToInt(Integer::valueOf).sum()
				/ openPositionList.size();
		System.out.println(aggLeverage);

		Collections.shuffle(symbols);
		tickerMap = MarketSentimentAnalyzer.getTickers(Coins.DESC, symbols.toArray(new String[0]));
		upMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_UP);
		downMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_DOWN);

		Date startTime = new Date();
		System.out.println(
				"**************************************************************************************************************************************************");
		System.out.println("Cron started at " + startTime);
		System.out.println("UpMovement :" + upMovement);
		System.out.println("DownMovement :" + downMovement);

		System.out.println("Open Orders " + openOrders.size());
		List<PositionInfo> buyPositions = CoinUtil.getOpenPositions(CoinUtil.openPosition(openPositions),
				Coins.BUY_SIDE);
		List<PositionInfo> sellPositions = CoinUtil.getOpenPositions(CoinUtil.openPosition(openPositions),
				Coins.SELL_SIDE);
		System.out.println("Open Positions " + openPositions.size());
		System.out.println("Sell Count: " + sellPositions.size() + "\n Sell Positions: " + sellPositions);

		System.out.println("Buy Count: " + buyPositions.size() + "\n Buy Positions: " + buyPositions);
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
			Map<String, Object> params = new HashMap<>();
			params.put("type", MarketType.MARKET.toString());
			params.put("symbol", coin);
			openOrderExist = CoinUtil.openOrderExist(coin, openOrderList);
			List<PositionInfo> positionInfoList = CoinUtil.getOpenPosition(coin, openPositions);
			PositionInfo positionInfo = positionInfoList.size() != 0 ? positionInfoList.get(0) : null;
			boolean openPositionExist = positionInfo != null ? true : false;
			if (Arrays.asList(Coins.SKIP_USDT_LIST).contains(coin))
				continue;
			if (upMovement > downMovement && coin.endsWith("USDT")) {
				params.put("side", Coins.BUY_SIDE);
				params.put("quantity", CoinUtil.getQuantity(coin, tickerMap.get(coin).getLastPrice()));
			} else if (downMovement > upMovement && coin.endsWith("USDC")) {
				params.put("side", Coins.SELL_SIDE);
				String usdcCoin = coin.replace("USDC", "USDT");
				List<PositionInfo> usdcPositionInfoList = CoinUtil.getOpenPosition(usdcCoin, openPositions);
				PositionInfo usdcPositionInfo = usdcPositionInfoList.size() != 0 ? usdcPositionInfoList.get(0) : null;
				if (usdcPositionInfo == null)
					continue;
				Double quantity = usdcPositionInfo.getPositionAmount();
				if (coin.equalsIgnoreCase("XRPUSDC") && quantity > 65)
					quantity = quantity / 2;
				params.put("quantity", String.valueOf(new DecimalFormat("#.##").format(quantity)));
			} else {
				continue;
			}
			System.out.println("Parameters: " + params);

			if (!(keepEitherOpenOrderOrOpenPosition && openOrderExist)) {
				try {

					if (!openPositionExist) {
						futureOrderManager.createFuturePosition(params, 0);
					} else if (positionInfo.getPositionAmount() < 0.0) {
						System.out.println("Handling Existing Sell Order : " + positionInfo);
						positionManager.handleNegativePosition(params, coin, positionInfo);
					} else if (positionInfo.getPositionAmount() > 0.0) {
						// continue;
						System.out.println("Handling Existing Buy Order : " + positionInfo);
						positionManager.handlePositivePosition(params, coin, positionInfo);
					}
				} catch (BinanceConnectorException | BinanceClientException e) {
					CoinUtil.handleException(errors, coin, e);
				} catch (Exception e) {
					System.out.println("Exception: " + e.getMessage());
				}
			}
			if (!(keepEitherOpenOrderOrOpenPosition && openOrderExist)) {
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

	public void takeUSDCUSDTPositionsSideBySide() {
		iteration = iteration + 1;
		boolean keepEitherOpenOrderOrOpenPosition = false;
		boolean openOrderExist = false;
		Integer upMovement = null;
		Integer downMovement = null;
		Map<String, TickerInfo> tickerMap = new HashMap<>();

		processed.clear();
		errored.clear();
		symbols.clear();
		// symbols = CoinUtil.getUsdtUsdcSymbols();
		List<PositionInfo> openPositions = new ArrayList<>();
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
		List<String> usdcSymbols = openPositionList.stream().filter(position -> {
			return position.getSymbol().endsWith("USDC") && !position.getSymbol().startsWith("1000");
		}).map(m -> {
			return m.getSymbol();
		}).collect(Collectors.toList());
		List<String> usdtSymbols = usdcSymbols.stream().map(symbol -> {
			return symbol.replace("USDC", "USDT");
		}).collect(Collectors.toList());

		openPositions = openPositionList.stream().filter(position -> {
			return (usdtSymbols.contains(position.getSymbol()) || position.getSymbol().endsWith("USDC"))
					&& !position.getSymbol().startsWith("1000");
		}).collect(Collectors.toList());
		openPositions = openPositionList.stream().filter(f -> f.getPositionAmount() != 0.0)
				.collect(Collectors.toList());
		symbols.addAll(usdcSymbols);
		symbols.addAll(usdtSymbols);

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

		System.out.println("Open Orders " + openOrders.size());
		List<PositionInfo> buyPositions = CoinUtil.getOpenPositions(CoinUtil.openPosition(openPositions),
				Coins.BUY_SIDE);
		Double totalBuyAmount = buyPositions.stream().mapToDouble(CoinUtil::getPositionAmount).sum();
		List<PositionInfo> sellPositions = CoinUtil.getOpenPositions(CoinUtil.openPosition(openPositions),
				Coins.SELL_SIDE);
		Double totalSellAmount = sellPositions.stream().mapToDouble(CoinUtil::getPositionAmount).sum();
		Integer buyAggLeverage = buyPositions.size() != 0
				? buyPositions.stream().map(m -> m.getLeverage()).mapToInt(Integer::valueOf).sum() / buyPositions.size()
				: 0;
		Integer sellAggLeverage = sellPositions.size() != 0
				? sellPositions.stream().map(m -> m.getLeverage()).mapToInt(Integer::valueOf).sum()
						/ sellPositions.size()
				: 0;
		List<PositionInfo> buyPositionsInProfit = buyPositions.stream().filter(f -> f.getUnRealizedProfit() > 0)
				.collect(Collectors.toList());
		System.out.println("Buy Positions Aggregiate Levarage: " + buyAggLeverage);
		System.out.println("Sell Positions Aggregiate Levarage: " + sellAggLeverage);
		System.out.println("Open Positions " + openPositions.size());
		System.out.println("Buy Positions in Profit: " + buyPositionsInProfit.size());
		System.out.println("Buy Amount: " + totalBuyAmount);
		System.out.println("Sell Amount: " + totalSellAmount);
		System.out.println("Sell Count: " + sellPositions.size() + "\n Sell Positions: " + sellPositions);
		System.out.println("Buy Count: " + buyPositions.size() + "\n Buy Positions: " + buyPositions);
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
			Map<String, Object> params = new HashMap<>();
			params.put("type", MarketType.MARKET.toString());
			params.put("symbol", coin);
			openOrderExist = CoinUtil.openOrderExist(coin, openOrderList);
			List<OpenOrderInfo> openOrderInfoList = orderManager.getOpenOrder(coin, openOrderList);
			List<PositionInfo> positionInfoList = CoinUtil.getOpenPosition(coin, openPositions);

			PositionInfo positionInfo = positionInfoList.size() != 0 ? positionInfoList.get(0) : null;
			boolean openPositionExist = positionInfo != null ? true : false;
			TickerInfo tickerInfo = tickerMap.get(coin);
			System.out.println("Coin " + coin + ", : Ticker info: " + tickerInfo);
			BalanceInfo balanceInfo = walletManager.getFutureWalletBalance("USDT");
			System.out.println("BalanceInfo info: " + balanceInfo.getAvailableBalance());
			if (Arrays.asList(Coins.SKIP_USDT_LIST).contains(coin))
				continue;
			if (coin.endsWith("T")) {
				params.put("side", Coins.BUY_SIDE);
				String usdcCoin = coin.replace("USDT", "USDC");
				List<PositionInfo> usdcPositionInfoList = CoinUtil.getOpenPosition(usdcCoin, openPositions);
				PositionInfo usdcPositionInfo = usdcPositionInfoList.size() != 0 ? usdcPositionInfoList.get(0) : null;
				Double quantity = null;
				if (usdcPositionInfo != null) {
					quantity = Math.abs(usdcPositionInfo.getPositionAmount());
					quantity += quantity / 4;
					if (openPositionExist) {
						quantity = Math.abs(positionInfo.getPositionAmount() - Math.abs(usdcPositionInfo.getPositionAmount()));
					}
					if (CoinUtil.getPositionAmount(usdcPositionInfo) >= 65) {
						quantity = quantity / 2;
					}
					params.put("quantity", String.valueOf(new DecimalFormat("#.##").format(quantity)));
					Double usdtAmount = CoinUtil.getPositionAmount(positionInfo);
					Double usdcAmount = CoinUtil.getPositionAmount(usdcPositionInfo);

					if (downMovement == 0 && openPositionExist && positionInfo.getUnRealizedProfit() > 0) {
//						PositionCalculator.calculateUsdtAmount(Double.valueOf(positionInfo.getNotional()),
//								positionInfo.getLeverage());

						Double profitInPercentage = PositionCalculator
								.calculatePercentageProfit(positionInfo.getUnRealizedProfit(), usdtAmount);
						if (profitInPercentage > 50) {
							System.out.println("Buy usdtAmount: " + usdtAmount + " , Sell usdcAmount: " + usdcAmount);
							if (usdcAmount >= usdtAmount || Math.abs(usdcPositionInfo.getPositionAmount()) >= Math
									.abs(positionInfo.getPositionAmount())) {
								if (CoinUtil.getPositionAmount(usdcPositionInfo) <= 30
										&& buyPositionsInProfit.size() > 6) {
									quantity = quantity * 2;
								}
								params.put("quantity", String.valueOf(new DecimalFormat("#.##").format(quantity)));
								if (Double.valueOf(balanceInfo.getAvailableBalance()) > 0)
									orderManager.createFutureOpenOrder(coin, positionInfo, openOrderInfoList,
											usdcPositionInfo,true);
								if (buyPositionsInProfit.size() > 6 && Double.valueOf(
										balanceInfo.getAvailableBalance()) >= ((quantity * tickerInfo.getLastPrice())
												/ positionInfo.getLeverage()))
									futureOrderManager.createFuturePosition(params, 0);
								System.out.println("Doubled Buy order for param: " + params);
								continue;
							} else {
								if (Double.valueOf(balanceInfo.getAvailableBalance()) > 0) {
									orderManager.createFutureOpenOrder(coin, positionInfo, openOrderInfoList,
											usdcPositionInfo,true);
								}
							}
						}
					}
				} else {
					if (buyPositionsInProfit.size() < 2) {
						// Do not create new order inst
						continue;
					}
					params.put("quantity", CoinUtil.getQuantity(coin, tickerMap.get(coin).getLastPrice()));
				}

			} else if (coin.endsWith("USDC")) {
				String usdtCoin = coin.replace("USDC", "USDT");
				List<PositionInfo> usdtPositionInfoList = CoinUtil.getOpenPosition(usdtCoin, openPositions);
				PositionInfo usdtPositionInfo = usdtPositionInfoList.size() != 0 ? usdtPositionInfoList.get(0) : null;
				if (usdtPositionInfo == null || downMovement == 0)
					continue;
				Double quantity = Double.valueOf(CoinUtil.getQuantity(coin, tickerMap.get(coin).getLastPrice()));
				params.put("side", Coins.SELL_SIDE);
				params.put("quantity", String.valueOf(new DecimalFormat("#.##").format(quantity)));
				Integer usdcAmount = openPositionExist ? (int) CoinUtil.getPositionAmount(positionInfo) : 0;
				if (usdcAmount > 0) {
					Double profitInPercentage = PositionCalculator
							.calculatePercentageProfit(positionInfo.getUnRealizedProfit(), usdcAmount);
					if (openPositionExist && profitInPercentage > 50.0
							&& (Double.valueOf(balanceInfo.getAvailableBalance()) != 0.0
									&& Double.valueOf(balanceInfo.getAvailableBalance()) >= usdcAmount
									&& upMovement > downMovement)) {
						orderManager.createFutureOpenOrder(coin, positionInfo, openOrderInfoList, usdtPositionInfo,true);
					}
				}
				if (downMovement > 11 || tickerMap.get(coin).getPriceChangePercent() < 0) // ||
																							// (Double.valueOf(balanceInfo.getAvailableBalance())
																							// > totalSellAmount &&
																							// Double.valueOf(balanceInfo.getAvailableBalance())
																							// < totalBuyAmount)
				{
					if (downMovement >= 22) {
						Integer usdtAmount = (int) CoinUtil.getPositionAmount(usdtPositionInfo);
						if (usdtAmount >= usdcAmount) {
							System.out.println("SELL usdtAmount: " + usdtAmount + " , usdcAmount: " + usdcAmount);
							quantity = usdtPositionInfo.getPositionAmount()
									- (openPositionExist ? Math.abs(positionInfo.getPositionAmount()) : 0);
							System.out.println("SELL quantity difference: " + quantity + " Coin: " + coin);
							if (quantity != 0.0 && Double.valueOf(balanceInfo.getAvailableBalance()) > 0.0
									&& Double.valueOf(
											balanceInfo.getAvailableBalance()) > ((quantity * tickerInfo.getLastPrice())
													/ usdtPositionInfo.getLeverage())) {
								if (CoinUtil.getPositionAmount(usdtPositionInfo) / 2 > 65.0)
									quantity = quantity / 2;
								params.put("quantity", String.valueOf(new DecimalFormat("#.##").format(quantity)));
								futureOrderManager.createFuturePosition(params, 0);
								System.out.println("Stablised SELL order for param: " + params);
							}
							continue;
						}
					}

					if (CoinUtil.getPositionAmount(usdtPositionInfo) > 65
							&& (Arrays.asList(Coins.SKIP_USDC_LIST).contains(coin) || downMovement < upMovement)) {
						quantity = quantity / 4;
					} else if (downMovement < upMovement) {
						quantity = quantity / 2;
					}
					params.put("quantity", String.valueOf(new DecimalFormat("#.##").format(quantity)));
				}
			} else {
				continue;
			}
			System.out.println("Parameters: " + params);

			if (!(keepEitherOpenOrderOrOpenPosition && openOrderExist)) {
				try {
					if (!openPositionExist && Double.valueOf(balanceInfo.getAvailableBalance()) > 0.0) {
						futureOrderManager.createFuturePosition(params, 0);
					} else if (positionInfo.getPositionAmount() < 0.0 && Double
							.valueOf(balanceInfo.getAvailableBalance()) > CoinUtil.getPositionAmount(positionInfo)) {
						System.out.println("Handling Existing Sell Order : " + positionInfo);
						positionManager.handleNegativePosition(params, coin, positionInfo);
					} else if (positionInfo.getPositionAmount() > 0.0 && Double
							.valueOf(balanceInfo.getAvailableBalance()) > CoinUtil.getPositionAmount(positionInfo)) {
						// continue;
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
		System.out.println("UpMovement : " + upMovement);
		System.out.println("DownMovement : " + downMovement);

		printResult(new ArrayList<>(keySets), errors, errored, startTime);
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
		openOrders = new ArrayList<>();
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

		symbols = openPositionList.stream().map(m -> m.getSymbol()).collect(Collectors.toList());

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
							positionManager.handleNegativePosition(parameters, coin, positionInfo);
						} else if (positionInfo.getPositionAmount() > 0.0) {
							// continue;
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
						positionManager.handleNegativePosition(parameters, coin, positionInfo);
					} else if (positionInfo.getPositionAmount() > 0.0) {
						// continue;
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
		System.out.println(
				"===================================================================================================================================");
		printResult(new ArrayList<>(setOf1000), errors, errored, startTime);
	}

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
		} else if (positionInfo.getPositionAmount() < 0.0) {
			System.out.println("Handling Existing Sell Order : " + positionInfo);
			positionManager.handleNegativePosition(parameters, coin, positionInfo);
		} else if (positionInfo.getPositionAmount() > 0.0) {
			System.out.println("Handling Existing Buy Order : " + positionInfo);
			positionManager.handlePositivePosition(parameters, coin, positionInfo);
		}
	}
}