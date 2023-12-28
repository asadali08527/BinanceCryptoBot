package com.bcb.orders.futures.test.websockets;

import java.util.LinkedHashMap;
import java.util.Map;

import com.bcb.client.SpotClient;
import com.bcb.config.PrivateConfig;
import com.bcb.exceptions.BinanceClientException;
import com.bcb.exceptions.BinanceConnectorException;
import com.bcb.impl.SpotClientImpl;
import com.bcb.utils.DefaultUrls;

public class OcoOrder {
	    private OcoOrder() {
	    }
	    private static final double quantity = 1.83;
	    private static final double price = 26.248;
	    private static final double stopPrice = 34.140;

	    public static void main(String[] args) {
	        Map<String, Object> parameters = new LinkedHashMap<>();

	        SpotClient client = new SpotClientImpl(PrivateConfig.TAA_API_KEY, PrivateConfig.TAA_SECRET_KEY, DefaultUrls.PROD_URL);

	        parameters.put("symbol", "MOVRUSDT");
	        parameters.put("side", "BUY");
	        parameters.put("stopPrice", stopPrice);
	        parameters.put("quantity", quantity);
	        parameters.put("price", price);

	        try {
	            String result = client.createTrade().ocoOrder(parameters);
	            System.out.println(result);
	        } catch (BinanceConnectorException e) {
	            System.err.println((String) String.format("fullErrMessage: %s", e.getMessage()));
	        } catch (BinanceClientException e) {
	            System.err.println((String) String.format("fullErrMessage: %s \nerrMessage: %s \nerrCode: %d \nHTTPStatusCode: %d",
	                    e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode()));
	        }
	    }

}
