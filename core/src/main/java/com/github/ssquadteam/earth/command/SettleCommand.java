package com.github.ssquadteam.earth.command;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.EarthPlugin;
import com.github.ssquadteam.earth.api.event.player.EarthPlayerSettleEvent;
import com.github.ssquadteam.earth.api.event.town.EarthTownSettleEvent;
import com.github.ssquadteam.earth.model.*;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.CorePath;
import com.github.ssquadteam.earth.utility.HelperUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SettleCommand extends CommandBase {

	public SettleCommand() {
		// Define name and sender support
		super("settle",true, false);
		// name
		addArgument(
				newArg("name",false,false)
		);
    }
	
	public void execute(Earth earth, CommandSender sender, List<String> args) {
		// Sender must be player
		KonPlayer player = earth.getPlayerManager().getPlayer(sender);
		if (player == null) {
			sendInvalidSenderMessage(sender);
			return;
		}
		// Parse arguments
		if (args.isEmpty()) {
            ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_MISSING_NAME.getMessage());
		} else if (args.size() > 1) {
           ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_SPACE_NAME.getMessage());
		} else {
			// Single argument
        	if(player.isBarbarian()) {
        		ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
                return;
        	}
			Player bukkitPlayer = player.getBukkitPlayer();
			// Check for permission
			if(!bukkitPlayer.hasPermission("earth.create.town")) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_PERMISSION.getMessage()+" earth.create.town");
				return;
			}
			// Check for other plugin flags
			if(earth.getIntegrationManager().getWorldGuard().isEnabled()) {
				// Check new territory claims
				Location settleLoc = bukkitPlayer.getLocation();
				int radius = earth.getCore().getInt(CorePath.TOWNS_INIT_RADIUS.getPath());
				World locWorld = settleLoc.getWorld();
				for(Point point : HelperUtil.getAreaPoints(settleLoc, radius)) {
					if(!earth.getIntegrationManager().getWorldGuard().isChunkClaimAllowed(locWorld,point,bukkitPlayer)) {
						// A region is denying this action
						ChatUtil.sendError(bukkitPlayer, MessagePath.REGION_ERROR_CLAIM_DENY.getMessage());
						return;
					}
				}
			}
			// Check max town limit
			KonKingdom settleKingdom = player.getKingdom();
			World settleWorld = bukkitPlayer.getLocation().getWorld();
			if(settleWorld != null) {
				boolean isPerWorld = earth.getCore().getBoolean(CorePath.KINGDOMS_MAX_TOWN_LIMIT_PER_WORLD.getPath(),false);
				int maxTownLimit = earth.getCore().getInt(CorePath.KINGDOMS_MAX_TOWN_LIMIT.getPath(),0);
				maxTownLimit = Math.max(maxTownLimit,0); // clamp to 0 minimum
				if(maxTownLimit != 0) {
					int numTownsInWorld = 0;
					if(isPerWorld) {
						// Find towns within the given world
						for(KonTown town : settleKingdom.getCapitalTowns()) {
							if(town.getWorld().equals(settleWorld)) {
								numTownsInWorld++;
							}
						}
					} else {
						// Find all towns
						numTownsInWorld = settleKingdom.getCapitalTowns().size();
					}
					if(numTownsInWorld >= maxTownLimit) {
						// Limit reached
						if(isPerWorld) {
							ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_FAIL_LIMIT_WORLD.getMessage(numTownsInWorld,maxTownLimit));
						} else {
							ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_FAIL_LIMIT_ALL.getMessage(numTownsInWorld,maxTownLimit));
						}
						return;
					}
				}
			}
			// Check officer only
			boolean isOfficerOnly = earth.getCore().getBoolean(CorePath.TOWNS_SETTLE_OFFICER_ONLY.getPath(),false);
			if(isOfficerOnly && !settleKingdom.isOfficer(bukkitPlayer.getUniqueId())) {
				// Player is not an officer and must be in order to settle
				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_OFFICER_ONLY.getMessage());
				return;
			}

			double cost = earth.getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SETTLE.getPath());
        	double incr = earth.getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SETTLE_INCREMENT.getPath());
			boolean isIncrementKingdom = earth.getCore().getBoolean(CorePath.TOWNS_SETTLE_INCREMENT_KINGDOM.getPath(),false);
			int townCount;
			if (isIncrementKingdom) {
				// All kingdom towns
				townCount = player.getKingdom().getNumTowns();
			} else {
				// Towns that have the player as the lord
				townCount = earth.getKingdomManager().getPlayerLordships(player);
			}
        	double adj_cost = (((double)townCount)*incr) + cost;
        	if(cost > 0) {
	        	if(EarthPlugin.getBalance(bukkitPlayer) < adj_cost) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(adj_cost));
	                return;
				}
        	}
        	String townName = args.get(0);
        	
        	if(earth.validateName(townName,bukkitPlayer) != 0) {
        		// sends player message within the method
        		return;
        	}
        	// Fire pre event
        	EarthPlayerSettleEvent invokeEvent = new EarthPlayerSettleEvent(earth, player, player.getKingdom(), bukkitPlayer.getLocation(), townName);
			Earth.callEarthEvent(invokeEvent);
			if(invokeEvent.isCancelled()) {
				return;
			}
        	// Add town
        	int settleStatus = earth.getKingdomManager().createTown(bukkitPlayer.getLocation(), townName, player.getKingdom().getName());
        	if(settleStatus == 0) { // on successful settle..
        		KonTown town = player.getKingdom().getTown(townName);
        		// Teleport player to safe place around monument, facing monument
        		earth.getKingdomManager().teleportAwayFromCenter(town);
				// Send messages
        		ChatUtil.sendNotice(sender, MessagePath.COMMAND_SETTLE_NOTICE_SUCCESS.getMessage(townName));
        		ChatUtil.sendBroadcast(MessagePath.COMMAND_SETTLE_BROADCAST_SETTLE.getMessage(bukkitPlayer.getName(),townName,player.getKingdom().getName()));
        		// Optionally apply starter shield
        		int starterShieldDuration = earth.getCore().getInt(CorePath.TOWNS_SHIELD_NEW_TOWNS.getPath(),0);
        		if(starterShieldDuration > 0) {
        			earth.getShieldManager().shieldSet(town,starterShieldDuration);
        		}
        		// Play a success sound
				Earth.playTownSettleSound(bukkitPlayer.getLocation());
				// Set player as Lord
        		town.setPlayerLord(player.getOfflineBukkitPlayer());
        		// Update directive progress
        		earth.getDirectiveManager().updateDirectiveProgress(player, KonDirective.SETTLE_TOWN);
        		// Update stats
        		earth.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.SETTLED,1);
        		earth.getKingdomManager().updatePlayerMembershipStats(player);
        		// Update labels
        		earth.getMapHandler().drawLabel(town);
        		earth.getMapHandler().drawLabel(town.getKingdom().getCapital());
        		
        		// Fire post event
        		EarthTownSettleEvent invokePostEvent = new EarthTownSettleEvent(earth, town, player, town.getKingdom());
    			Earth.callEarthEvent(invokePostEvent);
        	} else {
        		int distance;
        		switch(settleStatus) {
        		case 1:
        			ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_OVERLAP.getMessage());
					ChatUtil.sendNotice(sender, MessagePath.COMMAND_SETTLE_NOTICE_MAP_HINT.getMessage());
        			break;
        		case 2:
        			ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_PLACEMENT.getMessage());
					ChatUtil.sendNotice(sender, MessagePath.COMMAND_SETTLE_NOTICE_MAP_HINT.getMessage());
        			break;
        		case 3:
        			ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_NAME.getMessage());
        			break;
        		case 4:
        			ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_TEMPLATE.getMessage());
        			break;
        		case 5:
        			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
        			break;
        		case 6:
        			distance = earth.getTerritoryManager().getDistanceToClosestTerritory(bukkitPlayer.getLocation());
        			int min_distance_sanc = earth.getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_SANCTUARY.getPath());
        			int min_distance_town = earth.getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_TOWN.getPath());
        			int min_distance = Math.min(min_distance_sanc, min_distance_town);
					ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_PROXIMITY.getMessage(distance,min_distance));
					ChatUtil.sendNotice(sender, MessagePath.COMMAND_SETTLE_NOTICE_MAP_HINT.getMessage());
        			break;
        		case 7:
        			distance = earth.getTerritoryManager().getDistanceToClosestTerritory(bukkitPlayer.getLocation());
        			int max_distance_all = earth.getCore().getInt(CorePath.TOWNS_MAX_DISTANCE_ALL.getPath());
        			ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_MAX.getMessage(distance,max_distance_all));
					ChatUtil.sendNotice(sender, MessagePath.COMMAND_SETTLE_NOTICE_MAP_HINT.getMessage());
        			break;
        		case 21:
        			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
        			break;
        		case 22:
        			ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_FLAT.getMessage());
        			break;
        		case 23:
        			ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_HEIGHT.getMessage());
        			break;
        		case 12:
        			ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_HEIGHT.getMessage());
        			break;
        		case 13:
        			ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_INIT.getMessage());
        			break;
        		case 14:
        			ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_AIR.getMessage());
        			break;
        		case 15:
        			ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_WATER.getMessage());
        			break;
        		case 16:
        			ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_CONTAINER.getMessage());
        			break;
        		default:
        			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
        			break;
        		}
        	}
        	
			if(cost > 0 && settleStatus == 0) {
	            if(EarthPlugin.withdrawPlayer(bukkitPlayer, adj_cost)) {
	            	earth.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)cost);
	            }
			}
        }
	}
	
	@Override
	public List<String> tabComplete(Earth earth, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		// Give suggestions
		if(args.size() == 1) {
			tabList.add("***");
		}
		return matchLastArgToList(tabList,args);
	}
}
