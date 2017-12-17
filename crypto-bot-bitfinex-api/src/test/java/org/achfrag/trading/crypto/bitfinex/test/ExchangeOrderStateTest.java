package org.achfrag.trading.crypto.bitfinex.test;

import org.achfrag.trading.crypto.bitfinex.entity.ExchangeOrderState;
import org.junit.Assert;
import org.junit.Test;

public class ExchangeOrderStateTest {

	@Test
	public void testStateFromString() {
		Assert.assertEquals(ExchangeOrderState.STATE_ACTIVE, ExchangeOrderState.fromString("ACTIVE"));
		Assert.assertEquals(ExchangeOrderState.STATE_EXECUTED, ExchangeOrderState.fromString("EXECUTED @ 18867.0(-0.01)"));
		Assert.assertEquals(ExchangeOrderState.STATE_CANCELED, ExchangeOrderState.fromString("CANCELED"));
		Assert.assertEquals(ExchangeOrderState.STATE_PARTIALLY_FILLED, ExchangeOrderState.fromString("PARTIALLY FILLED"));
		Assert.assertEquals(ExchangeOrderState.STATE_POSTONLY_CANCELED, ExchangeOrderState.fromString("POSTONLY CANCELED"));		
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testStateFromStringInvalid() {
		ExchangeOrderState.fromString("ABC");
	}
}
