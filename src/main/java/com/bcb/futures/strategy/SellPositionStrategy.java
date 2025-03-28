package com.bcb.futures.strategy;

import java.util.List;
import java.util.Map;

import com.bcb.futures.strategy.executor.StrategyExecutorV3;
import com.bcb.transfer.OpenOrderInfo;
import com.bcb.transfer.TickerInfo;

public class SellPositionStrategy implements PositionProcessingStrategy {
    private final StrategyExecutorV3 executor;

    public SellPositionStrategy(StrategyExecutorV3 executor) {
        this.executor = executor;
    }

    @Override
    public void processPositions(Map<String, TickerInfo> tickerMap, List<OpenOrderInfo> openOrders) {
        for (Map.Entry<String, TickerInfo> entry : tickerMap.entrySet()) {
            

            
        }
    }
}