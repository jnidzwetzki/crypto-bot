package org.achfrag.crypto.bitfinex;

import java.util.concurrent.TimeUnit;

import org.achfrag.crypto.bitfinex.commands.PingCommand;

class HeartbeatThread implements Runnable {

	/**
	 * The API timeout
	 */
	private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);
	
	
	/**
	 * The API timeout
	 */
	private static final long HEARTBEAT = TimeUnit.SECONDS.toMillis(5);
	
	
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
					
					final long nextHeartbeat = bitfinexApiBroker.getLastHeatbeat().get() + HEARTBEAT;

					if(nextHeartbeat < System.currentTimeMillis()) {
						bitfinexApiBroker.sendCommand(new PingCommand());
					}
					
					final long heartbeatTimeout = bitfinexApiBroker.getLastHeatbeat().get() + TIMEOUT;
					
					if(heartbeatTimeout < System.currentTimeMillis()) {
						BitfinexApiBroker.logger.error("Heartbeat timeout reconnecting");
						
						// Disable auto reconnect to ignore session closed 
						// events, and preventing duplicate reconnects
						bitfinexApiBroker.setAutoReconnectEnabled(false);
						final boolean reconnectState = bitfinexApiBroker.reconnect();
						bitfinexApiBroker.setAutoReconnectEnabled(true);
						
						waitIfReconnectFailed(reconnectState);
					}
					
				}
				
				Thread.sleep(3000);
			}
		} catch(Throwable e) {
			BitfinexApiBroker.logger.error("Got exception", e);
		}
	}

	/** 
	 * Wait some time before the next reconnect is executed
	 * @param reconnectState
	 */
	private void waitIfReconnectFailed(final boolean reconnectState) {
		if(! reconnectState == false) {
			// Wait some time for the reconnect
			try {
				Thread.sleep(TimeUnit.SECONDS.toMillis(10));
			} catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
			}
		}
	}
	
}