package org.achfrag.crypto.bitfinex.commands;

import org.achfrag.crypto.pair.CurrencyPair;
import org.json.JSONObject;

public class SubscribeCandles extends AbstractAPICommand {

	private CurrencyPair currencyPair;
	
	public final static String TIMEFRAME_1M = "1m";
	
	public final static String TIMEFRAME_5M = "5m";

	private String timeframe;

	public SubscribeCandles(final CurrencyPair currencyPair, final String timeframe) {
		this.currencyPair = currencyPair;
		this.timeframe = timeframe;
	}

	@Override
	public String getCommand() {
		final JSONObject subscribeJson = new JSONObject();
		subscribeJson.put("event", "subscribe");
		subscribeJson.put("channel", "candles");
		subscribeJson.put("key", "trade:" + timeframe + ":" + currencyPair.toBitfinexString());
		
		System.out.println(subscribeJson.toString());
		
		return subscribeJson.toString();
	}

}
