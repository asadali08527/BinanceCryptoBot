package com.bcb.service;

import com.bcb.futures.manager.WalletManager;
import com.bcb.transfer.BalanceInfo;

public class WalletService {
    private final WalletManager walletManager;

    public WalletService(WalletManager walletManager) {
        this.walletManager = walletManager;
    }

    public BalanceInfo getFutureWalletBalance(String asset) {
        return walletManager.getFutureWalletBalance(asset);
    }
}
