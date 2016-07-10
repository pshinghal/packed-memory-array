package com.pshinghal.cacheoblivious;

// TODO: extend Collection
public interface OrderedNumericCollection extends Iterable<Integer> {
	void put(int num);

	int size();
}
