package com.bcb.futures.strategy.executor;

import java.util.*;
public class Solution2 {
	int minDist(int arr[], int n, int x, int y) {
        // Check if the array is null or has insufficient length
        if (arr == null || n < 2) {
            return -1; // Return -1 for invalid inputs
        }

        int prevIndex = -1; // Track the last seen index of x or y
        int minDist = Integer.MAX_VALUE;

        for (int i = 0; i < n; i++) {
            if (arr[i] == x || arr[i] == y) {
                // If it's the first occurrence, just store the index
                if (prevIndex != -1 && arr[i] != arr[prevIndex]) {
                    // Calculate the distance if it's a different element
                    minDist = Math.min(minDist, i - prevIndex);
                }
                prevIndex = i; // Update the last seen index
            }
        }

        // If no minimum distance is found, return -1
        return minDist == Integer.MAX_VALUE ? -1 : minDist;
    }

 
    public static void main(String[] args)
    {
        Solution2 min = new Solution2();
        int arr[] = { 3, 5, 4, 2, 6, 5, 6, 6, 5, 4, 8, 3 };
        int n = arr.length;
        int x = 3;
        int y = 5;
        
        System.out.println("Minimum distance between " + x
                           + " and " + y + " is "
                           + min.minDist(arr, n, x, y));
    }
}
