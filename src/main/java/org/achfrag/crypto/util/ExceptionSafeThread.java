package org.achfrag.crypto.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ExceptionSafeThread implements Runnable {
	
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(ExceptionSafeThread.class);

	/**
	 * Run method that catches throwables
	 */
	@Override
	public void run() {
		try {
			// Begin hook
			beginHook();
			
			// Do the real work
			runThread();
			
			// End hook
			endHook();
		} catch(Throwable e) {
			logger.error("Got exception during thread execution", e);
			afterExceptionHook();
		}
	}

	/**
	 * The after exception hook. Will be called after an exception.
	 */
	protected void afterExceptionHook() {
		// Default: Do nothing
	}
	
	/**
	 * The begin hook
	 */
	protected void beginHook() {
		// Default: Do nothing
	}
	
	/**
	 * The end hook
	 */
	protected void endHook() {
		// Default: Do nothing
	}
	
	/**
	 * The real run method
	 * @throws Exception 
	 */
	protected abstract void runThread() throws Exception;

}
