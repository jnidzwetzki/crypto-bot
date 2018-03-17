package com.github.jnidzwetzki.cryptobot;

public class CliTools {
	
	/**
	 * Clear the screen
	 */
	public static void clearScreen() {  
	    System.out.print("\033[H\033[2J");  
	    System.out.flush();  
	}  
}
