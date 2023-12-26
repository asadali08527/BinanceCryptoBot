package com.bcb.trade.constants;

public class Coins {
    public static final String USDT = "USDT";
    public static final String TYPE_LIMIT = "LIMIT";
    public static final String TYPE_MARKET = "MARKET";
    public static final String TIME_IN_FORCE = "GTC" ;
    public static final double POSITION_CLOSE_THRESOLD_PERCENTAGE = 10.0;
    public static final double PROFIT_THRESOLD_PERCENTAGE = 15.0;
    public static final String BUY_SIDE = "BUY";
    public static final String SELL_SIDE = "SELL";
    public static final String HOLD_SIDE = "HOLD";
    public static final Double PRICE_CHANGE_PERCENTAGE_THRESHOLD = 5.0;
    public static final Double PRICE_CHANGE_PERCENTAGE_THRESHOLD_5 = 5.0;
    public static final Double PRICE_CHANGE_PERCENTAGE_THRESHOLD_10 = 10.0;
    public static final Double PRICE_CHANGE_PERCENTAGE_THRESHOLD_15 = 15.0;

    public static final String ERROR_CODE_1111 = "-1111";
    public static final String ERROR_CODE_4164 = "-4164";
    public static final String ERROR_CODE_4003 = "-4003";
    public static final String ERROR_CODE_2022 = "-2022";
    public static final String ERROR_CODE_1121 = "-1121";
    public static final String ERROR_CODE_4141 = "-4141";
    public static final String ERROR_CODE_1102 = "-1102";
    public static final String ERROR_CODE_4014 = "-4014";

    public static final double BASE_LEVERAGE_25 = 25.0;
    public static final double BASE_LEVERAGE_12 = 12.5;
    public static final double BASE_LEVERAGE_50 = 50.0;
    public static final double BASE_LEVERAGE_75 = 75.0;
    public static final double BASE_LEVERAGE_100 = 50.0;
    public static final Double ENTRY_THRESHOLD_PERCENT = 5.0;
    public static final Double EXIT_THRESHOLD_PERCENT = 5.0;
    public static final int SHORT_TERM_PERIOD = 10;//10 minutes
    public static final int LONG_TERM_PERIOD = 60;//15 minutes
    public static final String ERROR_CODE_4400 = "-4400";
    public static final String ERROR_CODE_1003 = "-1003";


    public static String[] ZERO_DIGIT_FUTURE_USDT_COINS =  {"ADAUSDT","XLMUSDT","ICXUSDT","VETUSDT","ONGUSDT","HOTUSDT","ZILUSDT","FETUSDT","IOSTUSDT","CELRUSDT","ENJUSDT","MATICUSDT","ONEUSDT","FTMUSDT","DOGEUSDT","DUSKUSDT","ANKRUSDT","MTLUSDT","DENTUSDT","KEYUSDT","CHZUSDT","XTZUSDT","RENUSDT","RVNUSDT","HBARUSDT","NKNUSDT","KAVAUSDT","ARPAUSDT","IOTXUSDT","RLCUSDT","OGNUSDT","BNTUSDT","MBLUSDT","COTIUSDT","STPTUSDT","CTSIUSDT","CHRUSDT","STMXUSDT","KNCUSDT","LRCUSDT","COMPUSDT","DGBUSDT","SXPUSDT","STORJUSDT","MANAUSDT","BALUSDT","BLZUSDT","CRVUSDT","SANDUSDT","OCEANUSDT","RSRUSDT","SUSHIUSDT","YFIIUSDT","RUNEUSDT","UMAUSDT","BELUSDT","OXTUSDT","FLMUSDT","XVSUSDT","ALPHAUSDT","AAVEUSDT","NEARUSDT","FILUSDT","AUDIOUSDT","CTKUSDT","AKROUSDT","AXSUSDT","STRAXUSDT","UNFIUSDT","ROSEUSDT","XEMUSDT","SKLUSDT","GRTUSDT","1INCHUSDT","REEFUSDT","CELOUSDT","RIFUSDT","TRUUSDT","CKBUSDT","TWTUSDT","LITUSDT","SFPUSDT","DODOUSDT","CAKEUSDT","BADGERUSDT","ALICEUSDT","LINAUSDT","SUPERUSDT","CFXUSDT","TLMUSDT","BAKEUSDT","SLPUSDT","MASKUSDT","NUUSDT","XVGUSDT","ATAUSDT","GTCUSDT","KEEPUSDT","C98USDT","MINAUSDT","WAXPUSDT","IDEXUSDT","GALAUSDT","YGGUSDT","FRONTUSDT","AGLDUSDT","RADUSDT","POWRUSDT","JASMYUSDT","BICOUSDT","CVXUSDT","PEOPLEUSDT","SPELLUSDT","ACHUSDT","IMXUSDT","GLMRUSDT","ANCUSDT","WOOUSDT","TUSDT","ASTRUSDT","GMTUSDT","APEUSDT","STEEMUSDT","GALUSDT","LDOUSDT","LEVERUSDT","STGUSDT","HFTUSDT","PHBUSDT","HIFIUSDT","AGIXUSDT","AMBUSDT","USTCUSDT","IDUSDT","LOOMUSDT","SNTUSDT","COMBOUSDT","MAVUSDT","PENDLEUSDT","ARKMUSDT","WLDUSDT","SEIUSDT","CYBERUSDT","ARKUSDT","NTRNUSDT","TIAUSDT","MEMEUSDT","BEAMXUSDT","BLURUSDT","JTOUSDT","1000SATSUSDT"};
    public static String[] ONE_DIGIT_FUTURE_USDT_COINS = {"QTUMUSDT","ADAUSDT","EOSUSDT","IOTAUSDT","XLMUSDT","ONTUSDT","TRXUSDT","ICXUSDT","VETUSDT","USDCUSDT","ONGUSDT","HOTUSDT","ZILUSDT","ZRXUSDT","FETUSDT","BATUSDT","XMRUSDT","IOSTUSDT","CELRUSDT","OMGUSDT","THETAUSDT","ENJUSDT","MATICUSDT","ONEUSDT","FTMUSDT","ALGOUSDT","DOGEUSDT","DUSKUSDT","ANKRUSDT","MTLUSDT","DENTUSDT","KEYUSDT","CHZUSDT","BANDUSDT","XTZUSDT","RENUSDT","RVNUSDT","HBARUSDT","NKNUSDT","STXUSDT","KAVAUSDT","ARPAUSDT","IOTXUSDT","RLCUSDT","OGNUSDT","BNTUSDT","MBLUSDT","COTIUSDT","STPTUSDT","SOLUSDT","CTSIUSDT","CHRUSDT","STMXUSDT","KNCUSDT","LRCUSDT","COMPUSDT","ZENUSDT","SNXUSDT","DGBUSDT","SXPUSDT","STORJUSDT","MANAUSDT","BALUSDT","BLZUSDT","ANTUSDT","CRVUSDT","SANDUSDT","OCEANUSDT","NMRUSDT","RSRUSDT","TRBUSDT","SUSHIUSDT","YFIIUSDT","KSMUSDT","EGLDUSDT","RUNEUSDT","UMAUSDT","BELUSDT","UNIUSDT","OXTUSDT","AVAXUSDT","FLMUSDT","XVSUSDT","ALPHAUSDT","AAVEUSDT","NEARUSDT","FILUSDT","INJUSDT","AUDIOUSDT","CTKUSDT","AKROUSDT","AXSUSDT","STRAXUSDT","UNFIUSDT","ROSEUSDT","XEMUSDT","SKLUSDT","GRTUSDT","1INCHUSDT","REEFUSDT","CELOUSDT","RIFUSDT","TRUUSDT","CKBUSDT","TWTUSDT","LITUSDT","SFPUSDT","DODOUSDT","CAKEUSDT","BADGERUSDT","ALICEUSDT","LINAUSDT","PERPUSDT","SUPERUSDT","CFXUSDT","TLMUSDT","BAKEUSDT","SLPUSDT","ARUSDT","MASKUSDT","NUUSDT","XVGUSDT","ATAUSDT","GTCUSDT","KEEPUSDT","KLAYUSDT","BONDUSDT","C98USDT","QNTUSDT","MINAUSDT","WAXPUSDT","IDEXUSDT","GALAUSDT","ILVUSDT","YGGUSDT","FRONTUSDT","AGLDUSDT","RADUSDT","DARUSDT","ENSUSDT","POWRUSDT","JASMYUSDT","RNDRUSDT","BICOUSDT","FXSUSDT","HIGHUSDT","CVXUSDT","PEOPLEUSDT","SPELLUSDT","JOEUSDT","ACHUSDT","IMXUSDT","GLMRUSDT","API3USDT","ANCUSDT","WOOUSDT","TUSDT","ASTRUSDT","GMTUSDT","APEUSDT","STEEMUSDT","GALUSDT","LDOUSDT","OPUSDT","LEVERUSDT","STGUSDT","POLYXUSDT","APTUSDT","HFTUSDT","PHBUSDT","HOOKUSDT","MAGICUSDT","HIFIUSDT","AGIXUSDT","AMBUSDT","USTCUSDT","GASUSDT","IDUSDT","ARBUSDT","LOOMUSDT","RDNTUSDT","EDUUSDT","SNTUSDT","COMBOUSDT","MAVUSDT","PENDLEUSDT","ARKMUSDT","WLDUSDT","SEIUSDT","CYBERUSDT","ARKUSDT","NTRNUSDT","TIAUSDT","MEMEUSDT","BEAMXUSDT","BLURUSDT","JTOUSDT","1000SATSUSDT"};
    public static String[] TWO_DIGIT_FUTURE_USDT_COINS = {"BNBUSDT","NEOUSDT","QTUMUSDT","ADAUSDT","EOSUSDT","IOTAUSDT","XLMUSDT","ONTUSDT","TRXUSDT","ETCUSDT","ICXUSDT","VETUSDT","USDCUSDT","LINKUSDT","WAVESUSDT","ONGUSDT","HOTUSDT","ZILUSDT","ZRXUSDT","FETUSDT","BATUSDT","XMRUSDT","IOSTUSDT","CELRUSDT","OMGUSDT","THETAUSDT","ENJUSDT","MATICUSDT","ATOMUSDT","ONEUSDT","FTMUSDT","ALGOUSDT","DOGEUSDT","DUSKUSDT","ANKRUSDT","MTLUSDT","DENTUSDT","KEYUSDT","CHZUSDT","BANDUSDT","XTZUSDT","RENUSDT","RVNUSDT","HBARUSDT","NKNUSDT","STXUSDT","KAVAUSDT","ARPAUSDT","IOTXUSDT","RLCUSDT","OGNUSDT","BNTUSDT","MBLUSDT","COTIUSDT","STPTUSDT","SOLUSDT","CTSIUSDT","CHRUSDT","STMXUSDT","KNCUSDT","LRCUSDT","COMPUSDT","ZENUSDT","SNXUSDT","DGBUSDT","SXPUSDT","STORJUSDT","MANAUSDT","BALUSDT","BLZUSDT","ANTUSDT","CRVUSDT","SANDUSDT","OCEANUSDT","NMRUSDT","DOTUSDT","RSRUSDT","TRBUSDT","SUSHIUSDT","YFIIUSDT","KSMUSDT","EGLDUSDT","RUNEUSDT","UMAUSDT","BELUSDT","UNIUSDT","OXTUSDT","AVAXUSDT","FLMUSDT","XVSUSDT","ALPHAUSDT","AAVEUSDT","NEARUSDT","FILUSDT","INJUSDT","AUDIOUSDT","CTKUSDT","AKROUSDT","AXSUSDT","STRAXUSDT","UNFIUSDT","ROSEUSDT","XEMUSDT","SKLUSDT","GRTUSDT","1INCHUSDT","REEFUSDT","CELOUSDT","RIFUSDT","TRUUSDT","CKBUSDT","TWTUSDT","LITUSDT","SFPUSDT","DODOUSDT","CAKEUSDT","BADGERUSDT","ALICEUSDT","LINAUSDT","PERPUSDT","SUPERUSDT","CFXUSDT","TLMUSDT","BAKEUSDT","SLPUSDT","ICPUSDT","ARUSDT","MASKUSDT","LPTUSDT","NUUSDT","XVGUSDT","ATAUSDT","GTCUSDT","KEEPUSDT","KLAYUSDT","BONDUSDT","C98USDT","QNTUSDT","MINAUSDT","WAXPUSDT","IDEXUSDT","GALAUSDT","ILVUSDT","YGGUSDT","FRONTUSDT","AGLDUSDT","RADUSDT","DARUSDT","ENSUSDT","POWRUSDT","JASMYUSDT","RNDRUSDT","BICOUSDT","FXSUSDT","HIGHUSDT","CVXUSDT","PEOPLEUSDT","SPELLUSDT","JOEUSDT","ACHUSDT","IMXUSDT","GLMRUSDT","API3USDT","ANCUSDT","WOOUSDT","TUSDT","ASTRUSDT","GMTUSDT","APEUSDT","STEEMUSDT","GALUSDT","LDOUSDT","OPUSDT","LEVERUSDT","STGUSDT","GMXUSDT","POLYXUSDT","APTUSDT","HFTUSDT","PHBUSDT","HOOKUSDT","MAGICUSDT","HIFIUSDT","AGIXUSDT","SSVUSDT","LQTYUSDT","AMBUSDT","USTCUSDT","GASUSDT","IDUSDT","ARBUSDT","LOOMUSDT","RDNTUSDT","EDUUSDT","SUIUSDT","SNTUSDT","COMBOUSDT","MAVUSDT","PENDLEUSDT","ARKMUSDT","WLDUSDT","SEIUSDT","CYBERUSDT","ARKUSDT","NTRNUSDT","TIAUSDT","MEMEUSDT","ORDIUSDT","BEAMXUSDT","BLURUSDT","JTOUSDT","1000SATSUSDT"};

    public static String[] COINS= {"BTC","LTC","ETH","NEO","BNB","QTUM","EOS","SNT","BNT","GAS","BCC","USDT","HSR","OAX","DNT","MCO","ICN","ZRX","OMG","WTC","YOYO","LRC","TRX","SNGLS","STRAT","BQX","FUN","KNC","CDT","XVG","IOTA","SNM","LINK","CVC","TNT","REP","MDA","MTL","SALT","NULS","SUB","STX","MTH","ADX","ETC","ENG","ZEC","AST","GNT","DGD","BAT","DASH","POWR","BTG","REQ","XMR","EVX","VIB","ENJ","VEN","ARK","XRP","MOD","STORJ","KMD","RCN","EDO","DATA","DLT","MANA","PPT","RDN","GXS","AMB","ARN","BCPT","CND","GVT","POE","BTS","FUEL","XZC","QSP","LSK","BCD","TNB","ADA","LEND","XLM","CMT","WAVES","WABI","GTO","ICX","OST","ELF","AION","WINGS","BRD","NEBL","NAV","VIBE","LUN","TRIG","APPC","CHAT","RLC","INS","PIVX","IOST","STEEM","NANO","AE","VIA","BLZ","SYS","RPX","NCASH","POA","ONT","ZIL","STORM","XEM","WAN","WPR","QLC","GRS","CLOAK","LOOM","BCN","TUSD","ZEN","SKY","THETA","IOTX","QKC","AGI","NXS","SC","NPXS","KEY","NAS","MFT","DENT","IQ","ARDR","HOT","VET","DOCK","POLY","VTHO","ONG","PHX","HC","GO","PAX","RVN","DCR","USDC","MITH","BCHABC","BCHSV","REN","BTT","USDS","FET","TFUEL","CELR","MATIC","ATOM","PHB","ONE","FTM","BTCB","USDSB","CHZ","COS","ALGO","ERD","DOGE","BGBP","DUSK","ANKR","WIN","TUSDB","COCOS","PERL","TOMO","BUSD","BAND","BEAM","HBAR","XTZ","NGN","DGB","NKN","GBP","EUR","KAVA","RUB","UAH","ARPA","TRY","CTXC","AERGO","BCH","TROY","BRL","VITE","FTT","AUD","OGN","DREP","BULL","BEAR","ETHBULL","ETHBEAR","XRPBULL","XRPBEAR","EOSBULL","EOSBEAR","TCT","WRX","LTO","ZAR","MBL","COTI","BKRW","BNBBULL","BNBBEAR","HIVE","STPT","SOL","IDRT","CTSI","CHR","BTCUP","BTCDOWN","HNT","JST","FIO","BIDR","STMX","MDT","PNT","COMP","IRIS","MKR","SXP","SNX","DAI","ETHUP","ETHDOWN","ADAUP","ADADOWN","LINKUP","LINKDOWN","DOT","RUNE","BNBUP","BNBDOWN","XTZUP","XTZDOWN","AVA","BAL","YFI","SRM","ANT","CRV","SAND","OCEAN","NMR","LUNA","IDEX","RSR","PAXG","WNXM","TRB","EGLD","BZRX","WBTC","KSM","SUSHI","YFII","DIA","BEL","UMA","EOSUP","TRXUP","EOSDOWN","TRXDOWN","XRPUP","XRPDOWN","DOTUP","DOTDOWN","NBS","WING","SWRV","LTCUP","LTCDOWN","CREAM","UNI","OXT","SUN","AVAX","BURGER","BAKE","FLM","SCRT","XVS","CAKE","SPARTA","UNIUP","UNIDOWN","ALPHA","ORN","UTK","NEAR","VIDT","AAVE","FIL","SXPUP","SXPDOWN","INJ","FILDOWN","FILUP","YFIUP","YFIDOWN","CTK","EASY","AUDIO","BCHUP","BCHDOWN","BOT","AXS","AKRO","HARD","KP3R","RENBTC","SLP","STRAX","UNFI","CVP","BCHA","FOR","FRONT","ROSE","HEGIC","AAVEUP","AAVEDOWN","PROM","BETH","SKL","GLM","SUSD","COVER","GHST","SUSHIUP","SUSHIDOWN","XLMUP","XLMDOWN","DF","JUV","PSG","BVND","GRT","CELO","TWT","REEF","OG","ATM","ASR","1INCH","RIF","BTCST","TRU","DEXE","CKB","FIRO","LIT","PROS","VAI","SFP","FXS","DODO","AUCTION","UFT","ACM","PHA","TVK","BADGER","FIS","OM","POND","ALICE","DEGO","BIFI","LINA"};
    public static String[] COINS_NO_IN_USE= {"BCCUSDT","USDTUSDT","HSRUSDT","OAXUSDT","DNTUSDT","MCOUSDT","ICNUSDT","WTCUSDT","YOYOUSDT","SNGLSUSDT","STRATUSDT","BQXUSDT","FUNUSDT","CDTUSDT","SNMUSDT","TNTUSDT","REPUSDT","MDAUSDT","SALTUSDT","NULSUSDT","SUBUSDT","MTHUSDT","ADXUSDT","ENGUSDT","ASTUSDT","GNTUSDT","DGDUSDT","BTGUSDT","REQUSDT","EVXUSDT","VIBUSDT","VENUSDT","MODUSDT","KMDUSDT","RCNUSDT","EDOUSDT","DATAUSDT","DLTUSDT","PPTUSDT","RDNUSDT","GXSUSDT","ARNUSDT","BCPTUSDT","CNDUSDT","GVTUSDT","POEUSDT","FUELUSDT","XZCUSDT","QSPUSDT","LSKUSDT","BCDUSDT","TNBUSDT","CMTUSDT","WABIUSDT","GTOUSDT","OSTUSDT","ELFUSDT","AIONUSDT","WINGSUSDT","BRDUSDT","NEBLUSDT","NAVUSDT","VIBEUSDT","LUNUSDT","TRIGUSDT","APPCUSDT","CHATUSDT","INSUSDT","PIVXUSDT","NANOUSDT","AEUSDT","VIAUSDT","SYSUSDT","RPXUSDT","NCASHUSDT","POAUSDT","STORMUSDT","WANUSDT","WPRUSDT","QLCUSDT","GRSUSDT","CLOAKUSDT","BCNUSDT","TUSDUSDT","SKYUSDT","QKCUSDT","AGIUSDT","NXSUSDT","NPXSUSDT","NASUSDT","MFTUSDT","IQUSDT","ARDRUSDT","DOCKUSDT","POLYUSDT","VTHOUSDT","PHXUSDT","HCUSDT","GOUSDT","PAXUSDT","DCRUSDT","MITHUSDT","BCHABCUSDT","BCHSVUSDT","USDSUSDT","TFUELUSDT","BTCBUSDT","USDSBUSDT","COSUSDT","ERDUSDT","BGBPUSDT","WINUSDT","TUSDBUSDT","PERLUSDT","BEAMUSDT","NGNUSDT","GBPUSDT","EURUSDT","RUBUSDT","UAHUSDT","TRYUSDT","CTXCUSDT","AERGOUSDT","TROYUSDT","BRLUSDT","VITEUSDT","AUDUSDT","DREPUSDT","BULLUSDT","BEARUSDT","ETHBULLUSDT","ETHBEARUSDT","XRPBULLUSDT","XRPBEARUSDT","EOSBULLUSDT","EOSBEARUSDT","TCTUSDT","WRXUSDT","LTOUSDT","ZARUSDT","BKRWUSDT","BNBBULLUSDT","BNBBEARUSDT","HIVEUSDT","IDRTUSDT","BTCUPUSDT","BTCDOWNUSDT","JSTUSDT","FIOUSDT","BIDRUSDT","PNTUSDT","IRISUSDT","DAIUSDT","ETHUPUSDT","ETHDOWNUSDT","ADAUPUSDT","ADADOWNUSDT","LINKUPUSDT","LINKDOWNUSDT","BNBUPUSDT","BNBDOWNUSDT","XTZUPUSDT","XTZDOWNUSDT","AVAUSDT","PAXGUSDT","WNXMUSDT","WBTCUSDT","DIAUSDT","EOSUPUSDT","TRXUPUSDT","EOSDOWNUSDT","TRXDOWNUSDT","XRPUPUSDT","XRPDOWNUSDT","DOTUPUSDT","DOTDOWNUSDT","NBSUSDT","WINGUSDT","SWRVUSDT","LTCUPUSDT","LTCDOWNUSDT","CREAMUSDT","SUNUSDT","BURGERUSDT","SPARTAUSDT","UNIUPUSDT","UNIDOWNUSDT","ORNUSDT","UTKUSDT","VIDTUSDT","SXPUPUSDT","SXPDOWNUSDT","FILDOWNUSDT","FILUPUSDT","YFIUPUSDT","YFIDOWNUSDT","EASYUSDT","BCHUPUSDT","BCHDOWNUSDT","BOTUSDT","HARDUSDT","KP3RUSDT","RENBTCUSDT","CVPUSDT","BCHAUSDT","FORUSDT","HEGICUSDT","AAVEUPUSDT","AAVEDOWNUSDT","PROMUSDT","BETHUSDT","GLMUSDT","SUSDUSDT","COVERUSDT","GHSTUSDT","SUSHIUPUSDT","SUSHIDOWNUSDT","XLMUPUSDT","XLMDOWNUSDT","DFUSDT","JUVUSDT","PSGUSDT","BVNDUSDT","OGUSDT","ATMUSDT","ASRUSDT","DEXEUSDT","FIROUSDT","PROSUSDT","VAIUSDT","AUCTIONUSDT","UFTUSDT","ACMUSDT","PHAUSDT","TVKUSDT","FISUSDT","OMUSDT","PONDUSDT","DEGOUSDT","BIFIUSDT"};
    public static String[] ALL_USDT_COINS = {"BTCUSDT","ETHUSDT","BNBUSDT","BCCUSDT","NEOUSDT","LTCUSDT","QTUMUSDT","ADAUSDT","EOSUSDT","TUSDUSDT","IOTAUSDT","XLMUSDT","ONTUSDT","TRXUSDT","ETCUSDT","ICXUSDT","VENUSDT","NULSUSDT","VETUSDT","PAXUSDT","BCHABCUSDT","BCHSVUSDT","USDCUSDT","LINKUSDT","WAVESUSDT","USDSUSDT","ONGUSDT","HOTUSDT","ZILUSDT","ZRXUSDT","FETUSDT","BATUSDT","XMRUSDT","ZECUSDT","IOSTUSDT","CELRUSDT","DASHUSDT","NANOUSDT","OMGUSDT","THETAUSDT","ENJUSDT","MITHUSDT","MATICUSDT","ATOMUSDT","TFUELUSDT","ONEUSDT","FTMUSDT","ALGOUSDT","USDSBUSDT","GTOUSDT","ERDUSDT","DOGEUSDT","DUSKUSDT","ANKRUSDT","WINUSDT","COSUSDT","NPXSUSDT","MTLUSDT","PERLUSDT","DENTUSDT","MFTUSDT","KEYUSDT","STORMUSDT","DOCKUSDT","WANUSDT","FUNUSDT","CHZUSDT","BANDUSDT","BEAMUSDT","XTZUSDT","RENUSDT","RVNUSDT","HCUSDT","HBARUSDT","NKNUSDT","STXUSDT","KAVAUSDT","ARPAUSDT","IOTXUSDT","RLCUSDT","MCOUSDT","CTXCUSDT","BCHUSDT","TROYUSDT","VITEUSDT","EURUSDT","OGNUSDT","DREPUSDT","BULLUSDT","BEARUSDT","ETHBULLUSDT","ETHBEARUSDT","TCTUSDT","WRXUSDT","LSKUSDT","BNTUSDT","LTOUSDT","EOSBULLUSDT","EOSBEARUSDT","XRPBULLUSDT","XRPBEARUSDT","STRATUSDT","AIONUSDT","MBLUSDT","COTIUSDT","BNBBULLUSDT","BNBBEARUSDT","STPTUSDT","WTCUSDT","DATAUSDT","XZCUSDT","SOLUSDT","CTSIUSDT","HIVEUSDT","CHRUSDT","BTCUPUSDT","BTCDOWNUSDT","GXSUSDT","ARDRUSDT","STMXUSDT","KNCUSDT","REPUSDT","LRCUSDT","PNTUSDT","COMPUSDT","BKRWUSDT","ZENUSDT","SNXUSDT","ETHUPUSDT","ETHDOWNUSDT","ADAUPUSDT","ADADOWNUSDT","LINKUPUSDT","LINKDOWNUSDT","VTHOUSDT","DGBUSDT","GBPUSDT","SXPUSDT","MKRUSDT","DAIUSDT","DCRUSDT","STORJUSDT","BNBUPUSDT","BNBDOWNUSDT","XTZUPUSDT","XTZDOWNUSDT","MANAUSDT","AUDUSDT","YFIUSDT","BALUSDT","BLZUSDT","IRISUSDT","KMDUSDT","JSTUSDT","ANTUSDT","CRVUSDT","SANDUSDT","OCEANUSDT","NMRUSDT","DOTUSDT","RSRUSDT","PAXGUSDT","WNXMUSDT","TRBUSDT","SUSHIUSDT","YFIIUSDT","KSMUSDT","EGLDUSDT","DIAUSDT","RUNEUSDT","FIOUSDT","UMAUSDT","EOSUPUSDT","EOSDOWNUSDT","TRXUPUSDT","TRXDOWNUSDT","XRPUPUSDT","XRPDOWNUSDT","DOTUPUSDT","DOTDOWNUSDT","BELUSDT","WINGUSDT","LTCUPUSDT","LTCDOWNUSDT","UNIUSDT","NBSUSDT","OXTUSDT","SUNUSDT","AVAXUSDT","FLMUSDT","UNIUPUSDT","UNIDOWNUSDT","ORNUSDT","UTKUSDT","XVSUSDT","ALPHAUSDT","AAVEUSDT","NEARUSDT","SXPUPUSDT","SXPDOWNUSDT","FILUSDT","FILUPUSDT","FILDOWNUSDT","YFIUPUSDT","YFIDOWNUSDT","INJUSDT","AUDIOUSDT","CTKUSDT","BCHUPUSDT","BCHDOWNUSDT","AKROUSDT","AXSUSDT","HARDUSDT","DNTUSDT","STRAXUSDT","UNFIUSDT","ROSEUSDT","AVAUSDT","XEMUSDT","AAVEUPUSDT","AAVEDOWNUSDT","SKLUSDT","SUSDUSDT","SUSHIUPUSDT","SUSHIDOWNUSDT","XLMUPUSDT","XLMDOWNUSDT","GRTUSDT","JUVUSDT","PSGUSDT","1INCHUSDT","REEFUSDT","OGUSDT","ATMUSDT","ASRUSDT","CELOUSDT","RIFUSDT","TRUUSDT","CKBUSDT","TWTUSDT","FIROUSDT","LITUSDT","SFPUSDT","DODOUSDT","CAKEUSDT","ACMUSDT","BADGERUSDT","FISUSDT","OMUSDT","PONDUSDT","DEGOUSDT","ALICEUSDT","LINAUSDT","PERPUSDT","RAMPUSDT","SUPERUSDT","CFXUSDT","EPSUSDT","AUTOUSDT","TKOUSDT","PUNDIXUSDT","TLMUSDT","1INCHUPUSDT","1INCHDOWNUSDT","BTGUSDT","MIRUSDT","BARUSDT","FORTHUSDT","BAKEUSDT","BURGERUSDT","SLPUSDT","SHIBUSDT","ICPUSDT","ARUSDT","POLSUSDT","MDXUSDT","MASKUSDT","LPTUSDT","NUUSDT","XVGUSDT","ATAUSDT","GTCUSDT","TORNUSDT","KEEPUSDT","ERNUSDT","KLAYUSDT","PHAUSDT","BONDUSDT","MLNUSDT","DEXEUSDT","C98USDT","CLVUSDT","QNTUSDT","FLOWUSDT","TVKUSDT","MINAUSDT","FARMUSDT","ALPACAUSDT","QUICKUSDT","MBOXUSDT","FORUSDT","REQUSDT","GHSTUSDT","WAXPUSDT","TRIBEUSDT","GNOUSDT","XECUSDT","ELFUSDT","DYDXUSDT","POLYUSDT","IDEXUSDT","VIDTUSDT","USDPUSDT","GALAUSDT","ILVUSDT","YGGUSDT","SYSUSDT","DFUSDT","FIDAUSDT","FRONTUSDT","CVPUSDT","AGLDUSDT","RADUSDT","BETAUSDT","RAREUSDT","LAZIOUSDT","CHESSUSDT","ADXUSDT","AUCTIONUSDT","DARUSDT","BNXUSDT","RGTUSDT","MOVRUSDT","CITYUSDT","ENSUSDT","KP3RUSDT","QIUSDT","PORTOUSDT","POWRUSDT","VGXUSDT","JASMYUSDT","AMPUSDT","PLAUSDT","PYRUSDT","RNDRUSDT","ALCXUSDT","SANTOSUSDT","MCUSDT","ANYUSDT","BICOUSDT","FLUXUSDT","FXSUSDT","VOXELUSDT","HIGHUSDT","CVXUSDT","PEOPLEUSDT","OOKIUSDT","SPELLUSDT","USTUSDT","JOEUSDT","ACHUSDT","IMXUSDT","GLMRUSDT","LOKAUSDT","API3USDT","BTTCUSDT","ACAUSDT","ANCUSDT","XNOUSDT","WOOUSDT","ALPINEUSDT","TUSDT","ASTRUSDT","GMTUSDT","KDAUSDT","APEUSDT","BSWUSDT","BIFIUSDT","MULTIUSDT","STEEMUSDT","MOBUSDT","NEXOUSDT","REIUSDT","GALUSDT","LDOUSDT","EPXUSDT","OPUSDT","LEVERUSDT","STGUSDT","LUNCUSDT","GMXUSDT","NEBLUSDT","POLYXUSDT","APTUSDT","OSMOUSDT","HFTUSDT","PHBUSDT","HOOKUSDT","MAGICUSDT","HIFIUSDT","RPLUSDT","PROSUSDT","AGIXUSDT","GNSUSDT","SYNUSDT","VIBUSDT","SSVUSDT","LQTYUSDT","AMBUSDT","BETHUSDT","USTCUSDT","GASUSDT","GLMUSDT","PROMUSDT","QKCUSDT","UFTUSDT","IDUSDT","ARBUSDT","LOOMUSDT","OAXUSDT","RDNTUSDT","WBTCUSDT","EDUUSDT","SUIUSDT","AERGOUSDT","PEPEUSDT","FLOKIUSDT","ASTUSDT","SNTUSDT","COMBOUSDT","MAVUSDT","PENDLEUSDT","ARKMUSDT","WBETHUSDT","WLDUSDT","FDUSDUSDT","SEIUSDT","CYBERUSDT","ARKUSDT","CREAMUSDT","GFTUSDT","IQUSDT","NTRNUSDT","TIAUSDT","MEMEUSDT","ORDIUSDT","BEAMXUSDT","PIVXUSDT","VICUSDT","BLURUSDT","VANRYUSDT","AEURUSDT","JTOUSDT","1000SATSUSDT"};
    public static String[] USDT_COINS_NOT_IN_FUTURE_ACTION = {"BCCUSDT","TUSDUSDT","VENUSDT","NULSUSDT","PAXUSDT","BCHABCUSDT","BCHSVUSDT","USDSUSDT","NANOUSDT","MITHUSDT","TFUELUSDT","USDSBUSDT","GTOUSDT","ERDUSDT","WINUSDT","COSUSDT","NPXSUSDT","PERLUSDT","MFTUSDT","STORMUSDT","DOCKUSDT","WANUSDT","FUNUSDT","BEAMUSDT","HCUSDT","MCOUSDT","CTXCUSDT","TROYUSDT","VITEUSDT","EURUSDT","DREPUSDT","BULLUSDT","BEARUSDT","ETHBULLUSDT","ETHBEARUSDT","TCTUSDT","WRXUSDT","LSKUSDT","LTOUSDT","EOSBULLUSDT","EOSBEARUSDT","XRPBULLUSDT","XRPBEARUSDT","STRATUSDT","AIONUSDT","BNBBULLUSDT","BNBBEARUSDT","WTCUSDT","DATAUSDT","XZCUSDT","HIVEUSDT","BTCUPUSDT","BTCDOWNUSDT","GXSUSDT","ARDRUSDT","REPUSDT","PNTUSDT","BKRWUSDT","ETHUPUSDT","ETHDOWNUSDT","ADAUPUSDT","ADADOWNUSDT","LINKUPUSDT","LINKDOWNUSDT","VTHOUSDT","GBPUSDT","DAIUSDT","DCRUSDT","BNBUPUSDT","BNBDOWNUSDT","XTZUPUSDT","XTZDOWNUSDT","AUDUSDT","IRISUSDT","KMDUSDT","JSTUSDT","PAXGUSDT","WNXMUSDT","DIAUSDT","FIOUSDT","EOSUPUSDT","EOSDOWNUSDT","TRXUPUSDT","TRXDOWNUSDT","XRPUPUSDT","XRPDOWNUSDT","DOTUPUSDT","DOTDOWNUSDT","WINGUSDT","LTCUPUSDT","LTCDOWNUSDT","NBSUSDT","SUNUSDT","UNIUPUSDT","UNIDOWNUSDT","ORNUSDT","UTKUSDT","SXPUPUSDT","SXPDOWNUSDT","FILUPUSDT","FILDOWNUSDT","YFIUPUSDT","YFIDOWNUSDT","BCHUPUSDT","BCHDOWNUSDT","HARDUSDT","DNTUSDT","AVAUSDT","AAVEUPUSDT","AAVEDOWNUSDT","SUSDUSDT","SUSHIUPUSDT","SUSHIDOWNUSDT","XLMUPUSDT","XLMDOWNUSDT","JUVUSDT","PSGUSDT","OGUSDT","ATMUSDT","ASRUSDT","FIROUSDT","ACMUSDT","FISUSDT","OMUSDT","PONDUSDT","DEGOUSDT","RAMPUSDT","EPSUSDT","AUTOUSDT","TKOUSDT","PUNDIXUSDT","1INCHUPUSDT","1INCHDOWNUSDT","BTGUSDT","MIRUSDT","BARUSDT","FORTHUSDT","BURGERUSDT","SHIBUSDT","POLSUSDT","MDXUSDT","TORNUSDT","ERNUSDT","PHAUSDT","MLNUSDT","DEXEUSDT","CLVUSDT","TVKUSDT","FARMUSDT","ALPACAUSDT","QUICKUSDT","MBOXUSDT","FORUSDT","REQUSDT","GHSTUSDT","TRIBEUSDT","GNOUSDT","XECUSDT","ELFUSDT","POLYUSDT","VIDTUSDT","USDPUSDT","SYSUSDT","DFUSDT","FIDAUSDT","CVPUSDT","BETAUSDT","RAREUSDT","LAZIOUSDT","CHESSUSDT","ADXUSDT","AUCTIONUSDT","RGTUSDT","MOVRUSDT","CITYUSDT","KP3RUSDT","QIUSDT","PORTOUSDT","VGXUSDT","AMPUSDT","PLAUSDT","PYRUSDT","ALCXUSDT","SANTOSUSDT","MCUSDT","ANYUSDT","FLUXUSDT","VOXELUSDT","OOKIUSDT","USTUSDT","LOKAUSDT","BTTCUSDT","ACAUSDT","XNOUSDT","ALPINEUSDT","KDAUSDT","BSWUSDT","BIFIUSDT","MULTIUSDT","MOBUSDT","NEXOUSDT","REIUSDT","EPXUSDT","LUNCUSDT","NEBLUSDT","OSMOUSDT","RPLUSDT","PROSUSDT","GNSUSDT","SYNUSDT","VIBUSDT","BETHUSDT","GLMUSDT","PROMUSDT","QKCUSDT","UFTUSDT","OAXUSDT","WBTCUSDT","AERGOUSDT","PEPEUSDT","FLOKIUSDT","ASTUSDT","WBETHUSDT","FDUSDUSDT","CREAMUSDT","GFTUSDT","IQUSDT","PIVXUSDT","VICUSDT","VANRYUSDT","AEURUSDT"};
    public static String[] FUTURE_USDT_COINS_IN_ACTION = {"ETHUSDT","BNBUSDT","NEOUSDT","LTCUSDT","QTUMUSDT","ADAUSDT","EOSUSDT","IOTAUSDT","XLMUSDT","ONTUSDT","TRXUSDT","ETCUSDT","ICXUSDT","VETUSDT","USDCUSDT","LINKUSDT","WAVESUSDT","ONGUSDT","HOTUSDT","ZILUSDT","ZRXUSDT","FETUSDT","BATUSDT","XMRUSDT","ZECUSDT","IOSTUSDT","CELRUSDT","DASHUSDT","OMGUSDT","THETAUSDT","ENJUSDT","MATICUSDT","ATOMUSDT","ONEUSDT","FTMUSDT","ALGOUSDT","DOGEUSDT","DUSKUSDT","ANKRUSDT","MTLUSDT","DENTUSDT","KEYUSDT","CHZUSDT","BANDUSDT","XTZUSDT","RENUSDT","RVNUSDT","HBARUSDT","NKNUSDT","STXUSDT","KAVAUSDT","ARPAUSDT","IOTXUSDT","RLCUSDT","BCHUSDT","OGNUSDT","BNTUSDT","MBLUSDT","COTIUSDT","STPTUSDT","SOLUSDT","CTSIUSDT","CHRUSDT","STMXUSDT","KNCUSDT","LRCUSDT","COMPUSDT","ZENUSDT","SNXUSDT","DGBUSDT","SXPUSDT","MKRUSDT","STORJUSDT","MANAUSDT","YFIUSDT","BALUSDT","BLZUSDT","ANTUSDT","CRVUSDT","SANDUSDT","OCEANUSDT","NMRUSDT","DOTUSDT","RSRUSDT","TRBUSDT","SUSHIUSDT","YFIIUSDT","KSMUSDT","EGLDUSDT","RUNEUSDT","UMAUSDT","BELUSDT","UNIUSDT","OXTUSDT","AVAXUSDT","FLMUSDT","XVSUSDT","ALPHAUSDT","AAVEUSDT","NEARUSDT","FILUSDT","INJUSDT","AUDIOUSDT","CTKUSDT","AKROUSDT","AXSUSDT","STRAXUSDT","UNFIUSDT","ROSEUSDT","XEMUSDT","SKLUSDT","GRTUSDT","1INCHUSDT","REEFUSDT","CELOUSDT","RIFUSDT","TRUUSDT","CKBUSDT","TWTUSDT","LITUSDT","SFPUSDT","DODOUSDT","CAKEUSDT","BADGERUSDT","ALICEUSDT","LINAUSDT","PERPUSDT","SUPERUSDT","CFXUSDT","TLMUSDT","BAKEUSDT","SLPUSDT","ICPUSDT","ARUSDT","MASKUSDT","LPTUSDT","NUUSDT","XVGUSDT","ATAUSDT","GTCUSDT","KEEPUSDT","KLAYUSDT","BONDUSDT","C98USDT","QNTUSDT","FLOWUSDT","MINAUSDT","WAXPUSDT","DYDXUSDT","IDEXUSDT","GALAUSDT","ILVUSDT","YGGUSDT","FRONTUSDT","AGLDUSDT","RADUSDT","DARUSDT","BNXUSDT","ENSUSDT","POWRUSDT","JASMYUSDT","RNDRUSDT","BICOUSDT","FXSUSDT","HIGHUSDT","CVXUSDT","PEOPLEUSDT","SPELLUSDT","JOEUSDT","ACHUSDT","IMXUSDT","GLMRUSDT","API3USDT","ANCUSDT","WOOUSDT","TUSDT","ASTRUSDT","GMTUSDT","APEUSDT","STEEMUSDT","GALUSDT","LDOUSDT","OPUSDT","LEVERUSDT","STGUSDT","GMXUSDT","POLYXUSDT","APTUSDT","HFTUSDT","PHBUSDT","HOOKUSDT","MAGICUSDT","HIFIUSDT","AGIXUSDT","SSVUSDT","LQTYUSDT","AMBUSDT","USTCUSDT","GASUSDT","IDUSDT","ARBUSDT","LOOMUSDT","RDNTUSDT","EDUUSDT","SUIUSDT","SNTUSDT","COMBOUSDT","MAVUSDT","PENDLEUSDT","ARKMUSDT","WLDUSDT","SEIUSDT","CYBERUSDT","ARKUSDT","NTRNUSDT","TIAUSDT","MEMEUSDT","ORDIUSDT","BEAMXUSDT","BLURUSDT","JTOUSDT","1000SATSUSDT"};
    public static String[] FUTURE_USDT_COINS_FAVOURITE = {"ETHUSDT","BNBUSDT","LTCUSDT","QTUMUSDT","ADAUSDT","EOSUSDT","IOTAUSDT","TRXUSDT","ETCUSDT", "DOGEUSDT", "USDCUSDT", "SANDUSDT", "MANAUSDT", "AXSUSDT", "ENJUSDT", "GALAUSDT", "APEUSDT","XMRUSDT", "ZECUSDT", "DASHUSDT", "ZENUSDT","ARKMUSDT","WLDUSDT","FETUSDT","AGIXUSDT","OCEANUSDT"};
    public static String[] FUTURE_USDT_COINS_BY_PRICE_AND_MARKETCAP = {"ETHUSDT", "BNBUSDT", "ADAUSDT", "SOLUSDT", "DOTUSDT", "DOGEUSDT", "USDCUSDT", "MATICUSDT", "LTCUSDT"};
    //DeFi-focused Coins (Type and Future Growth):
    public static String[] FUTURE_USDT_COINS_BY_TYPE_AND_GROWTH = {"UNIUSDT", "AVAXUSDT", "LINKUSDT", "CAKEUSDT", "AAVEUSDT", "SUSHIUSDT", "RUNEUSDT", "COMPUSDT", "YFIUSDT"};
    // Layer-1 Blockchains with Potential for Growth (Type and Future Growth):
    public static String[] FUTURE_USDT_COINS_BY_LAYER1_BLOCKCHAIN  = {"ALGOUSDT", "ATOMUSDT", "NEARUSDT", "FLOWUSDT", "EGLDUSDT", "ONEUSDT", "HBARUSDT", "FTMUSDT", "WAVESUSDT", "MINAUSDT"};
    // Privacy Coins
    public static String[] FUTURE_USDT_COINS_BY_PRIVACY_COINS  = {"XMRUSDT", "ZECUSDT", "DASHUSDT", "ZENUSDT"};
    //Metaverse and Gaming-Related Coins (Type and Future Growth)
    public static String[] FUTURE_USDT_COINS_BY_METAVERSE_AND_GAMING_COINS  = {"SANDUSDT", "MANAUSDT", "AXSUSDT", "ENJUSDT", "GALAUSDT", "APEUSDT"};
    //Meme Coins (Type and Volatility):
    public static String[] FUTURE_USDT_COINS_BY_MEME_COINS  = {"DOGEUSDT"};
    //Exchange-Issued Coins (Type and Utility):
    public static String[] FUTURE_USDT_COINS_BY_EXCHANGE_ISSUED_COINS  = {"BNBUSDT"};
    //Stablecoins (Type and Price Stability)
    public static String[] FUTURE_USDT_COINS_BY_STABLE_COINS  = {"USDCUSDT"};
    public static String[] FUTURE_USDT_AI_COINS  = {"ARKMUSDT","WLDUSDT","FETUSDT","AGIXUSDT","OCEANUSDT"};

}
