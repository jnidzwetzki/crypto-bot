package org.achfrag.trading.crypto.bitfinex.channel;

import org.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import org.achfrag.trading.crypto.bitfinex.entity.APIException;
import org.json.JSONArray;

public class DoNothingHandler implements ChannelHandler {

	@Override
	public void handleChannelData(final BitfinexApiBroker bitfinexApiBroker, 
			final JSONArray jsonArray) throws APIException {
		
	}

}
