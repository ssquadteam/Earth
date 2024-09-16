package com.github.ssquadteam.earth.command.admin;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.command.CommandBase;
import com.github.ssquadteam.earth.model.KonPlayer;
import com.github.ssquadteam.earth.model.KonTerritory;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class SetTravelAdminCommand extends CommandBase {
	
	public SetTravelAdminCommand() {
		// Define name and sender support
		super("settravel",true, true);
		// No arguments
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
		Location playerLoc = player.getBukkitPlayer().getLocation();
    	// Check for valid world
		if(!earth.isWorldValid(playerLoc.getWorld())) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
			return;
		}
		// Check if player's location is within a territory
		if(earth.getTerritoryManager().isChunkClaimed(playerLoc)) {
			KonTerritory territory = earth.getTerritoryManager().getChunkTerritory(playerLoc);
			if (territory == null) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
				return;
			}
			territory.setSpawn(playerLoc);
			ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_SETTRAVEL_NOTICE_SUCCESS.getMessage(territory.getName()));
		} else {
			ChatUtil.sendError(sender, MessagePath.COMMAND_ADMIN_SETTRAVEL_ERROR_FAIL.getMessage());
		}
    }
    
    @Override
	public List<String> tabComplete(Earth earth, CommandSender sender, List<String> args) {
		// No arguments to complete
		return Collections.emptyList();
	}
}
