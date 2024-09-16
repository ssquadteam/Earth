package com.github.ssquadteam.earth.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.github.ssquadteam.earth.api.EarthAPI;

/**
 * A base event from Earth, with a reference to the API instance.
 * 
 * @author squadsteam
 *
 */
public abstract class EarthEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final EarthAPI earth;
	
	/**
	 * Construct an event with a reference to the Earth API
	 * 
	 * @param earth The EarthAPI instance
	 */
	public EarthEvent(EarthAPI earth) {
		this.earth = earth;
	}
	
	/**
	 * Get the EarthAPI instance that sent this event
	 * 
	 * @return The EarthAPI instance
	 */
	public EarthAPI getEarth() {
		return earth;
	}
    
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	/**
	 * Get the handler list
	 * 
	 * @return handlers
	 */
	public static HandlerList getHandlerList() {
        return handlers;
    }

}
