package com.github.ssquadteam.earth.api.model;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

/**
 * A camp with a bed for a single barbarian player.
 * 
 * @author squadsteam
 *
 */
public interface EarthCamp extends EarthTerritory {

	/**
	 * Checks whether the owner is currently online.
	 * 
	 * @return True when the camp owner is online, else false
	 */
    boolean isOwnerOnline();
	
	/**
	 * Gets the player who owns this camp.
	 * 
	 * @return The player owner
	 */
    OfflinePlayer getOwner();
	
	/**
	 * Checks whether the given player is the owner of this camp.
	 * 
	 * @param player The player to check
	 * @return True when the player is the owner, else false
	 */
    boolean isPlayerOwner(OfflinePlayer player);
	
	/**
	 * Gets the location of the bed in this camp.
	 * 
	 * @return The bed location
	 */
    Location getBedLocation();
	
	/**
	 * Checks whether this camp is currently protected from attacks.
	 * Camps are protected typically due to the owner being offline.
	 * When a camp is protected, it cannot be edited or accessed.
	 * 
	 * @return True when the camp is protected, else false
	 */
    boolean isProtected();
}
