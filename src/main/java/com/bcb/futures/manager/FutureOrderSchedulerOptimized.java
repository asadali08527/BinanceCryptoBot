package com.bcb.futures.manager;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.bcb.client.SpotClient;
import com.bcb.config.AccountConfig;
import com.bcb.config.PrivateConfig;
import com.bcb.futures.strategy.executor.StrategyExecutor;
import com.bcb.impl.SpotClientImpl;
import com.bcb.transfer.BalanceInfo;
import com.bcb.transfer.PositionInfo;

public class FutureOrderSchedulerOptimized {
	private static final int EXECUTION_INTERVAL_SECONDS = 40;

	List<PositionInfo> openPositions = null;
	List<PositionInfo> buyPositions = null;
	List<PositionInfo> sellPositions = null;
	BalanceInfo balanceInfo = null;

	List<PositionInfo> buyPositionsInProfit = null;
	Integer upMovement = 0;
	Integer downMovement = 0;
	private List<AccountConfig> accountConfigs = null;

	public FutureOrderSchedulerOptimized(List<AccountConfig> accountConfigs) {
		this.accountConfigs = accountConfigs;
	}

	public static void main(String[] args) {
		// Define account configurations
		List<AccountConfig> accountConfigs = List.of(
				//new AccountConfig(PrivateConfig.TEE_API_KEY, PrivateConfig.TEE_SECRET_KEY, PrivateConfig.BASE_URLS[0]),
				new AccountConfig(PrivateConfig.TAA_API_KEY, PrivateConfig.TAA_SECRET_KEY, PrivateConfig.BASE_URLS[0])
				);

		FutureOrderSchedulerOptimized scheduler = new FutureOrderSchedulerOptimized(accountConfigs);
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(accountConfigs.size());
		executor.scheduleAtFixedRate(scheduler::executeOppositeStrategyForAllAccounts, 0, EXECUTION_INTERVAL_SECONDS,
				TimeUnit.SECONDS);
	}

	private void executeOppositeStrategyForAllAccounts() {
		for (AccountConfig accountConfig : accountConfigs) {
			executeStrategyForAccount(accountConfig);
		}
	}

	private void executeStrategyForAccount(AccountConfig accountConfig) {
		SpotClient client = new SpotClientImpl(accountConfig.getApiKey(), accountConfig.getSecretKey(),
				accountConfig.getBaseUrl());
		PositionManager positionManager = new PositionManager(client);
		FutureOrderManager futureOrderManager = new FutureOrderManager(client);
		WalletManager walletManager = new WalletManager(client);
		OrderManager orderManager = new OrderManager(client);

		// Use these managers to execute the strategy for this account
		StrategyExecutor strategyExecutor = new StrategyExecutor(positionManager, futureOrderManager, walletManager,
				orderManager);
		strategyExecutor.executeStrategy();
	}

}
