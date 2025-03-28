package com.bcb.futures.strategy;


import com.bcb.transfer.TickerInfo;
import com.bcb.transfer.OpenOrderInfo;
import com.bcb.transfer.PositionInfo;
import com.bcb.futures.strategy.executor.StrategyExecutorV3;
import com.bcb.trade.constants.Coins;
import com.bcb.trade.util.CoinUtil;
import com.bcb.trade.util.PositionCalculator;
import java.util.Map;
import java.util.List;

public class BuyPositionStrategy implements PositionProcessingStrategy {
    private final StrategyExecutorV3 executor;

    public BuyPositionStrategy(StrategyExecutorV3 executor) {
        this.executor = executor;
    }

    @Override
    public void processPositions(Map<String, TickerInfo> tickerMap, List<OpenOrderInfo> openOrders) {
        for (Map.Entry<String, TickerInfo> entry : tickerMap.entrySet()) {
            String coin = entry.getKey();
            TickerInfo tickerInfo = entry.getValue();

           
        }
    }
    
}