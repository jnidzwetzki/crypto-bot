package org.achfrag.trading.crypto.bitfinex;

import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BitfinexClientFactory {
	
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(BitfinexClientFactory.class);

	/**
	 * Build a new bitfinex client
	 * @return
	 */
	public static BitfinexApiBroker buildBifinexClient() {
		final Properties prop = new Properties();
		
		try {
			final InputStream input = BitfinexClientFactory.class.getClassLoader().getResourceAsStream("auth.properties");
			
			if(input != null) {
				prop.load(input);
				
				if("true".equals(prop.getProperty("authEnabled"))) {
					final String apiKey = prop.getProperty("apiKey");
					final String apiSecret = prop.getProperty("apiSecret");
					
					if(apiKey == null || apiSecret == null) {
						logger.warn("API key or secret are null");
					} else {
						logger.info("Building authenticated client");
						return new BitfinexApiBroker(apiKey, apiSecret);
					}
				}
			}
		} catch(Exception e) {
			logger.error("Unable to load properties", e);
		}
		
		// Unauthenticated client
		logger.info("Building unauthenticated client");
		return new BitfinexApiBroker();
	}
	
}
