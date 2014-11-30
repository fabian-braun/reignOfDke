package ext_animorphs;

/**
 * Interface to represent ints up to 65535 (2^16-1)
 */
public interface IntSet {
	// add int to the collection
	void add(int i);

	// remove a location from the collection
	void remove(int i);

	// check for membership
	boolean contains(int i);

}
