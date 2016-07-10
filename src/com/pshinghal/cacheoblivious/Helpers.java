package com.pshinghal.cacheoblivious;

public class Helpers {
	/**
	 * Returns the smallest power of 2 that is greater than or equal to
	 * {@code num}
	 * 
	 * @see http://graphics.stanford.edu/~seander/bithacks.html#RoundUpPowerOf2
	 * @param num
	 * @return
	 */
	public static int binCeil(int num) {
		// TODO: figure out limits (won't work on INT_MAX?) and add exceptions
		num--;
		num |= num >> 1;
		num |= num >> 2;
		num |= num >> 4;
		num |= num >> 8;
		num |= num >> 16;
		num++;
		return num;
	}

	/**
	 * Returns the ceiling of the base 2 logarithm of {@code num}
	 * 
	 * @param num
	 * @return ceil(log_2(num))
	 */
	public static int log2(int num) {
		return 32 - Integer.numberOfLeadingZeros(num - 1);
	}
}
