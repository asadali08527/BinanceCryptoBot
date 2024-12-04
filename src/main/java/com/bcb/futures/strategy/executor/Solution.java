package com.bcb.futures.strategy.executor;


import java.util.HashSet;
import java.util.Set;

public class Solution {

    private static final String VOWELS = "AEIOU";

    private Set<String> uniqueWords = new HashSet<>();

    public Set<String> rearrange(String input) {
        char[] chars = input.toCharArray();
        boolean[] used = new boolean[chars.length];
        StringBuilder current = new StringBuilder();

        generatePermutations(chars, used, current, -1);
        return uniqueWords;
    }

    private void generatePermutations(char[] chars, boolean[] used, StringBuilder current, int prevCharType) {
        if (current.length() == chars.length) {
            // Ensure the word does not start with a vowel
            if (VOWELS.indexOf(current.charAt(0)) == -1) {
                uniqueWords.add(current.toString());
            }
            return;
        }

        for (int i = 0; i < chars.length; i++) {
            if (used[i]) continue;

            char currentChar = chars[i];
            int currentCharType = (VOWELS.indexOf(currentChar) != -1) ? 1 : 0; // 1 for vowel, 0 for consonant

            // Skip if the first character is a vowel
            if (current.length() == 0 && currentCharType == 1) {
                continue;
            }

            // Ensure no two consecutive letters are both vowels or both consonants
            if (prevCharType != -1 && prevCharType == currentCharType) {
                continue;
            }

            used[i] = true;
            current.append(currentChar);

            // Recur with the current character type as the previous character type
            generatePermutations(chars, used, current, currentCharType);

            // Backtrack
            used[i] = false;
            current.deleteCharAt(current.length() - 1);
        }
    }


    public static void main(String[] args) {
        Solution rearranger = new Solution();
        Set<String> results = rearranger.rearrange("AAAB");

        for (String word : results) {
            System.out.println(word);
        }
    }
}