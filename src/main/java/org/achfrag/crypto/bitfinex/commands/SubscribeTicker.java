package org.achfrag.crypto.bitfinex.commands;

import org.achfrag.crypto.pair.CurrencyPair;
import org.json.JSONObject;

public class SubscribeTicker extends AbstractAPICommand {

	private CurrencyPair currencyPair;

	public SubscribeTicker(final CurrencyPair currencyPair) {
		this.currencyPair = currencyPair;
	}

	@Override
	public String getCommand() {
		final JSONObject subscribeJson = new JSONObject();
		subscribeJson.put("event", "subscribe");
		subscribeJson.put("channel", "ticker");
		subscribeJson.put("symbol", currencyPair.toBitfinexString());
		
		return subscribeJson.toString();
	}

}
