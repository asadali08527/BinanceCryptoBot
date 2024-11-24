package com.bcb.config;

public class AccountConfig {
    private String apiKey;
    private String secretKey;
    private String baseUrl;

    public AccountConfig(String apiKey, String secretKey, String baseUrl) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
