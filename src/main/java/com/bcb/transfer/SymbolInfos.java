package com.bcb.transfer;

import java.util.ArrayList;
import java.util.List;

public class SymbolInfos {
    private List<SymbolInf> symbolInfoList = new ArrayList<>();

    public List<SymbolInf> getSymbolInfoList() {
        return symbolInfoList;
    }

    public void setSymbolInfoList(List<SymbolInf> symbolInfoList) {
        this.symbolInfoList = symbolInfoList;
    }

    @Override
    public String toString() {
        return "SymbolInfos{" +
                "symbolInfoList=" + symbolInfoList +
                '}';
    }
}
