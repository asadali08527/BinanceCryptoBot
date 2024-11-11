package com.bcb.config;

public class PrivateConfig {

    public static final String TAA_API_KEY =  System.getenv("TAA_BIN_API_KEY");
    public static final String TAA_SECRET_KEY = System.getenv("TAA_BIN_SECRET_KEY");
    public static final String TEE_API_KEY =  System.getenv("TEE_BIN_API_KEY");
    public static final String TEE_SECRET_KEY = System.getenv("TEE_BIN_SECRET_KEY");
    public static final String[] BASE_URLS = {"https://fapi.binance.com","https://api2.binance.com","https://api3.binance.com","https://api4.binance.com"};
}
