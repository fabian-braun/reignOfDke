package reignierOfDKE;

public enum MapSize {
	SMALL, MEDIUM, LARGE;
	public static final int MAP_SIZE_SMALL_THRESHOLD = 40;
	public static final int MAP_SIZE_MEDIUM_THRESHOLD = 60;
	public static final int MAP_SIZE_NO_SQUARE_MEDIUM_THRESHOLD = 50;

	public static MapSize get(int ySize, int xSize) {
		if (ySize < MAP_SIZE_SMALL_THRESHOLD
				&& xSize < MAP_SIZE_SMALL_THRESHOLD) {
			return MapSize.SMALL;
		}
		if (ySize < MAP_SIZE_MEDIUM_THRESHOLD
				&& xSize < MAP_SIZE_MEDIUM_THRESHOLD) {
			return MapSize.MEDIUM;
		}
		if (ySize > MAP_SIZE_MEDIUM_THRESHOLD
				&& xSize < MAP_SIZE_MEDIUM_THRESHOLD) {
			return MapSize.LARGE;
		}
		int largestDimension = Math.max(ySize, xSize);
		if (largestDimension < MAP_SIZE_NO_SQUARE_MEDIUM_THRESHOLD) {
			return MapSize.MEDIUM;
		} else {
			return MapSize.LARGE;
		}
	}

}