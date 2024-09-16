package com.github.ssquadteam.earth.api.event.town;

import com.github.ssquadteam.earth.api.EarthAPI;
import com.github.ssquadteam.earth.api.event.player.EarthPlayerSettleEvent;
import com.github.ssquadteam.earth.api.model.EarthKingdom;
import com.github.ssquadteam.earth.api.model.EarthPlayer;
import com.github.ssquadteam.earth.api.model.EarthTown;

/**
 * Called when a new town is settled by a player.
 * <p>
 * This event cannot be cancelled, as it is called after the town is created.
 * To prevent players settling towns, listen for {@link EarthPlayerSettleEvent EarthPlayerSettleEvent} and cancel it.
 * </p>
 * 
 * @author squadsteam
 *
 */
public class EarthTownSettleEvent extends EarthTownEvent {

	private final EarthPlayer player;
	private final EarthKingdom kingdom;
	
	/**
	 * Default constructor
	 * @param earth The API instance
	 * @param town The town
	 * @param player The player
	 * @param kingdom The kingdom
	 */
	public EarthTownSettleEvent(EarthAPI earth, EarthTown town, EarthPlayer player, EarthKingdom kingdom) {
		super(earth, town);
		this.player = player;
		this.kingdom = kingdom;
	}
	
	/**
	 * Gets the player that settled the town.
	 * This player is the town lord.
	 * 
	 * @return The player
	 */
	public EarthPlayer getPlayer() {
		return player;
	}
	
	/**
	 * Gets the kingdom of the new town.
	 * 
	 * @return The kingdom
	 */
	public EarthKingdom getKingdom() {
		return kingdom;
	}

}
