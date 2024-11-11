# Automated Crypto Trading Bot with Sentiment Analysis
## Overview
This repository contains the source code for an automated cryptocurrency trading bot developed in Java, leveraging the Binance API for interaction with cryptocurrency markets. The trading bot integrates real-time market sentiment analysis to make data-driven trading decisions, incorporating advanced order types and risk management strategies.

## Features
Sentiment Analysis: Utilizes market sentiment analysis to assess the overall market mood and make informed trading decisions.

### Advanced Order Types:

#### OCO (One-Cancels-the-Other) Orders: 
Implements OCO orders for risk management, allowing the bot to place both take profit and stop loss orders simultaneously.
#### Dynamic Position Management:
#### Buy and Sell Orders: 
Executes buy and sell orders for futures trading based on the current market conditions.
#### Automated Position Adjustments: 
Dynamically adjusts trading strategies and position sizes in response to real-time market data.
#### Error Handling and Rate Limits:

##### Reliability: 
Implements robust error-handling mechanisms to ensure the reliability and stability of the trading bot.
##### Rate Limit Management: 
Handles Binance API rate limits to prevent exceeding the allowed request frequency.
## How to Use
1. Clone the Repository:
```
git clone [BinanceCryptoBot](https://github.com/asadali08527/BinanceCryptoBot)
cd BinanceCryptoBot
```
2. Configure Binance API Keys:

+ Obtain Binance API key and secret from the Binance platform.
+ Update PrivateConfig.java with your API key and secret.

3. Run the Trading Bot:

+ Execute the FutureOrderScheduler class to start the automated trading bot.
```
javac FutureOrderScheduler.java
java FutureOrderScheduler
```
#### Customization:

Adjust the trading strategies, sentiment analysis parameters, and risk management settings in the source code based on your preferences.
#### Dependencies
Java,
Binance Java Connector,
Gson Library, etc
### Contribution
Contributions are welcome! If you encounter issues or have suggestions for improvements, please open an issue or submit a pull request.
