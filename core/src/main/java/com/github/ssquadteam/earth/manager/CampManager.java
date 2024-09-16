package com.github.ssquadteam.earth.manager;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.api.event.camp.EarthCampCreateEvent;
import com.github.ssquadteam.earth.api.manager.EarthCampManager;
import com.github.ssquadteam.earth.api.model.EarthCamp;
import com.github.ssquadteam.earth.api.model.EarthOfflinePlayer;
import com.github.ssquadteam.earth.model.KonCamp;
import com.github.ssquadteam.earth.model.KonCampGroup;
import com.github.ssquadteam.earth.model.KonOfflinePlayer;
import com.github.ssquadteam.earth.model.KonPlayer;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.CorePath;
import com.github.ssquadteam.earth.utility.HelperUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.data.type.Bed;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.*;

public class CampManager implements EarthCampManager {

	private final Earth earth;
	private final HashMap<String,KonCamp> barbarianCamps; // player uuids to camps
	private final HashMap<KonCamp,KonCampGroup> groupMap; // camps to groups
	private boolean isClanEnabled;
	private boolean isCampDataNull;
	
	public CampManager(Earth earth) {
		this.earth = earth;
		this.barbarianCamps = new HashMap<>();
		this.groupMap = new HashMap<>();
		this.isClanEnabled = false;
		this.isCampDataNull = false;
	}
	
	// intended to be called after database has connected and loaded player tables
	public void initCamps() {
		isClanEnabled = earth.getCore().getBoolean(CorePath.CAMPS_CLAN_ENABLE.getPath(),false);
		loadCamps();
		refreshGroups();
		ChatUtil.printDebug("Loaded camps and groups");
	}
	
	public boolean isCampSet(EarthOfflinePlayer player) {
		String uuid = player.getOfflineBukkitPlayer().getUniqueId().toString();
		return player.isBarbarian() && barbarianCamps.containsKey(uuid);
	}
	
	public KonCamp getCamp(EarthOfflinePlayer player) {
		String uuid = player.getOfflineBukkitPlayer().getUniqueId().toString();
		return getCamp(uuid);
	}
	
	public KonCamp getCamp(String uuid) {
		return barbarianCamps.get(uuid);
	}
	
	public ArrayList<KonCamp> getCamps() {
		return new ArrayList<>(barbarianCamps.values());
	}
	
	public ArrayList<String> getCampNames() {
		ArrayList<String> campNames = new ArrayList<>();
		for(KonCamp camp : barbarianCamps.values()) {
			campNames.add(camp.getName());
		}
		return campNames;
	}
	
	public void activateCampProtection(@Nullable KonOfflinePlayer offlinePlayer) {
		if(offlinePlayer == null)return;
		KonCamp camp = getCamp(offlinePlayer);
		if(camp == null) return;
		ChatUtil.printDebug("Set camp protection to true for player "+offlinePlayer.getOfflineBukkitPlayer().getName());
		camp.setProtected(true);
	}
	
	public void deactivateCampProtection(KonOfflinePlayer offlinePlayer) {
		if(offlinePlayer == null) return;
		KonCamp camp = getCamp(offlinePlayer);
		if(camp == null) return;
		ChatUtil.printDebug("Set camp protection to false for player "+offlinePlayer.getOfflineBukkitPlayer().getName());
		camp.setProtected(false);
	}
	
	/**
	 * addCamp - primary method for adding a camp for a barbarian
	 * @param loc - location of the camp
	 * @param player - player adding the camp
	 * @return status 	0 = success
	 * 					1 = camp init claims overlap with existing territory
	 * 					2 = camp already exists for player
	 * 					3 = player is not a barbarian
	 * 					4 = camps are disabled
	 *                  5 = world is invalid
	 *                  6 = not allowed to join clan group
	 */
	public int addCamp(Location loc, KonOfflinePlayer player) {
		boolean enable = earth.getCore().getBoolean(CorePath.CAMPS_ENABLE.getPath(),true);
		if(!enable) {
			ChatUtil.printDebug("Failed to add camp, feature disabled!");
			return 4;
		}
		if(!earth.isWorldValid(loc)) {
			ChatUtil.printDebug("Failed to add camp, location is in invalid world");
			return 5;
		}
		String uuid = player.getOfflineBukkitPlayer().getUniqueId().toString();
		if(!player.isBarbarian()) {
			ChatUtil.printDebug("Failed to add camp, player "+player.getOfflineBukkitPlayer().getName()+" "+uuid+" is not a barbarian!");
			return 3;
		}
		if(!barbarianCamps.containsKey(uuid)) {
			// Verify no overlapping init chunks
			int radius = earth.getCore().getInt(CorePath.CAMPS_INIT_RADIUS.getPath());
			World addWorld = loc.getWorld();
			for(Point point : HelperUtil.getAreaPoints(loc, radius)) {
				if(earth.getTerritoryManager().isChunkClaimed(point,addWorld)) {
					ChatUtil.printDebug("Found a chunk conflict in camp placement for player "+player.getOfflineBukkitPlayer().getName()+" "+uuid);
					return 1;
				}
			}
			// Verify camp group (clan) allowed placement
			boolean isOfflineJoinAllow = earth.getCore().getBoolean(CorePath.CAMPS_CLAN_ALLOW_JOIN_OFFLINE.getPath());
			if(isClanEnabled) {
				// Search surroundings for any adjacent camps with online members
				boolean isAdjacentCampPresent = false;
				boolean isAnyAdjacentOwnerOnline = false;
				for(Point point : HelperUtil.getBorderPoints(loc, radius+1)) {
					if(earth.getTerritoryManager().isChunkClaimed(point,loc.getWorld()) && earth.getTerritoryManager().getChunkTerritory(point,loc.getWorld()) instanceof KonCamp) {
						KonCamp adjCamp = (KonCamp)earth.getTerritoryManager().getChunkTerritory(point,loc.getWorld());
						isAdjacentCampPresent = true;
						if(adjCamp != null && adjCamp.isOwnerOnline()) {
							isAnyAdjacentOwnerOnline = true;
						}
					}
				}
				// Check if any adjacent camp has online members
				if(isAdjacentCampPresent && !isAnyAdjacentOwnerOnline && !isOfflineJoinAllow) {
					// Prevent this camp from being placed, there are no online owners in the adjacent camps
					ChatUtil.printDebug("Failed to add camp, no adjacent camps have online members.");
					return 6;
				}
			}
			// Attempt to add the camp
			KonCamp newCamp = new KonCamp(loc,player.getOfflineBukkitPlayer(),earth.getKingdomManager().getBarbarians(),earth);
			barbarianCamps.put(uuid,newCamp);
			newCamp.initClaim();
			// Update bar players
			newCamp.updateBarPlayers();
			//update the chunk cache, add points to primary world cache
			earth.getTerritoryManager().addAllTerritory(loc.getWorld(),newCamp.getChunkList());
			// Refresh groups
			refreshGroups();
			if(groupMap.containsKey(newCamp)) {
				Earth.playCampGroupSound(loc);
				// Notify group
				for(KonCamp groupCamp : groupMap.get(newCamp).getCamps()) {
					if(groupCamp.isOwnerOnline()) {
						KonPlayer ownerOnlinePlayer = earth.getPlayerManager().getPlayerFromID(groupCamp.getOwner().getUniqueId());
						if(ownerOnlinePlayer != null) {
							ChatUtil.sendNotice(ownerOnlinePlayer.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_CAMP_CLAN_ADD.getMessage(newCamp.getName()));
						}
					}
				}
			}
			earth.getMapHandler().drawUpdateTerritory(newCamp);
		} else {
			return 2;
		}
		return 0;
	}

	// Return false to cancel placing a bed
	public boolean addCampForPlayer(Location loc, KonPlayer player) {
		Player bukkitPlayer = player.getBukkitPlayer();
		// Check for other plugin flags
		if(earth.getIntegrationManager().getWorldGuard().isEnabled()) {
			// Check new territory claims
			int radius = earth.getCore().getInt(CorePath.CAMPS_INIT_RADIUS.getPath());
			World locWorld = loc.getWorld();
			for(Point point : HelperUtil.getAreaPoints(loc, radius)) {
				if(!earth.getIntegrationManager().getWorldGuard().isChunkClaimAllowed(locWorld,point,bukkitPlayer)) {
					// A region is denying this action
					ChatUtil.sendError(bukkitPlayer, MessagePath.REGION_ERROR_CLAIM_DENY.getMessage());
					return false;
				}
			}
		}
		// Try to add the camp
		int status = addCamp(loc, player);
		if(status == 0) { // on successful camp setup...
			KonCamp newCamp = getCamp(player);
			if(newCamp != null) {
				// Fire event
				EarthCampCreateEvent invokeEvent = new EarthCampCreateEvent(earth, newCamp, player);
				Earth.callEarthEvent(invokeEvent);
				// Post-camp setup
				player.getBukkitPlayer().setBedSpawnLocation(loc, true);
				ChatUtil.sendKonTitle(player, "", Earth.barbarianColor2+newCamp.getName());
			}
			ChatUtil.sendNotice(bukkitPlayer, MessagePath.PROTECTION_NOTICE_CAMP_CREATE.getMessage());
		} else {
			switch(status) {
				case 1:
					ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_CAMP_FAIL_OVERLAP.getMessage());
					return false;
				case 2:
					ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_CAMP_CREATE.getMessage());
					return false;
				case 3:
					ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_CAMP_FAIL_BARBARIAN.getMessage());
					return false;
				case 4:
					// This error message is removed because it could be annoying to see it every time a bed is placed when camps are disabled.
					break;
				case 5:
					// This error message is removed because it could be annoying to see it every time a bed is placed in an invalid world.
					break;
				case 6:
					ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_CAMP_FAIL_OFFLINE.getMessage());
					break;
				default:
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
					break;
			}
		}
		return true;
	}
	
	public boolean removeCamp(EarthOfflinePlayer player) {
		String uuid = player.getOfflineBukkitPlayer().getUniqueId().toString();
		return removeCamp(uuid);
	}
	
	public boolean removeCamp(KonCamp camp) {
		String uuid = camp.getOwner().getUniqueId().toString();
		return removeCamp(uuid);
	}
	
	public boolean removeCamp(String uuid) {
		if(barbarianCamps.containsKey(uuid)) {
			ArrayList<Point> campPoints = new ArrayList<>(barbarianCamps.get(uuid).getChunkList().keySet());
			earth.getShopHandler().deleteShopsInPoints(campPoints,barbarianCamps.get(uuid).getWorld());
			KonCamp removedCamp = barbarianCamps.remove(uuid);
			removedCamp.removeAllBarPlayers();
			// Ensure bed is broken
			if(removedCamp.getBedLocation().getBlock().getBlockData() instanceof Bed) {
				removedCamp.getBedLocation().getBlock().breakNaturally();
			}
			//update the chunk cache, remove all points from primary world
			earth.getTerritoryManager().removeAllTerritory(removedCamp.getWorld(),removedCamp.getChunkList().keySet());
			// Refresh groups
			Collection<KonCamp> groupSet = new ArrayList<>();
			if(groupMap.containsKey(removedCamp)) {
				groupSet = groupMap.get(removedCamp).getCamps();
			}
			refreshGroups();
			// Notify group
			for(KonCamp groupCamp : groupSet) {
				if(groupCamp.isOwnerOnline()) {
					KonPlayer ownerOnlinePlayer = earth.getPlayerManager().getPlayerFromID(groupCamp.getOwner().getUniqueId());
					if(ownerOnlinePlayer != null) {
						ChatUtil.sendNotice(ownerOnlinePlayer.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_CAMP_CLAN_REMOVE.getMessage(removedCamp.getName()));
					}
				}
			}
			earth.getMapHandler().drawRemoveTerritory(removedCamp);
			removedCamp = null;
		} else {
			ChatUtil.printDebug("Failed to remove camp for missing UUID "+uuid);
			return false;
		}
		ChatUtil.printDebug("Successfully removed camp for UUID "+uuid);
		return true;
	}
	
	public boolean isCampGrouped(EarthCamp camp) {
		return groupMap.containsKey(camp);
	}
	
	public KonCampGroup getCampGroup(EarthCamp camp) {
		return groupMap.get(camp);
	}
	
	public boolean isCampGroupsEnabled() {
		return isClanEnabled;
	}

	private void refreshGroups() {
		groupMap.clear();
		if(!isClanEnabled) return;
		int totalGroups = 0;
		int radius = earth.getCore().getInt(CorePath.CAMPS_INIT_RADIUS.getPath());
		Location center;
		// Evaluate each camp for adjacent camps, creating groups as necessary
		for(KonCamp currCamp : barbarianCamps.values()) {
			// Verify camp is not already in a group
			if(!groupMap.containsKey(currCamp)) {
				center = currCamp.getCenterLoc();
				// Search surroundings for all adjacent camps
				HashSet<KonCamp> adjCamps = new HashSet<>();
				for(Point point : HelperUtil.getBorderPoints(center, radius+1)) {
					if(earth.getTerritoryManager().isChunkClaimed(point,center.getWorld()) && earth.getTerritoryManager().getChunkTerritory(point,center.getWorld()) instanceof KonCamp) {
						KonCamp adjCamp = (KonCamp)earth.getTerritoryManager().getChunkTerritory(point,center.getWorld());
						adjCamps.add(adjCamp);
					}
				}
				// Find any existing groups
				HashSet<KonCampGroup> adjGroups = new HashSet<>();
				for(KonCamp adjCamp : adjCamps) {
					if(groupMap.containsKey(adjCamp)) {
						adjGroups.add(groupMap.get(adjCamp));
					}
				}
				// Attempt to form groups
				if(adjGroups.isEmpty()) {
					// There are no adjacent groups, try to make one
					if(!adjCamps.isEmpty()) {
						// Make a new group with the adjacent camp(s)
						KonCampGroup campGroup = new KonCampGroup();
						campGroup.addCamp(currCamp);
						groupMap.put(currCamp, campGroup);
						for(KonCamp adjCamp : adjCamps) {
							campGroup.addCamp(adjCamp);
							groupMap.put(adjCamp, campGroup);
						}
						//barbarianGroups.add(campGroup);
						totalGroups++;
					}
				} else {
					// There are other adjacent group(s)
					Iterator<KonCampGroup> groupIter = adjGroups.iterator();
					if(groupIter.hasNext()) {
						KonCampGroup mainGroup = groupIter.next(); // choose arbitrary group as main
						// Add camp and all adjacent camps to main group
						mainGroup.addCamp(currCamp);
						groupMap.put(currCamp, mainGroup);
						for(KonCamp adjCamp : adjCamps) {
							mainGroup.addCamp(adjCamp);
							groupMap.put(adjCamp, mainGroup);
						}
						// Merge other group(s) into main and prune
						while(groupIter.hasNext()) {
							KonCampGroup mergeGroup = groupIter.next();
							mainGroup.mergeGroup(mergeGroup);
							mergeGroup.clearCamps();
							//barbarianGroups.remove(mergeGroup);
							totalGroups--;
						}
					}
				}
			}
		}
		ChatUtil.printDebug("Refreshed "+totalGroups+" camp groups");
	}
	
	private void loadCamps() {
		boolean enable = earth.getCore().getBoolean(CorePath.CAMPS_ENABLE.getPath(),true);
		if(!enable) {
			ChatUtil.printConsoleAlert("Disabled barbarian camps");
			return;
		}
		FileConfiguration campsConfig = earth.getConfigManager().getConfig("camps");
        if (campsConfig.get("camps") == null) {
			ChatUtil.printConsoleError("Failed to load any camps from camps.yml! Check file permissions.");
			isCampDataNull = true;
            return;
        }
        ConfigurationSection campsSection = campsConfig.getConfigurationSection("camps");
        Set<String> playerSet = campsSection.getKeys(false);
        double x,y,z;
        List<Double> sectionList;
        String worldName;
        String defaultWorldName = earth.getCore().getString(CorePath.WORLD_NAME.getPath(),"world");
        int totalCamps = 0;
        for(String uuid : playerSet) {
        	if(campsSection.contains(uuid)) {
        		OfflinePlayer offlineBukkitPlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        		//ChatUtil.printDebug("Adding camp for player "+offlineBukkitPlayer.getName());
        		if(earth.getPlayerManager().isOfflinePlayer(offlineBukkitPlayer)) {
        			KonOfflinePlayer offlinePlayer = earth.getPlayerManager().getOfflinePlayer(offlineBukkitPlayer);
        			// Add stored camp
            		ConfigurationSection playerCampSection = campsSection.getConfigurationSection(uuid);
            		worldName = playerCampSection.getString("world",defaultWorldName);
            		sectionList = playerCampSection.getDoubleList("center");
            		x = sectionList.get(0);
            		y = sectionList.get(1);
            		z = sectionList.get(2);
            		World world = Bukkit.getWorld(worldName);
                	if(world != null) {
                		// Create the camp
	            		Location camp_center = new Location(world,x,y,z);
	            		int status = addCamp(camp_center, offlinePlayer);
	            		if(status == 0) {
		            		getCamp(offlinePlayer).addPoints(HelperUtil.formatStringToPoints(playerCampSection.getString("chunks")));
		            		earth.getTerritoryManager().addAllTerritory(world,getCamp(offlinePlayer).getChunkList());
		            		totalCamps++;
	            		} else {
	            			ChatUtil.printDebug("Failed to add camp for player "+offlineBukkitPlayer.getName()+", error code: "+status);
	            		}
                	} else {
                		String message = "Failed to load camp for player "+offlineBukkitPlayer.getName()+" in an unloaded world, "+worldName+". Check plugin load order.";
            			ChatUtil.printConsoleError(message);
            			earth.opStatusMessages.add(message);
                	}
        		} else {
					ChatUtil.printDebug("Failed to find player " + offlineBukkitPlayer.getName() + " when adding their camp");
				}
        	}
        }
        ChatUtil.printDebug("Updated all camps from camps.yml, total "+totalCamps);
    }
	
	public void saveCamps() {
		if(isCampDataNull && barbarianCamps.isEmpty()) {
			// There was probably an issue loading camps, do not save.
			ChatUtil.printConsoleError("Aborted saving camp data because a problem was encountered while loading data from camps.yml");
			return;
		}
		FileConfiguration campsConfig = earth.getConfigManager().getConfig("camps");
		campsConfig.set("camps", null); // reset camps config
		ConfigurationSection root = campsConfig.createSection("camps");
		for(String uuid : barbarianCamps.keySet()) {
			KonCamp camp = barbarianCamps.get(uuid);
			ConfigurationSection campSection = root.createSection(uuid);
			campSection.set("world", camp.getWorld().getName());
			campSection.set("center", new int[] {camp.getCenterLoc().getBlockX(),
					camp.getCenterLoc().getBlockY(),
					camp.getCenterLoc().getBlockZ()});
	        campSection.set("chunks", HelperUtil.formatPointsToString(camp.getChunkList().keySet()));
		}
	}
}
