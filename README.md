# crypto-bot

<a href="https://travis-ci.org/jnidzwetzki/crypto-bot">
  <img alt="Build Status" src="https://travis-ci.org/jnidzwetzki/crypto-bot.svg?branch=master">
</a>

A crypto currency trading bot. This project also contains a client for the Bitfinex WebSocket API (v2). At the moment, historical candles, orders, ticks, and wallets are supported.

**Warning:** Trading carries significant financial risk; you could lose a lot of money. If you are planning to use this software to trade, you should perform many tests and simulations first. This software is provided 'as is' and released under the _Apache 2.0 license_. 

# Examples

## Connecting and authorizing

```java 
final String apiKey = "....";
final String apiSecret = "....";

// For public operations (subscribe ticker, bars)
BitfinexApiBroker bitfinexApiBroker = BitfinexApiBroker();
bitfinexApiBroker.connect();

// For public and private operations (executing orders, read wallets)
BitfinexApiBroker bitfinexApiBroker = BitfinexApiBroker(apiKey, apiSecret);
bitfinexApiBroker.connect();
``` 

## Market order

```java
final BitfinexOrder order = BitfinexOrderBuilder
		.create(currency, BitfinexOrderType.MARKET, 0.002)
		.build();
		
bitfinexApiBroker.placeOrder(order);
```

## Order group

```java
final CurrencyPair currencyPair = CurrencyPair.BTC_USD;
final Tick lastValue = bitfinexApiBroker.getLastTick(currencyPair);

final int orderGroup = 4711;

final BitfinexOrder bitfinexOrder1 = BitfinexOrderBuilder
		.create(currencyPair, BitfinexOrderType.EXCHANGE_LIMIT, 0.002, lastValue.getClosePrice().toDouble() / 100.0 * 100.1)
		.setPostOnly()
		.withGroupId(orderGroup)
		.build();

final BitfinexOrder bitfinexOrder2 = BitfinexOrderBuilder
		.create(currencyPair, BitfinexOrderType.EXCHANGE_LIMIT, -0.002, lastValue.getClosePrice().toDouble() / 100.0 * 101)
		.setPostOnly()
		.withGroupId(orderGroup)
		.build();

// Cancel sell order when buy order failes
final Consumer<ExchangeOrder> ordercallback = (e) -> {
		
	if(e.getCid() == bitfinexOrder1.getCid()) {
		if(e.getState().equals(ExchangeOrder.STATE_CANCELED) 
				|| e.getState().equals(ExchangeOrder.STATE_POSTONLY_CANCELED)) {
			bitfinexApiBroker.cancelOrderGroup(orderGroup);
		}
	}
};

bitfinexApiBroker.addOrderCallback(ordercallback);

bitfinexApiBroker.placeOrder(bitfinexOrder1);
bitfinexApiBroker.placeOrder(bitfinexOrder2);
```
