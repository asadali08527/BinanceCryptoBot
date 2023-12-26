package com.bcb.orders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;

import com.bcb.client.SpotClient;
import com.bcb.config.PrivateConfig;
import com.bcb.impl.SpotClientImpl;
import com.bcb.utils.UrlBuilder;

public class TestFutureLimit {
    // Binance API Key and Secret
    private static final String API_KEY = PrivateConfig.TAA_API_KEY;
    private static final String API_SECRET = PrivateConfig.TAA_SECRET_KEY;

    // Binance API endpoint
    private static final String BASE_URL = "https://fapi.binance.com";

    // Function to create a signature for the request
    private static String generateSignature(String data)
            throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(API_SECRET.getBytes(), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hmacData = mac.doFinal(data.getBytes());
        return new String(Base64.getEncoder().encode(hmacData));
    }

    // Function to place a future order with stop loss and take profit
    private static String placeFutureOrder(String symbol, String side, String quantity,
                                           String price, String stopLossPrice, String takeProfitPrice)
            throws NoSuchAlgorithmException, InvalidKeyException, IOException {

        // Construct parameters
        Map<String, Object> params = new TreeMap<>();
        params.put("symbol", symbol);
        params.put("side", side);
        params.put("type", "LIMIT");
        params.put("quantity", quantity);
        params.put("price", price);
        params.put("timeInForce", "GTC");
        //params.put("stopPrice", stopLossPrice);
        params.put("closePosition", "false");
        params.put("newOrderRespType", "ACK");  // Set the response type (ACK, RESULT, FULL)
        System.out.println(params);
        SpotClient client = new SpotClientImpl(PrivateConfig.TAA_API_KEY, PrivateConfig.TAA_SECRET_KEY,PrivateConfig.BASE_URLS[0]);

        String result = client.createFutures().createFuturesPosition(params);
        System.out.println(result);

        return null;
    }

    // Example usage
    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeyException, IOException {

        System.out.println(new Date(1703505762406l));
        //Map<String, Object> params = new HashMap<>();
        //params.put("timestamp",UrlBuilder.buildTimestamp());
        Map<String, Object> params = new HashMap<>();
        List<String> symbols = new ArrayList<>();
        symbols.add("APEUSDT");
        symbols.add("ENJUSDT");
        //params.put("symbol","USDT");
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("symbol","USDT");
//        WebSocketApiClientImpl webSocketApiClient = new WebSocketApiClientImpl(PrivateConfig.TAA_API_KEY,new HmacSignatureGenerator(PrivateConfig.TAA_SECRET_KEY));
//        WebSocketApiAccount account = webSocketApiClient.account();
//        account.accountStatus(jsonObject);
        //System.out.print(account.);
        //params.put("asset","USDT");
        SpotClient client = new SpotClientImpl(PrivateConfig.TAA_API_KEY, PrivateConfig.TAA_SECRET_KEY,PrivateConfig.BASE_URLS[0]);

        params.put("timestamp", UrlBuilder.buildTimestamp());
        //String result = client.createFutures().getFuturesOpenOrder(params);
        //System.out.println(result);
        String result = client.createFutures().getFuturesWalletBalance(params);
        System.out.println(result);
//
//        Type orderListType = new TypeToken<List<OpenOrderInfo>>() {}.getType();
//        Gson gson = new Gson();
//        List<OpenOrderInfo> positionInfos = gson.fromJson(result, orderListType);
//        System.out.println(positionInfos);
        //PositionInfo positionInfo = positionInfos.get(0);
        //System.out.println(executeRequest(params));
//        String symbol = "ENJUSDT";
//        String side = "BUY";  // or "SELL"
//        String quantity = "35";
//        String price = "0.3541";
//        String stopLossPrice = "39000";
//        String takeProfitPrice = "41000";
//
//        String response = placeFutureOrder(symbol, side, quantity, price, stopLossPrice, takeProfitPrice);
        //System.out.println(response);
    }

    private static String queryBuilder(Map<String, Object> params) throws NoSuchAlgorithmException, InvalidKeyException {
        // Convert parameters to query string
        String queryString = params.isEmpty()?"":params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        // Add timestamp
        long timestamp = System.currentTimeMillis();
        queryString += "timestamp=" + timestamp;

        // Generate signature
        return generateSignature(queryString);
    }
    private static String executeRequest(Map<String, Object> params) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        // Convert parameters to query string
        String queryString = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        // Add timestamp
        long timestamp = System.currentTimeMillis();
        queryString += "timestamp=" + timestamp;

        // Generate signature
        String signature = generateSignature(queryString);
        //String signature = new HmacSignatureGenerator(PrivateConfig.TAA_SECRET_KEY).getSignature(queryString);

        // Construct final URL
        String url = BASE_URL + "/fapi/v1/openOrders?" + queryString + "&signature=" + signature;

        // Make the request
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-MBX-APIKEY", API_KEY);
        connection.setDoOutput(true);

        // Write request body
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = queryString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Read response
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
