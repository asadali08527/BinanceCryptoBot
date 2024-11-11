package com.bcb.orders.futures.test.websockets;

import org.json.JSONObject;

import com.bcb.client.WebSocketApiClient;
import com.bcb.client.WebSocketStreamClient;
import com.bcb.config.PrivateConfig;
import com.bcb.impl.WebSocketApiClientImpl;
import com.bcb.impl.WebSocketStreamClientImpl;
import com.bcb.utils.DefaultUrls;
import com.bcb.utils.signaturegenerator.HmacSignatureGenerator;
import com.bcb.utils.websocketcallback.WebSocketMessageCallback;

public class BinanceOCOOrderExample {
	
	 private static final double quantity = 0.0002;
	 private static final int waitTime = 5000;
	    
	public static void main(String[] args) throws InterruptedException {{

        // ws stream call
        WebSocketStreamClient streamClient = new WebSocketStreamClientImpl(DefaultUrls.WS_URL);

        WebSocketMessageCallback streamOnMsgCallback = (event) -> {
            System.out.println(event);
        };
        
        streamClient.tradeStream("BTCUSDT", streamOnMsgCallback);

        Thread.sleep(waitTime);

        // ws api call
        HmacSignatureGenerator signatureGenerator = new HmacSignatureGenerator(PrivateConfig.TAA_SECRET_KEY);
        WebSocketApiClient apiClient = new WebSocketApiClientImpl(PrivateConfig.TAA_API_KEY, signatureGenerator, DefaultUrls.WS_API_URL);
        apiClient.connect(((event) -> {
            System.out.println(event);
        }));

        JSONObject params = new JSONObject();
        params.put("quantity", quantity);

        apiClient.trade().newOrder("BTCUSDT", "BUY", "MARKET", params);

        Thread.sleep(waitTime);
        
        // closing all connections
        streamClient.closeAllConnections();
        apiClient.close();
    }
	}

}