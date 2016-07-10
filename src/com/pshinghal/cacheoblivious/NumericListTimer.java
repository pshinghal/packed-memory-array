package com.pshinghal.cacheoblivious;

import java.util.Iterator;
import java.util.Random;

public class NumericListTimer {
	private static Random random = new Random();

	private static void printTime(String task, long time) {
		double sec = time / 1000000000.0;
		System.out.println(task + " took " + sec + " seconds");
	}

	// TODO: When ONC extends Collection, use addAll()
	private static void fillCollection(OrderedNumericCollection collection,
			int numberOfItems) {
		if (collection == null) {
			return;
		}

		// TODO: Try separating random number generation (add up only access
		// times).
		long startTime = System.nanoTime();
		for (int i = 0; i < numberOfItems; i++) {
			collection.put(random.nextInt());
		}
		long endTime = System.nanoTime();

		printTime("fill(" + numberOfItems + ") ", endTime - startTime);
	}

	// private static void getRandomAccessTime(
	// OrderedNumericCollection collection, int numberOfAccesses) {
	// int collectionSize = collection.size();
	//
	// // TODO: Try separating random number generation (add up only access
	// // times).
	// long startTime = System.nanoTime();
	// for (int i = 0; i < numberOfAccesses; i++) {
	// collection.get(random.nextInt(collectionSize));
	// }
	// long endTime = System.nanoTime();
	//
	// printTime("randomAccess(" + numberOfAccesses + ")", endTime - startTime);
	// };

	private static void getSequentialAccessTime(
			OrderedNumericCollection collection) {
		int collectionSize = collection.size();

		Iterator<Integer> it = collection.iterator();
		long startTime = System.nanoTime();
		while (it.hasNext()) {
			it.next();
		}
		long endTime = System.nanoTime();

		printTime("sequentialAccess(" + collectionSize + ")", endTime
				- startTime);
	}

	public static void main(String[] args) {
		OrderedNumericCollection collection = null;
		fillCollection(collection, 10000000);
		// getRandomAccessTime(collection, 10000);
		getSequentialAccessTime(collection);
	}
}
