package com.bcb.futures.manager;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
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

public class FutureOrderSchedulerV3 {

    private static final Logger LOGGER = Logger.getLogger(FutureOrderSchedulerV3.class.getName());
    private static final int EXECUTION_INTERVAL_SECONDS = 40;
    private static final int COOLING_PERIOD_HOURS = 2;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    private final List<String> symbols;
    private final PositionManager positionManager;
    private final FutureOrderManager futureOrderManager;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private boolean pauseNewOrderFor2Hrs = false;
    private Date pauseTimeFor2Hrs = null;

    public FutureOrderSchedulerV3() {
        SpotClient client = createSpotClient();
        this.symbols = CoinUtil.getAllFutureCoinsByTypeAndCategory();
        this.positionManager = new PositionManager(client);
        this.futureOrderManager = new FutureOrderManager(client);
    }

    private SpotClient createSpotClient() {
        return new SpotClientImpl(PrivateConfig.TAA_API_KEY, PrivateConfig.TAA_SECRET_KEY, PrivateConfig.BASE_URLS[0]);
    }

    public static void main(String[] args) {
        FutureOrderSchedulerV3 scheduler = new FutureOrderSchedulerV3();
        scheduler.scheduleTasks();
    }

    public void scheduleTasks() {
        scheduler.scheduleAtFixedRate(this::takeUSDCUSDTPositionsSideBySide, 0, EXECUTION_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void takeUSDCUSDTPositionsSideBySide() {
        if (checkCoolingPeriod()) return;

        Map<String, TickerInfo> tickerMap = getTickerMap(symbols);
        int upMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_UP);
        int downMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_DOWN);

        LOGGER.info("Executing USDC-USDT Position Check.");
        LOGGER.info("UpMovement: " + upMovement + ", DownMovement: " + downMovement);

        List<PositionInfo> openPositions = getOpenPositions();
        List<OpenOrderInfo> openOrders = futureOrderManager.getOpenOrders();

        symbols.forEach(coin -> processCoin(coin, tickerMap, openPositions, openOrders, upMovement, downMovement));
    }

    private boolean checkCoolingPeriod() {
        if (pauseNewOrderFor2Hrs && CoinUtil.checkIfCoolingPeriodPassed(pauseTimeFor2Hrs)) {
            LOGGER.info("Cooling period passed, resuming order execution...");
            pauseNewOrderFor2Hrs = false;
            pauseTimeFor2Hrs = null;
        } else if (pauseNewOrderFor2Hrs) {
            LOGGER.warning("Job paused for " + COOLING_PERIOD_HOURS + " hours due to trading restrictions.");
            return true;
        }
        return false;
    }

    private Map<String, TickerInfo> getTickerMap(List<String> symbols) {
        Collections.shuffle(symbols);
        return MarketSentimentAnalyzer.getTickers(Coins.DESC, symbols.toArray(new String[0]));
    }

    private List<PositionInfo> getOpenPositions() {
        return positionManager.getAllOpenPositions().stream()
                .filter(position -> position.getPositionAmount() != 0.0)
                .collect(Collectors.toList());
    }

    private void processCoin(String coin, Map<String, TickerInfo> tickerMap, List<PositionInfo> openPositions, 
                             List<OpenOrderInfo> openOrders, int upMovement, int downMovement) {
        if (Arrays.asList(Coins.SKIP_USDT_LIST).contains(coin)) return;

        Map<String, Object> params = initializeParams(coin, tickerMap, upMovement, downMovement);

        boolean openOrderExist = CoinUtil.openOrderExist(coin, openOrders);
        List<PositionInfo> positionInfoList = CoinUtil.getOpenPosition(coin, openPositions);
        PositionInfo positionInfo = !positionInfoList.isEmpty() ? positionInfoList.get(0) : null;

        if (!openOrderExist && positionInfo == null) {
            createNewPosition(params);
        } else if (positionInfo != null) {
            handleExistingPosition(params, coin, positionInfo);
        }
    }

    private Map<String, Object> initializeParams(String coin, Map<String, TickerInfo> tickerMap, int upMovement, int downMovement) {
        Map<String, Object> params = new HashMap<>();
        params.put("type", MarketType.MARKET.toString());
        params.put("symbol", coin);

        if (upMovement > downMovement && coin.endsWith("USDT")) {
            params.put("side", Coins.BUY_SIDE);
            params.put("quantity", CoinUtil.getQuantity(coin, tickerMap.get(coin).getLastPrice()));
        } else if (downMovement > upMovement && coin.endsWith("USDC")) {
            params.put("side", Coins.SELL_SIDE);
            params.put("quantity", calculateSellQuantity(coin, tickerMap, openPositions));
        }
        return params;
    }

    private String calculateSellQuantity(String coin, Map<String, TickerInfo> tickerMap, List<PositionInfo> openPositions) {
        String usdcCoin = coin.replace("USDC", "USDT");
        List<PositionInfo> usdcPositionInfoList = CoinUtil.getOpenPosition(usdcCoin, openPositions);
        PositionInfo usdcPositionInfo = !usdcPositionInfoList.isEmpty() ? usdcPositionInfoList.get(0) : null;

        if (usdcPositionInfo == null) return null;

        double quantity = Math.abs(usdcPositionInfo.getPositionAmount());
        if (CoinUtil.getPositionAmount(usdcPositionInfo) > 65) {
            quantity /= 2;
        }
        return DECIMAL_FORMAT.format(quantity);
    }

    private void createNewPosition(Map<String, Object> params) {
        try {
            futureOrderManager.createFuturePosition(params, 0);
        } catch (BinanceConnectorException | BinanceClientException e) {
            LOGGER.warning("Failed to create new position: " + e.getMessage());
        }
    }

    private void handleExistingPosition(Map<String, Object> params, String coin, PositionInfo positionInfo) {
        try {
            if (positionInfo.getPositionAmount() < 0.0) {
                positionManager.handleNegativePosition(params, coin, positionInfo);
            } else if (positionInfo.getPositionAmount() > 0.0) {
                positionManager.handlePositivePosition(params, coin, positionInfo);
            }
        } catch (BinanceConnectorException | BinanceClientException e) {
            LOGGER.warning("Error handling existing position for coin " + coin + ": " + e.getMessage());
        }
    }

    private void printResults(List<String> processedCoins, List<String> errors, Date startTime) {
        Date finishedTime = new Date();
        LOGGER.info("Cron Finished at " + finishedTime);
        LOGGER.info("Total Time taken to Execute The Job: " + (finishedTime.getTime() - startTime.getTime()) / (60.0 * 1000.0) + " minutes");
        LOGGER.info("Processed Coins: " + processedCoins);
        LOGGER.info("Errors: " + errors);
    }
}
