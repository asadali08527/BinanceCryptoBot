package com.bcb.service;

import com.bcb.futures.manager.PositionManager;
import com.bcb.trade.constants.Coins;
import com.bcb.trade.util.CoinUtil;
import com.bcb.transfer.PositionInfo;
import java.util.List;
import java.util.stream.Collectors;

public class PositionService {
    private final PositionManager positionManager;

    public PositionService(PositionManager positionManager) {
        this.positionManager = positionManager;
    }

    public List<PositionInfo> getAllOpenPositions() {
        return positionManager.getAllOpenPositions();
    }

    public List<PositionInfo> filterPositions(List<PositionInfo> positions, String side) {
    	return CoinUtil.getOpenPositions(CoinUtil.openPosition(positions), side);
    }

    public List<PositionInfo> getProfitablePositions(List<PositionInfo> positions) {
        return positions.stream()
                .filter(position -> position.getUnRealizedProfit() > 0)
                .collect(Collectors.toList());
    }

    public int calculateAggregateLeverage(List<PositionInfo> positions) {
        return positions.isEmpty() ? 0
                : positions.stream().mapToInt(PositionInfo::getLeverage).sum() / positions.size();
    }

    public void closePosition(String symbol, PositionInfo positionInfo) {
        positionManager.deleteFuturesOpenOrder(symbol);
        positionManager.closeFuturePosition(symbol, positionInfo);
    }
}