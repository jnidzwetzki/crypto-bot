package org.achfrag.trading.crypto.bitfinex.commands;

import org.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import org.achfrag.trading.crypto.bitfinex.entity.BitfinexCurrencyPair;
import org.json.JSONObject;

public class SubscribeTickerCommand extends AbstractAPICommand {

	private String currencyPair;

	public SubscribeTickerCommand(final BitfinexCurrencyPair currencyPair) {
		this.currencyPair = currencyPair.toBitfinexString();
	}
	
	public SubscribeTickerCommand(final String currencyPair) {
		this.currencyPair = currencyPair;
	}

	@Override
	public String getCommand(final BitfinexApiBroker bitfinexApiBroker) {
		final JSONObject subscribeJson = new JSONObject();
		subscribeJson.put("event", "subscribe");
		subscribeJson.put("channel", "ticker");
		subscribeJson.put("symbol", currencyPair);
		
		return subscribeJson.toString();
	}
}
