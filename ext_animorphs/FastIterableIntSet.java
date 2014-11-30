package ext_animorphs;

public class FastIterableIntSet implements IntSet {
	public int size = 0;
	public StringBuilder keys = new StringBuilder();

	public void add(int i) {
		String key = String.valueOf((char) i);
		if (keys.indexOf(key) < 0) {
			keys.append(key);
			size++;
		}
	}

	public void remove(int i) {
		String key = String.valueOf((char) i);
		int index;
		if ((index = keys.indexOf(key)) >= 0) {
			keys.delete(index, index + 1);
			size--;
		}
	}

	public boolean contains(int i) {
		return keys.indexOf(String.valueOf((char) i)) >= 0;
	}

	public void clear() {
		size = 0;
		keys = new StringBuilder();
	}

	public char[] getChars() {
		char[] chars = new char[size];
		for (int i = size; --i >= 0;) {
			chars[i] = keys.charAt(i);
		}
		return chars;
	}

}
