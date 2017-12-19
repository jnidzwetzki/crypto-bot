package net.achfrag.trading.crypto.bitfinex.commands;

import org.json.JSONObject;

import net.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;

public class CancelOrderGroupCommand extends AbstractAPICommand {

	/**
	 * The order group
	 */
	private int orderGroup;

	public CancelOrderGroupCommand(final int orderGroup) {
		this.orderGroup = orderGroup;
	}

	@Override
	public String getCommand(BitfinexApiBroker bitfinexApiBroker) throws CommandException {
		final JSONObject cancelJson = new JSONObject();
		cancelJson.put("gid", orderGroup);
		
		final StringBuilder sb = new StringBuilder();
		sb.append("[0,\"oc_multi\", null, ");
		sb.append(cancelJson.toString());
		sb.append("]\n");
				
		return sb.toString();
	}

}
