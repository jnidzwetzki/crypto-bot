/*******************************************************************************
 *
 *    Copyright (C) 2015-2018 Jan Kristof Nidzwetzki
 *  
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License. 
 *    
 *******************************************************************************/
package com.github.jnidzwetzki.cryptobot.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.bboxdb.commons.MathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jnidzwetzki.bitfinex.v2.BitfinexApiBroker;

public class BitfinexClientFactory {
	
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(BitfinexClientFactory.class);

	/**
	 * Build a new bitfinex client
	 * @return
	 */
	public static List<BitfinexApiBroker> buildBifinexClient() {
		final Properties prop = new Properties();
		final List<BitfinexApiBroker> resultList = new ArrayList<>();
		
		try {
			final InputStream input = BitfinexClientFactory.class.getClassLoader().getResourceAsStream("auth.properties");
			
			if(input != null) {
				prop.load(input);
				final String apiConnectionsString = prop.getProperty("apiConnections");
				final int apiConnections = MathUtil.tryParseInt(apiConnectionsString, 
						() -> "Invalid integer value: " + apiConnectionsString);
						
				for(int i = 0; i < apiConnections; i++) {
					final String apiKey = prop.getProperty("apiKey." + Integer.toString(i));
					final String apiSecret = prop.getProperty("apiSecret." + Integer.toString(i));
					
					if(apiKey == null || apiSecret == null) {
						logger.warn("API key or secret are null ({})", i);
					} else {
						logger.info("Building authenticated client");
						final BitfinexApiBroker apiBroker = new BitfinexApiBroker(apiKey, apiSecret);
						resultList.add(apiBroker);
					}
				}
				
				return resultList;
			}
			
		} catch(Exception e) {
			logger.error("Unable to load properties", e);
		}
		
		logger.error("Properties not found");
		
		return resultList;
	}
	
}
