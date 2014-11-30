package ext_animorphs;

/**
 * Really shitty set: array of 65536
 */
public class FastSmallIntSet implements IntSet {
	private static final int HASH = 65536;
	public int size = 0;
	public boolean[] has = new boolean[HASH];

	public void add(int i) {
		int modded = i % HASH;
		if (!has[modded]) {
			size++;
			has[modded] = true;
		}
	}

	public void remove(int i) {
		int modded = i % HASH;
		if (has[modded]) {
			size--;
			has[modded] = false;
		}
	}

	public boolean contains(int i) {
		return has[i % HASH];
	}

	public void clear() {
		has = new boolean[HASH];
		size = 0;
	}
}
