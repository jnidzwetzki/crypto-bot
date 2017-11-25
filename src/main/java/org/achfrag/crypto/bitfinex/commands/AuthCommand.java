package org.achfrag.crypto.bitfinex.commands;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.achfrag.crypto.bitfinex.BitfinexApiBroker;
import org.json.JSONObject;

import com.google.common.io.BaseEncoding;

public class AuthCommand extends AbstractAPICommand {

	private static final String HMAC_SHA1_ALGORITHM = "HmacSHA384";

	@Override
	public String getCommand(final BitfinexApiBroker bitfinexApiBroker) throws CommandException {
		try {
			final String APIKey = bitfinexApiBroker.getApiKey();
			final String APISecret = bitfinexApiBroker.getApiSecret();
			
			final String authNonce = Long.toString(System.currentTimeMillis());
			final String authPayload = "AUTH" + authNonce;
			
			final SecretKeySpec signingKey = new SecretKeySpec(authPayload.getBytes(), HMAC_SHA1_ALGORITHM);
			final Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
			mac.init(signingKey);
			
			final String authSig = BaseEncoding.base32Hex().encode(mac.doFinal(APISecret.getBytes()));
			
			final JSONObject subscribeJson = new JSONObject();
			subscribeJson.put("event", "auth");
			subscribeJson.put("apiKey", APIKey);
			subscribeJson.put("authSig", authSig);
			subscribeJson.put("authPayload", authPayload);
			subscribeJson.put("authNonce", authNonce);

			return subscribeJson.toString();
		} catch (Exception e) {
			throw new CommandException(e);
		} 
	}

}
