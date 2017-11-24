package org.achfrag.crypto.bitfinex.commands;

import org.achfrag.crypto.bitfinex.BitfinexApiBroker;
import org.achfrag.crypto.bitfinex.misc.CurrencyPair;
import org.achfrag.crypto.bitfinex.misc.Timeframe;
import org.json.JSONObject;

public class SubscribeCandles extends AbstractAPICommand {

	private CurrencyPair currencyPair;

	private Timeframe timeframe;

	public SubscribeCandles(final CurrencyPair currencyPair, final Timeframe timeframe) {
		this.currencyPair = currencyPair;
		this.timeframe = timeframe;
	}

	@Override
	public String getCommand(final BitfinexApiBroker bitfinexApiBroker) {
		final JSONObject subscribeJson = new JSONObject();
		subscribeJson.put("event", "subscribe");
		subscribeJson.put("channel", "candles");
		subscribeJson.put("key", "trade:" + timeframe.getBitfinexString() + ":" + currencyPair.toBitfinexString());
		
		System.out.println(subscribeJson.toString());
		
		return subscribeJson.toString();
	}

}
