package com.bcb.futures.manager;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bcb.client.SpotClient;
import com.bcb.transfer.AccountBalanceInfo;
import com.bcb.transfer.AccountInfo;
import com.bcb.transfer.BalanceInfo;
import com.bcb.utils.UrlBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class PMWalletManager extends ExceptionManager {
	
	private final SpotClient client; 
	
	public PMWalletManager(SpotClient spotClient) {
		this.client = spotClient;
	}

	public List<BalanceInfo> getFutureWalletBalance(){
		Gson gson = new Gson();
		Map<String, Object> params = new LinkedHashMap<>();
        params.put("timestamp", UrlBuilder.buildTimestamp());
        try {
		String result = client.createPortfolioMarginFuture().getFuturesWalletBalance(params,true);
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
	
	public List<AccountBalanceInfo> getPapiAccountCryptoBalance(){
		Gson gson = new Gson();
		Map<String, Object> params = new LinkedHashMap<>();
        params.put("timestamp", UrlBuilder.buildTimestamp());
        try {
		String result = client.createPortfolioMarginFuture().getFuturesWalletBalance(params,true);
		Type orderListType = new TypeToken<List<AccountBalanceInfo>>() {
		}.getType();
		List<AccountBalanceInfo> balanceInfoList = gson.fromJson(result, orderListType);
		return balanceInfoList;
        }catch(Exception e) {
        	return Collections.EMPTY_LIST;
        }
	}
	public AccountBalanceInfo getAccountWalletBalance(String coin) {
	    List<AccountBalanceInfo> balanceInfoList = getPapiAccountCryptoBalance();
	    return balanceInfoList.stream()
	            .filter(f -> f.getAsset().equalsIgnoreCase(coin))
	            .findFirst()
	            .orElse(null);
	}
	public AccountInfo getAccountInfo() {
		Gson gson = new Gson();
		Map<String, Object> params = new LinkedHashMap<>();
        params.put("timestamp", UrlBuilder.buildTimestamp());
        try {
		String result = client.createPortfolioMarginFuture().getAccountInfo(params,true);
		Type orderListType = new TypeToken<AccountInfo>() {
		}.getType();
		return gson.fromJson(result, orderListType);
        }catch(Exception e) {
        	return null;
        }
	}
}
