package net.achfrag.trading.crypto.bitfinex.channel;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import net.achfrag.trading.crypto.bitfinex.entity.APIException;

public class HeartbeatHandler implements ChannelHandler {
	
	/**
	 * The Logger
	 */
	final static Logger logger = LoggerFactory.getLogger(HeartbeatHandler.class);

	@Override
	public void handleChannelData(final BitfinexApiBroker bitfinexApiBroker, 
			final JSONArray jsonArray) throws APIException {
		
		logger.debug("Got connection heartbeat");
		bitfinexApiBroker.updateConnectionHeartbeat();
	}

}
