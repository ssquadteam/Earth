package com.github.ssquadteam.earth.command.admin;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.command.CommandBase;
import com.github.ssquadteam.earth.model.KonOfflinePlayer;
import com.github.ssquadteam.earth.model.KonPlayer;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.HelperUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CampAdminCommand extends CommandBase {

	public CampAdminCommand() {
		// Define name and sender support
		super("camp",true, true);
		// Define arguments
		// create|remove <player>
		List<String> argNames = Arrays.asList("create", "remove");
		addArgument(
				newArg(argNames,true,false)
						.sub( newArg("player",false,false) )
		);
    }

	@Override
	public void execute(Earth earth, CommandSender sender, List<String> args) {
		// Sender must be player
		KonPlayer player = earth.getPlayerManager().getPlayer(sender);
		if (player == null) {
			sendInvalidSenderMessage(sender);
			return;
		}
		if (args.size() != 2) {
			sendInvalidArgMessage(sender);
			return;
        }
		String cmdMode = args.get(0);
		String playerName = args.get(1);
		// Qualify arguments
		KonOfflinePlayer targetPlayer = earth.getPlayerManager().getOfflinePlayerFromName(playerName);
		if(targetPlayer == null) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage());
			return;
		}
		String targetName = targetPlayer.getOfflineBukkitPlayer().getName();
		// Execute sub-commands
		if(cmdMode.equalsIgnoreCase("create")) {
			// Sender must be a player, to get location
			Location campLoc;
			if (sender instanceof Player) {
				campLoc = ((Player)sender).getLocation();
			} else {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_PLAYER.getMessage());
				return;
			}
			// Create a new camp for the target player
			if(earth.isWorldIgnored(campLoc.getWorld())) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
				return;
			}
			if(!targetPlayer.isBarbarian()) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INVALID_PLAYER.getMessage());
				return;
			}
			if(earth.getCampManager().isCampSet(targetPlayer)) {
				ChatUtil.sendError(sender, MessagePath.COMMAND_ADMIN_CAMP_ERROR_CREATE_EXIST.getMessage());
                return;
			}
			// Determine solid location to place bed
			Chunk chunk = campLoc.getChunk();
			Point point = HelperUtil.toPoint(campLoc);
			int xLocal = campLoc.getBlockX() - (point.x*16);
			int zLocal = campLoc.getBlockZ() - (point.y*16);
			int yFloor = chunk.getChunkSnapshot(true, false, false).getHighestBlockYAt(xLocal, zLocal);
			while((chunk.getBlock(xLocal, yFloor, zLocal).isPassable() || !chunk.getBlock(xLocal, yFloor, zLocal).getType().isOccluding()) && yFloor > 0) {
				yFloor--;
			}
			campLoc.setY(yFloor+1);

			// Place a bed
			// Author: LogicalDark
			// https://www.spigotmc.org/threads/door-and-bed-placement.217786/#post-4478501
			BlockState bedFoot = campLoc.getBlock().getState();
			BlockState bedHead = bedFoot.getBlock().getRelative(BlockFace.SOUTH).getState();
			BlockData bedHeadData = Bukkit.getServer().createBlockData("minecraft:white_bed[facing=south,occupied=false,part=head]");
			BlockData bedFootData = Bukkit.getServer().createBlockData("minecraft:white_bed[facing=south,occupied=false,part=foot]");
			bedFoot.setBlockData(bedFootData);
			bedHead.setBlockData(bedHeadData);
			bedFoot.update(true, false);
			bedHead.update(true, true);

			// Create the camp
			int status = earth.getCampManager().addCamp(campLoc, targetPlayer);
			switch(status) {
				case 0:
					ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_CAMP_NOTICE_CREATE.getMessage(targetName));
					break;
				case 1:
					ChatUtil.sendError(sender, MessagePath.PROTECTION_ERROR_CAMP_FAIL_OVERLAP.getMessage());
					break;
				case 2:
					ChatUtil.sendError(sender, MessagePath.PROTECTION_ERROR_CAMP_CREATE.getMessage());
					break;
				case 3:
					ChatUtil.sendError(sender, MessagePath.PROTECTION_ERROR_CAMP_FAIL_BARBARIAN.getMessage());
					break;
				case 4:
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
					break;
				case 5:
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
					break;
				case 6:
					ChatUtil.sendError(sender, MessagePath.PROTECTION_ERROR_CAMP_FAIL_OFFLINE.getMessage());
					break;
				default:
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
					break;
			}
		} else if(cmdMode.equalsIgnoreCase("remove")) {
			if(!earth.getCampManager().isCampSet(targetPlayer)) {
				ChatUtil.sendError(sender, MessagePath.COMMAND_ADMIN_CAMP_ERROR_DESTROY_EXIST.getMessage());
                return;
			}
			boolean status = earth.getCampManager().removeCamp(targetPlayer);
			if(status) {
				ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_CAMP_NOTICE_DESTROY.getMessage(targetName));
				KonPlayer onlineOwner = earth.getPlayerManager().getPlayerFromName(playerName);
				if(onlineOwner != null) {
					ChatUtil.sendError(onlineOwner.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_CAMP_DESTROY_OWNER.getMessage());
				}
			} else {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_FAILED.getMessage());
			}
		} else {
			sendInvalidArgMessage(sender);
		}

	}

	@Override
	public List<String> tabComplete(Earth earth, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		if (args.size() == 1) {
			tabList.add("create");
			tabList.add("remove");
		} else if (args.size() == 2) {
			// suggest player names
			tabList.addAll(earth.getPlayerManager().getAllPlayerNames());
		}
		return matchLastArgToList(tabList,args);
	}
}
