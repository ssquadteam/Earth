package com.github.ssquadteam.earth.api.event.town;

import org.bukkit.event.Cancellable;

import com.github.ssquadteam.earth.api.EarthAPI;
import com.github.ssquadteam.earth.api.model.EarthPlayer;
import com.github.ssquadteam.earth.api.model.EarthTown;

/**
 * Called before a barbarian player destroys a town by breaking its final critical block.
 * <p>
 * Barbarians can only destroy towns when allowed by the Earth configuration.
 * Canceling this event will prevent the town from being destroyed, but the final critical block will still be broken.
 * </p>
 * 
 * <p>
 * After this event is cancelled, the town will remain un-destroyed, but all critical blocks will be broken.
 * The monument will regenerate as normal after enemy players break the most recent monument block.
 * </p>
 * 
 * @author squadsteam
 *
 */
public class EarthTownDestroyEvent extends EarthTownEvent implements Cancellable {

	private boolean isCancelled;
	
	private EarthPlayer player;
	
	/**
	 * Default constructor
	 * @param earth The API instance
	 * @param town The town
	 * @param player The player
	 */
	public EarthTownDestroyEvent(EarthAPI earth, EarthTown town, EarthPlayer player) {
		super(earth, town);
		this.isCancelled = false;
	}

	/**
	 * Gets the player that is destroying the town.
	 * 
	 * @return The player
	 */
	public EarthPlayer getPlayer() {
		return player;
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
