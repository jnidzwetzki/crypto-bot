package org.achfrag.crypto.bitfinex.commands;

import org.achfrag.crypto.bitfinex.BitfinexApiBroker;
import org.json.JSONObject;

public class CancelOrder extends AbstractAPICommand {

	/**
	 * The cid
	 */
	private String id;

	public CancelOrder(final String id) {
		this.id = id;
	}

	@Override
	public String getCommand(final BitfinexApiBroker bitfinexApiBroker) throws CommandException {
		
		final JSONObject cancelJson = new JSONObject();
		cancelJson.put("id", id);
		
		final StringBuilder sb = new StringBuilder();
		sb.append("[0,\"on\", null, ");
		sb.append(cancelJson.toString());
		sb.append("]\n");
				
		return sb.toString();
	}

}
