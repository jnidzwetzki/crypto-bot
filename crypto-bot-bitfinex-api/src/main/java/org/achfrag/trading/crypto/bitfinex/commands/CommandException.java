package org.achfrag.trading.crypto.bitfinex.commands;

public class CommandException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2320930066822999221L;

	public CommandException() {
	}

	public CommandException(final String message) {
		super(message);
	}

	public CommandException(final Throwable cause) {
		super(cause);
	}

	public CommandException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public CommandException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
