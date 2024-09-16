package com.github.ssquadteam.earth.command.admin;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.command.CommandBase;
import com.github.ssquadteam.earth.model.*;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.HelperUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.Chunk;
import org.bukkit.Effect;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CaptureAdminCommand extends CommandBase {

	public CaptureAdminCommand() {
		// Define name and sender support
		super("capture",false, true);
		// Define arguments
		// <town> <kingdom>
		addArgument(
				newArg("town",false,false)
						.sub( newArg("kingdom",false,false) )
		);
    }

	@Override
	public void execute(Earth earth, CommandSender sender, List<String> args) {
		// Check for correct arguments
		if (args.size() != 2) {
			sendInvalidArgMessage(sender);
		}
		String townName = args.get(0);
		String kingdomName = args.get(1);

		// Check whether the town name is a town or the capital.
		// If it's a town, capture the town for the kingdom.
		// If it's a capital, remove the old kingdom and all other towns, exiles all members.

		// Check for existing kingdom
		if(!earth.getKingdomManager().isKingdom(kingdomName)) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
			return;
		}
		KonKingdom kingdom = earth.getKingdomManager().getKingdom(kingdomName);
		assert kingdom != null;
		// Check for existing town in kingdom
		if(!kingdom.isCreated() || kingdom.hasTown(townName) || kingdom.hasCapital(townName)) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return;
		}
		// Find town
		KonTown town = null;
		boolean isCapital = false;
		for(KonKingdom testKingdom : earth.getKingdomManager().getKingdoms()) {
			if(testKingdom.hasTown(townName)) {
				town = testKingdom.getTown(townName);
			} else if(testKingdom.hasCapital(townName)) {
				town = testKingdom.getCapital();
				isCapital = true;
			}
		}
		// Check for found town or capital name
		if(town == null) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(townName));
			return;
		}
		// Capture the town/capital
		String townKingdomName = town.getKingdom().getName();
		KonTown capturedTown = null;
		if(isCapital) {
			capturedTown = earth.getKingdomManager().captureCapital(townKingdomName, kingdom);
		} else {
			capturedTown = earth.getKingdomManager().captureTown(townName, town.getKingdom().getName(), kingdom);
		}
		// Check for successful capture
		if(capturedTown != null) {
			String newKingdomName = kingdom.getName();
			if(isCapital) {
				ChatUtil.sendBroadcast(MessagePath.PROTECTION_NOTICE_KINGDOM_CONQUER.getMessage(townKingdomName,newKingdomName));
			} else {
				ChatUtil.sendBroadcast(MessagePath.PROTECTION_NOTICE_CONQUER.getMessage(townName));
			}
			ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
			// For all online players...
			for(KonPlayer onlinePlayer : earth.getPlayerManager().getPlayersOnline()) {
				// Teleport all players inside center chunk to new spawn location
				if(capturedTown.isLocInsideCenterChunk(onlinePlayer.getBukkitPlayer().getLocation())) {
					onlinePlayer.getBukkitPlayer().teleport(earth.getSafeRandomCenteredLocation(capturedTown.getCenterLoc(), 2));
					onlinePlayer.getBukkitPlayer().playEffect(onlinePlayer.getBukkitPlayer().getLocation(), Effect.ANVIL_LAND, null);
				}
				// Remove mob targets
				if(capturedTown.isLocInside(onlinePlayer.getBukkitPlayer().getLocation())) {
					onlinePlayer.clearAllMobAttackers();
				}
				// Update particle border renders for nearby players
				for(Chunk chunk : HelperUtil.getAreaChunks(onlinePlayer.getBukkitPlayer().getLocation(), 2)) {
					if(capturedTown.hasChunk(chunk)) {
						earth.getTerritoryManager().updatePlayerBorderParticles(onlinePlayer);
						break;
					}
				}
			}
			// Broadcast to Dynmap
			int x = capturedTown.getCenterLoc().getBlockX();
			int y = capturedTown.getCenterLoc().getBlockY();
			int z = capturedTown.getCenterLoc().getBlockZ();
			earth.getMapHandler().postBroadcast(MessagePath.PROTECTION_NOTICE_CONQUER.getMessage(capturedTown.getName())+" ("+x+","+y+","+z+")");
			// Broadcast to Discord
			earth.getIntegrationManager().getDiscordSrv().sendGameToDiscordMessage("global", ":crossed_swords: **"+MessagePath.PROTECTION_NOTICE_CONQUER_DISCORD.getMessage(capturedTown.getName(),capturedTown.getKingdom().getName())+"**");
			capturedTown.getMonumentTimer().stopTimer();
			capturedTown.setAttacked(false,null);
			capturedTown.setBarProgress(1.0);
			capturedTown.updateBarTitle();
		} else {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_FAILED.getMessage());
		}
	}

	@Override
	public List<String> tabComplete(Earth earth, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		if (args.size() == 1) {
			// Suggest town names
			for(KonKingdom kingdom : earth.getKingdomManager().getKingdoms()) {
				tabList.addAll(kingdom.getTownNames());
				tabList.add(kingdom.getName());
			}
		} else if (args.size() == 2) {
			// Suggest enemy kingdom names
			tabList.addAll(earth.getKingdomManager().getKingdomNames());
		}
		return matchLastArgToList(tabList,args);
	}
}
