package net.achfrag.trading.crypto.bitfinex.callback.api;

import org.json.JSONArray;

import net.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import net.achfrag.trading.crypto.bitfinex.entity.APIException;

public interface APICallbackHandler {
	
	/**
	 * Handle data for the channel
	 * @param bitfinexApiBroker
	 * @param jsonArray
	 * @throws APIException 
	 */
	public void handleChannelData(final BitfinexApiBroker bitfinexApiBroker, final JSONArray jsonArray) 
			throws APIException;

}
