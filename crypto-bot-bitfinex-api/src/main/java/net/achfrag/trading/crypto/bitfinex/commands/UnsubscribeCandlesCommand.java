package net.achfrag.trading.crypto.bitfinex.commands;

import org.json.JSONObject;

import net.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import net.achfrag.trading.crypto.bitfinex.entity.BitfinexCurrencyPair;
import net.achfrag.trading.crypto.bitfinex.entity.Timeframe;

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
