package com.github.ssquadteam.earth.api.event.ruin;

import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;

import com.github.ssquadteam.earth.api.EarthAPI;
import com.github.ssquadteam.earth.api.model.EarthPlayer;
import com.github.ssquadteam.earth.api.model.EarthRuin;

/**
 * Called when a player breaks a critical block within a ruin, but before {@link EarthRuinCaptureEvent EarthRuinCaptureEvent}
 * <p>
 * Players can only break critical blocks inside of ruins.
 * Canceling this event prevents the block from braking.
 * </p>
 * 
 * @author squadsteam
 *
 */
public class EarthRuinAttackEvent extends EarthRuinEvent implements Cancellable {

	private boolean isCancelled;
	
	private final EarthPlayer attacker;
	private final Block block;
	
	/**
	 * Default constructor
	 * @param earth The API instance
	 * @param ruin The ruin
	 * @param attacker The attacking player
	 * @param block The block
	 */
	public EarthRuinAttackEvent(EarthAPI earth, EarthRuin ruin, EarthPlayer attacker, Block block) {
		super(earth, ruin);
		this.isCancelled = false;
		this.attacker = attacker;
		this.block = block;
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

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean val) {
		isCancelled = val;
	}

}
