package com.bcb.transfer;

import java.io.Serializable;

public class AccountBalanceInfo implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String asset;
    private String totalWalletBalance;
    private String crossMarginAsset;
    private String crossMarginBorrowed;
    private String crossMarginFree;
    private String crossMarginInterest;
    private String crossMarginLocked;
    private String umWalletBalance;
    private String umUnrealizedPNL;
    private String cmWalletBalance;
    private String cmUnrealizedPNL;
    private long updateTime;
    private String negativeBalance;

    // Getters and Setters
    public String getAsset() {
        return asset;
    }

    public void setAsset(String asset) {
        this.asset = asset;
    }

    public String getTotalWalletBalance() {
        return totalWalletBalance;
    }

    public void setTotalWalletBalance(String totalWalletBalance) {
        this.totalWalletBalance = totalWalletBalance;
    }

    public String getCrossMarginAsset() {
        return crossMarginAsset;
    }

    public void setCrossMarginAsset(String crossMarginAsset) {
        this.crossMarginAsset = crossMarginAsset;
    }

    public String getCrossMarginBorrowed() {
        return crossMarginBorrowed;
    }

    public void setCrossMarginBorrowed(String crossMarginBorrowed) {
        this.crossMarginBorrowed = crossMarginBorrowed;
    }

    public String getCrossMarginFree() {
        return crossMarginFree;
    }

    public void setCrossMarginFree(String crossMarginFree) {
        this.crossMarginFree = crossMarginFree;
    }

    public String getCrossMarginInterest() {
        return crossMarginInterest;
    }

    public void setCrossMarginInterest(String crossMarginInterest) {
        this.crossMarginInterest = crossMarginInterest;
    }

    public String getCrossMarginLocked() {
        return crossMarginLocked;
    }

    public void setCrossMarginLocked(String crossMarginLocked) {
        this.crossMarginLocked = crossMarginLocked;
    }

    public String getUmWalletBalance() {
        return umWalletBalance;
    }

    public void setUmWalletBalance(String umWalletBalance) {
        this.umWalletBalance = umWalletBalance;
    }

    public String getUmUnrealizedPNL() {
        return umUnrealizedPNL;
    }

    public void setUmUnrealizedPNL(String umUnrealizedPNL) {
        this.umUnrealizedPNL = umUnrealizedPNL;
    }

    public String getCmWalletBalance() {
        return cmWalletBalance;
    }

    public void setCmWalletBalance(String cmWalletBalance) {
        this.cmWalletBalance = cmWalletBalance;
    }

    public String getCmUnrealizedPNL() {
        return cmUnrealizedPNL;
    }

    public void setCmUnrealizedPNL(String cmUnrealizedPNL) {
        this.cmUnrealizedPNL = cmUnrealizedPNL;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public String getNegativeBalance() {
        return negativeBalance;
    }

    public void setNegativeBalance(String negativeBalance) {
        this.negativeBalance = negativeBalance;
    }

    @Override
    public String toString() {
        return "AccountBalanceInfo{" +
                "asset='" + asset + '\'' +
                ", totalWalletBalance='" + totalWalletBalance + '\'' +
                ", crossMarginAsset='" + crossMarginAsset + '\'' +
                ", crossMarginBorrowed='" + crossMarginBorrowed + '\'' +
                ", crossMarginFree='" + crossMarginFree + '\'' +
                ", crossMarginInterest='" + crossMarginInterest + '\'' +
                ", crossMarginLocked='" + crossMarginLocked + '\'' +
                ", umWalletBalance='" + umWalletBalance + '\'' +
                ", umUnrealizedPNL='" + umUnrealizedPNL + '\'' +
                ", cmWalletBalance='" + cmWalletBalance + '\'' +
                ", cmUnrealizedPNL='" + cmUnrealizedPNL + '\'' +
                ", updateTime=" + updateTime +
                ", negativeBalance='" + negativeBalance + '\'' +
                '}';
    }

}
