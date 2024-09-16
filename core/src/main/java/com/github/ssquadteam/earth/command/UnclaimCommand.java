package com.github.ssquadteam.earth.command;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.model.KonPlayer;
import com.github.ssquadteam.earth.model.KonPlayer.FollowType;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.CorePath;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnclaimCommand extends CommandBase {

	public UnclaimCommand() {
		// Define name and sender support
		super("unclaim",true, false);
		// None
		setOptionalArgs(true);
		// [auto]
		addArgument(
				newArg("auto",true,false)
		);
		// radius <value>
		addArgument(
				newArg("radius",true,false)
						.sub( newArg("value", false, false) )
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

		Player bukkitPlayer = player.getBukkitPlayer();
		World bukkitWorld = bukkitPlayer.getWorld();
    	boolean isEnabled = earth.getCore().getBoolean(CorePath.TOWNS_ALLOW_UNCLAIM.getPath(),false);
		if (!isEnabled) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
			return;
		}
		// Verify that this command is being used in the default world
		if(!earth.isWorldValid(bukkitWorld)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
			return;
		}
		// Verify no Barbarians are using this command
		if(player.isBarbarian()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
			return;
		}

		if(args.isEmpty()) {
			// Unclaim the single chunk containing playerLoc for the current territory.
			earth.getTerritoryManager().unclaimForPlayer(player, bukkitPlayer.getLocation());
		} else {
			// Has mode arguments
			String unclaimMode = args.get(0);
			switch(unclaimMode) {
				case "radius" :
					if(args.size() != 2) {
						sendInvalidArgMessage(bukkitPlayer);
						return;
					}

					final int min = 1;
					final int max = 5;
					int radius = Integer.parseInt(args.get(1));
					if(radius < min || radius > max) {
						ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_UNCLAIM_ERROR_RADIUS.getMessage(min,max));
						return;
					}
					earth.getTerritoryManager().unclaimRadiusForPlayer(player, bukkitPlayer.getLocation(), radius);
					break;

				case "auto" :
					boolean doAuto = false;
					// Check if player is already in an auto follow state
					if(player.isAutoFollowActive()) {
						// Check if player is already in claim state
						if(player.getAutoFollow().equals(FollowType.UNCLAIM)) {
							// Disable the auto state
							ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_DISABLE_AUTO.getMessage());
							player.setAutoFollow(FollowType.NONE);
						} else {
							// Change state
							doAuto = true;
						}
					} else {
						// Player is not in any auto mode, enter normal un-claim following state
						doAuto = true;
					}
					if(doAuto) {
						boolean isUnclaimSuccess = earth.getTerritoryManager().unclaimForPlayer(player, bukkitPlayer.getLocation());
						if(isUnclaimSuccess) {
							ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_ENABLE_AUTO.getMessage());
							player.setAutoFollow(FollowType.UNCLAIM);
						}
					}
					break;

				default :
					sendInvalidArgMessage(bukkitPlayer);
			}
		}
	}
	
	@Override
	public List<String> tabComplete(Earth earth, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		// Give suggestions
		if (args.size() == 1) {
			tabList.add("radius");
			tabList.add("auto");
		} else if (args.size() == 2) {
			if(args.get(0).equalsIgnoreCase("radius")){
				tabList.add("#");
			}
		}
		return matchLastArgToList(tabList,args);
	}
}
