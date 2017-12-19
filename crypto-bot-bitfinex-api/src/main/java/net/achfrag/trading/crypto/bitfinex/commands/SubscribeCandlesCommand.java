package net.achfrag.trading.crypto.bitfinex.commands;

import org.json.JSONObject;

import net.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import net.achfrag.trading.crypto.bitfinex.entity.BitfinexCurrencyPair;
import net.achfrag.trading.crypto.bitfinex.entity.Timeframe;

public class SubscribeCandlesCommand extends AbstractAPICommand {

	private BitfinexCurrencyPair currencyPair;

	private Timeframe timeframe;

	public SubscribeCandlesCommand(final BitfinexCurrencyPair currencyPair, final Timeframe timeframe) {
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
