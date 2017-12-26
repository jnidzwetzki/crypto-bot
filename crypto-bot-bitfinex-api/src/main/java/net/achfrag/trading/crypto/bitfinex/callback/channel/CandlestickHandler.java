package net.achfrag.trading.crypto.bitfinex.callback.channel;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.ta4j.core.BaseTick;
import org.ta4j.core.Tick;

import net.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import net.achfrag.trading.crypto.bitfinex.Const;
import net.achfrag.trading.crypto.bitfinex.entity.APIException;

public class CandlestickHandler implements ChannelCallbackHandler {

	/**
	 * Handle a candlestick callback
	 * @param channel
	 * @param subarray
	 */
	@Override
	public void handleChannelData(final BitfinexApiBroker bitfinexApiBroker, 
			final String channelSymbol, final JSONArray jsonArray) throws APIException {

		// channel symbol trade:1m:tLTCUSD
		final List<Tick> ticksBuffer = new ArrayList<>();
		
		// Snapshots contain multiple Bars, Updates only one
		if(jsonArray.get(0) instanceof JSONArray) {
			for (int pos = 0; pos < jsonArray.length(); pos++) {
				final JSONArray parts = jsonArray.getJSONArray(pos);	
				paseCandlestick(ticksBuffer, parts);
			}
		} else {
			paseCandlestick(ticksBuffer, jsonArray);
		}
		
		ticksBuffer.sort((t1, t2) -> t1.getEndTime().compareTo(t2.getEndTime()));

		bitfinexApiBroker.getTickerManager().handleTicksList(channelSymbol, ticksBuffer);
	}

	/**
	 * Parse a candlestick from JSON result
	 */
	private void paseCandlestick(final List<Tick> ticksBuffer, final JSONArray parts) {
		// 0 = Timestamp, 1 = Open, 2 = Close, 3 = High, 4 = Low,  5 = Volume
		final Instant i = Instant.ofEpochMilli(parts.getLong(0));
		final ZonedDateTime withTimezone = ZonedDateTime.ofInstant(i, Const.BITFINEX_TIMEZONE);
		
		final double open = parts.getDouble(1);
		final double close = parts.getDouble(2);
		final double high = parts.getDouble(3);
		final double low = parts.getDouble(4);
		final double volume = parts.getDouble(5);
		
		final Tick tick = new BaseTick(withTimezone, open, high, low, close, volume);
		ticksBuffer.add(tick);
	}


}
