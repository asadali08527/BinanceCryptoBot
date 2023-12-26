package com.bcb.client;

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
import com.bcb.utils.ProxyAuth;


public interface SpotClient {
    void setShowLimitUsage(boolean showLimitUsage);
    void setProxy(ProxyAuth proxy);
    void unsetProxy();
    Blvt createBlvt();
    BSwap createBswap();
    C2C createC2C();
    Convert createConvert();
    CryptoLoans createCryptoLoans();
    Fiat createFiat();
    Futures createFutures();
    GiftCard createGiftCard();
    Market createMarket();
    Margin createMargin();
    Mining createMining();
    NFT createNFT();
    Pay createPay();
    PortfolioMargin createPortfolioMargin();
    Rebate createRebate();
    Savings createSavings();
    Staking createStaking();
    SubAccount createSubAccount();
    Trade createTrade();
    UserData createUserData();
    Wallet createWallet();
}
