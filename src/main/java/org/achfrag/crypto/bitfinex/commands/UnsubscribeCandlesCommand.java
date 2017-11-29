package org.achfrag.crypto.bitfinex.commands;

import org.achfrag.crypto.bitfinex.BitfinexApiBroker;
import org.achfrag.crypto.bitfinex.entity.CurrencyPair;
import org.achfrag.crypto.bitfinex.entity.Timeframe;
import org.json.JSONObject;

public class UnsubscribeCandlesCommand extends AbstractAPICommand {


	private CurrencyPair currencyPair;
	
	private Timeframe timeframe;

	public UnsubscribeCandlesCommand(final CurrencyPair currencyPair, final Timeframe timeframe) {
		this.currencyPair = currencyPair;
		this.timeframe = timeframe;
	}

	@Override
	public String getCommand(final BitfinexApiBroker bitfinexApiBroker) {
		
		final String symbol = "trade:" + timeframe.getBitfinexString() + ":" + currencyPair.toBitfinexString();
		
		final Integer channel = bitfinexApiBroker.getChannelForSymbol(symbol);
		
		if(channel == -1) {
			throw new IllegalArgumentException("Unknown symbol: " + symbol);
		}
		
		final JSONObject subscribeJson = new JSONObject();
		subscribeJson.put("event", "unsubscribe");
		subscribeJson.put("chanId", channel);
		
		return subscribeJson.toString();
	}

}
