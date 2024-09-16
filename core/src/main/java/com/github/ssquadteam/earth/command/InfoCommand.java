package com.github.ssquadteam.earth.command;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.model.*;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;

public class InfoCommand extends CommandBase {
	
	public InfoCommand() {
		// Define name and sender support
		super("info",true, false);
		// None
		setOptionalArgs(true);
		// player|kingdom|capital|town|sanctuary|ruin <name>
		List<String> argNames = Arrays.asList("player", "kingdom", "capital", "town", "sanctuary", "ruin");
		addArgument(
				newArg(argNames,true,false)
						.sub( newArg("name",false,false) )
		);
    }

	@Override
	public void execute(Earth earth, CommandSender sender, List<String> args) {
		// Sender must be player
		KonPlayer player = earth.getPlayerManager().getPlayer(sender);
		if (player == null) {
			ChatUtil.printDebug("Command executed with null player", true);
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return;
		}
		// Parse arguments
		if (args.isEmpty()) {
			// No arguments
			// Display the player's kingdom info
			earth.getDisplayManager().displayKingdomInfoMenu(player, player.getKingdom());
		} else if(args.size() == 2) {
			// Two arguments
			String infoType = args.get(0);
			String infoName = args.get(1);
			// Force type to lowercase for matching
			switch(infoType.toLowerCase()) {
				case "player":
					KonOfflinePlayer otherPlayer = earth.getPlayerManager().getOfflinePlayerFromName(infoName);
					if(otherPlayer != null) {
						earth.getDisplayManager().displayPlayerInfoMenu(player, otherPlayer);
						return;
					}
					break;
				case "kingdom":
					if(earth.getKingdomManager().isKingdom(infoName)) {
						KonKingdom kingdom = earth.getKingdomManager().getKingdom(infoName);
						earth.getDisplayManager().displayKingdomInfoMenu(player, kingdom);
						return;
					} else if(infoName.equalsIgnoreCase(MessagePath.LABEL_BARBARIANS.getMessage())) {
						KonKingdom kingdom = earth.getKingdomManager().getBarbarians();
						earth.getDisplayManager().displayKingdomInfoMenu(player, kingdom);
						return;
					}
					break;
				case "capital":
					for(KonKingdom k : earth.getKingdomManager().getKingdoms()) {
						if(k.hasCapital(infoName)) {
							earth.getDisplayManager().displayTownInfoMenu(player, k.getCapital());
							return;
						}
					}
					break;
				case "town":
					for(KonKingdom k : earth.getKingdomManager().getKingdoms()) {
						if(k.hasTown(infoName)) {
							earth.getDisplayManager().displayTownInfoMenu(player, k.getTown(infoName));
							return;
						}
					}
					break;
				case "ruin":
					if(earth.getRuinManager().isRuin(infoName)) {
						KonRuin ruin = earth.getRuinManager().getRuin(infoName);
						earth.getDisplayManager().displayRuinInfoMenu(player, ruin);
						return;
					}
					break;
				case "sanctuary":
					if(earth.getSanctuaryManager().isSanctuary(infoName)) {
						KonSanctuary sanctuary = earth.getSanctuaryManager().getSanctuary(infoName);
						earth.getDisplayManager().displaySanctuaryInfoMenu(player, sanctuary);
						return;
					}
					break;
				default:
					sendInvalidArgMessage(sender);
					return;
			}
			// None of the types could find a valid name
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(infoName));
		} else {
			// Wrong number of arguments
			sendInvalidArgMessage(sender);
		}
	}
	
	@Override
	public List<String> tabComplete(Earth earth, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		// Give suggestions
		if(args.size() == 1) {
			tabList.add("player");
			tabList.add("kingdom");
			tabList.add("capital");
			tabList.add("town");
			tabList.add("ruin");
			tabList.add("sanctuary");
		} else if(args.size() == 2) {
			String type = args.get(0).toLowerCase();
			switch(type) {
				case "player":
					for(OfflinePlayer bukkitOfflinePlayer : earth.getPlayerManager().getAllOfflinePlayers()) {
						tabList.add(bukkitOfflinePlayer.getName());
					}
					break;
				case "kingdom":
					tabList.addAll(earth.getKingdomManager().getKingdomNames());
					tabList.add(MessagePath.LABEL_BARBARIANS.getMessage());
					break;
				case "capital":
					tabList.addAll(earth.getKingdomManager().getKingdomNames());
					break;
				case "town":
					for(KonKingdom kingdom : earth.getKingdomManager().getKingdoms()) {
						tabList.addAll(kingdom.getTownNames());
					}
					break;
				case "ruin":
					tabList.addAll(earth.getRuinManager().getRuinNames());
					break;
				case "sanctuary":
					tabList.addAll(earth.getSanctuaryManager().getSanctuaryNames());
					break;
				default:
					break;
			}
		}
		return matchLastArgToList(tabList,args);
	}
}
