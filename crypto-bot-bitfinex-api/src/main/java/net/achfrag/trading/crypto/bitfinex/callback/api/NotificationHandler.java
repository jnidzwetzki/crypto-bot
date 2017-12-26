package net.achfrag.trading.crypto.bitfinex.callback.api;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import net.achfrag.trading.crypto.bitfinex.entity.APIException;
import net.achfrag.trading.crypto.bitfinex.entity.ExchangeOrder;
import net.achfrag.trading.crypto.bitfinex.entity.ExchangeOrderState;

public class NotificationHandler implements APICallbackHandler {

	/**
	 * The Logger
	 */
	final static Logger logger = LoggerFactory.getLogger(NotificationHandler.class);
	
	@Override
	public void 	handleChannelData(final BitfinexApiBroker bitfinexApiBroker, 
			final JSONArray jsonArray) throws APIException {
		
		logger.debug("Got notification callback {}", jsonArray.toString());


		final JSONArray notifificationValue = jsonArray.optJSONArray(2);

		// Test for order error callback
		// [0,"n",[null,"on-req",null,null,[null,null,1513970684865000,"tBTCUSD",null,null,0.001,0.001,"EXCHANGE MARKET",null,null,null,null,null,null,null,12940,null,null,null,null,null,null,0,null,null],null,"ERROR","Invalid order: minimum size for BTC/USD is 0.002"]]
		if(notifificationValue != null) {
			if("on-req".equals(notifificationValue.getString(1))) {
				
				final JSONArray order = notifificationValue.optJSONArray(4);
				final String state = notifificationValue.optString(6);
				final String stateValue = notifificationValue.optString(7);

				if("ERROR".equals(state)) {
					final ExchangeOrder exchangeOrder = new ExchangeOrder();
					exchangeOrder.setApikey(bitfinexApiBroker.getApiKey());
					exchangeOrder.setCid(order.getLong(2));
					exchangeOrder.setSymbol(order.getString(3));
					exchangeOrder.setState(ExchangeOrderState.STATE_ERROR);
					
					logger.error("State for order {} is {}, reason is {}", exchangeOrder.getOrderId(), state, stateValue);

					bitfinexApiBroker.getOrderManager().updateOrder(exchangeOrder);
				}
			}
		}
	}

}
