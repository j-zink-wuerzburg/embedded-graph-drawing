package de.uniwue.informatik.util;

public class ExtendedEuclideanAlgorithm {
	
	/**
	 * Treats negative numbers as if they were positive numbers
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static int gcd(int a, int b) {
		int r = Math.abs(b);
		int oldR = Math.abs(a);
		while (r != 0) {
			int quotient = oldR / r;
			int storeR = r;
			r = oldR - quotient * r;
			oldR = storeR;
		}
		return oldR;
	}
}
