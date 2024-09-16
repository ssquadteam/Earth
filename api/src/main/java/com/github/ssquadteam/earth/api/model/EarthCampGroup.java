package com.github.ssquadteam.earth.api.model;

import org.bukkit.OfflinePlayer;

import java.util.Collection;
/**
 * A collection of adjacent camps.
 * 
 * @author squadsteam
 *
 */
public interface EarthCampGroup {

	/**
	 * Checks whether this camp group contains the given camp instance.
	 * 
	 * @param camp The camp to check
	 * @return True when this camp group contains the camp, else false
	 */
    boolean containsCamp(EarthCamp camp);
	
	/**
	 * Gets all camps in this camp group.
	 * 
	 * @return All camps
	 */
    Collection<? extends EarthCamp> getCamps();
	
	/**
	 * Checks whether the given player has a camp that is a part of this camp group.
	 * The player must be a barbarian with a set-up camp.
	 * Their camp must be placed adjacent to other camps within this camp group.
	 * Camp group members can optionally edit blocks and access chests in each other's camps.
	 * 
	 * @param player The player to check
	 * @return True when this player is a member of this camp group, else false
	 */
    boolean isPlayerMember(OfflinePlayer player);
}
