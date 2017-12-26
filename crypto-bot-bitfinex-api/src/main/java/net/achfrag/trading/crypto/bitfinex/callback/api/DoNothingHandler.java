package net.achfrag.trading.crypto.bitfinex.callback.api;

import org.json.JSONArray;

import net.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import net.achfrag.trading.crypto.bitfinex.entity.APIException;

public class DoNothingHandler implements APICallbackHandler {

	@Override
	public void handleChannelData(final BitfinexApiBroker bitfinexApiBroker, 
			final JSONArray jsonArray) throws APIException {
		
	}

}
