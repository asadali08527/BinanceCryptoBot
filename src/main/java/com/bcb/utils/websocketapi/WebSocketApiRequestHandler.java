package com.bcb.utils.websocketapi;

import com.bcb.enums.RequestType;
import com.bcb.exceptions.BinanceConnectorException;
import com.bcb.utils.JSONParser;
import com.bcb.utils.ParameterChecker;
import com.bcb.utils.UrlBuilder;
import com.bcb.utils.WebSocketConnection;
import com.bcb.utils.signaturegenerator.SignatureGenerator;

import org.json.JSONObject;

public class WebSocketApiRequestHandler {
    private final SignatureGenerator signatureGenerator;
    private final String apiKey;
    private WebSocketConnection connection;

    public WebSocketApiRequestHandler(WebSocketConnection connection, String apiKey, SignatureGenerator signatureGenerator) {
        if (connection == null) {
            throw new BinanceConnectorException("[WebSocketApiRequestHandler] WebSocketConnection cannot be null");
        }
        this.connection = connection;
        this.apiKey = apiKey;
        this.signatureGenerator = signatureGenerator;
    }

    public void publicRequest(String method) {
        this.request(RequestType.PUBLIC, method, null);
    }

    public void publicRequest(String method, JSONObject parameters) {
        this.request(RequestType.PUBLIC, method, parameters);
    }

    public void apiRequest(String method, JSONObject parameters) {
        this.request(RequestType.WITH_API_KEY, method, parameters);

    }

    public void signedRequest(String method, JSONObject parameters) {
        this.request(RequestType.SIGNED, method, parameters);
    }

    public void request(RequestType requestType, String method, JSONObject parameters) {
        Object requestId = ParameterChecker.processId(JSONParser.pullValue(parameters, "requestId"), "requestId"); 
        ParameterChecker.checkParameterType(method, String.class, "method");

        switch (requestType) {
            case PUBLIC:
                this.connection.send(JSONParser.buildJSONString(requestId, method, parameters));
                break;
            case WITH_API_KEY:
                ParameterChecker.checkParameterType(this.apiKey, String.class, "apiKey");
                parameters = JSONParser.addKeyValue(parameters, "apiKey", this.apiKey);

                this.connection.send(JSONParser.buildJSONString(requestId, method, parameters));
                break;
            case SIGNED:
                ParameterChecker.checkParameterType(this.apiKey, String.class, "apiKey");
                parameters = JSONParser.addKeyValue(parameters, "apiKey", this.apiKey);
                if (!parameters.has("timestamp")) {
                    parameters.put("timestamp", UrlBuilder.buildTimestamp());
                }

                // signature
                ParameterChecker.checkParameterType(this.signatureGenerator, SignatureGenerator.class, "signatureGenerator");
                String payload = UrlBuilder.joinQueryParameters(JSONParser.sortJSONObject(parameters));
                String signature = this.signatureGenerator.getSignature(payload);
                parameters.put("signature", signature);

                this.connection.send(JSONParser.buildJSONString(requestId, method, parameters));
                break;
            default:
                throw new BinanceConnectorException("[WebSocketApiRequestHandler] Invalid request type: " + requestType);
        }
    }
}
