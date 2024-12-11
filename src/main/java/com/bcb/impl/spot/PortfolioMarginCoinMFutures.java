package com.bcb.impl.spot;

import java.util.Map;

import com.bcb.enums.HttpMethod;
import com.bcb.utils.ParameterChecker;
import com.bcb.utils.ProxyAuth;
import com.bcb.utils.RequestHandler;
import com.bcb.utils.signaturegenerator.HmacSignatureGenerator;
import com.bcb.utils.signaturegenerator.SignatureGenerator;

public class PortfolioMarginCoinMFutures {
    private final String baseUrl;
    private final RequestHandler requestHandler;
    private final boolean showLimitUsage;

    public PortfolioMarginCoinMFutures(String baseUrl, String apiKey, String secretKey, boolean showLimitUsage, ProxyAuth proxy) {
        this.baseUrl = baseUrl;
        this.requestHandler = new RequestHandler(apiKey, new HmacSignatureGenerator(secretKey), proxy);
        this.showLimitUsage = showLimitUsage;
    }

    public PortfolioMarginCoinMFutures(String baseUrl, String apiKey, SignatureGenerator signatureGenerator, boolean showLimitUsage, ProxyAuth proxy) {
        this.baseUrl = baseUrl;
        this.requestHandler = new RequestHandler(apiKey, signatureGenerator, proxy);
        this.showLimitUsage = showLimitUsage;
    }

    // Endpoints
    private final String FUTURES_TRANSFER = "/sapi/v1/futures/transfer";
    private final String FUTURES_TRANSFER_PAPI = "/papi/v1/cm/futures/transfer";

    private final String FUTURE_ALL_ORDERS = "/fapi/v1/allOrders";
    private final String FUTURE_ALL_ORDERS_PAPI = "/papi/v1/cm/allOrders";

    private final String FUTURE_OPEN_POSITION = "/fapi/v2/positionRisk";
    private final String FUTURE_OPEN_POSITION_PAPI = "/papi/v1/cm/positionRisk";

    private final String FUTURE_CREATE_POSITION = "/fapi/v1/order";
    private final String FUTURE_CREATE_POSITION_PAPI = "/papi/v1/cm/order";

    private final String FUTURE_OPEN_ORDERS = "/fapi/v1/openOrders";
    //private final String FUTURE_OPEN_ORDERS_PAPI = "/papi/v1/cm/openOrders";
    private final String FUTURE_OPEN_ORDERS_PAPI = "/papi/v1/cm/conditional/allOrders";

    private final String FUTURE_OPEN_ORDER = "/fapi/v1/openOrder";
    private final String FUTURE_OPEN_ORDER_PAPI = "/papi/v1/cm/openOrder";

    private final String FUTURE_WALLET_BALANCE = "/fapi/v2/balance";
    private final String FUTURE_WALLET_BALANCE_PAPI = "/papi/v1/balance";

    private final String FUTURE_DELETE_ORDER = "/fapi/v1/order";
    private final String FUTURE_DELETE_ORDER_PAPI = "/papi/v1/cm/order";
    
    private final String FUTURE_ACCOUNT_INFO = "/fapi/v2/account";
    private final String FUTURE_ACCOUNT_INFO_PAPI = "/papi/v1/account";

    // Methods
    public String futuresTransfer(Map<String, Object> parameters, boolean isPapi) {
        String endpoint = isPapi ? FUTURES_TRANSFER_PAPI : FUTURES_TRANSFER;
        ParameterChecker.checkParameter(parameters, "asset", String.class);
        ParameterChecker.checkRequiredParameter(parameters, "amount");
        ParameterChecker.checkParameter(parameters, "type", Integer.class);
        return requestHandler.sendSignedRequest(baseUrl, endpoint, parameters, HttpMethod.POST, showLimitUsage);
    }

    public String futuresTransferHistory(Map<String, Object> parameters, boolean isPapi) {
        String endpoint = isPapi ? FUTURES_TRANSFER_PAPI : FUTURES_TRANSFER;
        ParameterChecker.checkParameter(parameters, "asset", String.class);
        ParameterChecker.checkParameter(parameters, "startTime", Long.class);
        return requestHandler.sendSignedRequest(baseUrl, endpoint, parameters, HttpMethod.GET, showLimitUsage);
    }

    public String getFuturesAllOrders(Map<String, Object> parameters, boolean isPapi) {
        String endpoint = isPapi ? FUTURE_ALL_ORDERS_PAPI : FUTURE_ALL_ORDERS;
        return requestHandler.sendSignedRequest(baseUrl, endpoint, parameters, HttpMethod.GET, showLimitUsage);
    }

    public String getFuturesOpenPosition(Map<String, Object> parameters, boolean isPapi) {
        String endpoint = isPapi ? FUTURE_OPEN_POSITION_PAPI : FUTURE_OPEN_POSITION;
        ParameterChecker.checkParameter(parameters, "symbol", String.class);
        return requestHandler.sendSignedRequest(baseUrl, endpoint, parameters, HttpMethod.GET, showLimitUsage);
    }

    public String getAllFuturesOpenPosition(Map<String, Object> parameters, boolean isPapi) {
        String endpoint = isPapi ? FUTURE_OPEN_POSITION_PAPI : FUTURE_OPEN_POSITION;
        return requestHandler.sendSignedRequest(baseUrl, endpoint, parameters, HttpMethod.GET, showLimitUsage);
    }

    public String createFuturesPosition(Map<String, Object> parameters, boolean isPapi) {
        String endpoint = isPapi ? FUTURE_CREATE_POSITION_PAPI : FUTURE_CREATE_POSITION;
        ParameterChecker.checkParameter(parameters, "symbol", String.class);
        ParameterChecker.checkParameter(parameters, "side", String.class);
        ParameterChecker.checkParameter(parameters, "type", String.class);
        ParameterChecker.checkParameter(parameters, "quantity", String.class);
        return requestHandler.sendSignedRequest(baseUrl, endpoint, parameters, HttpMethod.POST, showLimitUsage);
    }

    public String getFuturesOpenOrders(Map<String, Object> parameters, boolean isPapi) {
        String endpoint = isPapi ? FUTURE_OPEN_ORDERS_PAPI : FUTURE_OPEN_ORDERS;
        return requestHandler.sendSignedRequest(baseUrl, endpoint, parameters, HttpMethod.GET, showLimitUsage);
    }

    public String getFuturesOpenOrder(Map<String, Object> parameters, boolean isPapi) {
        String endpoint = isPapi ? FUTURE_OPEN_ORDER_PAPI : FUTURE_OPEN_ORDER;
        return requestHandler.sendSignedRequest(baseUrl, endpoint, parameters, HttpMethod.GET, showLimitUsage);
    }

    public String getFuturesWalletBalance(Map<String, Object> parameters, boolean isPapi) {
        String endpoint = isPapi ? FUTURE_WALLET_BALANCE_PAPI : FUTURE_WALLET_BALANCE;
        return requestHandler.sendSignedRequest(baseUrl, endpoint, parameters, HttpMethod.GET, showLimitUsage);
    }
    
    public String getAccountInfo(Map<String, Object> parameters, boolean isPapi) {
        String endpoint = isPapi ? FUTURE_ACCOUNT_INFO_PAPI : FUTURE_ACCOUNT_INFO;
        return requestHandler.sendSignedRequest(baseUrl, endpoint, parameters, HttpMethod.GET, showLimitUsage);
    }

    public String deleteFuturesOpenOrder(Map<String, Object> parameters, boolean isPapi) {
        String endpoint = isPapi ? FUTURE_DELETE_ORDER_PAPI : FUTURE_DELETE_ORDER;
        return requestHandler.sendSignedRequest(baseUrl, endpoint, parameters, HttpMethod.DELETE, showLimitUsage);
    }
    
    private final String FUTURE_LIMIT_ORDER = "/papi/v1/cm/conditional/order";
	public String createLimitOrder(Map<String, Object> parameters, boolean isPapi) {
		String endpoint = isPapi?FUTURE_LIMIT_ORDER:FUTURE_CREATE_POSITION;
		ParameterChecker.checkParameter(parameters, "symbol", String.class);
        ParameterChecker.checkParameter(parameters, "side", String.class);
        ParameterChecker.checkParameter(parameters, "stopPrice", String.class);
        ParameterChecker.checkParameter(parameters, "quantity", String.class);
        return requestHandler.sendSignedRequest(baseUrl, endpoint, parameters, HttpMethod.POST, showLimitUsage);
	}
}
