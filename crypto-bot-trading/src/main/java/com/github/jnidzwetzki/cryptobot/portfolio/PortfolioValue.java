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
package com.github.jnidzwetzki.cryptobot.portfolio;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "portfolio_value")
public class PortfolioValue {

	@Id
	@GeneratedValue
	private long id;
	
	private String apikey;
	
	private double usdValue;
	
	private long timestamp;
	
	public PortfolioValue() {
		this.timestamp = System.currentTimeMillis();
	}

	public long getId() {
		return id;
	}

	public void setId(final long id) {
		this.id = id;
	}

	public String getApikey() {
		return apikey;
	}

	public void setApikey(final String apikey) {
		this.apikey = apikey;
	}

	public double getUsdValue() {
		return usdValue;
	}

	public void setUsdValue(final double usdValue) {
		this.usdValue = usdValue;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(final long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return "PortfolioValue [id=" + id + ", apikey=" + apikey + ", usdValue=" + usdValue + ", timestamp=" + timestamp
				+ "]";
	}


	
}
