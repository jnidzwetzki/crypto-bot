package org.achfrag.trading.crypto.bitfinex;

import java.time.ZoneId;
import java.time.ZoneOffset;

public class Const {

	/**
	 * The default timezone
	 */
	public final static ZoneId TIMEZONE = ZoneId.of("Europe/Berlin");

	/**
	 * The bitfinex timezone
	 */
	public final static ZoneId BITFINEX_TIMEZONE = ZoneOffset.UTC;

}
