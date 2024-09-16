package com.github.ssquadteam.earth.api.event.camp;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;

import com.github.ssquadteam.earth.api.EarthAPI;
import com.github.ssquadteam.earth.api.model.EarthCamp;
import com.github.ssquadteam.earth.api.model.EarthPlayer;

/**
 * Called before a player breaks a camp's bed to destroy it.
 * <p>
 * This event is called when any player breaks the camp's bed, even the camp owner.
 * Canceling this event will prevent the bed from breaking, and the camp will not be destroyed.
 * </p>
 * 
 * @author squadsteam
 *
 */
public class EarthCampDestroyEvent extends EarthCampEvent implements Cancellable {

	private boolean isCancelled;
	
	private final EarthPlayer player;
	private final Location location;
	
	/**
	 * Default constructor
	 * @param earth The API instance
	 * @param camp The camp
	 * @param player The player
	 * @param location The location
	 */
	public EarthCampDestroyEvent(EarthAPI earth, EarthCamp camp, EarthPlayer player, Location location) {
		super(earth, camp);
		this.isCancelled = false;
		this.player = player;
		this.location = location;
	}
	
	/**
	 * Gets the player that is destroying the camp's bed.
	 * 
	 * @return The player
	 */
	public EarthPlayer getPlayer() {
		return player;
	}
	
	/**
	 * Gets the location of camp's bed.
	 * 
	 * @return The location
	 */
	public Location getLocation() {
		return location;
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
