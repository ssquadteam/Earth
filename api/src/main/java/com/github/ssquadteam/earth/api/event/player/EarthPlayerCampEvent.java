package com.github.ssquadteam.earth.api.event.player;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;

import com.github.ssquadteam.earth.api.EarthAPI;
import com.github.ssquadteam.earth.api.model.EarthPlayer;

/**
 * Called before a barbarian player places a bed in the wild to create their camp.
 * <p>
 * Canceling this event will prevent the camp creation and bed block placement.
 * </p>
 * 
 * @author squadsteam
 *
 */
public class EarthPlayerCampEvent extends EarthPlayerEvent implements Cancellable {

	private boolean isCancelled;
	
	private final Location location;
	
	/**
	 * Default constructor
	 * @param earth The API instance
	 * @param player The player
	 * @param location The location
	 */
	public EarthPlayerCampEvent(EarthAPI earth, EarthPlayer player, Location location) {
		super(earth, player);
		this.isCancelled = false;
		this.location = location;
	}
	
	/**
	 * Gets the location of the new camp.
	 * This is the where the bed is being placed.
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
