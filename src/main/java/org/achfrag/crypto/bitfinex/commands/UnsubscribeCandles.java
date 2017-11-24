package org.achfrag.crypto.bitfinex.commands;

import org.achfrag.crypto.bitfinex.BitfinexApiBroker;
import org.achfrag.crypto.bitfinex.misc.Timeframe;
import org.achfrag.crypto.pair.CurrencyPair;
import org.json.JSONObject;

public class UnsubscribeCandles extends AbstractAPICommand {


	private CurrencyPair currencyPair;
	
	private Timeframe timeframe;

	public UnsubscribeCandles(final CurrencyPair currencyPair, final Timeframe timeframe) {
		this.currencyPair = currencyPair;
		this.timeframe = timeframe;
	}

	@Override
	public String getCommand(final BitfinexApiBroker bitfinexApiBroker) {
		
		final String symbol = "trade:" + timeframe.getBitfinexString() + ":" + currencyPair.toBitfinexString();
		
		if(! bitfinexApiBroker.getChannelMap().containsKey(symbol)) {
			throw new IllegalArgumentException("Unknown symbol: " + symbol);
		}
		
		final Integer channel = bitfinexApiBroker.getChannelMap().get(symbol);
		
		final JSONObject subscribeJson = new JSONObject();
		subscribeJson.put("event", "unsubscribe");
		subscribeJson.put("chanId", channel);
		
		return subscribeJson.toString();
	}

}
