package com.github.ssquadteam.earth.api.event.player;

import com.github.ssquadteam.earth.api.EarthAPI;
import com.github.ssquadteam.earth.api.event.EarthEvent;
import com.github.ssquadteam.earth.api.model.EarthPlayer;

/**
 * The base player event.
 * 
 * @author squadsteam
 *
 */
public class EarthPlayerEvent extends EarthEvent {

	private final EarthPlayer player;
	
	/**
	 * Default constructor
	 * @param earth The API instance
	 * @param player The player
	 */
	public EarthPlayerEvent(EarthAPI earth, EarthPlayer player) {
		super(earth);
		this.player = player;
	}
	
	/**
	 * Gets the player associated with the event.
	 * 
	 * @return The player
	 */
	public EarthPlayer getPlayer() {
		return player;
	}
	
}
