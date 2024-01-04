package com.bcb.futures.manager;

import java.util.Date;
import java.util.Map;

import com.bcb.exceptions.BinanceClientException;
import com.bcb.exceptions.BinanceConnectorException;
import com.bcb.trade.constants.Coins;
import com.bcb.trade.util.CoinUtil;

public class ExceptionManager {
	private static final String PAUSING_ORDER_MESSAGE = "ReduceOnly Order is getting rejected: Pausing new orders ";
    private static final String INSUFFICIENT_MARGIN_MESSAGE = "Margin is insufficient: Pausing new orders";

	
	protected void handleConnectorException(BinanceConnectorException e) {
        System.err.println(String.format("fullErrMessage: %s", e.getMessage()));
    }

	protected void handleClientException(Map<String, Object> parameters, BinanceClientException e, int retry) {
        System.err.println(String.format("fullErrMessage: %s \nerrMessage: %s \nerrCode: %d \nHTTPStatusCode: %d",
                e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode()));

        String errorCode = String.valueOf(e.getErrorCode());

        if (Coins.ERROR_CODE_4400.equalsIgnoreCase(errorCode) && retry <= 1) {
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
        } else if (Coins.ERROR_CODE_2019.equalsIgnoreCase(errorCode)) {
            System.out.println(INSUFFICIENT_MARGIN_MESSAGE);
            FutureOrderScheduler.pauseCreateOrders = true;
            //FutureOrderScheduler.pauseTimefor2Hrs = new Date();
            printErrorMessage(e.getMessage(),parameters);
        }else if (Coins.ERROR_CODE_4141.equalsIgnoreCase(errorCode)) {
            //System.out.println("Symbol is closed. " + parameters);
        	printErrorMessage(e.getMessage(),parameters);
        } else if (Coins.ERROR_CODE_1102.equalsIgnoreCase(errorCode) || Coins.ERROR_CODE_4014.equalsIgnoreCase(errorCode)) {
        	printErrorMessage(e.getMessage(),parameters);
        } else {
        	printErrorMessage(e.getMessage(),parameters);
        }
    }

    protected void printErrorMessage(String errorMessage, Map<String, Object> parameters) {
        FutureOrderScheduler.errored.add(String.valueOf(parameters.get("symbol")));
        System.out.println(errorMessage + parameters);
    }
    
    protected void handleInvalidSymbol(Map<String, Object> parameters) {
        FutureOrderScheduler.invalidSymbol.add(String.valueOf(parameters.get("symbol")));
    }

    protected void handleGenericException(Map<String, Object> parameters, Exception e) {
        FutureOrderScheduler.errored.add(String.valueOf(parameters.get("symbol")));
        e.printStackTrace();
        System.out.println("Error occurred while processing " + parameters.get("symbol") + " | Error: " + e.getMessage());
    }
    
    
}
