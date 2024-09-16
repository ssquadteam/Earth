package com.github.ssquadteam.earth.api.event.player;

import org.bukkit.event.Cancellable;

import com.github.ssquadteam.earth.api.EarthAPI;
import com.github.ssquadteam.earth.api.event.EarthEvent;
import com.github.ssquadteam.earth.api.model.EarthKingdom;
import com.github.ssquadteam.earth.api.model.EarthOfflinePlayer;

/**
 * Called before the given player has been exiled from their current kingdom and made into a barbarian.
 * <p>
 * Canceling this event will prevent the player from being exiled.
 * </p>
 * 
 * @author squadsteam
 *
 */
public class EarthPlayerExileEvent extends EarthEvent implements Cancellable {

	private boolean isCancelled;
	private final EarthOfflinePlayer offlinePlayer;
	private final EarthKingdom oldKingdom;
	
	/**
	 * Default constructor
	 * @param earth The API instance
	 * @param offlinePlayer The player
	 * @param oldKingdom The kingdom
	 */
	public EarthPlayerExileEvent(EarthAPI earth, EarthOfflinePlayer offlinePlayer, EarthKingdom oldKingdom) {
		super(earth);
		this.isCancelled = false;
		this.offlinePlayer = offlinePlayer;
		this.oldKingdom = oldKingdom;
	}
	
	/**
	 * Gets the player that is being exiled.
	 * This player can be online or offline.
	 * 
	 * @return The player
	 */
	public EarthOfflinePlayer getPlayer() {
		return offlinePlayer;
	}
	
	/**
	 * Gets the kingdom that the player is leaving
	 * 
	 * @return The old kingdom
	 */
	public EarthKingdom getOldKingdom() {
		return oldKingdom;
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
