package com.bcb.orders.futures.test.websockets;


import org.json.JSONObject;

import com.bcb.client.WebSocketApiClient;
import com.bcb.config.PrivateConfig;
import com.bcb.impl.WebSocketApiClientImpl;
import com.bcb.utils.DefaultUrls;
import com.bcb.utils.signaturegenerator.HmacSignatureGenerator;




public final class NewOcoOrder {

    private NewOcoOrder() {
    }

    private static final double limitPrice = 41638;
    private static final double stopPrice = 42500.0;
    private static final double stopLimitPrice = 40000.90;
    private static final double quantity = 0.00025;
    private static final int waitTime = 3000;

    public static void main(String[] args) throws InterruptedException {

        HmacSignatureGenerator signatureGenerator = new HmacSignatureGenerator(PrivateConfig.TAA_SECRET_KEY);
        WebSocketApiClient wsApiClient = new WebSocketApiClientImpl(PrivateConfig.TAA_API_KEY, signatureGenerator, DefaultUrls.WS_API_URL);

        wsApiClient.connect(((message) -> {
            System.out.println(message);
        }));
      
        JSONObject params = new JSONObject();
        params.put("requestId", "randomId");
        params.put("stopPrice", stopPrice);
        params.put("stopLimitPrice", stopLimitPrice);
        params.put("stopLimitTimeInForce", "GTC");
      
        wsApiClient.trade().newOcoOrder("BTCUSDT", "BUY", limitPrice, quantity, params);
      
        Thread.sleep(waitTime);
      
        wsApiClient.close();
    }
}
