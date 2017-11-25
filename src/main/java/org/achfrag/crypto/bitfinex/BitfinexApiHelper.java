package org.achfrag.crypto.bitfinex;

import java.util.regex.Pattern;

public class BitfinexApiHelper {

	/**
	 * The channel and payload pattern
	 */
	public final static Pattern CHANNEL_PATTERN = Pattern.compile("\\[(\\d+),(\\[.*)\\]");

	/**
	 * The payload element tokenizer
	 */
	public final static Pattern CHANNEL_ELEMENT_PATTERN = Pattern.compile("\\[([^\\]]+)\\]");


}
