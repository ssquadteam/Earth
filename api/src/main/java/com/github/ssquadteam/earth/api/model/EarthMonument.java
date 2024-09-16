package com.github.ssquadteam.earth.api.model;

import org.bukkit.Location;

/**
 * A monument structure within a town.
 * 
 * @author squadsteam
 *
 */
public interface EarthMonument {

	/**
	 * Checks whether this monument is valid.
	 * A monument is valid when it is copied from a valid monument template.
	 * 
	 * @return True when the monument is valid, else false
	 */
    boolean isValid();
	
	/**
	 * Gets the travel location within the monument.
	 * Players that travel to the town with this monument will be teleported here.
	 * 
	 * @return The travel location
	 */
    Location getTravelPoint();
	
	/**
	 * Gets the bottom Y coordinate of this monument in the world.
	 * Defaults to the Y value of the town's center location.
	 * 
	 * @return The Y coordinate of the base of this monument
	 */
    int getBaseY();
	
	/**
	 * Gets the height in blocks of this monument.
	 * 
	 * @return The height
	 */
    int getHeight();
	
	/**
	 * Checks whether the given location is inside of this monument.
	 * A monument occupies only a portion of the Y axis within a chunk, so this method is useful to determine
	 * whether a location is actually inside the monument structure.
	 * 
	 * @param loc The location to check
	 * @return True when the location is inside the monument, else false
	 */
    boolean isLocInside(Location loc);
	
	/**
	 * Gets the number of critical hits that this monument has.
	 * Each critical hit represents a broken critical block inside the monument structure.
	 * When there are as many critical hits as max, the town is captured.
	 * When the town is no longer under attack, the monument will regenerate and clear the critical hits.
	 * 
	 * @return The number of critical hits
	 */
    int getCriticalHits();
	
}
