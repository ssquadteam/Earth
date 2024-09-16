package com.github.ssquadteam.earth.api.event.camp;

import com.github.ssquadteam.earth.api.EarthAPI;
import com.github.ssquadteam.earth.api.event.player.EarthPlayerCampEvent;
import com.github.ssquadteam.earth.api.model.EarthCamp;
import com.github.ssquadteam.earth.api.model.EarthPlayer;

/**
 * Called when a new camp is created by a barbarian player.
 * <p>
 * This event cannot be cancelled, as it is called after the camp is created.
 * To prevent players creating camps, listen for {@link EarthPlayerCampEvent EarthPlayerCampEvent} and cancel it.
 * </p>
 * 
 * @author squadsteam
 *
 */
public class EarthCampCreateEvent extends EarthCampEvent {

	private final EarthPlayer player;
	
	/**
	 * Default constructor
	 * @param earth The API instance
	 * @param camp The camp
	 * @param player The player
	 */
	public EarthCampCreateEvent(EarthAPI earth, EarthCamp camp, EarthPlayer player) {
		super(earth, camp);
		this.player = player;
	}
	
	/**
	 * Gets the player that created the camp.
	 * 
	 * @return The player
	 */
	public EarthPlayer getPlayer() {
		return player;
	}

}
