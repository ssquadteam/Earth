package com.github.ssquadteam.earth.api.event.ruin;

import com.github.ssquadteam.earth.api.EarthAPI;
import com.github.ssquadteam.earth.api.event.EarthEvent;
import com.github.ssquadteam.earth.api.model.EarthRuin;

/**
 * The base ruin event.
 * 
 * @author squadsteam
 *
 */
public class EarthRuinEvent extends EarthEvent {

	private final EarthRuin ruin;
	
	/**
	 * Default constructor
	 * @param earth The API instance
	 * @param ruin The ruin
	 */
	public EarthRuinEvent(EarthAPI earth, EarthRuin ruin) {
		super(earth);
		this.ruin = ruin;
	}
	
	/**
	 * Gets the ruin associated with the event.
	 * 
	 * @return The ruin
	 */
	public EarthRuin getRuin() {
		return ruin;
	}

}
