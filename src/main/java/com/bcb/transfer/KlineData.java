package com.bcb.transfer;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class KlineData {

    List<Kline> klineList;

    public List<Kline> getKlineList() {
        return klineList;
    }

    public void setKlineList(List<Kline> klineList) {
        this.klineList = klineList;
    }
}

