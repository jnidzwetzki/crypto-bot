package net.achfrag.crypto.util;

import com.google.common.math.DoubleMath;

public class MathHelper {

	/**
	 * Compare two double values for almost equality
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean almostEquals(final double a, final double b) {
		if(a == b) {
			return true;
		}
		
		return DoubleMath.fuzzyEquals(a, b, a / 1000.0);
	}
}
