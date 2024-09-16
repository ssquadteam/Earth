package com.github.ssquadteam.earth.api.event.player;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;

import com.github.ssquadteam.earth.api.EarthAPI;
import com.github.ssquadteam.earth.api.model.EarthKingdom;
import com.github.ssquadteam.earth.api.model.EarthPlayer;

/**
 * Called before a player settles a new town using the "/k settle" command.
 * <p>
 * This event is called before the town is created.
 * Canceling this event will prevent the town from being settled.
 * </p>
 * 
 * @author squadsteam
 *
 */
public class EarthPlayerSettleEvent extends EarthPlayerEvent implements Cancellable {

	private boolean isCancelled;
	
	private final EarthKingdom kingdom;
	private final Location location;
	private final String name;
	
	/**
	 * Default constructor
	 * @param earth The API instance
	 * @param player The player
	 * @param kingdom The kingdom
	 * @param location The location
	 * @param name The name of the town
	 */
	public EarthPlayerSettleEvent(EarthAPI earth, EarthPlayer player, EarthKingdom kingdom, Location location, String name) {
		super(earth, player);
		this.isCancelled = false;
		this.kingdom = kingdom;
		this.location = location;
		this.name = name;
	}
	
	/**
	 * A convenience method to get the kingdom of the player settling the new town.
	 * 
	 * @return The kingdom
	 */
	public EarthKingdom getKingdom() {
		return kingdom;
	}
	
	/**
	 * Gets the location where the new town will be created.
	 * 
	 * @return The center location of the new town
	 */
	public Location getLocation() {
		return location;
	}
	
	/**
	 * Gets the name of the new town.
	 * 
	 * @return The name
	 */
	public String getName() {
		return name;
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
