package com.github.ssquadteam.earth.api.manager;

import com.github.ssquadteam.earth.api.model.EarthCamp;
import com.github.ssquadteam.earth.api.model.EarthCampGroup;
import com.github.ssquadteam.earth.api.model.EarthOfflinePlayer;

import java.util.ArrayList;

/**
 * A manager for barbarian camps in Earth.
 * 
 * @author squadsteam
 *
 */
public interface EarthCampManager {

	/**
	 * Checks if the given player has a camp set up, and is a barbarian.
	 * 
	 * @param player The player to check
	 * @return True if the player has a camp, else false
	 */
    boolean isCampSet(EarthOfflinePlayer player);
	
	/**
	 * Gets the camp that belongs to a barbarian player. Returns null when the camp does not exist.
	 * 
	 * @param player The camp owner
	 * @return The camp
	 */
    EarthCamp getCamp(EarthOfflinePlayer player);
	
	/**
	 * Gets all camps.
	 * 
	 * @return The list of camps
	 */
    ArrayList<? extends EarthCamp> getCamps();
	
	/**
	 * Remove a camp territory and break the bed inside.
	 * 
	 * @param player The camp owner
	 * @return True when the camp is removed successfully, else false
	 */
    boolean removeCamp(EarthOfflinePlayer player);
	
	/**
	 * Checks whether camp groups (clans) are enabled in the Earth configuration.
	 * 
	 * @return True when camp groups are enabled, else false
	 */
    boolean isCampGroupsEnabled();
	
	/**
	 * Checks whether the given camp is part of a camp group (clan).
	 * A camp group is a collection of adjacent camps.
	 * 
	 * @param camp The camp to check
	 * @return True when the camp is part of a group, else false
	 */
    boolean isCampGrouped(EarthCamp camp);
	
	/**
	 * Gets the camp group of the given camp.
	 * Returns null if no group exists.
	 * 
	 * @param camp The camp
	 * @return The camp group, or null if no group exists
	 */
    EarthCampGroup getCampGroup(EarthCamp camp);
}
