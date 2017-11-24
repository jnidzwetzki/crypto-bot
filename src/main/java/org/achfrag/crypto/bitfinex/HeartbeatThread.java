package org.achfrag.crypto.bitfinex;

import java.util.concurrent.TimeUnit;

import org.achfrag.crypto.bitfinex.commands.PingCommand;

class HeartbeatThread implements Runnable {

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
					
					final long heartbeatTimeout = bitfinexApiBroker.lastHeatbeat + TimeUnit.SECONDS.toMillis(15);
					
					if(heartbeatTimeout < System.currentTimeMillis()) {
						BitfinexApiBroker.logger.error("Heartbeat timeout reconnecting");
						
						// Disable auto reconnect to ignose session closed 
						// evenents, and preventing duplicate reconnects
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