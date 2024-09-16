package com.github.ssquadteam.earth.api.model;

import org.bukkit.OfflinePlayer;


/**
 * Represents an offline player in Earth.
 * This interface wraps around Bukkit's OfflinePlayer interface.
 * 
 * @author squadsteam
 *
 */
public interface EarthOfflinePlayer {

	/**
	 * Get the OfflinePlayer instance represented by this object.
	 * 
	 * @return The offline player
	 */
    OfflinePlayer getOfflineBukkitPlayer();
	
	/**
	 * Get the player's current kingdom.
	 * 
	 * @return The current kingdom
	 */
    EarthKingdom getKingdom();
	
	/**
	 * Get the player's exile kingdom. This is the previous kingdom that the player was a member of.
	 * 
	 * @return The exile kingdom
	 */
    EarthKingdom getExileKingdom();
	
	/**
	 * Get whether the player is a barbarian.
	 * 
	 * @return True when the player is a barbarian, else false
	 */
    boolean isBarbarian();
	
}
