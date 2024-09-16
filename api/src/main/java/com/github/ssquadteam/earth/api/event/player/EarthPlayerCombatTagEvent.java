package com.github.ssquadteam.earth.api.event.player;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;

import com.github.ssquadteam.earth.api.EarthAPI;
import com.github.ssquadteam.earth.api.model.EarthPlayer;

/**
 * Called when a player is attacked by another player.
 * <p>
 * Attacking another player causes the victim to become combat tagged.
 * A player that is combat tagged is restricted from using a list of commands from the Earth configuration.
 * Canceling this event will prevent the victim from becoming combat tagged.
 * </p>
 * 
 * @author squadsteam
 *
 */
public class EarthPlayerCombatTagEvent extends EarthPlayerEvent implements Cancellable {

	private boolean isCancelled;
	
	private final EarthPlayer attacker;
	private final Location location;
	
	/**
	 * Default constructor
	 * @param earth The API instance
	 * @param player The player
	 * @param attacker The attacking player
	 * @param location The location
	 */
	public EarthPlayerCombatTagEvent(EarthAPI earth, EarthPlayer player, EarthPlayer attacker, Location location) {
		super(earth, player);
		this.isCancelled = false;
		this.attacker = attacker;
		this.location = location;
	}
	
	/**
	 * Gets the attacking player.
	 * 
	 * @return The attacker
	 */
	public EarthPlayer getAttacker() {
		return attacker;
	}
	
	/**
	 * Gets the location that the player was combat tagged at.
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
