package org.achfrag.crypto.bitfinex.commands;

import org.achfrag.crypto.bitfinex.BitfinexApiBroker;
import org.achfrag.crypto.bitfinex.BitfinexOrder;

public class OrderCommand extends AbstractAPICommand {

	private String uuid;
	private BitfinexOrder bitfinexOrder;

	public OrderCommand(final String uuid, final BitfinexOrder bitfinexOrder) {
		this.uuid = uuid;
		this.bitfinexOrder = bitfinexOrder;
	}

	@Override
	public String getCommand(final BitfinexApiBroker bitfinexApiBroker) throws CommandException {
		final StringBuilder sb = new StringBuilder();
		
		sb.append("[0,\"on\", null, {");
		sb.append("\"cid:\"" + uuid + ",");
		sb.append("\"type:\"" + bitfinexOrder.getType().getBifinexString() + ",");
		sb.append("\"symbol:\"" + bitfinexOrder.getSymbol() + ",");
		sb.append("\"amount:\"" + bitfinexOrder.getAmount() + ",");
		sb.append("\"price:\"" + bitfinexOrder.getPrice() + ",");

		if(bitfinexOrder.getPriceTrailing() != Double.NaN) {
			sb.append("\"price_trailing:\"" + bitfinexOrder.getPriceTrailing() + ",");
		}
		
		if(bitfinexOrder.getPriceAuxLimit() != Double.NaN) {
			sb.append("\"price_aux_limit:\"" + bitfinexOrder.getPriceAuxLimit() + ",");
		}
		
		if(bitfinexOrder.isHidden()) {
			sb.append("\"hidden\": 1");
		}
		
		if(bitfinexOrder.isPostOnly()) {
			sb.append("\"postonly\": 1");
		}
		
		sb.append("}]\n");
		
		return sb.toString();
	}

}
