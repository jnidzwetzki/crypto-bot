package org.achfrag.crypto.bitfinex.commands;

import org.achfrag.crypto.bitfinex.BitfinexApiBroker;

public abstract class AbstractAPICommand {

	public abstract String getCommand(final BitfinexApiBroker bitfinexApiBroker) throws CommandException;

}
