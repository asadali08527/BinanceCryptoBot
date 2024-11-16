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

public class FutureOrderScheduler2 {

    private static final int EXECUTION_INTERVAL_MINUTES = 3;
    private static final int TWO_HOURS_IN_MILLIS = 2 * 60 * 60 * 1000;
    private PositionManager positionManager = null;
    private FutureOrderManager futureOrderManager = null;
    private List<String> symbols = new ArrayList<>();

    private boolean pauseNewOrderFor2Hrs = false;
    private Date pauseTimeFor2Hrs = null;
    private List<OpenOrderInfo> openOrders = new ArrayList<>();
    private List<String> errored = new ArrayList<>();
    private List<String> processed = new ArrayList<>();

    public FutureOrderScheduler2() {
        this.symbols = CoinUtil.getAllFutureCoinsByTypeAndCategory();
        this.positionManager = new PositionManager(createSpotClient());
        this.futureOrderManager = new FutureOrderManager(createSpotClient());
    }

    private SpotClient createSpotClient() {
        return new SpotClientImpl(PrivateConfig.TAA_API_KEY, PrivateConfig.TAA_SECRET_KEY, PrivateConfig.BASE_URLS[0]);
    }

    public static void main(String[] args) {
        FutureOrderScheduler2 scheduler = new FutureOrderScheduler2();
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(scheduler::takePositions, 0, EXECUTION_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    public void takePositions() {
        if (pauseNewOrderFor2Hrs && !isCoolingPeriodPassed()) {
            System.out.println("Cooling period in progress, pausing execution.");
            return;
        }

        initializeForNewRun();
        symbols = loadSymbolsToProcess();
        Map<String, TickerInfo> tickerMap = MarketSentimentAnalyzer.getTickers(Coins.DESC, symbols.toArray(new String[0]));

        Integer upMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_UP);
        Integer downMovement = MarketSentimentAnalyzer.marketMovement(tickerMap, Coins.MOVEMENT_DOWN);
        
        displayStartCronInfo(upMovement, downMovement);

        for (String coin : tickerMap.keySet()) {
            if (pauseNewOrderFor2Hrs) {
                System.out.println("Pausing job due to trading rules violation.");
                break;
            }
            processCoin(coin, tickerMap, upMovement, downMovement);
        }
        printRunResults(tickerMap.keySet());
    }

    private void initializeForNewRun() {
        processed.clear();
        errored.clear();
        openOrders = futureOrderManager.getOpenOrders();
    }

    private List<String> loadSymbolsToProcess() {
        List<PositionInfo> openPositions = positionManager.getAllOpenPositions();
        List<String> usdcSymbols = openPositions.stream()
                .filter(position -> position.getSymbol().endsWith("USDC") && !position.getSymbol().startsWith("1000"))
                .map(PositionInfo::getSymbol)
                .collect(Collectors.toList());
        
        List<String> usdtSymbols = usdcSymbols.stream().map(symbol -> symbol.replace("USDC", "USDT")).collect(Collectors.toList());

        List<String> allSymbols = new ArrayList<>(usdcSymbols);
        allSymbols.addAll(usdtSymbols);
        Collections.shuffle(allSymbols);
        return allSymbols;
    }

    private boolean isCoolingPeriodPassed() {
        if (pauseTimeFor2Hrs == null) return true;
        boolean coolingPeriodPassed = System.currentTimeMillis() - pauseTimeFor2Hrs.getTime() >= TWO_HOURS_IN_MILLIS;
        if (coolingPeriodPassed) {
            pauseNewOrderFor2Hrs = false;
            pauseTimeFor2Hrs = null;
        }
        return coolingPeriodPassed;
    }

    private void displayStartCronInfo(Integer upMovement, Integer downMovement) {
        Date startTime = new Date();
        System.out.println("********************************************************************");
        System.out.println("Cron started at " + startTime);
        System.out.println("UpMovement: " + upMovement);
        System.out.println("DownMovement: " + downMovement);
        System.out.println("Open Orders Count: " + openOrders.size());
    }

    private void processCoin(String coin, Map<String, TickerInfo> tickerMap, Integer upMovement, Integer downMovement) {
        try {
            Map<String, Object> params = buildParamsForOrder(coin, tickerMap, upMovement, downMovement);
            if (params == null) return;
            
            if (shouldCreateOrder(coin)) {
                PositionInfo positionInfo = CoinUtil.getOpenPosition(coin, positionManager.getAllOpenPositions()).stream().findFirst().orElse(null);
                executeOrderBasedOnPosition(positionInfo, params, coin);
            }
        } catch (BinanceConnectorException | BinanceClientException e) {
            CoinUtil.handleException(errored, coin, e);
        } catch (Exception e) {
            System.out.println("Exception processing coin " + coin + ": " + e.getMessage());
        }
    }

    private Map<String, Object> buildParamsForOrder(String coin, Map<String, TickerInfo> tickerMap, Integer upMovement, Integer downMovement) {
        Map<String, Object> params = new HashMap<>();
        params.put("type", MarketType.MARKET.toString());
        params.put("symbol", coin);

        if (upMovement > downMovement && coin.endsWith("USDT")) {
            params.put("side", Coins.BUY_SIDE);
            params.put("quantity", CoinUtil.getQuantity(coin, tickerMap.get(coin).getLastPrice()));
        } else if (downMovement > upMovement && coin.endsWith("USDC")) {
            params.put("side", Coins.SELL_SIDE);
            Double quantity = calculateAdjustedQuantity(coin, tickerMap.get(coin).getLastPrice());
            params.put("quantity", String.valueOf(new DecimalFormat("#.##").format(quantity)));
        } else {
            return null; // Skip processing if conditions are not met
        }
        return params;
    }

    private Double calculateAdjustedQuantity(String coin, Double lastPrice) {
        PositionInfo positionInfo = CoinUtil.getOpenPosition(coin.replace("USDC", "USDT"), positionManager.getAllOpenPositions()).stream().findFirst().orElse(null);
        return positionInfo != null ? positionInfo.getPositionAmount() * 0.5 : Double.valueOf(CoinUtil.getQuantity(coin, lastPrice));
    }

    private boolean shouldCreateOrder(String coin) {
        return !CoinUtil.openOrderExist(coin, openOrders) && !pauseNewOrderFor2Hrs;
    }

    private void executeOrderBasedOnPosition(PositionInfo positionInfo, Map<String, Object> params, String coin) throws BinanceConnectorException, BinanceClientException {
        if (positionInfo == null) {
            System.out.println("Creating new order for " + params);
            futureOrderManager.createFuturePosition(params, 0);
        } else {
            manageExistingPosition(params, coin, positionInfo);
        }
    }

    private void manageExistingPosition(Map<String, Object> params, String coin, PositionInfo positionInfo) throws BinanceConnectorException, BinanceClientException {
        if (positionInfo.getPositionAmount() < 0.0) {
            System.out.println("Handling Existing Sell Order for " + coin + ": " + positionInfo);
            positionManager.handleNegativePosition(params, coin, positionInfo);
        } else if (positionInfo.getPositionAmount() > 0.0) {
            System.out.println("Handling Existing Buy Order for " + coin + ": " + positionInfo);
            positionManager.handlePositivePosition(params, coin, positionInfo);
        }
    }

    private void printRunResults(Set<String> keySets) {
        System.out.println("********************************************************************");
        System.out.println("Cron finished at " + new Date());
        System.out.println("Processed Coins: " + processed.size());
        System.out.println("Errored Coins: " + errored);
    }
}
