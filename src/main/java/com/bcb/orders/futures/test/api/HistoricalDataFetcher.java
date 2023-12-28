package com.bcb.orders.futures.test.api;

import com.bcb.config.PrivateConfig;
import com.bcb.enums.TimeInterval;
import com.bcb.impl.SpotClientImpl;
import com.bcb.transfer.Kline;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HistoricalDataFetcher {

    static SpotClientImpl spotClient = new SpotClientImpl(PrivateConfig.TEE_API_KEY, PrivateConfig.TEE_SECRET_KEY);

    public static List<String> getHistoricalPrices(String coin, TimeInterval interval ) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        long endTime = System.currentTimeMillis();
        long startTime = System.currentTimeMillis() - (interval.getMillis() ); // Adjust as needed
        parameters.put("symbol", coin);
        parameters.put("interval", "5m");
        parameters.put("startTime",startTime);
        parameters.put("endTime",endTime);
        //System.out.println(parameters);
        List<Kline> historicalData = fetchHistoricalData(parameters);
        //Process the historical data as needed
        return historicalData.stream().map(m->m.getClosePrice()).collect(Collectors.toList());
    }

    private static List<Kline> fetchHistoricalData(Map<String, Object> parameters) {
        String result = spotClient.createMarket().klines(parameters);
        //System.out.println(result);
        Type orderListType = new TypeToken<List<Object[]>>() {}.getType();
        Gson gson = new Gson();
        List<Object[]> data = gson.fromJson(result, orderListType);
        List<Kline> klineList = data.stream().map(m->{
            return new Kline(Double.valueOf(String.valueOf(m[0])).longValue(),String.valueOf(m[1]),String.valueOf(m[2]),String.valueOf(m[3]),String.valueOf(m[4]),String.valueOf(m[5]),Double.valueOf(String.valueOf(m[6])).longValue(),String.valueOf(m[7]),Double.valueOf(String.valueOf(m[8])).intValue(),String.valueOf(m[9]),String.valueOf(m[10]),String.valueOf(m[11]));
        }).collect(Collectors.toList());
        return klineList;
    }

}

