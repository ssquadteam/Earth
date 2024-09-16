package com.github.ssquadteam.earth.command;



import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.model.KonPlayer;
import com.github.ssquadteam.earth.model.KonPrefixType;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PrefixCommand extends CommandBase {

	public PrefixCommand() {
		// Define name and sender support
		super("prefix",true, false);
		// None
		setOptionalArgs(true);
		// menu
		addArgument(
				newArg("menu",true,false)
		);
		// clear
		addArgument(
				newArg("clear",true,false)
		);
		// set <title>
		addArgument(
				newArg("set",true,false)
						.sub( newArg("title",false,false) )
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
		// Check for disabled feature
		if(!earth.getAccomplishmentManager().isEnabled()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
			return;
		}
		// Check for barbarian player
		if(player.isBarbarian()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
			return;
		}
		// Parse arguments
		if (args.isEmpty()) {
			// Open menu
			earth.getDisplayManager().displayPrefixMenu(player);
		} else {
			// Has arguments
			String subCmd = args.get(0);
			switch (subCmd.toLowerCase()) {
				case "menu":
					// Open menu
					earth.getDisplayManager().displayPrefixMenu(player);
					break;
				case "clear":
					// Clear prefix title
					earth.getAccomplishmentManager().disablePlayerPrefix(player);
					break;
				case "set":
					// Set new title
					if (args.size() == 2) {
						String titleName = args.get(1);
						if (KonPrefixType.isPrefixName(titleName)) {
							KonPrefixType prefix = KonPrefixType.getPrefixByName(titleName);
							earth.getAccomplishmentManager().applyPlayerPrefix(player,prefix);
						} else {
							ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(titleName));
						}
					} else {
						sendInvalidArgMessage(sender);
					}
					break;
				default:
					sendInvalidArgMessage(sender);
			}
		}
	}

	@Override
	public List<String> tabComplete(Earth earth, CommandSender sender, List<String> args) {
		// Sender must be player
		KonPlayer player = earth.getPlayerManager().getPlayer(sender);
		if (player == null) return Collections.emptyList();
		List<String> tabList = new ArrayList<>();
		// Give suggestions
		if(args.size() == 1) {
			tabList.add("menu");
			tabList.add("clear");
			tabList.add("set");
		} else if (args.size() == 2) {
			String subCmd = args.get(0);
			if (subCmd.equalsIgnoreCase("set")) {
				tabList.addAll(player.getPlayerPrefix().getPrefixNames());
			}
		}
		return matchLastArgToList(tabList,args);
	}
}
