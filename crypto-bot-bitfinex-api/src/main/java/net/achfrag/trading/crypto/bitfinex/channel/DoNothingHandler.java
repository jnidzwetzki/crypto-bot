package net.achfrag.trading.crypto.bitfinex.channel;

import org.json.JSONArray;

import net.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import net.achfrag.trading.crypto.bitfinex.entity.APIException;

public class DoNothingHandler implements ChannelHandler {

	@Override
	public void handleChannelData(final BitfinexApiBroker bitfinexApiBroker, 
			final JSONArray jsonArray) throws APIException {
		
	}

}
