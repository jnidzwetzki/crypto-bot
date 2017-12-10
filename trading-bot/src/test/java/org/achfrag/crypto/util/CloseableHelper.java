/*******************************************************************************
 *
 *    Copyright (C) 2015-2017 the BBoxDB project
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
package org.achfrag.crypto.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;

public class CloseableHelper {

	/**
	 * A consumer that prints the exception on stderr
	 */
	public static final Consumer<Exception> PRINT_EXCEPTION_ON_STDERR = 
				(e) -> System.err.println("Exception while closing: " + e);
	
	/**
	 * Close the closeable without throwing an exception or logging
	 * @param closeable
	 */
	public static void closeWithoutException(final Closeable closeable) {
		closeWithoutException(closeable, null);
	}
	
	/**
	 * Close the closeable without throwing an exception or logging
	 * @param closeable
	 */
	public static void closeWithoutException(final Closeable closeable, final Consumer<Exception> consumer) {
		
		/**
		 * Wrap the Closeable interface into a AutoCloseable interface and resuse the
		 * AutoCloseable close logic
		 */
		final AutoCloseable closeableWrapper = new AutoCloseable() {	
			@Override
			public void close() throws IOException {
				closeable.close();
			}
		};
		
		closeWithoutException(closeableWrapper, consumer);
	}
	
	/**
	 * Close the closeable without throwing an exception or logging
	 * @param closeable
	 */
	public static void closeWithoutException(final AutoCloseable closeable) {
		closeWithoutException(closeable, null);
	}
	
	/**
	 * Close the closeable without throwing an exception or logging
	 * @param closeable
	 */
	public static void closeWithoutException(final AutoCloseable closeable, final Consumer<Exception> consumer) {
		if(closeable == null) {
			return;
		}
		
		try {
			closeable.close();
		} catch (Exception e) {
			if(consumer != null) {
				consumer.accept(e);
			}
		}
	}

}
