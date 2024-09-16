package com.github.ssquadteam.earth.command.admin;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.command.CommandBase;
import com.github.ssquadteam.earth.model.*;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TravelAdminCommand  extends CommandBase {
	
	public TravelAdminCommand() {
		// Define name and sender support
		super("travel",true, true);
		// Define arguments
		List<String> argNames = Arrays.asList("town", "kingdom", "camp", "ruin", "sanctuary", "monument");
		// town|kingdom|camp|ruin|sanctuary|monument <name>
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
			sendInvalidSenderMessage(sender);
			return;
		}
		if (args.size() != 2) {
			sendInvalidArgMessage(sender);
			return;
		}
		String destinationName = args.get(1);
		Location destination = null;
		// Parse arguments to get destination location
		switch(args.get(0).toLowerCase()) {
			case "town":
				if (earth.getKingdomManager().isTown(destinationName)) {
					KonTown town = earth.getKingdomManager().getTown(destinationName);
					if (town != null) {
						destination = town.getSpawnLoc();
					}
				}
				break;
			case "kingdom":
				if (earth.getKingdomManager().isKingdom(destinationName)) {
					KonKingdom kingdom = earth.getKingdomManager().getKingdom(destinationName);
					if (kingdom != null) {
						destination = kingdom.getCapital().getSpawnLoc();
					}
				}
				break;
			case "camp":
				KonOfflinePlayer campPlayer = earth.getPlayerManager().getOfflinePlayerFromName(destinationName);
				if (campPlayer != null && earth.getCampManager().isCampSet(campPlayer)) {
					KonCamp camp = earth.getCampManager().getCamp(campPlayer);
					if (camp != null) {
						destination = camp.getSpawnLoc();
					}
				}
				break;
			case "ruin":
				if (earth.getRuinManager().isRuin(destinationName)) {
					KonRuin ruin = earth.getRuinManager().getRuin(destinationName);
					if (ruin != null) {
						destination = ruin.getSpawnLoc();
					}
				}
				break;
			case "sanctuary":
				if (earth.getSanctuaryManager().isSanctuary(destinationName)) {
					KonSanctuary sanctuary = earth.getSanctuaryManager().getSanctuary(destinationName);
					if (sanctuary != null) {
						destination = sanctuary.getSpawnLoc();
					}
				}
				break;
			case "monument":
				if (earth.getSanctuaryManager().isTemplate(destinationName)) {
					KonMonumentTemplate monument = earth.getSanctuaryManager().getTemplate(destinationName);
					if (monument != null) {
						destination = monument.getSpawnLoc();
					}
				}
				break;
			default:
				sendInvalidArgMessage(sender);
				break;
		}
		// Evaluate destination
		if (destination == null) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(destinationName));
			return;
		}
		// Teleport player to destination
		earth.telePlayerLocation(player.getBukkitPlayer(), destination);
    }
    
    @Override
	public List<String> tabComplete(Earth earth, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		if (args.size() == 1) {
			tabList.add("town");
			tabList.add("kingdom");
			tabList.add("camp");
			tabList.add("ruin");
			tabList.add("sanctuary");
			tabList.add("monument");
		} else if (args.size() == 2) {
			switch (args.get(0)) {
				case "town":
					for(KonKingdom kingdom : earth.getKingdomManager().getKingdoms()) {
						tabList.addAll(kingdom.getTownNames());
					}
					break;
				case "kingdom":
					tabList.addAll(earth.getKingdomManager().getKingdomNames());
					break;
				case "camp":
					for(KonCamp camp : earth.getCampManager().getCamps()) {
						tabList.add(camp.getOwner().getName());
					}
					break;
				case "ruin":
					tabList.addAll(earth.getRuinManager().getRuinNames());
					break;
				case "sanctuary":
					tabList.addAll(earth.getSanctuaryManager().getSanctuaryNames());
					break;
				case "monument":
					tabList.addAll(earth.getSanctuaryManager().getAllTemplateNames());
					break;
			}
		}
		return matchLastArgToList(tabList,args);
	}
}
