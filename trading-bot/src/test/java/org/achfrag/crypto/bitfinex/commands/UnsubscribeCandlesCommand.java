package org.achfrag.crypto.bitfinex.commands;

import org.achfrag.crypto.bitfinex.BitfinexApiBroker;
import org.achfrag.crypto.bitfinex.entity.BitfinexCurrencyPair;
import org.achfrag.crypto.bitfinex.entity.Timeframe;
import org.json.JSONObject;

public class UnsubscribeCandlesCommand extends AbstractAPICommand {


	private BitfinexCurrencyPair currencyPair;
	
	private Timeframe timeframe;

	public UnsubscribeCandlesCommand(final BitfinexCurrencyPair currencyPair, final Timeframe timeframe) {
		this.currencyPair = currencyPair;
		this.timeframe = timeframe;
	}

	@Override
	public String getCommand(final BitfinexApiBroker bitfinexApiBroker) {
		
		final String symbol = currencyPair.toBifinexCandlestickString(timeframe);
		
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
