package net.achfrag.trading.crypto.bitfinex.callback.api;

import java.util.concurrent.CountDownLatch;

import org.json.JSONArray;

import com.google.common.collect.Table;

import net.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import net.achfrag.trading.crypto.bitfinex.entity.APIException;
import net.achfrag.trading.crypto.bitfinex.entity.Wallet;

public class WalletHandler implements APICallbackHandler {

	@Override
	public void handleChannelData(final BitfinexApiBroker bitfinexApiBroker, final JSONArray jsonArray) 
			throws APIException {
		
		final JSONArray wallets = jsonArray.getJSONArray(2);
		
		// Snapshot or update
		if(! (wallets.get(0) instanceof JSONArray)) {
			handleWalletcallback(bitfinexApiBroker, wallets);
		} else {
			for(int walletPos = 0; walletPos < wallets.length(); walletPos++) {
				final JSONArray walletArray = wallets.getJSONArray(walletPos);
				handleWalletcallback(bitfinexApiBroker, walletArray);
			}
		}
		
		notifyLatch(bitfinexApiBroker);
	}

	/**
	 * Notify the wallet latch
	 * @param bitfinexApiBroker
	 */
	private void notifyLatch(final BitfinexApiBroker bitfinexApiBroker) {
		
		final CountDownLatch walletSnapshotLatch = bitfinexApiBroker.getWalletSnapshotLatch();
		
		if(walletSnapshotLatch != null) {
			walletSnapshotLatch.countDown();
		}
	}

	/**
	 * Handle the callback for a single wallet
	 * @param bitfinexApiBroker 
	 * @param walletArray
	 * @throws APIException 
	 */
	private void handleWalletcallback(final BitfinexApiBroker bitfinexApiBroker, final JSONArray walletArray) throws APIException {
		final String walletType = walletArray.getString(0);
		final String currency = walletArray.getString(1);
		final double balance = walletArray.getDouble(2);
		final float unsettledInterest = walletArray.getFloat(3);
		final float balanceAvailable = walletArray.optFloat(4, -1);
		
		final Wallet wallet = new Wallet(walletType, currency, balance, unsettledInterest, balanceAvailable);

		final Table<String, String, Wallet> walletTable = bitfinexApiBroker.getWalletTable();
		
		synchronized (walletTable) {
			walletTable.put(walletType, currency, wallet);
			walletTable.notifyAll();
		}
	}

}
