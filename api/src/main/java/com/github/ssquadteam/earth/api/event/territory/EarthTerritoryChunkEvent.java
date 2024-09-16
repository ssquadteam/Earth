package com.github.ssquadteam.earth.api.event.territory;

import com.github.ssquadteam.earth.api.EarthAPI;
import com.github.ssquadteam.earth.api.event.EarthEvent;
import com.github.ssquadteam.earth.api.model.EarthTerritory;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.Cancellable;

import java.awt.*;
import java.util.Set;

/**
 * Represents a generic change of a territory's land chunk.
 * <p>
 * The territory will be modified by either claiming or unclaiming the given chunk.
 * Normal players typically can only claim land for towns, and cannot unclaim any land.
 * Admins can claim and unclaim land for any territory.
 * When this event is cancelled, the territory will be unmodified.
 * </p>
 * @author squadsteam
 *
 */
public class EarthTerritoryChunkEvent extends EarthEvent implements Cancellable {

	private boolean isCancelled;
	
	private final EarthTerritory territory;
	private final Location location;
	private final Set<Point> points;
	private final boolean isClaimed;
	
	/**
	 * Default constructor
	 * @param earth The API instance
	 * @param territory The territory
	 * @param location The location
	 * @param points The point(s) of land
	 * @param isClaimed Are the point(s) claimed or unclaimed
	 */
	public EarthTerritoryChunkEvent(EarthAPI earth, EarthTerritory territory, Location location, Set<Point> points, boolean isClaimed) {
		super(earth);
		this.isCancelled = false;
		this.territory = territory;
		this.location = location;
		this.points = points;
		this.isClaimed = isClaimed;
	}
	
	/**
	 * Gets the territory that is being modified.
	 * 
	 * @return The territory
	 */
	public EarthTerritory getTerritory() {
		return territory;
	}
	
	/**
	 * Gets the location in the chunk of land being modified.
	 * 
	 * @return The location
	 */
	public Location getLocation() {
		return location;
	}
	
	/**
	 * Gets the world of the chunk(s) of land being modified.
	 * This is a convenience method, getting the world from the location.
	 * 
	 * @return The world
	 */
	public World getWorld() {
		return location.getWorld();
	}
	
	/**
	 * Gets the set of points representing land chunks which were modified.
	 * Each point represents a chunk, in the same world as the location.
	 * 
	 * @return The points of land
	 */
	public Set<Point> getPoints() {
		return points;
	}
	
	/**
	 * Checks whether the territory is claiming the chunk or not.
	 * 
	 * @return True when the territory is claiming the chunk, else false when it is un-claiming it.
	 */
	public boolean isClaimed() {
		return isClaimed;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean val) {
		isCancelled = val;
	}
}
