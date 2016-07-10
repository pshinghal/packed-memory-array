package com.pshinghal.cacheoblivious;

import java.util.Iterator;

public class PackedMemoryArray implements OrderedNumericCollection {
	public class EmptySegmentException extends Exception {
		private static final long serialVersionUID = 7480411482270288173L;
	}

	// Each segment MUST have an element in it, otherwise there are all kinds of
	// problems

	// If this was 4, we would have empty segments. Figure out the implicit
	// relationship between the base capacity and the density thresholds
	private static final int BASE_CAPACITY = 2;
	// This is probably involved in that relationship, too
	private static final int RESIZE_SCALING_FACTOR = 2;

	private static final double ROOT_DENSITY_THRESHOLD = 0.75;
	private static final double DENSITY_THRESHOLD_MULTIPLIER = 0.25;
	// Densities are calculated as (RDT + (DTM * (depth/height)))
	// Root depth = 0, leaf depth = height

	private int size;
	private int[] store;
	private boolean[] containsData;
	private int height;
	private int segmentSize;

	@Override
	public void put(int num) {
		// Should do one of 3 things:
		// 1. insert first element into empty store
		// 2. insert element into non-full leaf (with minimum shift)
		// 3. insert element into "node" where (size + 1) is within threshold
		// 4. if root is out of threshold, resize and copy with balancing
		try {
			if (store == null) {
				insertFirstNumber(num);
//				System.out.println("first");
				return;
			}
			// System.out.println("getting insertion segment");
			int insertionSegment = getInsertionSegment(num);
			// System.out.println("inserting in segment " + insertionSegment);
			boolean inserted = insertInSegment(insertionSegment, num);
			if (inserted) {
//				System.out.println("in-segment at " + insertionSegment);
				return;
			}
			// segment is full, find the right node to insert at
			// System.out.println("finding valid node");
			int validNodeDepth = findNodeDepthWithinThreshold(insertionSegment);
			// System.out.println("found valid node");
			if (validNodeDepth < 0) {
				// System.out.println("resizing");
				resizeAndInsert(num);
//				System.out.println("resize");
			} else {
				// System.out.println("rebalancing depth " + validNodeDepth);
				rebalanceAndInsert(insertionSegment, validNodeDepth, num);
//				System.out.println("rebalance at " + validNodeDepth);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void resizeAndInsert(int numToInsert) {
		int newCap = capacity() * RESIZE_SCALING_FACTOR;
		int[] tempStore = new int[newCap];
		boolean[] tempContainsData = new boolean[newCap];

		boolean inserted = false;
		int insertionIndex = 0;
		for (int i = 0; i < capacity(); i++) {
			if (containsData[i]) {
				if (store[i] > numToInsert && !inserted) {
					tempStore[insertionIndex] = numToInsert;
					inserted = true;
					insertionIndex++;
				}
				tempStore[insertionIndex] = store[i];
				insertionIndex++;
			}
		}
		if (!inserted) {
			tempStore[insertionIndex] = numToInsert;
			inserted = true;
		}

		// assert insertionIndex == size
		size++;

		segmentSize = Helpers.binCeil(Helpers.log2(newCap));
		int numSegments = newCap / segmentSize;
		// assert numSegments is a power of 2
		height = Helpers.log2(numSegments);

		containsData = tempContainsData;
		store = tempStore;
		redistributeFreeSlots(0, numSegments - 1, size);
		redistributeLeftSquashedElements(0, numSegments - 1, size);
	}

	// Assumes current segment is full
	private int findNodeDepthWithinThreshold(int segmentNumber) {
		if (height <= 0) {
			return -1;
		}

		int depth = height;
		int firstSegment = segmentNumber;
		int numSegments = 1;
		int firstCountedSegment = segmentNumber;
		int lastCountedSegment = segmentNumber;
		int count = segmentSize;
		int thresholdSize = (int) Math
				.round(Math
						.floor(numSegments
								* segmentSize
								* (ROOT_DENSITY_THRESHOLD + (DENSITY_THRESHOLD_MULTIPLIER * ((double) depth / height)))));

		while (count + 1 > thresholdSize && depth > 0) {
			depth--;
			int level = height - depth;
			firstSegment >>= level;
			firstSegment <<= level;
			numSegments *= 2;
			for (int i = firstSegment; i < firstCountedSegment; i++) {
				count += segmentOccupancy(i);
			}
			firstCountedSegment = firstSegment;
			for (int i = lastCountedSegment + 1; i < firstSegment + numSegments; i++) {
				count += segmentOccupancy(i);
			}
			lastCountedSegment = firstSegment + numSegments - 1;
			thresholdSize = (int) Math
					.round(Math
							.floor(numSegments
									* segmentSize
									* (ROOT_DENSITY_THRESHOLD + (DENSITY_THRESHOLD_MULTIPLIER * ((double) depth / height)))));
		}

		if (count + 1 <= thresholdSize) {
			return depth;
		} else {
			// if root is out of threshold
			return -1;
		}
	}

	private int segmentOccupancy(int segmentNumber) {
		int count = 0;
		for (int i = segmentStart(segmentNumber); i < segmentEnd(segmentNumber); i++) {
			if (containsData[i]) {
				count++;
			}
		}
		return count;
	}

	private boolean insertInSegment(int insertionSegment, int num) {
		int startIndex = segmentStart(insertionSegment);
		int endIndex = segmentEnd(insertionSegment);

		int lowerFreeSpot = -1;

		int index = startIndex;

		// Capacity check is not required since it's "absorbed" by endIndex
		// check (as long as store.length is always divisible by segmentSize)
		while (index < endIndex // && index < capacity()
				&& (!containsData[index] || store[index] <= num)) {
			if (!containsData[index]) {
				lowerFreeSpot = index;
			}
			index++;
		}

		if (lowerFreeSpot != -1) {
			shiftLeft(lowerFreeSpot, index);
			store[index - 1] = num;
			containsData[index - 1] = true;
			size++;
			return true;
		}

		if (index == endIndex) {
			// scanned till the end of segment, no empty space.
			// also, everything here is smaller (probably not relevant)
			return false;
		}

		int insertionPoint = index;
		while (index < endIndex && containsData[index]) {
			index++;
		}

		if (index == endIndex) {
			// scanned till the end of segment, no empty space.
			return false;
		}

		// index is at an empty slot
		shiftRight(insertionPoint, index + 1);
		store[insertionPoint] = num;
		containsData[insertionPoint] = true;
		size++;
		return true;
	}

	/**
	 * Shifts elements in [low,high-2] to [low+1,high-1], assuming the entire
	 * source range contains data
	 * 
	 * @param low
	 * @param high
	 */
	private void shiftRight(int low, int high) {
		// assert high - low >= 2
		containsData[low] = false;
		containsData[high - 1] = true;
		for (int i = high - 1; i > low; i--) {
			store[i] = store[i - 1];
		}
	}

	/**
	 * Shifts elements in [low+1,high-1] to [low,high-2], assuming the entire
	 * source range contains data (or is of length 0)
	 * 
	 * @param low
	 * @param high
	 */
	private void shiftLeft(int low, int high) {
		if (high - low == 1) {
			return;
		}
		containsData[low] = true;
		containsData[high - 1] = false;
		for (int i = low; i < high - 1; i++) {
			store[i] = store[i + 1];
		}
	}

	private int getInsertionSegment(int num) throws EmptySegmentException {
		if (store == null) {
			// System.out.println("null return -1");
			return -1;
		}
		if (numSegments() == 1) {
			// System.out.println("one-segment return 0");
			return 0;
		}

		int low = 0;
		int high = numSegments() - 1;
		if (segmentIsGreaterThan(low, num)) {
			// System.out.println("low return 0");
			return 0;
		}
		if (!segmentIsGreaterThan(high, num)) {
			return high;
		}

		while (high - low > 1) {
			int mid = low + ((high - low) / 2);
			if (segmentIsGreaterThan(mid, num)) {
				high = mid;
			} else {
				low = mid;
			}
		}

		return low;
	}

	// Works only if store.length is always divisible by segmentSize
	// TODO Set this up as a resize-updated variable instead?
	private int numSegments() {
		if (capacity() == 0) {
			return 0;
		}
		return capacity() / segmentSize;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public Iterator<Integer> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	public void debugPrintData() {
		int lastNumber = -1;
		boolean isBad = false;
		for (int i = 0; i < capacity(); i++) {
			if (containsData[i]) {
				if (store[i] < lastNumber) {
					isBad = true;
				}
				lastNumber = store[i];
				System.out.print(store[i] + " ");
			} else {
				System.out.print("- ");
			}
		}
		System.out.println();
		if (isBad) {
			System.out.println("THAT'S A BAD ONE!!!");
		}
	}

	public int[] debugGetArray() {
		int[] arr = new int[size];
		int idx = 0;

		for (int i = 0; i < capacity(); i++) {
			if (containsData[i]) {
				arr[idx] = store[i];
				idx++;
			}
		}

		return arr;
	}

	public PackedMemoryArray() {
		store = null;
		containsData = null;
		size = 0;
		segmentSize = 0;
		height = 1;
	}

	private void insertFirstNumber(int num) {
		height = 0;
		segmentSize = BASE_CAPACITY;
		size++;
		store = new int[BASE_CAPACITY];
		containsData = new boolean[BASE_CAPACITY];
		store[0] = num;
		containsData[0] = true;
	}

	/**
	 * @param segmentNumber
	 * @param num
	 * @return true if the leftmost number in the segment is greater than
	 *         {@code num}
	 * @throws EmptySegmentException
	 */
	private boolean segmentIsGreaterThan(int segmentNumber, int num)
			throws EmptySegmentException {
		int startIndex = segmentStart(segmentNumber);
		int endIndex = segmentEnd(segmentNumber);
		for (int i = startIndex; i < endIndex; i++) {
			if (!containsData[i]) {
				continue;
			}
			if (store[i] > num) {
				return true;
			} else {
				return false;
			}
		}
		throw new EmptySegmentException();
	}

	private int capacity() {
		if (store == null) {
			return 0;
		}
		return store.length;
	}

	/**
	 * @return storage index corresponding to the first element in the given
	 *         segment
	 */
	private int segmentStart(int segmentNumber) {
		return segmentSize * segmentNumber;
	}

	/**
	 * @return storage index corresponding to the element immediately after the
	 *         last element in the given segment
	 */
	private int segmentEnd(int segmentNumber) {
		return segmentSize * (segmentNumber + 1);
	}

	private void rebalanceAndInsert(int insertionSegment, int nodeDepth,
			int numToInsert) {
		int level = height - nodeDepth;
		int firstSegment = (insertionSegment >> level) << level;
		int numSegments = 1 << level;
		int lastSegment = firstSegment + numSegments - 1;

		int numElements = squashToLeft(firstSegment, lastSegment) + 1;
		redistributeFreeSlots(firstSegment, lastSegment, numElements);
		redistributeLeftSquashedElementsAndInsert(firstSegment, lastSegment,
				numElements - 1, numToInsert);
		size++;
	}

	private void redistributeLeftSquashedElementsAndInsert(int firstSegment,
			int lastSegment, int numLeftSquashedElements, int numToInsert) {
		int startIndex = segmentStart(firstSegment);
		int endIndex = segmentEnd(lastSegment);
		int contentIndex = startIndex + numLeftSquashedElements - 1;
		boolean inserted = false;

		for (int i = endIndex - 1; i >= startIndex; i--) {
			if (containsData[i]) {
				if (contentIndex >= startIndex
						&& (store[contentIndex] > numToInsert || inserted)) {
					store[i] = store[contentIndex];
					contentIndex--;
				} else {
					// assert inserted == false
					store[i] = numToInsert;
					inserted = true;
				}
			}
		}
	}

	private void rebalanceSegment(int segmentNumber) {
		rebalanceSegments(segmentNumber, segmentNumber);
	}

	/**
	 * two-pass rebalancing method
	 * 
	 * @param firstSegment
	 *            segment number to start rebalancing at (inclusive)
	 * @param lastSegment
	 *            segment number to stop rebalancing at (inclusive)
	 */
	private void rebalanceSegments(int firstSegment, int lastSegment) {
		int num = squashToLeft(firstSegment, lastSegment);
		redistributeFreeSlots(firstSegment, lastSegment, num);
		redistributeLeftSquashedElements(firstSegment, lastSegment, num);
	}

	private void redistributeLeftSquashedElements(int firstSegment,
			int lastSegment, int numElements) {
		int startIndex = segmentStart(firstSegment);
		int endIndex = segmentEnd(lastSegment);
		int contentIndex = startIndex + numElements - 1;
		for (int i = endIndex - 1; i >= startIndex; i--) {
			if (containsData[i]) {
				store[i] = store[contentIndex];
				contentIndex--;
			}
		}
	}

	/**
	 * move all elements in the given segments to a single contiguous block on
	 * the left side. NOTE: THE SLOTS AFTER THE CONTIGUOUS BLOCK MAY STILL
	 * CONTAIN THE ORIGINAL DATA. This method relies on the return value to
	 * bound the contiguous block
	 * 
	 * @param firstSegment
	 * @param lastSegment
	 * @return the number of elements that form the contiguous block
	 */
	private int squashToLeft(int firstSegment, int lastSegment) {
		int startIndex = segmentStart(firstSegment);
		int endIndex = segmentEnd(lastSegment);
		int numElements = 0;
		int insertionIndex = startIndex;

		for (int i = startIndex; i < endIndex; i++) {
			if (containsData[i]) {
				store[insertionIndex] = store[i];
				insertionIndex++;
				numElements++;
			}
		}

		return numElements;
	}

	/**
	 * evenly distribute occupied slots across given segments based on
	 * {@code http://stackoverflow.com/questions/23161009/how-to-evenly-distribute-array-members}
	 * 
	 * @param firstSegment
	 * @param lastSegment
	 * @param numOccupied
	 */
	private void redistributeFreeSlots(int firstSegment, int lastSegment,
			int numOccupied) {
		int startIndex = segmentStart(firstSegment);
		int endIndex = segmentEnd(lastSegment);
		double occupiedFrequency = (double) numOccupied
				/ (endIndex - startIndex);
		double expectedOccupied = 0;
		int allocatedOccupied = 0;
		for (int i = startIndex; i < endIndex; i++) {
			expectedOccupied += occupiedFrequency;
			int toAllocate = (int) Math.round(expectedOccupied
					- allocatedOccupied);
			// assert toAllocate is 0 or 1;
			allocatedOccupied += toAllocate;
			containsData[i] = toAllocate > 0;
		}
	}

	;

	;

	;

	;

	;

	;

	;

}