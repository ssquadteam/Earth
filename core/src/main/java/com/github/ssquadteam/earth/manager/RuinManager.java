package com.github.ssquadteam.earth.manager;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.EarthPlugin;
import com.github.ssquadteam.earth.api.manager.EarthRuinManager;
import com.github.ssquadteam.earth.api.model.EarthRuin;
import com.github.ssquadteam.earth.model.KonKingdom;
import com.github.ssquadteam.earth.model.KonPlayer;
import com.github.ssquadteam.earth.model.KonPropertyFlag;
import com.github.ssquadteam.earth.model.KonRuin;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.CorePath;
import com.github.ssquadteam.earth.utility.HelperUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.awt.*;
import java.util.List;
import java.util.*;

public class RuinManager implements EarthRuinManager {

	private final Earth earth;
	private final HashMap<String, KonRuin> ruinMap; // lower case name maps to ruin object
	private Material ruinCriticalBlock;
	private boolean isRuinDataNull;
	
	public RuinManager(Earth earth) {
		this.earth = earth;
		this.ruinMap = new HashMap<>();
		this.ruinCriticalBlock = Material.OBSIDIAN;
		this.isRuinDataNull = false;
	}
	
	public void initialize() {
		loadCriticalBlocks();
		loadRuins();
		regenAllRuins();
		ChatUtil.printDebug("Ruin Manager is ready");
	}
	
	public void regenAllRuins() {
		for(KonRuin ruin : ruinMap.values()) {
			ruin.regenCriticalBlocks();
		}
		ChatUtil.printDebug("Regenerated all ruin critical blocks");
	}
	
	public void removeAllGolems() {
		for(KonRuin ruin : ruinMap.values()) {
			ruin.removeAllGolems();
		}
	}
	
	public void rewardPlayers(KonRuin ruin, KonKingdom kingdom) {
		int rewardFavor = earth.getCore().getInt(CorePath.RUINS_CAPTURE_REWARD_FAVOR.getPath(),0);
		int rewardExp = earth.getCore().getInt(CorePath.RUINS_CAPTURE_REWARD_EXP.getPath(),0);
		for(KonPlayer friendly : getRuinPlayers(ruin,kingdom)) {
			// Give reward to player
			if(rewardFavor > 0) {
				ChatUtil.printDebug("Ruin capture favor rewarded to player "+friendly.getBukkitPlayer().getName());
	            if(EarthPlugin.depositPlayer(friendly.getBukkitPlayer(), rewardFavor)) {
	            	ChatUtil.sendNotice(friendly.getBukkitPlayer(), ChatColor.LIGHT_PURPLE+MessagePath.PROTECTION_NOTICE_RUIN.getMessage(ruin.getName()));
	            }
			}
			if(rewardExp > 0) {
				friendly.getBukkitPlayer().giveExp(rewardExp);
				ChatUtil.sendNotice(friendly.getBukkitPlayer(), MessagePath.GENERIC_NOTICE_REWARD_EXP.getMessage(rewardExp));
			}
		}
	}
	
	// returns list of players in kingdom inside ruin
	public List<KonPlayer> getRuinPlayers(KonRuin ruin, KonKingdom kingdom) {
		List<KonPlayer> players = new ArrayList<>();
		for(KonPlayer friendly : earth.getPlayerManager().getPlayersInKingdom(kingdom)) {
			if(isLocInsideRuin(ruin,friendly.getBukkitPlayer().getLocation())) {
				players.add(friendly);
			}
		}
		return players;
	}
	
	public boolean isRuin(String name) {
		// Check for lower-case name only
		return ruinMap.containsKey(name.toLowerCase());
	}
	
	public boolean isLocInsideRuin(EarthRuin ruinArg, Location loc) {
		boolean result = false;
		if(ruinArg instanceof KonRuin) {
			KonRuin ruin = (KonRuin) ruinArg;
			if(earth.getTerritoryManager().isChunkClaimed(loc)) {
				if(ruin.equals(earth.getTerritoryManager().getChunkTerritory(loc))) {
					result = true;
				}
			}
		}
		return result;
	}
	
	public boolean addRuin(Location loc, String name) {
		boolean result = false;
		if(earth.validateNameConstraints(name) == 0) {
			// Verify no overlapping init chunks
			for(Point point : HelperUtil.getAreaPoints(loc, 2)) {
				if(earth.getTerritoryManager().isChunkClaimed(point,loc.getWorld())) {
					ChatUtil.printDebug("Found a chunk conflict during ruin init: "+name);
					return false;
				}
			}
			// Add ruin to map with lower-case key
			String nameLower = name.toLowerCase();
			ruinMap.put(nameLower, new KonRuin(loc, name, earth.getKingdomManager().getNeutrals(), earth));
			ruinMap.get(nameLower).initClaim();
			ruinMap.get(nameLower).updateBarPlayers();
			// Update territory cache
			earth.getTerritoryManager().addAllTerritory(loc.getWorld(),ruinMap.get(nameLower).getChunkList());
			// Update border particles
			earth.getTerritoryManager().updatePlayerBorderParticles(loc);
			// Update maps
			earth.getMapHandler().drawUpdateTerritory(ruinMap.get(nameLower));
			result = true;
		}
		return result;
	}
	
	public boolean removeRuin(String name) {
		boolean result = false;
		KonRuin oldRuin = ruinMap.remove(name.toLowerCase());
		if(oldRuin != null) {
			ArrayList<KonPlayer> nearbyPlayers = earth.getPlayerManager().getPlayersNearTerritory(oldRuin);
			oldRuin.removeAllBarPlayers();
			oldRuin.removeAllGolems();
			// Update territory cache
			earth.getTerritoryManager().removeAllTerritory(oldRuin.getCenterLoc().getWorld(), oldRuin.getChunkList().keySet());
			// Update border particles
			for(KonPlayer player : nearbyPlayers) {
				earth.getTerritoryManager().updatePlayerBorderParticles(player);
			}
			earth.getMapHandler().drawRemoveTerritory(oldRuin);
			ChatUtil.printDebug("Removed Ruin "+name);
			oldRuin = null;
			result = true;
		}
		return result;
	}
	
	public boolean renameRuin(String name, String newName) {
		boolean result = false;
		if(isRuin(name) && earth.validateNameConstraints(newName) == 0) {
			ruinMap.get(name.toLowerCase()).setName(newName);
			KonRuin ruin = ruinMap.remove(name.toLowerCase());
			ruinMap.put(newName.toLowerCase(), ruin);
			ruin.updateBarTitle();
			ruin.updateBarPlayers();
			result = true;
		}
		return result;
	}

	public boolean resetRuin(String name) {
		if(!isRuin(name)) return false;
		KonRuin ruin = ruinMap.get(name.toLowerCase());
		ruin.resetRuinCapture();
		return true;
	}
	
	public KonRuin getRuin(String name) {
		return ruinMap.get(name.toLowerCase());
	}
	
	public Collection<KonRuin> getRuins() {
		return ruinMap.values();
	}
	
	public Set<String> getRuinNames() {
		Set<String> result = new HashSet<>();
		for(KonRuin ruin : ruinMap.values()) {
			result.add(ruin.getName());
		}
		return result;
	}
	
	public Material getRuinCriticalBlock() {
		return ruinCriticalBlock;
	}
	
	private void loadCriticalBlocks() {
		String ruinCriticalBlockTypeName = earth.getCore().getString(CorePath.RUINS_CRITICAL_BLOCK.getPath(),"");
		try {
			ruinCriticalBlock = Material.valueOf(ruinCriticalBlockTypeName);
		} catch(IllegalArgumentException e) {
			String message = "Invalid ruin critical block \""+ruinCriticalBlockTypeName+"\" given in core.ruins.critical_block, using default OBSIDIAN";
    		ChatUtil.printConsoleError(message);
    		earth.opStatusMessages.add(message);
		}
	}
	
	private void loadRuins() {
		FileConfiguration ruinsConfig = earth.getConfigManager().getConfig("ruins");
        if (ruinsConfig.get("ruins") == null) {
        	ChatUtil.printConsoleError("Failed to load any ruins from ruins.yml! Check file permissions.");
			isRuinDataNull = true;
            return;
        }
        double x,y,z;
        List<Double> sectionList;
        String worldName;
        KonRuin ruin;
        // Load all Ruins
        ConfigurationSection ruinsSection = ruinsConfig.getConfigurationSection("ruins");
        for(String ruinName : ruinsConfig.getConfigurationSection("ruins").getKeys(false)) {
        	ConfigurationSection ruinSection = ruinsSection.getConfigurationSection(ruinName);
        	worldName = ruinSection.getString("world","world");
        	sectionList = ruinSection.getDoubleList("center");
    		x = sectionList.get(0);
    		y = sectionList.get(1);
    		z = sectionList.get(2);
    		World world = Bukkit.getWorld(worldName);
    		if(world != null) {
	        	Location ruin_center = new Location(world,x,y,z);
	        	if(addRuin(ruin_center,ruinName)) {
	        		ruin = getRuin(ruinName);
	        		if(ruin != null) {
	        			ruin.addPoints(HelperUtil.formatStringToPoints(ruinSection.getString("chunks","")));
	                	for(Location loc : HelperUtil.formatStringToLocations(ruinSection.getString("criticals",""),world)) {
	                		loc.setWorld(world);
	                		ruin.setCriticalLocation(loc);
	            		}
	                	for(Location loc : HelperUtil.formatStringToLocations(ruinSection.getString("spawns",""),world)) {
	                		loc.setWorld(world);
	                		ruin.addSpawnLocation(loc);
	            		}
	                	earth.getTerritoryManager().addAllTerritory(world,ruin.getChunkList());
						// Set properties
						ConfigurationSection ruinPropertiesSection = ruinSection.getConfigurationSection("properties");
						if(ruinPropertiesSection != null) {
							for (String propertyName : ruinPropertiesSection.getKeys(false)) {
								boolean value = ruinPropertiesSection.getBoolean(propertyName);
								KonPropertyFlag property = KonPropertyFlag.getFlag(propertyName);
								boolean status = ruin.setPropertyValue(property, value);
								if (!status) {
									ChatUtil.printDebug("Failed to set invalid property " + propertyName + " to Ruin " + ruinName);
								}
							}
						}
						// Update title
						ruin.updateBarTitle();
	        		} else {
	        			String message = "Could not load ruin "+ruinName+", ruins.yml may be corrupted and needs to be deleted.";
	            		ChatUtil.printConsoleError(message);
	            		earth.opStatusMessages.add(message);
	        		}
	        	} else {
	        		String message = "Failed to load ruin "+ruinName+", ruins.yml may be corrupted and needs to be deleted.";
	        		ChatUtil.printConsoleError(message);
	        		earth.opStatusMessages.add(message);
	        	}
    		} else {
    			String message = "Failed to load ruin "+ruinName+" in an unloaded world, "+worldName+". Check plugin load order.";
    			ChatUtil.printConsoleError(message);
    			earth.opStatusMessages.add(message);
    		}
        }
	}
	
	public void saveRuins() {
		if(isRuinDataNull && ruinMap.isEmpty()) {
			// There was probably an issue loading ruins, do not save.
			ChatUtil.printConsoleError("Aborted saving ruin data because a problem was encountered while loading data from ruins.yml");
			return;
		}
		FileConfiguration ruinsConfig = earth.getConfigManager().getConfig("ruins");
		ruinsConfig.set("ruins", null); // reset ruins config
		ConfigurationSection root = ruinsConfig.createSection("ruins");
		for(String name : ruinMap.keySet()) {
			KonRuin ruin = ruinMap.get(name);
			ConfigurationSection ruinSection = root.createSection(ruin.getName());
			ruinSection.set("world", ruin.getWorld().getName());
			ruinSection.set("center", new int[] {ruin.getCenterLoc().getBlockX(),
					ruin.getCenterLoc().getBlockY(),
					ruin.getCenterLoc().getBlockZ()});
			ruinSection.set("chunks", HelperUtil.formatPointsToString(ruin.getChunkList().keySet()));
			ruinSection.set("criticals", HelperUtil.formatLocationsToString(ruin.getCriticalLocations()));
			ruinSection.set("spawns", HelperUtil.formatLocationsToString(ruin.getSpawnLocations()));
			// Properties
			ConfigurationSection ruinPropertiesSection = ruinSection.createSection("properties");
			for(KonPropertyFlag flag : KonPropertyFlag.values()) {
				if(ruin.hasPropertyValue(flag)) {
					ruinPropertiesSection.set(flag.toString(), ruin.getPropertyValue(flag));
				}
			}
		}
	}
	
}
