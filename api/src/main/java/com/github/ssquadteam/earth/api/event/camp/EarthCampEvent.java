package com.github.ssquadteam.earth.api.event.camp;

import com.github.ssquadteam.earth.api.EarthAPI;
import com.github.ssquadteam.earth.api.event.EarthEvent;
import com.github.ssquadteam.earth.api.model.EarthCamp;

/**
 * The base camp event.
 * 
 * @author squadsteam
 *
 */
public class EarthCampEvent extends EarthEvent {

	private final EarthCamp camp;
	
	/**
	 * Default constructor
	 * @param earth The API instance
	 * @param camp The camp
	 */
	public EarthCampEvent(EarthAPI earth, EarthCamp camp) {
		super(earth);
		this.camp = camp;
	}
	
	/**
	 * Gets the town associated with the event.
	 * 
	 * @return The town
	 */
	public EarthCamp getCamp() {
		return camp;
	}

}
