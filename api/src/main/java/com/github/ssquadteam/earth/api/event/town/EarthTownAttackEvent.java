package com.github.ssquadteam.earth.api.event.town;

import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;

import com.github.ssquadteam.earth.api.EarthAPI;
import com.github.ssquadteam.earth.api.model.EarthPlayer;
import com.github.ssquadteam.earth.api.model.EarthTown;

/**
 * Called when an enemy player breaks a block within a town.
 * <p>
 * This event occurs for all blocks broken inside town land, including inside of the monument and for critical blocks.
 * Canceling this event prevents the block from braking.
 * </p>
 * 
 * @author squadsteam
 *
 */
public class EarthTownAttackEvent extends EarthTownEvent implements Cancellable {

	private boolean isCancelled;
	
	private final EarthPlayer attacker;
	private final Block block;
	private final boolean isMonument;
	private final boolean isCritical;
	
	/**
	 * Default constructor
	 * @param earth The API instance
	 * @param town The town
	 * @param attacker The attacking player
	 * @param block The block
	 * @param isMonument Is the block inside the monument
	 * @param isCritical Is the block a critical monument block
	 */
	public EarthTownAttackEvent(EarthAPI earth, EarthTown town, EarthPlayer attacker, Block block, boolean isMonument, boolean isCritical) {
		super(earth, town);
		this.isCancelled = false;
		this.attacker = attacker;
		this.block = block;
		this.isMonument = isMonument;
		this.isCritical = isCritical;
	}
	
	/**
	 * Gets the attacking player.
	 * 
	 * @return The attacker
	 */
	public EarthPlayer getAttacker() {
		return attacker;
	}
	
	/**
	 * Gets the block broken by the attacker.
	 * 
	 * @return The block
	 */
	public Block getBlock() {
		return block;
	}
	
	/**
	 * Checks whether the block was broken anywhere inside the monument.
	 * 
	 * @return True when the block is inside the town monument, else false
	 */
	public boolean isMonument() {
		return isMonument;
	}
	
	/**
	 * Checks whether the block is a critical block in the monument.
	 * 
	 * @return True when the block is a critical block, else false
	 */
	public boolean isCritical() {
		return isCritical;
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
