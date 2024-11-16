package com.bcb.transfer;

public class BalanceInfo {
    private String accountAlias;
    private String asset;
    private String balance;
    private String crossWalletBalance;
    private String crossUnPnl;
    private String availableBalance;
    private String maxWithdrawAmount;
    private boolean marginAvailable;
    private long updateTime;

    // Getters and Setters
    public String getAccountAlias() {
        return accountAlias;
    }

    public void setAccountAlias(String accountAlias) {
        this.accountAlias = accountAlias;
    }

    public String getAsset() {
        return asset;
    }

    public void setAsset(String asset) {
        this.asset = asset;
    }

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public String getCrossWalletBalance() {
        return crossWalletBalance;
    }

    public void setCrossWalletBalance(String crossWalletBalance) {
        this.crossWalletBalance = crossWalletBalance;
    }

    public String getCrossUnPnl() {
        return crossUnPnl;
    }

    public void setCrossUnPnl(String crossUnPnl) {
        this.crossUnPnl = crossUnPnl;
    }

    public String getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(String availableBalance) {
        this.availableBalance = availableBalance;
    }

    public String getMaxWithdrawAmount() {
        return maxWithdrawAmount;
    }

    public void setMaxWithdrawAmount(String maxWithdrawAmount) {
        this.maxWithdrawAmount = maxWithdrawAmount;
    }

    public boolean isMarginAvailable() {
        return marginAvailable;
    }

    public void setMarginAvailable(boolean marginAvailable) {
        this.marginAvailable = marginAvailable;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "BalanceInfo{" +
                "accountAlias='" + accountAlias + '\'' +
                ", asset='" + asset + '\'' +
                ", balance='" + balance + '\'' +
                ", crossWalletBalance='" + crossWalletBalance + '\'' +
                ", crossUnPnl='" + crossUnPnl + '\'' +
                ", availableBalance='" + availableBalance + '\'' +
                ", maxWithdrawAmount='" + maxWithdrawAmount + '\'' +
                ", marginAvailable=" + marginAvailable +
                ", updateTime=" + updateTime +
                '}';
    }
}

