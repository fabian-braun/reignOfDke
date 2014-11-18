package dualcore;

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
	 * This field represents the location we attacked last.
	 */
	private MapLocation lastAttackedLocation;

	/**
	 * This field defines whether or not to write debug information to the
	 * output.
	 */
	private final boolean debug = false;

	/**
	 * This field represents a range within which we do not want to be shooting.
	 */
	private final int closeRange = 4;

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

		MapLocation attackingLocation;

		// If we do not have a location that we attacked last, randomly select a
		// new one
		if (lastAttackedLocation == null) {
			// Get a random location from the locations with the maximum cow
			// growth around us
			attackingLocation = locations
					.get(randall.nextInt(locations.size()));

			// Debug
			if (debug) {
				System.out.println(String.format(
						"Initiating attack on [%d,%d]", attackingLocation.x,
						attackingLocation.y));
			}
		} else {
			// Otherwise, we attack a location slightly closer to us than the
			// last one
			attackingLocation = getCloserLocation(lastAttackedLocation,
					closeRange);

			// Check if we found anything
			if (attackingLocation == null) {
				// If not, get a random one
				attackingLocation = locations.get(randall.nextInt(locations
						.size()));

				// Debug
				if (debug) {
					System.out.println(String.format(
							"Initiating attack on [%d,%d]",
							attackingLocation.x, attackingLocation.y));
				}
			} else if (debug) {
				// Debug
				System.out.println(String.format("Attacking closer on [%d,%d]",
						attackingLocation.x, attackingLocation.y));
			}
		}

		// Check if we can attack this location
		if (rc.canAttackSquare(attackingLocation)) {
			// Attack that location
			rc.attackSquareLight(attackingLocation);
		}

		// Save the location
		lastAttackedLocation = attackingLocation;

		// Clear the last attacked location if it was closer than closeRange
		// squares to
		// us, since we do not want to shoot at any location in a range of one
		// less than closeRange of
		// our own
		List<MapLocation> neighboursCloseRange = getNeighbours(
				lastAttackedLocation, closeRange);
		if (neighboursCloseRange.contains(myLocation)) {
			lastAttackedLocation = null;
		}
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
		if (debug) {
			System.out.println(String.format("%d,%d", myLocation.x,
					myLocation.y));
		}

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
		if (debug) {
			System.out.println(String.format("max: %f, size: %d", max, growth
					.get(max).size()));
		}

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
		if (isXOnMap(location.x) && isYOnMap(location.y)) {
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

	/**
	 * Determine a location closer to the location of the
	 * <code>NoiseTower</code> from a specified location.
	 * 
	 * @param location
	 *            The location to find a closer location for.
	 * @return <code>MapLocation</code> representing a closer location.
	 */
	private MapLocation getCloserLocation(MapLocation location, int closeRange) {
		int x = 0;
		// If the x is more than closeRange away from our own x
		if (location.x < myLocation.x - closeRange) {
			// Go one towards us
			x = location.x + 1;
		} else if (location.x > myLocation.x + closeRange) {
			x = location.x - 1;
		} else {
			return null;
		}

		int y = 0;
		// If the y is more than closeRange away from our own y
		if (location.y < myLocation.y - closeRange) {
			// Go one towards us
			y = location.y + 1;
		} else if (location.y > myLocation.y + closeRange) {
			y = location.y - 1;
		} else {
			return null;
		}

		// Return our closer location
		return new MapLocation(x, y);
	}

	/**
	 * Creates a list of locations adjacent to a location a specific distance
	 * away.
	 * 
	 * @param from
	 *            The location to get the adjacent locations for.
	 * @param distance
	 *            The distance away from the origin location that the neighbours
	 *            should be.
	 * @return List of <code>MapLocation</code>
	 */
	private List<MapLocation> getNeighbours(MapLocation from, int distance) {
		// Initialise a list to write to
		List<MapLocation> neighbours = new ArrayList<MapLocation>();

		// Check in each direction and try to add it
		addNeighbour(neighbours, new MapLocation(from.x - distance, from.y
				- distance));
		addNeighbour(neighbours, new MapLocation(from.x - distance, from.y));
		addNeighbour(neighbours, new MapLocation(from.x - distance, from.y
				+ distance));
		addNeighbour(neighbours, new MapLocation(from.x, from.y - distance));
		addNeighbour(neighbours, new MapLocation(from.x, from.y + distance));
		addNeighbour(neighbours, new MapLocation(from.x + distance, from.y
				- distance));
		addNeighbour(neighbours, new MapLocation(from.x + distance, from.y));
		addNeighbour(neighbours, new MapLocation(from.x + distance, from.y
				+ distance));

		// Return the list
		return neighbours;
	}

	/**
	 * Adds a location to a list of locations, if the specified location exists.
	 * 
	 * @param neighbours
	 *            The list of locations.
	 * @param potentialNeighbour
	 *            The location to be added to the list
	 */
	private void addNeighbour(List<MapLocation> neighbours,
			MapLocation potentialNeighbour) {
		// Check if the location's x and y are within the map boundaries
		if (isXOnMap(potentialNeighbour.x) && isYOnMap(potentialNeighbour.y)) {
			// Add it to the list
			neighbours.add(potentialNeighbour);
		}
	}

	/**
	 * Check if an x coordinate is within the boundaries of the current map.
	 * 
	 * @param x
	 *            The x coordinate to check.
	 * @return Whether or not the coordinate is on the map.
	 */
	private boolean isXOnMap(int x) {
		// Using width because of x
		return x >= 0 && x < mapWidth;
	}

	/**
	 * Check if a y coordinate is within the boundaries of the current map.
	 * 
	 * @param y
	 *            The y coordinate to check.
	 * @return Whether or not the coordinate is on the map.
	 */
	private boolean isYOnMap(int y) {
		// Using height because of y
		return y >= 0 && y < mapHeight;
	}
}
