package org.achfrag.trading.crypto.bitfinex.channel;

import org.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import org.achfrag.trading.crypto.bitfinex.entity.APIException;
import org.json.JSONArray;

public interface ChannelHandler {
	
	/**
	 * Handle data for the channel
	 * @param bitfinexApiBroker
	 * @param jsonArray
	 * @throws APIException 
	 */
	public void handleChannelData(final BitfinexApiBroker bitfinexApiBroker, final JSONArray jsonArray) 
			throws APIException;

}
