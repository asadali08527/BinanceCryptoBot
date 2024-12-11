package com.bcb.transfer;

public class AccountInfo {
    private String uniMMR;
    private String accountEquity;
    private String actualEquity;
    private String accountInitialMargin;
    private String accountMaintMargin;
    private String accountStatus;
    private String virtualMaxWithdrawAmount;
    private String totalAvailableBalance;
    private String totalMarginOpenLoss;
    private long updateTime;

    // Getters and Setters
    public String getUniMMR() {
        return uniMMR;
    }

    public void setUniMMR(String uniMMR) {
        this.uniMMR = uniMMR;
    }

    public String getAccountEquity() {
        return accountEquity;
    }

    public void setAccountEquity(String accountEquity) {
        this.accountEquity = accountEquity;
    }

    public String getActualEquity() {
        return actualEquity;
    }

    public void setActualEquity(String actualEquity) {
        this.actualEquity = actualEquity;
    }

    public String getAccountInitialMargin() {
        return accountInitialMargin;
    }

    public void setAccountInitialMargin(String accountInitialMargin) {
        this.accountInitialMargin = accountInitialMargin;
    }

    public String getAccountMaintMargin() {
        return accountMaintMargin;
    }

    public void setAccountMaintMargin(String accountMaintMargin) {
        this.accountMaintMargin = accountMaintMargin;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getVirtualMaxWithdrawAmount() {
        return virtualMaxWithdrawAmount;
    }

    public void setVirtualMaxWithdrawAmount(String virtualMaxWithdrawAmount) {
        this.virtualMaxWithdrawAmount = virtualMaxWithdrawAmount;
    }

    public String getTotalAvailableBalance() {
        return totalAvailableBalance;
    }

    public void setTotalAvailableBalance(String totalAvailableBalance) {
        this.totalAvailableBalance = totalAvailableBalance;
    }

    public String getTotalMarginOpenLoss() {
        return totalMarginOpenLoss;
    }

    public void setTotalMarginOpenLoss(String totalMarginOpenLoss) {
        this.totalMarginOpenLoss = totalMarginOpenLoss;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "AccountInfo{" +
                "uniMMR='" + uniMMR + '\'' +
                ", accountEquity='" + accountEquity + '\'' +
                ", actualEquity='" + actualEquity + '\'' +
                ", accountInitialMargin='" + accountInitialMargin + '\'' +
                ", accountMaintMargin='" + accountMaintMargin + '\'' +
                ", accountStatus='" + accountStatus + '\'' +
                ", virtualMaxWithdrawAmount='" + virtualMaxWithdrawAmount + '\'' +
                ", totalAvailableBalance='" + totalAvailableBalance + '\'' +
                ", totalMarginOpenLoss='" + totalMarginOpenLoss + '\'' +
                ", updateTime=" + updateTime +
                '}';
    }
}

