package org.achfrag.trading.crypto.bitfinex.commands;

import org.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;

public abstract class AbstractAPICommand {

	public abstract String getCommand(final BitfinexApiBroker bitfinexApiBroker) throws CommandException;

}
