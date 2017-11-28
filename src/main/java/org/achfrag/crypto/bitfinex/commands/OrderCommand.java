package org.achfrag.crypto.bitfinex.commands;

import org.achfrag.crypto.bitfinex.BitfinexApiBroker;
import org.achfrag.crypto.bitfinex.BitfinexOrder;
import org.json.JSONObject;

public class OrderCommand extends AbstractAPICommand {

	private String uuid;
	private BitfinexOrder bitfinexOrder;

	public OrderCommand(final String uuid, final BitfinexOrder bitfinexOrder) {
		this.uuid = uuid;
		this.bitfinexOrder = bitfinexOrder;
	}

	@Override
	public String getCommand(final BitfinexApiBroker bitfinexApiBroker) throws CommandException {
		
		final JSONObject orderJson = new JSONObject();
		orderJson.put("cid", uuid);
		orderJson.put("type", bitfinexOrder.getType().getBifinexString());
		orderJson.put("symbol", bitfinexOrder.getSymbol());
		orderJson.put("amount", bitfinexOrder.getAmount());
		orderJson.put("price", bitfinexOrder.getPrice());

		if(bitfinexOrder.getPriceTrailing() != Double.NaN) {
			orderJson.put("price_trailing", bitfinexOrder.getPriceTrailing());
		}
		
		if(bitfinexOrder.getPriceAuxLimit() != Double.NaN) {
			orderJson.put("price_aux_limit", bitfinexOrder.getPriceAuxLimit());
		}
		
		if(bitfinexOrder.isHidden()) {
			orderJson.put("hidden", "1");
		}
		
		if(bitfinexOrder.isPostOnly()) {
			orderJson.put("postonly", "1");
		}
		
		final StringBuilder sb = new StringBuilder();
		sb.append("[0,\"on\", null, {");
		sb.append(bitfinexOrder.toString());
		sb.append("}]\n");
		
		return sb.toString();
	}

}
