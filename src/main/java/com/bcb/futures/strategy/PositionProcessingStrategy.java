package com.bcb.futures.strategy;

import com.bcb.transfer.TickerInfo;
import com.bcb.transfer.OpenOrderInfo;
import java.util.Map;
import java.util.List;

public interface PositionProcessingStrategy {
    void processPositions(Map<String, TickerInfo> tickerMap, List<OpenOrderInfo> openOrders);
}
