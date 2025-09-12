package com.bcb.service;

import java.util.List;
import java.util.Map;

import com.bcb.futures.manager.FutureOrderManager;
import com.bcb.futures.manager.OrderManager;
import com.bcb.transfer.OpenOrderInfo;
import com.bcb.transfer.PositionInfo;

public class OrderService {
    private final FutureOrderManager futureOrderManager;
    private final OrderManager orderManager;

    public OrderService(FutureOrderManager futureOrderManager, OrderManager orderManager) {
        this.futureOrderManager = futureOrderManager;
        this.orderManager = orderManager;
    }

    public List<OpenOrderInfo> getOpenOrders() {
        return futureOrderManager.getOpenOrders();
    }

    public void createFuturePosition(Map<String, Object> params, int leverage) {
        futureOrderManager.createFuturePosition(params, leverage);
    }

    public void createFutureOpenOrder(String symbol, PositionInfo positionInfo, List<OpenOrderInfo> openOrders, PositionInfo oppositePositionInfo, boolean onEntryPrice) {
        orderManager.createFutureOpenOrder(symbol, positionInfo, openOrders, oppositePositionInfo,onEntryPrice);
    }

    public double calculateOrderQuantity(String symbol, PositionInfo positionInfo, List<OpenOrderInfo> openOrders, PositionInfo oppositePositionInfo) {
        return orderManager.calculateOrderQuantity(symbol, positionInfo, openOrders, oppositePositionInfo);
    }

	public List<OpenOrderInfo> filterOpenOrders(String symbol, List<OpenOrderInfo> openOrders) {
		return orderManager.filterOpenOrders(symbol, openOrders);
	}
}