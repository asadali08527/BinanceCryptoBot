package com.bcb.impl.spot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.bcb.client.SpotClient;
import com.bcb.impl.SpotClientImpl;
import com.bcb.transfer.ExchangeInfos;
import com.google.gson.Gson;

public class Exchange {

    public ExchangeInfos exchangeInfos(){
        SpotClient client = new SpotClientImpl();
        Market market = client.createMarket();
        Map<String, Object> parameters = new LinkedHashMap<>();

        String result = market.exchangeInfo(parameters);
        Gson gson = new Gson();
        ExchangeInfos exchangeInfos = gson.fromJson(result, ExchangeInfos.class);
        List<String> futureUSDTCoinList =exchangeInfos.getSymbols().stream().filter(f->f.getSymbol().endsWith("USDT")).map(m->m.getSymbol()).collect(Collectors.toList());
        System.out.println(futureUSDTCoinList);
        System.out.println(futureUSDTCoinList.size());
        return exchangeInfos;
    }
}
