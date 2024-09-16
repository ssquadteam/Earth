package com.github.ssquadteam.earth.manager;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.EarthPlugin;
import com.github.ssquadteam.earth.api.manager.EarthShieldManager;
import com.github.ssquadteam.earth.api.model.EarthTown;
import com.github.ssquadteam.earth.model.*;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.CorePath;
import com.github.ssquadteam.earth.utility.HelperUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ShieldManager implements EarthShieldManager {

	private final Earth earth;
	private boolean isShieldsEnabled;
	private boolean isArmorsEnabled;
	private final ArrayList<KonShield> shields;
	private final ArrayList<KonArmor> armors;
	
	public ShieldManager(Earth earth) {
		this.earth = earth;
		this.isShieldsEnabled = false;
		this.isArmorsEnabled = false;
		this.shields = new ArrayList<>();
		this.armors = new ArrayList<>();
	}
	
	public void initialize() {
		if(loadShields()) {
			isShieldsEnabled = earth.getCore().getBoolean(CorePath.TOWNS_ENABLE_SHIELDS.getPath(),false);
		}
		if(loadArmors()) {
			isArmorsEnabled = earth.getCore().getBoolean(CorePath.TOWNS_ENABLE_ARMOR.getPath(),false);
		}
		// Check for shields or armor that must be disabled, due to reload
		for(KonKingdom kingdom : earth.getKingdomManager().getKingdoms()) {
			for(KonTown town : kingdom.getTowns()) {
				if(!isShieldsEnabled && town.isShielded()) {
					town.deactivateShield();
				}
				if(!isArmorsEnabled && town.isArmored()) {
					town.deactivateArmor();
				}
			}
		}
		ChatUtil.printDebug("Shield Manager is ready, shields: "+isShieldsEnabled+", armors: "+isArmorsEnabled);
	}
	
	private boolean loadShields() {
		// Attempt to load shields from file
		shields.clear();
		FileConfiguration shieldsConfig = earth.getConfigManager().getConfig("shields");
        if (shieldsConfig.get("shields") == null) {
        	ChatUtil.printDebug("There is no shields section in shields.yml");
            return false;
        }
		// Add shields and use default values when fields are missing
        KonShield newShield;
		for(String shieldName : shieldsConfig.getConfigurationSection("shields").getKeys(false)) {
        	int shieldTime = 120;
        	int shieldCost = 50;
			int shieldCostPerResident = 0;
			int shieldCostPerLand = 0;
        	ConfigurationSection shieldSection = shieldsConfig.getConfigurationSection("shields."+shieldName);
			assert shieldSection != null;
			if(shieldSection.contains("charge")) {
        		shieldTime = shieldSection.getInt("charge",0);
    		} else {
    			ChatUtil.printConsoleError("Shields.yml is missing \"charge\" section for shield \""+shieldName+"\", using default 120.");
    		}
        	if(shieldSection.contains("cost")) {
        		shieldCost = shieldSection.getInt("cost",0);
    		} else {
    			ChatUtil.printConsoleError("Shields.yml is missing \"cost\" section for shield \""+shieldName+"\", using default 50.");
			}
			if(shieldSection.contains("cost_per_resident")) {
				shieldCostPerResident = shieldSection.getInt("cost_per_resident",0);
			} else {
				ChatUtil.printConsoleError("Shields.yml is missing \"cost_per_resident\" section for shield \""+shieldName+"\", using default 0.");
			}
			if(shieldSection.contains("cost_per_land")) {
				shieldCostPerLand = shieldSection.getInt("cost_per_land",0);
			} else {
				ChatUtil.printConsoleError("Shields.yml is missing \"cost_per_land\" section for shield \""+shieldName+"\", using default 0.");
			}
			newShield = new KonShield(shieldName, shieldTime, shieldCost, shieldCostPerResident, shieldCostPerLand);
			shields.add(newShield);
		}
		return true;
	}
	
	private boolean loadArmors() {
		// Attempt to load armors from file
		armors.clear();
		FileConfiguration shieldsConfig = earth.getConfigManager().getConfig("shields");
        if (shieldsConfig.get("armors") == null) {
        	ChatUtil.printDebug("There is no armors section in shields.yml");
            return false;
        }
		// Add armors and use default values when fields are missing
        KonArmor newArmor;
		for(String armorName : shieldsConfig.getConfigurationSection("armors").getKeys(false)) {
        	int armorBlocks = 50;
        	int armorCost = 20;
			int armorCostPerResident = 0;
			int armorCostPerLand = 0;
        	ConfigurationSection armorSection = shieldsConfig.getConfigurationSection("armors."+armorName);
			assert armorSection != null;
			if(armorSection.contains("charge")) {
        		armorBlocks = armorSection.getInt("charge",0);
    		} else {
				ChatUtil.printConsoleError("Shields.yml is missing \"charge\" section for armor \""+armorName+"\", using default 50.");
    		}
        	if(armorSection.contains("cost")) {
        		armorCost = armorSection.getInt("cost",0);
    		} else {
    			ChatUtil.printConsoleError("Shields.yml is missing \"cost\" section for armor \""+armorName+"\", using default 20.");
    		}
			if(armorSection.contains("cost_per_resident")) {
				armorCostPerResident = armorSection.getInt("cost_per_resident",0);
			} else {
				ChatUtil.printConsoleError("Shields.yml is missing \"cost_per_resident\" section for armor \""+armorName+"\", using default 0.");
			}
			if(armorSection.contains("cost_per_land")) {
				armorCostPerLand = armorSection.getInt("cost_per_land",0);
			} else {
				ChatUtil.printConsoleError("Shields.yml is missing \"cost_per_land\" section for armor \""+armorName+"\", using default 0.");
			}
			newArmor = new KonArmor(armorName, armorBlocks, armorCost, armorCostPerResident, armorCostPerLand);
			armors.add(newArmor);
		}
		return true;
	}
	
	public KonShield getShield(String id) {
		KonShield result = null;
		for(KonShield shield : shields) {
			if(shield.getId().equalsIgnoreCase(id)) {
				result = shield;
				break;
			}
		}
		return result;
	}

	public KonArmor getArmor(String id) {
		KonArmor result = null;
		for(KonArmor armor : armors) {
			if(armor.getId().equalsIgnoreCase(id)) {
				result = armor;
				break;
			}
		}
		return result;
	}
	
	public boolean isShieldsEnabled() {
		return isShieldsEnabled;
	}
	
	public boolean isArmorsEnabled() {
		return isArmorsEnabled;
	}
	
	public List<KonShield> getShields() {
		return shields;
	}
	
	public List<KonArmor> getArmors() {
		return armors;
	}

	public int getTotalCostShield(KonShield shield, KonTown town) {
		return shield.getCost() + (shield.getCostPerResident()*town.getNumResidents()) + (shield.getCostPerLand()*town.getNumLand());
	}

	public int getTotalCostArmor(KonArmor armor, KonTown town) {
		return armor.getCost() + (armor.getCostPerResident()*town.getNumResidents()) + (armor.getCostPerLand()*town.getNumLand());
	}

	public boolean activateTownShield(KonShield shield, KonTown town, Player bukkitPlayer, boolean ignoreCost) {
		if(!isShieldsEnabled) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
			return false;
		}
		
		Date now = new Date();
		int shieldTime = shield.getDurationSeconds();
		int endTime = (int)(now.getTime()/1000) + shieldTime;
		
		// Check that the player has enough favor
		int requiredCost = getTotalCostShield(shield, town);
		if(!ignoreCost && EarthPlugin.getBalance(bukkitPlayer) < requiredCost) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.MENU_SHIELD_FAIL_COST.getMessage(requiredCost));
            return false;
		}
		
		// Check that town is not under attack optionally
		boolean isAttackCheckEnabled = earth.getCore().getBoolean(CorePath.TOWNS_SHIELDS_WHILE_ATTACKED.getPath(),false);
		if(!isAttackCheckEnabled && town.isAttacked()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.MENU_SHIELD_FAIL_ATTACK.getMessage());
            return false;
		}
		
		// Check that town does not have an active shield optionally
		boolean isShieldAddEnabled = earth.getCore().getBoolean(CorePath.TOWNS_SHIELDS_ADD.getPath(),false);
		if(!isShieldAddEnabled && town.isShielded()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.MENU_SHIELD_FAIL_ADD.getMessage());
            return false;
		}
		
		// Check that town does not exceed maximum optionally
		int maxShields = earth.getCore().getInt(CorePath.TOWNS_MAX_SHIELDS.getPath(),0);
		if(maxShields > 0 && shield.getDurationSeconds()+town.getRemainingShieldTimeSeconds() > maxShields) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.MENU_SHIELD_FAIL_MAX.getMessage(maxShields));
            return false;
		}
		
		// Passed checks, activate the shield
		String timeFormat = HelperUtil.getTimeFormat(shield.getDurationSeconds(), ChatColor.AQUA);
		String shieldName = shield.getId()+" "+MessagePath.LABEL_SHIELD.getMessage();
		if(town.isShielded()) {
			endTime = town.getShieldEndTime() + shieldTime;
			ChatUtil.sendNotice(bukkitPlayer, MessagePath.MENU_SHIELD_ACTIVATE_ADD.getMessage(shieldName,timeFormat));
			ChatUtil.printDebug("Activated town shield addition "+shield.getId()+" to town "+town.getName()+" for end time "+endTime);
		} else {
			ChatUtil.sendNotice(bukkitPlayer, MessagePath.MENU_SHIELD_ACTIVATE_NEW.getMessage(shieldName,timeFormat));
			ChatUtil.printDebug("Activated new town shield "+shield.getId()+" to town "+town.getName()+" for end time "+endTime);
		}
		town.activateShield(endTime);
		
		// Withdraw cost
		KonPlayer player = earth.getPlayerManager().getPlayer(bukkitPlayer);
        if(!ignoreCost && EarthPlugin.withdrawPlayer(bukkitPlayer, requiredCost)) {
        	if(player != null) {
        		earth.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR, requiredCost);
        	}
        }
		return true;
	}

	public boolean activateTownArmor(KonArmor armor, KonTown town, Player bukkitPlayer, boolean ignoreCost) {
		if(!isArmorsEnabled) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
			return false;
		}
		
		// Check that the player has enough favor
		int requiredCost = getTotalCostArmor(armor, town);
		if(!ignoreCost && EarthPlugin.getBalance(bukkitPlayer) < requiredCost) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.MENU_SHIELD_FAIL_COST.getMessage(requiredCost));
            return false;
		}
		
		// Check that town is not under attack optionally
		boolean isAttackCheckEnabled = earth.getCore().getBoolean(CorePath.TOWNS_ARMOR_WHILE_ATTACKED.getPath(),false);
		if(!isAttackCheckEnabled && town.isAttacked()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.MENU_SHIELD_FAIL_ATTACK.getMessage());
            return false;
		}
		
		// Check that town does not have an active armor optionally
		boolean isArmorAddEnabled = earth.getCore().getBoolean(CorePath.TOWNS_ARMOR_ADD.getPath(),false);
		if(!isArmorAddEnabled && town.isArmored()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.MENU_SHIELD_FAIL_ADD.getMessage());
            return false;
		}
		
		// Check that town does not exceed maximum optionally
		int maxArmor = earth.getCore().getInt(CorePath.TOWNS_MAX_ARMOR.getPath(),0);
		if(maxArmor > 0 && armor.getBlocks()+town.getArmorBlocks() > maxArmor) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.MENU_SHIELD_FAIL_MAX.getMessage(maxArmor));
            return false;
		}
		
		// Passed checks, activate the armor
		int newArmor = armor.getBlocks();
		String armorName = armor.getId()+" "+MessagePath.LABEL_ARMOR.getMessage();
		if(town.isArmored()) {
			newArmor = town.getArmorBlocks() + armor.getBlocks();
			ChatUtil.sendNotice(bukkitPlayer, MessagePath.MENU_SHIELD_ACTIVATE_ADD.getMessage(armorName,armor.getBlocks()));
			ChatUtil.printDebug("Activated town armor addition "+armor.getId()+" to town "+town.getName()+" for blocks "+armor.getBlocks());
		} else {
			ChatUtil.sendNotice(bukkitPlayer, MessagePath.MENU_SHIELD_ACTIVATE_NEW.getMessage(armorName,armor.getBlocks()));
			ChatUtil.printDebug("Activated new town armor "+armor.getId()+" to town "+town.getName()+" for blocks "+armor.getBlocks());
		}
		town.activateArmor(newArmor);
		
		// Withdraw cost
		KonPlayer player = earth.getPlayerManager().getPlayer(bukkitPlayer);
        if(!ignoreCost && EarthPlugin.withdrawPlayer(bukkitPlayer, requiredCost)) {
        	if(player != null) {
        		earth.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR, requiredCost);
        	}
        }
		return true;
	}
	
	public int getTownShieldTime(EarthTown townArg) {
		int result = -1;
		if(townArg instanceof KonTown) {
			KonTown town = (KonTown) townArg;
			result = town.getShieldEndTime();
		}
		return result;
	}
	
	public int getTownArmorBlocks(EarthTown townArg) {
		int result = -1;
		if(townArg instanceof KonTown) {
			KonTown town = (KonTown) townArg;
			result = town.getArmorBlocks();
		}
		return result;
	}
	
	/*
	 * Override methods, used for admin commands
	 */
	
	/**
	 * Sets a town's shields for (value) seconds from now
	 * @param townArg The town to modify shields
	 * @param value Time in seconds from now to end shields. If 0, deactivate shields.
	 */
	public boolean shieldSet(EarthTown townArg, int value) {
		boolean result = false;
		if(!(townArg instanceof KonTown)) return false;
		KonTown town = (KonTown) townArg;
		Date now = new Date();
		int newEndTime = (int)(now.getTime()/1000) + value;
		if(town.isShielded()) {
			if(value <= 0) {
				town.deactivateShield();
			} else {
				town.activateShield(newEndTime);
			}
			result = true;
		} else {
			if(value > 0) {
				town.activateShield(newEndTime);
				result = true;
			}
		}

		return result;
	}
	
	/**
	 * Adds to a town's shields in seconds, positive or negative
	 * @param townArg The town to modify shields
	 * @param value Time in seconds to add to current shields. If result is <= 0, deactivate shields.
	 */
	public boolean shieldAdd(EarthTown townArg, int value) {
		boolean result = false;
		if(townArg instanceof KonTown) {
			KonTown town = (KonTown) townArg;
			Date now = new Date();
			int newEndTime = (int)(now.getTime()/1000) + value;
			if(town.isShielded()) {
				newEndTime = town.getShieldEndTime() + value;
			}
			Date end = new Date((long)newEndTime*1000);
			if(now.after(end)) {
				// End time is less than now, deactivate shields and do nothing
				if(town.isShielded()) {
					town.deactivateShield();
					result = true;
				}
			} else {
				// End time is in the future, add to shields
				town.activateShield(newEndTime);
				result = true;
			}
		}
		return result;
	}

	/**
	 * Sets a town's armor for (value) blocks
	 * @param townArg The town to modify armor
	 * @param value Amount of blocks to set the town's armor to. If 0, deactivate armor.
	 */
	public boolean armorSet(EarthTown townArg, int value) {
		boolean result = false;
		if(!(townArg instanceof KonTown)) return false;
		KonTown town = (KonTown) townArg;
		if(town.isArmored()) {
			if(value <= 0) {
				town.deactivateArmor();
			} else {
				town.activateArmor(value);
			}
			result = true;
		} else {
			if(value > 0) {
				town.activateArmor(value);
				result = true;
			}
		}
		return result;
	}
	
	/**
	 * Adds to a town's armor in blocks, positive or negative
	 * @param townArg The town to modify armor
	 * @param value Blocks to add to current armor. If result is <= 0, deactivate armor.
	 */
	public boolean armorAdd(EarthTown townArg, int value) {
		boolean result = false;
		if(!(townArg instanceof KonTown)) return false;
		KonTown town = (KonTown) townArg;
		int newBlocks = value;
		if(town.isArmored()) {
			newBlocks = town.getArmorBlocks() + value;
		}
		if(newBlocks <= 0) {
			// Blocks is 0 or less, deactivate armor and do nothing
			if(town.isArmored()) {
				town.deactivateArmor();
				result = true;
			}
		} else {
			// Blocks are valid, add to armor
			town.activateArmor(newBlocks);
			result = true;
		}
		return result;
	}
	
}
