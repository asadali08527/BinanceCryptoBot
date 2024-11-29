package com.bcb.utils;

public class PrecisionAdjuster {
    /**
     * Adjusts the precision of a given double by reducing one decimal place.
     * 
     * @param price The original price as a double.
     * @return The adjusted price with one less decimal place.
     */
    public static double adjustPrecision(double price) {
        String priceStr = String.valueOf(price);
        int dotIndex = priceStr.indexOf(".");
        
        if (dotIndex == -1) {
            // No decimal point found, return the original value
            return price;
        }
        
        int precisionLength = priceStr.length() - dotIndex - 1;
        
        if (precisionLength <= 1) {
            // If there is only one or no decimal place, return the original value
            return price;
        }
        
        // Remove the last decimal digit
        String adjustedStr = priceStr.substring(0, priceStr.length() - 1);
        return Double.parseDouble(adjustedStr);
    }
    
    public static void main(String[] args) {
        double price = 221.737723;
        double adjustedPrice = adjustPrecision(price);
        System.out.printf("Original Price: "+price+", Adjusted Price: "+ adjustedPrice);
    }
}
