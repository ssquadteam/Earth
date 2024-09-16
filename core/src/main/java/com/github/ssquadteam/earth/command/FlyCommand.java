package com.github.ssquadteam.earth.command;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.model.KonPlayer;
import com.github.ssquadteam.earth.model.KonTerritory;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class FlyCommand extends CommandBase {

	public FlyCommand() {
		// Define name and sender support
		super("fly",true, false);
		// No Arguments
    }

	@Override
	public void execute(Earth earth, CommandSender sender, List<String> args) {
		// Sender must be player
		KonPlayer player = earth.getPlayerManager().getPlayer(sender);
		if (player == null) {
			sendInvalidSenderMessage(sender);
			return;
		}
		if (!args.isEmpty()) {
			sendInvalidArgMessage(sender);
			return;
		}
		Player bukkitPlayer = player.getBukkitPlayer();
		if(bukkitPlayer.getGameMode().equals(GameMode.SURVIVAL)) {
			if(player.isFlyEnabled()) {
				player.setIsFlyEnabled(false);
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_DISABLE_AUTO.getMessage());
			} else {
				// Verify player is in friendly territory
				boolean isFriendly = false;
				if(earth.getTerritoryManager().isChunkClaimed(bukkitPlayer.getLocation())) {
					KonTerritory territory = earth.getTerritoryManager().getChunkTerritory(bukkitPlayer.getLocation());
					if(territory != null && territory.getKingdom().equals(player.getKingdom())) {
						isFriendly = true;
					}
				}
				if(isFriendly) {
					player.setIsFlyEnabled(true);
					player.setFlyDisableWarmup(false);
					ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_ENABLE_AUTO.getMessage());
				} else {
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
				}
			}
		} else {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
		}
	}
	
	@Override
	public List<String> tabComplete(Earth earth, CommandSender sender, List<String> args) {
		// No arguments to complete
		return Collections.emptyList();
	}
	
}
