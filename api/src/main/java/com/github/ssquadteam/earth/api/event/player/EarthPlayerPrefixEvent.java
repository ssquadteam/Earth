package com.github.ssquadteam.earth.api.event.player;

import com.github.ssquadteam.earth.api.EarthAPI;
import com.github.ssquadteam.earth.api.model.EarthPlayer;

/**
 * Called after a player changes their accomplishment prefix.
 * 
 * @author squadsteam
 *
 */
public class EarthPlayerPrefixEvent extends EarthPlayerEvent {

	private final String prefix;
	private final boolean isDisabled;
	
	/**
	 * Default constructor
	 * @param earth The API instance
	 * @param player The player
	 * @param prefix The prefix
	 * @param isDisabled Is the prefix disabled
	 */
	public EarthPlayerPrefixEvent(EarthAPI earth, EarthPlayer player, String prefix, boolean isDisabled) {
		super(earth, player);
		this.prefix = prefix;
		this.isDisabled = isDisabled;
	}
	
	/**
	 * Gets the current prefix of the player, as shown in chat.
	 * When isDisabled returns true, no prefix will show in chat, and the prefix will be an empty string.
	 * 
	 * @return The prefix, or an empty string when disabled
	 */
	public String getPrefix() {
		return prefix;
	}
	
	/**
	 * Checks whether the player disabled their prefix.
	 * 
	 * @return True when the prefix was disabled, else false
	 */
	public boolean isDisabled() {
		return isDisabled;
	}

}
