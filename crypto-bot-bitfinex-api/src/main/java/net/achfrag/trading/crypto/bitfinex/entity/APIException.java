package net.achfrag.trading.crypto.bitfinex.entity;

public class APIException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6230854947150239935L;

	public APIException() {
	}

	public APIException(final String message) {
		super(message);
	}

	public APIException(final Throwable cause) {
		super(cause);
	}

	public APIException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public APIException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
