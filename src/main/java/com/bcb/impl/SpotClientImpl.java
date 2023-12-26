package com.bcb.impl;

import com.bcb.client.SpotClient;
import com.bcb.impl.spot.BSwap;
import com.bcb.impl.spot.Blvt;
import com.bcb.impl.spot.C2C;
import com.bcb.impl.spot.Convert;
import com.bcb.impl.spot.CryptoLoans;
import com.bcb.impl.spot.Fiat;
import com.bcb.impl.spot.Futures;
import com.bcb.impl.spot.GiftCard;
import com.bcb.impl.spot.Margin;
import com.bcb.impl.spot.Market;
import com.bcb.impl.spot.Mining;
import com.bcb.impl.spot.NFT;
import com.bcb.impl.spot.Pay;
import com.bcb.impl.spot.PortfolioMargin;
import com.bcb.impl.spot.Rebate;
import com.bcb.impl.spot.Savings;
import com.bcb.impl.spot.Staking;
import com.bcb.impl.spot.SubAccount;
import com.bcb.impl.spot.Trade;
import com.bcb.impl.spot.UserData;
import com.bcb.impl.spot.Wallet;
import com.bcb.utils.DefaultUrls;
import com.bcb.utils.ProxyAuth;
import com.bcb.utils.signaturegenerator.HmacSignatureGenerator;
import com.bcb.utils.signaturegenerator.SignatureGenerator;

public class SpotClientImpl implements SpotClient {
    private final String apiKey;
    private final SignatureGenerator signatureGenerator;
    private final String baseUrl;
    private boolean showLimitUsage = false;
    private ProxyAuth proxy = null;

    public SpotClientImpl() {
        this(DefaultUrls.PROD_URL);
    }

    public SpotClientImpl(String baseUrl) {
        this("", (SignatureGenerator) null, baseUrl);
    }

    public SpotClientImpl(String baseUrl, boolean showLimitUsage) {
        this(baseUrl);
        this.showLimitUsage = showLimitUsage;
    }

    public SpotClientImpl(String apiKey, String secretKey) {
        this(apiKey, secretKey, DefaultUrls.PROD_URL);
    }

    public SpotClientImpl(String apiKey, String secretKey, String baseUrl) {
        this(apiKey, new HmacSignatureGenerator(secretKey), baseUrl);
    }

    public SpotClientImpl(String apiKey, SignatureGenerator signatureGenerator, String baseUrl) {
        this.apiKey = apiKey;
        this.signatureGenerator = signatureGenerator;
        this.baseUrl = baseUrl;
    }

    @Override
    public void setShowLimitUsage(boolean showLimitUsage) {
        this.showLimitUsage = showLimitUsage;
    }

    @Override
    public void setProxy(ProxyAuth proxy) {
        this.proxy = proxy;
    }
    
    @Override
    public void unsetProxy() {
        this.proxy = null;
    }

    @Override
    public Blvt createBlvt() {
        return new Blvt(baseUrl, apiKey, signatureGenerator, showLimitUsage, proxy);
    }

    @Override
    public BSwap createBswap() {
        return new BSwap(baseUrl, apiKey, signatureGenerator, showLimitUsage, proxy);
    }

    @Override
    public C2C createC2C() {
        return new C2C(baseUrl, apiKey, signatureGenerator, showLimitUsage, proxy);
    }

    @Override
    public Convert createConvert() {
        return new Convert(baseUrl, apiKey, signatureGenerator, showLimitUsage, proxy);
    }

    @Override
    public CryptoLoans createCryptoLoans() {
        return new CryptoLoans(baseUrl, apiKey, signatureGenerator, showLimitUsage, proxy);
    }

    @Override
    public Fiat createFiat() {
        return new Fiat(baseUrl, apiKey, signatureGenerator, showLimitUsage, proxy);
    }

    @Override
    public Futures createFutures() {
        return new Futures(baseUrl, apiKey, signatureGenerator, showLimitUsage, proxy);
    }

    @Override
    public GiftCard createGiftCard() {
        return new GiftCard(baseUrl, apiKey, signatureGenerator, showLimitUsage, proxy); }

    @Override
    public Margin createMargin() {
        return new Margin(baseUrl, apiKey, signatureGenerator, showLimitUsage, proxy);
    }

    @Override
    public Market createMarket() {
        return new Market(baseUrl, apiKey, showLimitUsage, proxy);
    }

    @Override
    public Mining createMining() {
        return new Mining(baseUrl, apiKey, signatureGenerator, showLimitUsage, proxy);
    }

    @Override
    public NFT createNFT() {
        return new NFT(baseUrl, apiKey, signatureGenerator, showLimitUsage, proxy);
    }

    @Override
    public Pay createPay() {
        return new Pay(baseUrl, apiKey, signatureGenerator, showLimitUsage, proxy);
    }

    @Override
    public PortfolioMargin createPortfolioMargin() {
        return new PortfolioMargin(baseUrl, apiKey, signatureGenerator, showLimitUsage, proxy);
    }

    @Override
    public Rebate createRebate() {
        return new Rebate(baseUrl, apiKey, signatureGenerator, showLimitUsage, proxy);
    }

    @Override
    public Savings createSavings() {
        return new Savings(baseUrl, apiKey, signatureGenerator, showLimitUsage, proxy);
    }

    @Override
    public Staking createStaking() {
        return new Staking(baseUrl, apiKey, signatureGenerator, showLimitUsage, proxy);
    }

    @Override
    public SubAccount createSubAccount() {
        return new SubAccount(baseUrl, apiKey, signatureGenerator, showLimitUsage, proxy);
    }

    @Override
    public Trade createTrade() {
        return new Trade(baseUrl, apiKey, signatureGenerator, showLimitUsage, proxy);
    }

    @Override
    public UserData createUserData() {
        return new UserData(baseUrl, apiKey, showLimitUsage, proxy);
    }

    @Override
    public Wallet createWallet() {
        return new Wallet(baseUrl, apiKey, signatureGenerator, showLimitUsage, proxy);
    }
}
