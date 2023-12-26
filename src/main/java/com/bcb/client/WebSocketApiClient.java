package com.bcb.client;

import com.bcb.impl.websocketapi.WebSocketApiAccount;
import com.bcb.impl.websocketapi.WebSocketApiGeneral;
import com.bcb.impl.websocketapi.WebSocketApiMarket;
import com.bcb.impl.websocketapi.WebSocketApiTrade;
import com.bcb.impl.websocketapi.WebSocketApiUserDataStream;
import com.bcb.utils.websocketcallback.WebSocketClosedCallback;
import com.bcb.utils.websocketcallback.WebSocketClosingCallback;
import com.bcb.utils.websocketcallback.WebSocketFailureCallback;
import com.bcb.utils.websocketcallback.WebSocketMessageCallback;
import com.bcb.utils.websocketcallback.WebSocketOpenCallback;

public interface WebSocketApiClient {
    void connect(WebSocketMessageCallback onMessageCallback);
    void connect(WebSocketOpenCallback onOpenCallback, WebSocketMessageCallback onMessageCallback, WebSocketClosingCallback onClosingCallback, WebSocketClosedCallback onClosedCallback, WebSocketFailureCallback onFailureCallback);
    void close();
    WebSocketApiGeneral general();
    WebSocketApiMarket market();
    WebSocketApiTrade trade();
    WebSocketApiAccount account();
    WebSocketApiUserDataStream userDataStream();
}
