/*******************************************************************************
 *
 *    Copyright (C) 2015-2018 Jan Kristof Nidzwetzki
 *  
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License. 
 *    
 *******************************************************************************/
package com.github.jnidzwetzki.cryptobot.util;

import com.google.common.math.DoubleMath;

public class MathHelper {

	/**
	 * Compare two double values for almost equality
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean almostEquals(final double a, final double b) {
		return almostEquals(a, b, 1000);
	}
	
	/**
	 * Compare two double values for almost equality
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean almostEquals(final double a, final double b, final double delta) {
		if(a == b) {
			return true;
		}
		
		return DoubleMath.fuzzyEquals(a, b, a / delta);
	}
}
