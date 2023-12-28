package com.bcb.futures.manager;

import java.util.Date;
import java.util.Map;

import com.bcb.client.SpotClient;
import com.bcb.exceptions.BinanceClientException;
import com.bcb.exceptions.BinanceConnectorException;
import com.bcb.trade.constants.Coins;
import com.bcb.trade.util.CoinUtil;

class FutureOrderManager {
	
	private static final String REDUCING_PRECISION_MESSAGE = "Position created by reducing precision for coin ";
	private static final String DOUBLING_QUANTITY_MESSAGE = "Position Retrying by doubling quantity for coin ";
	private static final String INCREASING_QUANTITY_MESSAGE = "Position Retrying by increasing quantity for coin ";
	private static final String PAUSING_ORDER_MESSAGE = "ReduceOnly Order is getting rejected: Pausing new orders";
	
	private static FutureOrderManager instance;

    private final SpotClient client;

    FutureOrderManager(SpotClient client) {
        this.client = client;
    }

    public static FutureOrderManager getInstance(SpotClient client) {
        if (instance == null) {
            instance = new FutureOrderManager(client);
        }
        return instance;
    }

    public void createFuturePosition(Map<String, Object> parameters, int retry) {
        try {
            String result = client.createFutures().createFuturesPosition(parameters);
            FutureOrderScheduler.processed.add(String.valueOf(parameters.get("symbol")));
            System.out.println("Position Creation status for coin " + parameters + " Result: " + result);
        } catch (BinanceConnectorException e) {
            handleConnectorException(e);
        } catch (BinanceClientException e) {
            handleClientException(parameters, e, retry);
        } catch (Exception e) {
            handleGenericException(parameters, e);
        }
    }

    private void handleConnectorException(BinanceConnectorException e) {
        System.err.println(String.format("fullErrMessage: %s", e.getMessage()));
    }

    private void handleClientException(Map<String, Object> parameters, BinanceClientException e, int retry) {
        System.err.println(String.format("fullErrMessage: %s \nerrMessage: %s \nerrCode: %d \nHTTPStatusCode: %d",
                e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode()));

        String errorCode = String.valueOf(e.getErrorCode());

        if (Coins.ERROR_CODE_1111.equalsIgnoreCase(errorCode) && retry <= 1) {
            retryAndLog(parameters, retry, REDUCING_PRECISION_MESSAGE);
        } else if (Coins.ERROR_CODE_4164.equalsIgnoreCase(errorCode) && retry <= 4) {
            retryAndLog(parameters, retry, DOUBLING_QUANTITY_MESSAGE);
        } else if (Coins.ERROR_CODE_4003.equalsIgnoreCase(errorCode) && retry <= 2) {
            retryAndLog(parameters, retry, INCREASING_QUANTITY_MESSAGE);
        } else if (Coins.ERROR_CODE_4400.equalsIgnoreCase(errorCode) && retry <= 1) {
            System.out.println("Skipping due to error: Futures Trading Quantitative Rules violated, only reduceOnly order is allowed, please try again later. " + parameters);
            FutureOrderScheduler.pauseNewOrderFor2Hrs = true;
            FutureOrderScheduler.pauseTimefor2Hrs = new Date();
        } else if (Coins.ERROR_CODE_1121.equalsIgnoreCase(errorCode)) {
        	printErrorMessage(e.getMessage(),parameters);
            handleInvalidSymbol(parameters);
        } else if (Coins.ERROR_CODE_2022.equalsIgnoreCase(errorCode)) {
            System.out.println(PAUSING_ORDER_MESSAGE);
            FutureOrderScheduler.pauseNewOrderFor2Hrs = true;
            printErrorMessage(e.getMessage(),parameters);
        } else if (Coins.ERROR_CODE_4141.equalsIgnoreCase(errorCode)) {
            //System.out.println("Symbol is closed. " + parameters);
        	printErrorMessage(e.getMessage(),parameters);
        } else if (Coins.ERROR_CODE_1102.equalsIgnoreCase(errorCode) || Coins.ERROR_CODE_4014.equalsIgnoreCase(errorCode)) {
        	printErrorMessage(e.getMessage(),parameters);
        } else {
        	printErrorMessage(e.getMessage(),parameters);
        }
    }

    private void printErrorMessage(String errorMessage, Map<String, Object> parameters) {
        FutureOrderScheduler.errored.add(String.valueOf(parameters.get("symbol")));
        System.out.println(errorMessage + parameters);
    }
    private void retryAndLog(Map<String, Object> parameters, int retry, String logMessage) {
        retry += 1;
        parameters.put("quantity", CoinUtil.adjustPrecision(String.valueOf(parameters.get("quantity"))));
        parameters.remove("timestamp");
        parameters.remove("signature");
        createFuturePosition(parameters, retry);
        System.out.println(logMessage + parameters);
    }

    private void handleInvalidSymbol(Map<String, Object> parameters) {
        FutureOrderScheduler.invalidSymbol.add(String.valueOf(parameters.get("symbol")));
    }

    private void handleGenericException(Map<String, Object> parameters, Exception e) {
        FutureOrderScheduler.errored.add(String.valueOf(parameters.get("symbol")));
        e.printStackTrace();
        System.out.println("Error occurred while processing " + parameters.get("symbol") + " | Error: " + e.getMessage());
    }

	public String getFuturesOpenOrders(Map<String, Object> parameters) {
		try {
			return client.createFutures().getFuturesOpenOrders(parameters);
		} catch (Exception e) {
			e.printStackTrace(); 
			return ""; 
		}
	}

}
