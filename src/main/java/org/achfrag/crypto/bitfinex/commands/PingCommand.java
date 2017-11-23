package org.achfrag.crypto.bitfinex.commands;

import org.json.JSONObject;

public class PingCommand extends AbstractAPICommand {

	@Override
	public String getCommand() {
		final JSONObject subscribeJson = new JSONObject();
		subscribeJson.put("event", "ping");
		return subscribeJson.toString();
	}

}
