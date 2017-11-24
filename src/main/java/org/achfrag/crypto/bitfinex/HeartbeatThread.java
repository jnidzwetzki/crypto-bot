package org.achfrag.crypto.bitfinex;

import java.util.concurrent.TimeUnit;

import org.achfrag.crypto.bitfinex.commands.PingCommand;

class HeartbeatThread implements Runnable {

	/**
	 * The API timeout
	 */
	private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);
	
	/**
	 * The API broker
	 */
	private final BitfinexApiBroker bitfinexApiBroker;

	/**
	 * @param bitfinexApiBroker
	 */
	HeartbeatThread(BitfinexApiBroker bitfinexApiBroker) {
		this.bitfinexApiBroker = bitfinexApiBroker;
	}

	@Override
	public void run() {
		try {
			while(! Thread.interrupted()) {
				if(bitfinexApiBroker.websocketEndpoint != null) {
					bitfinexApiBroker.sendCommand(new PingCommand());
					
					final long heartbeatTimeout = bitfinexApiBroker.lastHeatbeat + TIMEOUT;
					
					if(heartbeatTimeout < System.currentTimeMillis()) {
						BitfinexApiBroker.logger.error("Heartbeat timeout reconnecting");
						
						// Disable auto reconnect to ignore session closed 
						// events, and preventing duplicate reconnects
						bitfinexApiBroker.setAutoReconnectEnabled(false);
						bitfinexApiBroker.reconnect();
						bitfinexApiBroker.setAutoReconnectEnabled(true);
					}
					
				}
				
				Thread.sleep(3000);
			}
		} catch(Throwable e) {
			BitfinexApiBroker.logger.error("Got exception", e);
		}
	}
	
}