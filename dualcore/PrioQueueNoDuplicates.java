package dualcore;

import java.util.Comparator;
import java.util.PriorityQueue;

public class PrioQueueNoDuplicates<E> extends PriorityQueue<E> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1688861377010146985L;

	public PrioQueueNoDuplicates(int i, Comparator<E> comparator) {
		super(i, comparator);
	}

	@Override
	public boolean add(E e) {
		if (contains(e)) {
			remove(e);
		}
		return super.add(e);
	}

}
