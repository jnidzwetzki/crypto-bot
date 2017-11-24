package org.achfrag.crypto.bitfinex.commands;

import org.json.JSONObject;

public class UnsubscribeChannel extends AbstractAPICommand {
	
	/**
	 * The channel
	 */
	private final int channel;

	public UnsubscribeChannel(final int channel) {
		this.channel = channel;
	}

	@Override
	public String getCommand() {
		final JSONObject subscribeJson = new JSONObject();
		subscribeJson.put("event", "unsubscribe");
		subscribeJson.put("chanId", channel);
		
		return subscribeJson.toString();
	}

}
