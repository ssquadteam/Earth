package com.github.ssquadteam.earth.api.event.ruin;

import com.github.ssquadteam.earth.api.EarthAPI;
import com.github.ssquadteam.earth.api.model.EarthPlayer;
import com.github.ssquadteam.earth.api.model.EarthRuin;
import org.bukkit.event.Cancellable;

import java.util.List;

/**
 * Called before a player captures a ruin, but after {@link EarthRuinAttackEvent EarthRuinAttackEvent}
 * <p>
 * Players capture ruins by breaking all critical blocks inside.
 * When the final critical block is broken, all players located inside of the ruin receive a reward.
 * Canceling this event will stop the final critical block break and prevent the ruin from being captured.
 * </p>
 * 
 * @author squadsteam
 *
 */
public class EarthRuinCaptureEvent extends EarthRuinEvent implements Cancellable {

	private boolean isCancelled;
	
	private final EarthPlayer player;
	private final List<? extends EarthPlayer> rewardPlayers;
	
	/**
	 * Default constructor
	 * @param earth The API instance
	 * @param ruin The ruin
	 * @param player The player
	 * @param rewardPlayers The players receiving reward
	 */
	public EarthRuinCaptureEvent(EarthAPI earth, EarthRuin ruin, EarthPlayer player, List<? extends EarthPlayer> rewardPlayers) {
		super(earth, ruin);
		this.isCancelled = false;
		this.player = player;
		this.rewardPlayers = rewardPlayers;
	}

	/**
	 * Gets the player that captured this ruin.
	 * This player broke the final critical block.
	 * 
	 * @return The player
	 */
	public EarthPlayer getPlayer() {
		return player;
	}
	
	/**
	 * Gets the list of players that get a reward for capturing the ruin.
	 * 
	 * @return The list of players
	 */
	public List<? extends EarthPlayer> getRewardPlayers() {
		return rewardPlayers;
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
