package com.github.ssquadteam.earth.command;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.EarthPlugin;
import com.github.ssquadteam.earth.api.model.EarthTerritoryType;
import com.github.ssquadteam.earth.manager.KingdomManager;
import com.github.ssquadteam.earth.manager.TravelManager.TravelDestination;
import com.github.ssquadteam.earth.model.*;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.CorePath;
import com.github.ssquadteam.earth.utility.MessagePath;
import com.github.ssquadteam.earth.utility.RandomWildLocationSearchTask;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TravelCommand extends CommandBase {
	
	public TravelCommand() {
		// Define name and sender support
		super("travel",true, false);
		// town <town>
		addArgument(
				newArg("town", true, false)
						.sub( newArg("town", false, false) )
		);
		// kingdom <kingdom>
		addArgument(
				newArg("kingdom", true, false)
						.sub( newArg("kingdom", false, false) )
		);
		// sanctuary <sanctuary>
		addArgument(
				newArg("sanctuary", true, false)
						.sub( newArg("sanctuary", false, false) )
		);
		// capital|home|camp|wild
		List<String> argNames = Arrays.asList("capital", "home", "camp", "wild");
		addArgument(
				newArg(argNames,true,false)
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
		KingdomManager kManager = earth.getKingdomManager();
		Player bukkitPlayer = player.getBukkitPlayer();
		World bukkitWorld = bukkitPlayer.getWorld();
		if(!earth.isWorldValid(bukkitWorld)) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
			return;
		}

		// Verify player is not in enemy territory
		boolean blockEnemyTravel = earth.getCore().getBoolean(CorePath.KINGDOMS_NO_ENEMY_TRAVEL.getPath());
		boolean blockCampTravel = earth.getCore().getBoolean(CorePath.CAMPS_NO_ENEMY_TRAVEL.getPath());
		Location playerLoc = bukkitPlayer.getLocation();
		if(earth.getTerritoryManager().isChunkClaimed(playerLoc)) {
			KonTerritory locTerritory = earth.getTerritoryManager().getChunkTerritory(playerLoc);
			assert locTerritory != null;
			KonKingdom locKingdom = locTerritory.getKingdom();
			boolean isKingdomTravelAllowed = locTerritory.getTerritoryType().equals(EarthTerritoryType.SANCTUARY) ||
					locKingdom.equals(player.getKingdom()) ||
					kManager.isPlayerPeace(player, locKingdom) ||
					kManager.isPlayerTrade(player, locKingdom) ||
					kManager.isPlayerAlly(player, locKingdom);
			boolean isCampTravelBlocked = (locTerritory instanceof KonCamp) && !((KonCamp)locTerritory).isPlayerOwner(bukkitPlayer);
			if((blockEnemyTravel && !isKingdomTravelAllowed) || (blockCampTravel && isCampTravelBlocked)) {
				ChatUtil.sendError(sender, MessagePath.COMMAND_TRAVEL_ERROR_ENEMY_TERRITORY.getMessage());
				return;
			}
		}

		// Determine travel destination and location
		if (args.isEmpty()) {
			sendInvalidArgMessage(sender);
			return;
		}
		String travelType = args.get(0);
		Location travelLoc = null;
		TravelDestination destination;
		KonTerritory travelTerritory = null;
		boolean isSanctuaryTravel = earth.getCore().getBoolean(CorePath.TRAVEL_ENABLE_SANCTUARY.getPath(),false);
		boolean isCapitalTravel = earth.getCore().getBoolean(CorePath.TRAVEL_ENABLE_CAPITAL.getPath(),false);
		boolean isCampTravel = earth.getCore().getBoolean(CorePath.TRAVEL_ENABLE_CAMP.getPath(),false);
		boolean isHomeTravel = earth.getCore().getBoolean(CorePath.TRAVEL_ENABLE_HOME.getPath(),false);
		boolean isWildTravel = earth.getCore().getBoolean(CorePath.TRAVEL_ENABLE_WILD.getPath(),false);
		boolean isTownTravel = earth.getCore().getBoolean(CorePath.TRAVEL_ENABLE_TOWNS.getPath(),false);
		// Parse arguments
		switch (travelType.toLowerCase()) {
			case "capital":
				// Travel to player's own capital
				if(player.isBarbarian()) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
					return;
				}
				if(!isCapitalTravel) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
					return;
				}
				KonCapital travelCapital = player.getKingdom().getCapital();
				if(travelCapital == null) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
					return;
				}
				// Check for property flag
				if(!travelCapital.getPropertyValue(KonPropertyFlag.TRAVEL)) {
					ChatUtil.sendKonBlockedFlagTitle(player);
					ChatUtil.sendError(sender, MessagePath.COMMAND_TRAVEL_ERROR_DISABLED.getMessage());
					return;
				}
				travelLoc = travelCapital.getSpawnLoc();
				destination = TravelDestination.CAPITAL;
				travelTerritory = travelCapital;
				break;
			case "camp":
				// Travel to player's own camp
				if(!player.isBarbarian()) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
					return;
				}
				if(!earth.getCampManager().isCampSet(player)) {
					ChatUtil.sendError(sender, MessagePath.COMMAND_TRAVEL_ERROR_NO_CAMP.getMessage());
					return;
				}
				if(!isCampTravel) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
					return;
				}
				KonCamp travelCamp = earth.getCampManager().getCamp(player);
				travelLoc = travelCamp.getSpawnLoc();
				destination = TravelDestination.CAMP;
				travelTerritory = travelCamp;
				break;
			case "home":
				// Travel to player's own bed home
				if(player.isBarbarian()) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
					return;
				}
				Location bedLoc = player.getBukkitPlayer().getBedSpawnLocation();
				if(bedLoc == null) {
					ChatUtil.sendError(sender, MessagePath.COMMAND_TRAVEL_ERROR_NO_HOME.getMessage());
					return;
				}
				if(!isHomeTravel) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
					return;
				}
				travelLoc = bedLoc;
				destination = TravelDestination.HOME;
				break;
			case "wild":
				// Travel to random wild location
				if(!isWildTravel) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
					return;
				}
				ChatUtil.sendNotice(sender, MessagePath.COMMAND_TRAVEL_NOTICE_WILD_SEARCH.getMessage());
				destination = TravelDestination.WILD;
				// Wild travel is a little different.
				// It takes a lot of effort to load random chunks and find a valid travel location,
				// too much effort for 1 tick. So the search will take place in a separate thread
				// over multiple ticks. When the task is finished, use a lamba expression to submit the travel.
				RandomWildLocationSearchTask task = new RandomWildLocationSearchTask(earth,bukkitWorld,location -> {
					if(location == null) {
						ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TRAVEL_ERROR_WILD_TIMEOUT.getMessage());
					} else {
						earth.getTravelManager().submitTravel(bukkitPlayer, destination, null, location);
					}
				});
				// schedule the task to run once every 20 ticks (20 ticks per second)
				task.runTaskTimer(earth.getPlugin(), 0L, 20L);
				break;
			case "sanctuary":
				if (args.size() != 2) {
					sendInvalidArgMessage(sender);
					return;
				}
				if(!isSanctuaryTravel) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
					return;
				}
				// Get sanctuary name
				String sanctuaryName = args.get(1);
				if(earth.getSanctuaryManager().isSanctuary(sanctuaryName)) {
					// Name is a sanctuary
					KonSanctuary travelSanctuary = earth.getSanctuaryManager().getSanctuary(sanctuaryName);
					if(travelSanctuary == null) {
						ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
						return;
					}
					// Check for property flag
					if(!travelSanctuary.getPropertyValue(KonPropertyFlag.TRAVEL)) {
						ChatUtil.sendKonBlockedFlagTitle(player);
						ChatUtil.sendError(sender, MessagePath.COMMAND_TRAVEL_ERROR_DISABLED.getMessage());
						return;
					}
					travelLoc = travelSanctuary.getSpawnLoc();
					destination = TravelDestination.SANCTUARY;
					travelTerritory = travelSanctuary;
				} else {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(sanctuaryName));
					return;
				}
				break;
			case "kingdom":
				if (args.size() != 2) {
					sendInvalidArgMessage(sender);
					return;
				}
				if(!isCapitalTravel) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
					return;
				}
				// Get kingdom name
				String kingdomName = args.get(1);
				if(earth.getKingdomManager().isKingdom(kingdomName)) {
					// Name is a kingdom
					if(player.isBarbarian()) {
						ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
						return;
					}
					KonCapital travelKingdom = earth.getKingdomManager().getCapital(kingdomName);
					if(travelKingdom == null) {
						ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
						return;
					}
					// Check for player's own kingdom or allied kingdom
					if(!player.getKingdom().equals(travelKingdom.getKingdom()) && !earth.getKingdomManager().isPlayerAlly(player,travelKingdom.getKingdom())) {
						ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
						return;
					}
					// Check for property flag
					if(!travelKingdom.getPropertyValue(KonPropertyFlag.TRAVEL)) {
						ChatUtil.sendKonBlockedFlagTitle(player);
						ChatUtil.sendError(sender, MessagePath.COMMAND_TRAVEL_ERROR_DISABLED.getMessage());
						return;
					}
					travelLoc = travelKingdom.getSpawnLoc();
					destination = TravelDestination.CAPITAL;
					travelTerritory = travelKingdom;
				} else {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
					return;
				}
				break;
			case "town":
				if (args.size() != 2) {
					sendInvalidArgMessage(sender);
					return;
				}
				if(!isTownTravel) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
					return;
				}
				// Get town name
				String townName = args.get(1);
				if(earth.getKingdomManager().isTown(townName)) {
					// Name is a town
					if(player.isBarbarian()) {
						ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
						return;
					}
					KonTown travelTown = earth.getKingdomManager().getTown(townName);
					if(travelTown == null) {
						ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
						return;
					}
					// Check for player's own town or allied town
					if(!player.getKingdom().equals(travelTown.getKingdom()) && !earth.getKingdomManager().isPlayerAlly(player,travelTown.getKingdom())) {
						ChatUtil.sendError(sender, MessagePath.COMMAND_TRAVEL_ERROR_NO_TOWN.getMessage());
						return;
					}
					// Check for property flag
					if(!travelTown.getPropertyValue(KonPropertyFlag.TRAVEL)) {
						ChatUtil.sendKonBlockedFlagTitle(player);
						ChatUtil.sendError(sender, MessagePath.COMMAND_TRAVEL_ERROR_DISABLED.getMessage());
						return;
					}
					// Check town travel cooldown
					if(travelTown.isPlayerTravelDisabled(bukkitPlayer.getUniqueId())) {
						String cooldown = travelTown.getPlayerTravelCooldownString(bukkitPlayer.getUniqueId());
						ChatUtil.sendError(sender, MessagePath.COMMAND_TRAVEL_ERROR_COOLDOWN.getMessage(cooldown,travelTown.getName()));
						return;
					}
					travelLoc = travelTown.getSpawnLoc();
					destination = TravelDestination.TOWN;
					travelTerritory = travelTown;
				} else {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(townName));
					return;
				}
				break;
			default:
				sendInvalidArgMessage(sender);
				return;
		}

		// Submit the travel to the manager. It will take things from here.
		earth.getTravelManager().submitTravel(bukkitPlayer, destination, travelTerritory, travelLoc);
	}
	
	@Override
	public List<String> tabComplete(Earth earth, CommandSender sender, List<String> args) {
		// Sender must be player
		KonPlayer player = earth.getPlayerManager().getPlayer(sender);
		if (player == null) return Collections.emptyList();
		List<String> tabList = new ArrayList<>();

		boolean isSanctuaryTravel = earth.getCore().getBoolean(CorePath.TRAVEL_ENABLE_SANCTUARY.getPath(),false);
		boolean isCapitalTravel = earth.getCore().getBoolean(CorePath.TRAVEL_ENABLE_CAPITAL.getPath(),false);
		boolean isTownTravel = earth.getCore().getBoolean(CorePath.TRAVEL_ENABLE_TOWNS.getPath(),false);
		boolean isHomeTravel = earth.getCore().getBoolean(CorePath.TRAVEL_ENABLE_HOME.getPath(),false);
		boolean isCampTravel = earth.getCore().getBoolean(CorePath.TRAVEL_ENABLE_CAMP.getPath(),false);
		boolean isWildTravel = earth.getCore().getBoolean(CorePath.TRAVEL_ENABLE_WILD.getPath(),false);

		if (args.size() == 1) {
			if (isWildTravel) {
				tabList.add("wild");
			}
			if (isSanctuaryTravel) {
				tabList.add("sanctuary");
			}
			if (player.isBarbarian() && isCampTravel) {
				tabList.add("camp");
			}
			if (!player.isBarbarian() && isHomeTravel) {
				tabList.add("home");
			}
			if (!player.isBarbarian() && isCapitalTravel) {
				tabList.add("capital");
				tabList.add("kingdom");
			}
			if (!player.isBarbarian() && isTownTravel) {
				tabList.add("town");
			}
		} else if (args.size() == 2) {
			String travelType = args.get(0);
			if (isSanctuaryTravel && travelType.equalsIgnoreCase("sanctuary")) {
				tabList.addAll(earth.getSanctuaryManager().getSanctuaryNames());
			} else if (isCapitalTravel && travelType.equalsIgnoreCase("kingdom")) {
				for(KonKingdom kingdom : earth.getKingdomManager().getKingdoms()) {
					if(player.getKingdom().equals(kingdom) || earth.getKingdomManager().isKingdomAlliance(player.getKingdom(),kingdom)) {
						tabList.add(kingdom.getName());
					}
				}
			} else if (isTownTravel && travelType.equalsIgnoreCase("town")) {
				for(KonKingdom kingdom : earth.getKingdomManager().getKingdoms()) {
					if(player.getKingdom().equals(kingdom) || earth.getKingdomManager().isKingdomAlliance(player.getKingdom(),kingdom)) {
						tabList.addAll(kingdom.getTownNames());
					}
				}
			}
		}

		return matchLastArgToList(tabList,args);
	}
}
