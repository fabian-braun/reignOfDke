package teamreignofdke;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

/**
 * Class representing the behaviour of a noise tower in the
 * <code>teamreignofdke</code> Battlecode player.
 * 
 * @author Antonie
 */
public class NoiseTower extends AbstractRobotType {

	/**
	 * This field indicates whether or not the <code>NoiseTower</code> is
	 * inactive.
	 */
	private boolean inactive = false;

	/**
	 * This field represents the cow growth on the current map.
	 */
	private double[][] mapCowGrowth;

	/**
	 * This field represents the location of the <code>NoiseTower</code>.
	 */
	private MapLocation myLocation;

	/**
	 * This field represents a list of the locations around the
	 * <code>NoiseTower</code> that have the highest cow growth.
	 */
	private ArrayList<MapLocation> locations;

	/**
	 * This field represents the current map's height.
	 */
	private int mapHeight;

	/**
	 * This field represents the current map's width.
	 */
	private int mapWidth;

	/**
	 * Constructs an instance of the <code>NoiseTower</code> class.
	 * 
	 * @param rc
	 *            The <code>RobotController</code> instance supplied by the
	 *            Battlecode server.
	 */
	public NoiseTower(RobotController rc) {
		super(rc);
	}

	/**
	 * Defines the behaviour of the noise tower.
	 * 
	 * @throws GameActionException
	 */
	@Override
	protected void act() throws GameActionException {
		if (inactive || !rc.isActive()) {
			return;
		}
		if (rc.isConstructing()) {
			inactive = true;
			return;
		}

		// Get a random location from the locations with the maximum cow growth
		// around us
		MapLocation location = locations.get(randall.nextInt(locations.size()));

		// Debug
		// System.out.println(String.format("Attacking [%d,%d]", location.x,
		// location.y));

		// Attack that location
		rc.attackSquareLight(location);
	}

	/**
	 * Initialises a noise tower.
	 * 
	 * @throws GameActionException
	 */
	@Override
	protected void init() throws GameActionException {
		// Get the width and height of the current map
		mapHeight = rc.getMapHeight();
		mapWidth = rc.getMapWidth();
		// Get and remember the cow growth on the map
		mapCowGrowth = rc.senseCowGrowth();
		// Get our own location
		myLocation = rc.getLocation();

		// Debug
		// System.out.println(String.format("%d,%d", myLocation.x,
		// myLocation.y));

		// Create a hashmap, indexed by cowgrowth to store the nearby locations
		// in. Start 4 squares away from our own location to avoid shooting too
		// close to ourselves.
		HashMap<Double, List<MapLocation>> growth = new HashMap<Double, List<MapLocation>>();
		for (int i = 4; i < 13; i++) {
			for (int j = 4; j < 13; j++) {
				// Check the location to the NW (negative direction for both X
				// and Y)
				MapLocation northWestLocation = new MapLocation(myLocation.x
						- i, myLocation.y - j);
				addNearbyGrowthLocation(growth, northWestLocation);

				// Check the location to the NE (negative direction for X and
				// positive direction for Y)
				MapLocation northEastLocation = new MapLocation(myLocation.x
						- i, myLocation.y + j);
				addNearbyGrowthLocation(growth, northEastLocation);

				// Check the location to the SW (positive direction for X and
				// negative direction for Y)
				MapLocation southWestLocation = new MapLocation(myLocation.x
						+ i, myLocation.y - j);
				addNearbyGrowthLocation(growth, southWestLocation);

				// Check the location to the SE (positive direction for both X
				// and Y)
				MapLocation southEastLocation = new MapLocation(myLocation.x
						+ i, myLocation.y + j);
				addNearbyGrowthLocation(growth, southEastLocation);
			}
		}

		// Track the maximum cow growth
		double max = 0;
		// Loop over the keys in the Hashmap
		for (Iterator<Double> iterator = growth.keySet().iterator(); iterator
				.hasNext();) {
			Double value = iterator.next();
			// Debug
			System.out.println(String.format("%f : %d", value, growth
					.get(value).size()));
			// If this value is larger than our current max, overwrite
			if (value > max) {
				max = value;
			}
		}

		// Debug
		System.out.println(String.format("max: %f, size: %d", max,
				growth.get(max).size()));

		// Store all locations with the maximum cow growth around us
		locations = new ArrayList<MapLocation>(growth.get(max).size());
		locations.addAll(growth.get(max));
	}

	/**
	 * Adds a location to the nearby growth map.
	 * 
	 * @param nearbyMap
	 *            The current nearby growth map.
	 * @param location
	 *            The <code>MapLocation</code> to add.
	 */
	private void addNearbyGrowthLocation(
			HashMap<Double, List<MapLocation>> nearbyMap, MapLocation location) {
		// Check if the coordinates exist
		if (location.x >= 0 && location.y >= 0 && location.x < mapWidth
				&& location.y < mapHeight) {
			// Read cow growth
			double cowGrowth = mapCowGrowth[location.x][location.y];
			// If we don't have an entry in our hashmap yet, create one
			if (!nearbyMap.containsKey(cowGrowth)) {
				nearbyMap.put(cowGrowth, new ArrayList<MapLocation>());
			}
			// Add the location
			nearbyMap.get(cowGrowth).add(location);
		}
	}
}
