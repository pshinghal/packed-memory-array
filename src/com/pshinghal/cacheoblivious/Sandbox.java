package com.pshinghal.cacheoblivious;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Sandbox {

	private static void printTime(String task, long time) {
		double sec = time / 1000000000.0;
		System.out.println(task + " took " + sec + " seconds");
	}

	private static void testPMA() {	
		PackedMemoryArray pma = new PackedMemoryArray();
		int n = 10000000;
		int max = 100000;
		Random random = new Random();
		long startTime = System.nanoTime();
		int[] arr = new int[n];
		for (int i = 0; i < n; i++) {
			arr[i] = random.nextInt(max);
		}
		long endTime = System.nanoTime();
		printTime("filling arr", endTime - startTime);

		// int[] arr = new int[]{24, 42, 49, 23, 35, 48, 1, 43, 97, 35};

		// System.out.println(Arrays.toString(arr));
		// int temp;

		startTime = System.nanoTime();
		for (int i = 0; i < arr.length; i++) {
			// if (i == 7) {
			// temp = 1;
			//
			// }
			// System.out.println(arr[i]);
			pma.put(arr[i]);
			// pma.debugPrintData();
		}
		endTime = System.nanoTime();
		printTime("filling pma", endTime - startTime);

		startTime = System.nanoTime();
		Arrays.sort(arr);
		endTime = System.nanoTime();
		printTime("sorting arr", endTime - startTime);
		startTime = System.nanoTime();
		int[] pArr = pma.debugGetArray();
		endTime = System.nanoTime();
		printTime("getting pma array", endTime - startTime);
		// System.out.println(arr.length + " " + pArr.length);
		// System.out.println(Arrays.toString(arr));
		// System.out.println(Arrays.toString(pArr));

		System.out.println(Arrays.equals(arr, pArr));

	}

	private static void testBST() {
		BST bst = new BST();
		Random rand = new Random();
		for (int i = 0; i < 1000; i++) {
			int temp = rand.nextInt(1000);
			bst.insertNum(temp);
		}
		int[] arr = bst.getArray();
		System.out.println(arr.length);
		System.out.println(Arrays.toString(arr));
	}
	
	private static void testAll() {
		int n = 10000000;
		int max = 1000000000;
		Random random = new Random();
		long startTime = System.nanoTime();
		int[] arr = new int[n];
		for (int i = 0; i < n; i++) {
			arr[i] = random.nextInt(max);
		}
		long endTime = System.nanoTime();
		printTime("filling arr", endTime - startTime);

		startTime = System.nanoTime();
		Arrays.sort(arr);
		endTime = System.nanoTime();
		printTime("sorting arr", endTime - startTime);
		
		PackedMemoryArray pma = new PackedMemoryArray();
		startTime = System.nanoTime();
		for (int i = 0; i < arr.length; i++) {
			pma.put(arr[i]);
		}
		endTime = System.nanoTime();
		printTime("filling pma", endTime - startTime);

		BST bst = new BST();
		startTime = System.nanoTime();
		for (int i = 0; i < arr.length; i++) {
			bst.insertNum(arr[i]);
		}
		endTime = System.nanoTime();
		printTime("filling bst", endTime - startTime);

		startTime = System.nanoTime();
		int[] pArr = pma.debugGetArray();
		endTime = System.nanoTime();
		printTime("getting pma array", endTime - startTime);

		startTime = System.nanoTime();
		int[] bArr = bst.getArray();
		endTime = System.nanoTime();
		printTime("getting bst array", endTime - startTime);

		System.out.println(Arrays.equals(arr, pArr));
		System.out.println(Arrays.equals(arr, bArr));
	}
	
	public static void main(String[] args) {
		testAll();
	}

}

// results converge when max is around 100,000
// greater max is better since there are fewer "collisions"
// tweaking some code should make this "collisions" issue go away

// running log (n = 10000000, max = 100000)
// filling arr took 0.154126771 seconds
// filling pma took 8.257368654 seconds
// sorting arr took 0.66434517 seconds
// getting pma array took 0.024516325 seconds
// true

// running log (n = 1000000, max = 100000)
// filling arr took 0.021628582 seconds
// filling pma took 0.466010765 seconds
// sorting arr took 0.11055703 seconds
// getting pma array took 0.008711783 seconds
// true

// running log (n = 100000, max = 100000)
// filling arr took 0.004438997 seconds
// filling pma took 0.06501903 seconds
// sorting arr took 0.023218251 seconds
// getting pma array took 0.007709499 seconds
// true

//running log (n = 100000000, max = 100000)
//filling arr took 1.264003651 seconds
//filling pma took 160.300860099 seconds
//sorting arr took 6.412388199 seconds
//getting pma array took 0.288785399 seconds
//true

//running log (n = 10000000, max = 100000)
//filling arr took 0.136819772 seconds
//filling pma took 8.96153766 seconds
//filling bst took 5.836467044 seconds
//sorting arr took 0.670163528 seconds
//getting pma array took 0.026400048 seconds
//getting bst array took 0.074393268 seconds

//running log (n = 10000000, max = 1000000000)
//filling arr took 0.171608963 seconds
//filling pma took 9.746449105 seconds
//filling bst took 22.428170266 seconds
//sorting arr took 0.965309112 seconds
//getting pma array took 0.049494981 seconds
//getting bst array took 0.501947374 seconds