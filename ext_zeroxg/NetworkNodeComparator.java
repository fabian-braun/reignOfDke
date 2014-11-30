package ext_zeroxg;

import java.util.Comparator;

public class NetworkNodeComparator implements Comparator<NetworkNode> {
	@Override
	public int compare(NetworkNode a, NetworkNode b) {
		return (a.g + a.h) - (b.g + b.h);
	}
}