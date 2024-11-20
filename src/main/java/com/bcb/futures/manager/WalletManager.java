package com.bcb.futures.manager;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.bcb.client.SpotClient;
import com.bcb.transfer.BalanceInfo;
import com.bcb.transfer.OpenOrderInfo;
import com.bcb.utils.UrlBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class WalletManager extends ExceptionManager {
	
	private final SpotClient client; 
	
	public WalletManager(SpotClient spotClient) {
		this.client = spotClient;
	}

	public List<BalanceInfo> getFutureWalletBalance(){
		Gson gson = new Gson();
		Map<String, Object> params = new LinkedHashMap<>();
        params.put("timestamp", UrlBuilder.buildTimestamp());
        try {
		String result = client.createFutures().getFuturesWalletBalance(params);
		Type orderListType = new TypeToken<List<BalanceInfo>>() {
		}.getType();
		List<BalanceInfo> balanceInfoList = gson.fromJson(result, orderListType);
		return balanceInfoList;
        }catch(Exception e) {
        	return Collections.EMPTY_LIST;
        }
	}
	public BalanceInfo getFutureWalletBalance(String coin) {
	    List<BalanceInfo> balanceInfoList = getFutureWalletBalance();
	    return balanceInfoList.stream()
	            .filter(f -> f.getAsset().equalsIgnoreCase(coin))
	            .findFirst()
	            .orElse(null);
	}
}
