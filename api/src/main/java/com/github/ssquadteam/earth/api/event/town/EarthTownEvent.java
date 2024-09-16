package com.github.ssquadteam.earth.api.event.town;

import com.github.ssquadteam.earth.api.EarthAPI;
import com.github.ssquadteam.earth.api.event.EarthEvent;
import com.github.ssquadteam.earth.api.model.EarthTown;

/**
 * The base town event.
 * 
 * @author squadsteam
 *
 */
public class EarthTownEvent extends EarthEvent {

	private final EarthTown town;
	
	/**
	 * Default constructor
	 * @param earth The API instance
	 * @param town The town
	 */
	public EarthTownEvent(EarthAPI earth, EarthTown town) {
		super(earth);
		this.town = town;
	}
	
	/**
	 * Gets the town associated with the event.
	 * 
	 * @return The town
	 */
	public EarthTown getTown() {
		return town;
	}

}
