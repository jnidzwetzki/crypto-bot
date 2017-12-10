package org.achfrag.trading.crypto.bitfinex.commands;

import org.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import org.achfrag.trading.crypto.bitfinex.entity.BitfinexOrder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderCommand extends AbstractAPICommand {

	private final BitfinexOrder bitfinexOrder;
	
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(OrderCommand.class);

	public OrderCommand(final BitfinexOrder bitfinexOrder) {
		this.bitfinexOrder = bitfinexOrder;
	}

	@Override
	public String getCommand(final BitfinexApiBroker bitfinexApiBroker) throws CommandException {
		
		final JSONObject orderJson = new JSONObject();
		orderJson.put("cid", bitfinexOrder.getCid());
		orderJson.put("type", bitfinexOrder.getType().getBifinexString());
		orderJson.put("symbol", bitfinexOrder.getSymbol().toBitfinexString());
		orderJson.put("amount", Double.toString(bitfinexOrder.getAmount()));
		
		if(bitfinexOrder.getPrice() != -1) {
			orderJson.put("price", Double.toString(bitfinexOrder.getPrice()));
		}

		if(bitfinexOrder.getPriceTrailing() != -1) {
			orderJson.put("price_trailing", bitfinexOrder.getPriceTrailing());
		}
		
		if(bitfinexOrder.getPriceAuxLimit() != -1) {
			orderJson.put("price_aux_limit", bitfinexOrder.getPriceAuxLimit());
		}
		
		if(bitfinexOrder.isHidden()) {
			orderJson.put("hidden", 1);
		} else {
			orderJson.put("hidden", 0);
		}
		
		if(bitfinexOrder.isPostOnly()) {
			orderJson.put("postonly", 1);
		}
		
		if(bitfinexOrder.getGroupId() > 0) {
			orderJson.put("gid", bitfinexOrder.getGroupId());
		}
		
		final StringBuilder sb = new StringBuilder();
		sb.append("[0,\"on\", null, ");
		sb.append(orderJson.toString());
		sb.append("]\n");
		
		logger.debug(sb.toString());
		
		return sb.toString();
	}

}
