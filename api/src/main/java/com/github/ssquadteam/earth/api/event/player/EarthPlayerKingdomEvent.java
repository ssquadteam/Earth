package com.github.ssquadteam.earth.api.event.player;

import org.bukkit.event.Cancellable;

import com.github.ssquadteam.earth.api.EarthAPI;
import com.github.ssquadteam.earth.api.model.EarthKingdom;
import com.github.ssquadteam.earth.api.model.EarthPlayer;

/**
 * Called before the given player has been assigned to the given kingdom.
 * <p>
 * Canceling this event will prevent the player from changing kingdoms.
 * </p>
 * 
 * @author squadsteam
 */
public class EarthPlayerKingdomEvent extends EarthPlayerEvent implements Cancellable {

	private final EarthKingdom newKingdom;
	private final EarthKingdom oldKingdom;
	private boolean isCancelled;
	
	/**
	 * Default constructor
	 * @param earth The API instance
	 * @param player The player
	 * @param newKingdom The new kingdom
	 * @param oldKingdom The old kingdom
	 */
	public EarthPlayerKingdomEvent(EarthAPI earth, EarthPlayer player, EarthKingdom newKingdom, EarthKingdom oldKingdom) {
		super(earth, player);
		this.newKingdom = newKingdom;
		this.oldKingdom = oldKingdom;
		this.isCancelled = false;
	}
	
	/**
	 * Gets the kingdom that the player is changing to
	 * 
	 * @return The new kingdom
	 */
	public EarthKingdom getNewKingdom() {
		return newKingdom;
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
