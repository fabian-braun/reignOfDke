package teamKingOfTasks;

import java.util.Comparator;

public class TaskComparator implements Comparator<Task> {

	public int compare(Task t1, Task t2) {
		return t1.getPriority() - t2.getPriority();
	}

}
