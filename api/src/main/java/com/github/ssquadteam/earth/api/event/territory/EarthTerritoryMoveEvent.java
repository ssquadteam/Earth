package com.github.ssquadteam.earth.api.event.territory;

import org.bukkit.event.Cancellable;

import com.github.ssquadteam.earth.api.EarthAPI;
import com.github.ssquadteam.earth.api.event.EarthEvent;
import com.github.ssquadteam.earth.api.model.EarthPlayer;
import com.github.ssquadteam.earth.api.model.EarthTerritory;

/**
 * Called before a player enters or leaves a territory. 
 * <p>
 * This only happens at chunk boundaries.
 * The wild is not a territory, and is represented as a null territory field.
 * When this event is cancelled, the player's movement will also be cancelled and they will be prevented from moving.
 * </p>
 * 
 * @author squadsteam
 *
 */
public class EarthTerritoryMoveEvent extends EarthEvent implements Cancellable {

	private boolean isCancelled;
	
	private final EarthTerritory territoryTo;
	private final EarthTerritory territoryFrom;
	private final EarthPlayer player;
	
	/**
	 * Default constructor
	 * @param earth The API instance
	 * @param territoryTo The territory moving to
	 * @param territoryFrom The territory moving from
	 * @param player The player
	 */
	public EarthTerritoryMoveEvent(EarthAPI earth, EarthTerritory territoryTo, EarthTerritory territoryFrom, EarthPlayer player) {
		super(earth);
		this.isCancelled = false;
		this.territoryTo = territoryTo;
		this.territoryFrom = territoryFrom;
		this.player = player;
	}
	
	/**
	 * Get the player that moved between territories.
	 * 
	 * @return The player
	 */
	public EarthPlayer getPlayer() {
		return player;
	}
	
	/**
	 * Get the territory that the player is moving into.
	 * Returns null when moving into the wild.
	 * 
	 * @return The territory, or null when wild
	 */
	public EarthTerritory getTerritoryTo() {
		return territoryTo;
	}
	
	/**
	 * Get the territory that the player is moving out of.
	 * Returns null when moving out of the wild.
	 * 
	 * @return The territory, or null when wild
	 */
	public EarthTerritory getTerritoryFrom() {
		return territoryFrom;
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
