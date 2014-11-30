package ext_animorphs;

/**
 * Really shittiest set: array of 65536, no size
 */
public class FastestIntSet implements IntSet {
	private static final int HASH = 65536;
	public boolean[] has = new boolean[HASH];

	public void add(int i) {
		has[i % HASH] = true;

	}

	public void remove(int i) {
		has[i % HASH] = false;
	}

	public boolean contains(int i) {
		return has[i % HASH];
	}

}
