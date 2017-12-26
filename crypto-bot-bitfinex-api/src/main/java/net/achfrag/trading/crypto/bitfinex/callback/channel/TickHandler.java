package net.achfrag.trading.crypto.bitfinex.callback.channel;

import java.time.ZonedDateTime;

import org.json.JSONArray;
import org.ta4j.core.BaseTick;
import org.ta4j.core.Tick;

import net.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import net.achfrag.trading.crypto.bitfinex.Const;
import net.achfrag.trading.crypto.bitfinex.entity.APIException;

public class TickHandler implements ChannelCallbackHandler {

	/**
	 * Handle a tick callback
	 * @param channel
	 * @param subarray
	 */
	@Override
	public void handleChannelData(final BitfinexApiBroker bitfinexApiBroker, 
			final String channelSymbol, final JSONArray jsonArray) throws APIException {


		// 0 = BID
		// 2 = ASK
		// 6 = Price
		final double price = jsonArray.getDouble(6);
		
		final Tick tick = new BaseTick(ZonedDateTime.now(Const.BITFINEX_TIMEZONE), price, price, 
				price, price, price);

		bitfinexApiBroker.getTickerManager().handleNewTick(channelSymbol, tick);
	}
}
