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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jfree.chart.axis.Tick;

public class SupportResistanceDetector {
	
	// +/- 0.5%
	private final static double DIFF = 0.005;

	public static Map<Double, Integer> getResistanceLevels(final List<Tick> ticks) {
		final Map<Double, Integer> result = new HashMap<>();
		
		
		
		return result;
	}
	
	public static Map<Double, Integer> getSupportLevels(final List<Tick> ticks) {
		final Map<Double, Integer> result = new HashMap<>();
		
		return result;
	}
	
}
