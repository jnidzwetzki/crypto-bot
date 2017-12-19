package net.achfrag.trading.crypto.bitfinex.commands;

import org.json.JSONObject;

import net.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;

public class PingCommand extends AbstractAPICommand {

	@Override
	public String getCommand(final BitfinexApiBroker bitfinexApiBroker) {
		final JSONObject subscribeJson = new JSONObject();
		subscribeJson.put("event", "ping");
		return subscribeJson.toString();
	}

}
