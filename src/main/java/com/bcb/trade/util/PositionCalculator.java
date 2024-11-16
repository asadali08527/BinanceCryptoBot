package com.bcb.trade.util;

public class PositionCalculator {

    /**
     * Calculates the USDT amount using the formula: notional / leverage.
     * 
     * @param notional The notional value from the PositionInfo DTO.
     * @param leverage The leverage value from the PositionInfo DTO.
     * @return The calculated USDT amount.
     */
    public static double calculateUsdtAmount(double notional, int leverage) {
        if (leverage == 0) {
            throw new IllegalArgumentException("Leverage cannot be zero.");
        }
        return notional / leverage;
    }

    /**
     * Calculates the percentage profit using the formula:
     * (unRealizedProfit / usdtAmount) * 100.
     * 
     * @param unRealizedProfit The unrealized profit value from the PositionInfo DTO.
     * @param usdtAmount       The USDT amount calculated from calculateUsdtAmount.
     * @return The calculated percentage profit.
     */
    public static double calculatePercentageProfit(double unRealizedProfit, double usdtAmount) {
        if (usdtAmount == 0) {
            throw new IllegalArgumentException("USDT amount cannot be zero.");
        }
        return (unRealizedProfit / usdtAmount) * 100;
    }
    
    public static void main(String[] args) {
        // Example data from PositionInfo DTO
        double notional = 1102.64680481;
        int leverage = 50;
        double unRealizedProfit = -88.46849519;

        // Calculate USDT amount
        double usdtAmount = calculateUsdtAmount(notional, leverage);
        System.out.println("USDT Amount: " + usdtAmount);

        // Calculate percentage profit
        double percentageProfit = calculatePercentageProfit(unRealizedProfit, usdtAmount);
        System.out.println("Percentage Profit: " + percentageProfit);
    }
}
