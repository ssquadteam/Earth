package com.github.ssquadteam.earth.manager;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.EarthPlugin;
import com.github.ssquadteam.earth.api.event.player.EarthPlayerExileEvent;
import com.github.ssquadteam.earth.api.event.player.EarthPlayerKingdomEvent;
import com.github.ssquadteam.earth.api.manager.EarthKingdomManager;
import com.github.ssquadteam.earth.api.model.*;
import com.github.ssquadteam.earth.model.*;
import com.github.ssquadteam.earth.model.KonKingdomScoreAttributes.KonKingdomScoreAttribute;
import com.github.ssquadteam.earth.model.KonPlayerScoreAttributes.KonPlayerScoreAttribute;
import com.github.ssquadteam.earth.utility.Timer;
import com.github.ssquadteam.earth.utility.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.*;

//TODO: Split this class up into smaller manager classes, like
// - KingdomManager: All kingdom query, actions, memberships, etc
// - TownManager: All town query, actions, memberships, etc
// - DiplomacyManager: All kingdom relationships, diplomacy actions
public class KingdomManager implements EarthKingdomManager, Timeable {
	
	private final Earth earth;
	private final HashMap<String, KonKingdom> kingdomMap;
	private KonKingdom barbarians;
	private KonKingdom neutrals;
	private final HashMap<PotionEffectType,Integer> townNerfs;
	private Material townCriticalBlock;
	private int maxCriticalHits;
	private final HashSet<Material> armorBlocks;
	private boolean isArmorBlockWhitelist;
	private int joinCooldownSeconds;
	private int exileCooldownSeconds;
	private final HashMap<UUID,Integer> joinPlayerCooldowns;
	private final HashMap<UUID,Integer> exilePlayerCooldowns;
	private final ArrayList<DiplomacyTicket> diplomacyTickets;
	private boolean isKingdomDataNull;

	// Config Settings
	private boolean isAdminOnly;
	private long payIntervalSeconds;
	private double payPerChunk;
	private double payPerResident;
	private double payLimit;
	private int payPercentOfficer;
	private int payPercentMaster;
	private double costCreate;
	private double costRename;
	private double costTemplate;
	private boolean discountEnable;
	private int discountPercent;
	private boolean discountStack;
	private double costDiplomacyWar;
	private double costDiplomacyPeace;
	private double costDiplomacyTrade;
	private double costDiplomacyAlliance;
	private boolean townDestroyLordEnable;
	private boolean townDestroyMasterEnable;

	private final Timer payTimer;
	
	public KingdomManager(Earth earth) {
		this.earth = earth;
		this.kingdomMap = new HashMap<>();
		this.barbarians = null;
		this.neutrals = null;
		this.townNerfs = new HashMap<>();
		this.townCriticalBlock = Material.OBSIDIAN;
		this.maxCriticalHits = 12;
		this.armorBlocks = new HashSet<>();
		this.isArmorBlockWhitelist = false;
		this.joinCooldownSeconds = 0;
		this.exileCooldownSeconds = 0;
		this.joinPlayerCooldowns = new HashMap<>();
		this.exilePlayerCooldowns = new HashMap<>();
		this.diplomacyTickets = new ArrayList<>();
		this.isKingdomDataNull = false;

		this.isAdminOnly = false;
		this.payIntervalSeconds = 0;
		this.payPerChunk = 0;
		this.payPerResident = 0;
		this.payLimit = 0;
		this.payPercentOfficer = 0;
		this.payPercentMaster = 0;
		this.costCreate = 0;
		this.costRename = 0;
		this.costTemplate = 0;
		this.costDiplomacyWar = 0;
		this.costDiplomacyPeace = 0;
		this.costDiplomacyTrade = 0;
		this.costDiplomacyAlliance = 0;
		this.payTimer = new Timer(this);
	}
	
	public void initialize() {
		barbarians = new KonKingdom(MessagePath.LABEL_BARBARIANS.getMessage(),earth);
		neutrals = new KonKingdom(MessagePath.LABEL_NEUTRALS.getMessage(),earth);
		loadArmorBlacklist();
		loadJoinExileCooldowns();
		loadKingdoms();
		makeTownNerfs();
		ChatUtil.printDebug("Kingdom Manager is ready");
	}
	
	public void loadOptions() {
		// Kingdom Settings
		isAdminOnly				= earth.getCore().getBoolean(CorePath.KINGDOMS_CREATE_ADMIN_ONLY.getPath());
		payIntervalSeconds 		= earth.getCore().getLong(CorePath.FAVOR_KINGDOMS_PAY_INTERVAL_SECONDS.getPath());
		payPerChunk 			= earth.getCore().getDouble(CorePath.FAVOR_KINGDOMS_PAY_PER_CHUNK.getPath());
		payPerResident 			= earth.getCore().getDouble(CorePath.FAVOR_KINGDOMS_PAY_PER_RESIDENT.getPath());
		payLimit 				= earth.getCore().getDouble(CorePath.FAVOR_KINGDOMS_PAY_LIMIT.getPath());
		payPercentOfficer   	= earth.getCore().getInt(CorePath.FAVOR_KINGDOMS_BONUS_OFFICER_PERCENT.getPath());
		payPercentMaster    	= earth.getCore().getInt(CorePath.FAVOR_KINGDOMS_BONUS_MASTER_PERCENT.getPath());
		costCreate 	        	= earth.getCore().getDouble(CorePath.FAVOR_KINGDOMS_COST_CREATE.getPath());
		costRename 	        	= earth.getCore().getDouble(CorePath.FAVOR_KINGDOMS_COST_RENAME.getPath());
		costTemplate        	= earth.getCore().getDouble(CorePath.FAVOR_KINGDOMS_COST_TEMPLATE.getPath());
		costDiplomacyWar 		= earth.getCore().getDouble(CorePath.FAVOR_DIPLOMACY_COST_WAR.getPath());
		costDiplomacyPeace 		= earth.getCore().getDouble(CorePath.FAVOR_DIPLOMACY_COST_PEACE.getPath());
		costDiplomacyTrade 		= earth.getCore().getDouble(CorePath.FAVOR_DIPLOMACY_COST_TRADE.getPath());
		costDiplomacyAlliance 	= earth.getCore().getDouble(CorePath.FAVOR_DIPLOMACY_COST_ALLIANCE.getPath());
		
		payPerChunk = payPerChunk < 0 ? 0 : payPerChunk;
		payPerResident = payPerResident < 0 ? 0 : payPerResident;
		payLimit = payLimit < 0 ? 0 : payLimit;
		costCreate = costCreate < 0 ? 0 : costCreate;
		costRename = costRename < 0 ? 0 : costRename;
		costTemplate = costTemplate < 0 ? 0 : costTemplate;
		costDiplomacyWar = Math.max(costDiplomacyWar,0);
		costDiplomacyPeace = Math.max(costDiplomacyPeace,0);
		costDiplomacyTrade = Math.max(costDiplomacyTrade,0);
		costDiplomacyAlliance = Math.max(costDiplomacyAlliance,0);
		payPercentOfficer = Math.max(payPercentOfficer, 0);
		payPercentOfficer = Math.min(payPercentOfficer, 100);
		payPercentMaster = Math.max(payPercentMaster, 0);
		payPercentMaster = Math.min(payPercentMaster, 100);

		payTimer.stopTimer();
		if(payIntervalSeconds > 0) {
			payTimer.setTime((int)payIntervalSeconds);
			payTimer.startLoopTimer();
		}

		// Town Settings
		townDestroyLordEnable   = earth.getCore().getBoolean(CorePath.TOWNS_ALLOW_DESTROY_LORD.getPath());
		townDestroyMasterEnable = earth.getCore().getBoolean(CorePath.TOWNS_ALLOW_DESTROY_MASTER.getPath());

		discountEnable 	    = earth.getCore().getBoolean(CorePath.TOWNS_DISCOUNT_ENABLE.getPath());
		discountPercent		= earth.getCore().getInt(CorePath.TOWNS_DISCOUNT_PERCENT.getPath());
		discountStack		= earth.getCore().getBoolean(CorePath.TOWNS_DISCOUNT_STACK.getPath());

		discountPercent = Math.max(discountPercent,0);
		discountPercent = Math.min(discountPercent,100);
	}

	public String getKingdomPayTime() {
		String noColor = "";
		int timerCount = Math.max(payTimer.getTime(),0);
		return HelperUtil.getTimeFormat(timerCount, noColor);
	}
	
	public double getCostCreate() {
		return costCreate;
	}
	
	public double getCostRename() {
		return costRename;
	}
	
	public double getCostTemplate() {
		return costTemplate;
	}

	public boolean getIsDiscountEnable() {
		return discountEnable;
	}

	public int getDiscountPercent() {
		return discountPercent;
	}

	public boolean getIsDiscountStack() {
		return discountStack;
	}

	public boolean getIsTownDestroyLordEnable() {
		return townDestroyLordEnable;
	}

	public boolean getIsTownDestroyMasterEnable() {
		return townDestroyMasterEnable;
	}
	
	@Override
	public void onEndTimer(int taskID) {
		if(taskID == 0) {
			ChatUtil.printDebug("Kingdom Pay Timer ended with null taskID!");
		} else if(taskID == payTimer.getTaskID()) {
			ChatUtil.printDebug("Kingdom Pay timer completed a new cycle");
			disbursePayments();
		}
		
	}
	
	/**
	 * Kingdom payments for members
	 * Total payment is based on all towns & land.
	 * Kingdom members only get paid based on the towns & land they are lord of.
	 * Kingdom officers/master get a bonus = percentage of total payment.
	 */
	private void disbursePayments() {

		// Map of online players and their payments
		HashMap<UUID,Double> payments = new HashMap<>();
		// Initialize payment table with online players
		for (KonPlayer onlinePlayer : earth.getPlayerManager().getPlayersOnline()) {
			payments.put(onlinePlayer.getBukkitPlayer().getUniqueId(), 0.0);
		}

		// Determine pay amounts by town + capital
		ChatUtil.printDebug("Evaluating kingdom payments...");
		for(KonKingdom kingdom : kingdomMap.values()) {
			ChatUtil.printDebug("  Kingdom of "+kingdom.getName());
			// Determine bonus amounts for kingdom masters and officers
			int kingdomLand = kingdom.getNumLand();
			int kingdomPopulation = kingdom.getNumMembers();
			double baseBonus = (kingdomLand * payPerChunk) + (kingdomPopulation * payPerResident);
			double masterBonus = baseBonus * ((double)payPercentMaster/100);
			double officerBonus = baseBonus * ((double)payPercentOfficer/100);
			// Give master bonus
			UUID masterId = kingdom.getMaster();
			if (payments.containsKey(masterId)) {
				payments.put(masterId,payments.get(masterId)+masterBonus);
				Player masterPlayer = Bukkit.getPlayer(masterId);
				String masterName = masterPlayer == null ? "unknown" : masterPlayer.getName();
				ChatUtil.printDebug("  + "+masterName+" Master Bonus = "+masterBonus);
			}
			// Give officers bonus
			for (OfflinePlayer officer : kingdom.getPlayerOfficersOnly()) {
				UUID officerId = officer.getUniqueId();
				if (payments.containsKey(officerId)) {
					payments.put(officerId,payments.get(officerId)+officerBonus);
					Player officerPlayer = Bukkit.getPlayer(officerId);
					String officerName = officerPlayer == null ? "unknown" : officerPlayer.getName();
					ChatUtil.printDebug("  + "+officerName+" Officer Bonus = "+officerBonus);
				}
			}
			// Determine payment amounts from lordships
			for(KonTown town : kingdom.getCapitalTowns()) {
				int townLand = town.getNumLand();
				int townPopulation = town.getNumResidents();
				double townPay = (townLand * payPerChunk) + (townPopulation * payPerResident);
				UUID lordId = town.getLord();
				if (payments.containsKey(lordId)) {
					payments.put(lordId,payments.get(lordId)+townPay);
					Player lordPlayer = Bukkit.getPlayer(lordId);
					String lordName = lordPlayer == null ? "unknown" : lordPlayer.getName();
					ChatUtil.printDebug("  + "+lordName+" Town Lord of "+town.getName()+" = "+townPay);
				}
			}
		}

		// Deposit payments
		ChatUtil.printDebug("Depositing kingdom payments...");
		boolean isEssentialsEnabled = earth.getIntegrationManager().getEssentialsXAPI().isEnabled();
		boolean isAfkPayEnabled = earth.getCore().getBoolean(CorePath.INTEGRATION_ESSENTIALSX_OPTIONS_ENABLE_AFK_PAYMENTS.getPath(),true);
		for(UUID id : payments.keySet()) {
			double payAmount = payments.get(id);
			if(payLimit > 0 && payAmount > payLimit) {
				payAmount = payLimit;
			}
			Player bukkitPlayer = Bukkit.getPlayer(id);
			KonPlayer player = earth.getPlayerManager().getPlayer(bukkitPlayer);
			if(payAmount > 0 && player != null) {
				String playerName = bukkitPlayer == null ? "unknown" : bukkitPlayer.getName();
				if (isEssentialsEnabled && !isAfkPayEnabled && player.isAfk()) {
					// EssentialsX integration is enabled with no payments while AFK.
					// This player is AFK. Do not pay them.
					ChatUtil.printDebug("  "+playerName+" Skipped payment while AFK");
				} else {
					ChatUtil.printDebug("  "+playerName+" = "+payAmount);
					if(EarthPlugin.depositPlayer(bukkitPlayer, payAmount)) {
						ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_KINGDOM_NOTICE_PAY.getMessage());
					}
				}
			}
		}
	}

	/**
	 * Checks location constraints for new towns/capitals
	 * @param loc - The location of the new territory
	 * @return Status Code
	 * 			0 - Success
	 * 			1 - Error, location is in an invalid world
	 * 			2 - Error, location too close to another territory
	 * 			3 - Error, location too far from other territories
	 * 			4 - Error, location overlaps with other territories
	 */
	private int validateTownLocationConstraints(Location loc) {
		// Verify valid world
		if(!earth.isWorldValid(loc.getWorld())) {
			ChatUtil.printDebug("Failed to add town, invalid world: "+loc.getWorld().getName());
			return 1;
		}
		// Verify proximity to other territories
		int min_distance_sanc = earth.getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_SANCTUARY.getPath());
		int min_distance_town = earth.getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_TOWN.getPath());
		int searchDistance;
		int minDistance = Integer.MAX_VALUE;
		for(KonKingdom kingdom : kingdomMap.values()) {
			searchDistance = HelperUtil.chunkDistance(loc, kingdom.getCapital().getCenterLoc());
			if(searchDistance != -1 && min_distance_town > 0 && searchDistance < min_distance_town) {
				ChatUtil.printDebug("Failed to add town, too close to capital "+kingdom.getCapital().getName());
				return 2;
			}
			if(searchDistance != -1 && searchDistance < minDistance) {
				minDistance = searchDistance;
			}
			for(KonTown town : kingdom.getTowns()) {
				searchDistance = HelperUtil.chunkDistance(loc, town.getCenterLoc());
				if(searchDistance != -1 && min_distance_town > 0 && searchDistance < min_distance_town) {
					ChatUtil.printDebug("Failed to add town, too close to town "+town.getName());
					return 2;
				}
				if(searchDistance != -1 && searchDistance < minDistance) {
					minDistance = searchDistance;
				}
			}
		}
		for(KonRuin ruin : earth.getRuinManager().getRuins()) {
			searchDistance = HelperUtil.chunkDistance(loc, ruin.getCenterLoc());
			if(searchDistance != -1 && min_distance_town > 0 && searchDistance < min_distance_town) {
				ChatUtil.printDebug("Failed to add town, too close to ruin "+ruin.getName());
				return 2;
			}
			if(searchDistance != -1 && searchDistance < minDistance) {
				minDistance = searchDistance;
			}
		}
		for(KonSanctuary sanctuary : earth.getSanctuaryManager().getSanctuaries()) {
			searchDistance = HelperUtil.chunkDistance(loc, sanctuary.getCenterLoc());
			if(searchDistance != -1 && min_distance_sanc > 0 && searchDistance < min_distance_sanc) {
				ChatUtil.printDebug("Failed to add town, too close to sanctuary "+sanctuary.getName());
				return 2;
			}
			if(searchDistance != -1 && searchDistance < minDistance) {
				minDistance = searchDistance;
			}
		}
		// Verify max distance
		int maxLimit = earth.getCore().getInt(CorePath.TOWNS_MAX_DISTANCE_ALL.getPath(),0);
		if(maxLimit > 0 && minDistance > maxLimit) {
			ChatUtil.printDebug("Failed to add town, too far from other towns and capitals");
			return 3;
		}
		// Verify no overlapping init chunks
		int radius = earth.getCore().getInt(CorePath.TOWNS_INIT_RADIUS.getPath(),2);
		if(radius < 1) {
			radius = 1;
		}
		ChatUtil.printDebug("Checking for chunk conflicts with radius "+radius);
		for(Point point : HelperUtil.getAreaPoints(loc, radius)) {
			if(earth.getTerritoryManager().isChunkClaimed(point,loc.getWorld())) {
				ChatUtil.printDebug("Found a chunk conflict");
				return 4;
			}
		}
		// Return successfully
		return 0;
	}
	
	public void teleportAwayFromCenter(KonTown town) {
		// Teleport player to safe place around monument, facing monument
		for(KonPlayer occupant : earth.getPlayerManager().getPlayersOnline()) {
			if(town.isLocInsideCenterChunk(occupant.getBukkitPlayer().getLocation())) {
				Location tpLoc = earth.getSafeRandomCenteredLocation(town.getCenterLoc(), 2);
				if(tpLoc == null) {
					ChatUtil.printDebug("Failed to teleport player "+occupant.getBukkitPlayer().getName()+" to a safe location.");
					continue;
				}
        		double x0,x1,z0,z1;
        		x0 = tpLoc.getX();
        		x1 = town.getCenterLoc().getX();
        		z0 = tpLoc.getZ();
        		z1 = town.getCenterLoc().getZ();
        		float yaw = (float)(180-(Math.atan2((x0-x1),(z0-z1))*180/Math.PI));
        		//ChatUtil.printDebug("Settle teleport used x0,z0;x1,z1: "+x0+","+z0+";"+x1+","+z1+" and calculated yaw degrees: "+yaw);
        		tpLoc.setYaw(yaw);
        		if(occupant.getBukkitPlayer().isInsideVehicle()) {
        			ChatUtil.printDebug("Settling occupant player is in a vehicle, type "+ occupant.getBukkitPlayer().getVehicle().getType());
        			Entity vehicle = occupant.getBukkitPlayer().getVehicle();
        			List<Entity> passengers = vehicle.getPassengers();
        			occupant.getBukkitPlayer().leaveVehicle();
        			occupant.getBukkitPlayer().teleport(tpLoc,TeleportCause.PLUGIN);
        			new BukkitRunnable() {
        				public void run() {
        					vehicle.teleport(tpLoc,TeleportCause.PLUGIN);
        					for (Entity e : passengers) {
        						vehicle.addPassenger(e);
        					}
        				}
        			}.runTaskLater(earth.getPlugin(), 10L);
        			
        		} else {
        			occupant.getBukkitPlayer().teleport(tpLoc,TeleportCause.PLUGIN);
        		}
			}
		}
	}
	
	/*
	 * =================================================
	 * Kingdom Management Methods
	 * =================================================
	 */
	
	/**
	 * Creates a new kingdom.
	 * The new kingdom must perform checks for creating the capital, similar to a new town.
	 * The isAdmin argument controls whether the kingdom is admin operated (true) or player operated (false).
	 * When the kingdom is player operated:
	 * The player must be a barbarian, and is made the master of the kingdom;
	 * The player must pay favor to create the kingdom.
	 * The player is made the kingdom master, and lord of the capital.
	 * When the kingdom is admin operated:
	 * There is no cost to create, and there is no kingdom master.
	 * 
	 * @param centerLocation - The center location of the kingdom, used for capital
	 * @param kingdomName - Kingdom name
	 * @param templateName - Template name
	 * @param master - Player who is the kingdom master
	 * @param isAdmin - Is the kingdom admin operated (true) or player operated (false)
	 * @return 0	- Success
	 * 	       1	- Bad name
	 * 		   2	- Master is not a barbarian
	 *         3    - Not enough favor
	 *         4	- Invalid monument template
	 *         5	- Invalid world
	 *         6 	- Too close to another territory
	 *         7	- Too far from other territories
	 *         8	- Will overlap with other territories
	 *         11 - error, town init fail, monument paste failed
	 *   	   12 - error, town init fail, bad town height
	 *		   13 - error, town init fail, bad chunks
	 * 		   14 - error, town init fail, too much air below town
	 * 		   15 - error, town init fail, too much water below town
	 * 		   16 - error, town init fail, containers below monument
	 * 		   17 - error, town init fail, template is invalid
	 * 		   21 - error, town init fail, invalid monument
	 * 	  	   22 - error, town init fail, bad monument gradient
	 * 	  	   23 - error, town init fail, bad monument location (bedrock, outside gradient)
	 *         -1	- Internal error
	 */
	public int createKingdom(Location centerLocation, String kingdomName, String templateName, KonPlayer master, boolean isAdmin) {
		if(isAdminOnly && !isAdmin) {
			return -1;
		}

		/*
		 * Criteria for creating a kingdom:
		 * 
		 * Player must be a barbarian and not a member of another kingdom.
		 * Player must have favor to pay the cost to create.
		 * Player must provide valid name.
		 * Player must specify valid template.
		 * The location must be valid for creating the capital territory.
		 * 
		 */
		Player bukkitPlayer = master.getBukkitPlayer();
		// Verify name
		if(earth.validateNameConstraints(kingdomName) != 0) {
			return 1;
		}
		// Verify player is barbarian, not a member of another kingdom
		if(!isAdmin) {
			if(!master.isBarbarian()) {
				return 2;
			}
			for(KonKingdom kingdom : kingdomMap.values()) {
				if(kingdom.isMember(bukkitPlayer.getUniqueId())) {
					return 2;
				}
			}
		}
		// Verify valid monument template
		if(!earth.getSanctuaryManager().isValidTemplate(templateName)) {
			return 4;
		}
		KonMonumentTemplate template = earth.getSanctuaryManager().getTemplate(templateName);
		// Verify player can cover cost
		double totalCost = costCreate + template.getCost();
		if(!isAdmin && totalCost > 0 && EarthPlugin.getBalance(bukkitPlayer) < totalCost) {
			return 3;
    	}
		
		// Verify position (return codes 4 - 8)
		/*
		 * 			0 - Success
		 * 			1 - Error, location is in an invalid world
		 * 			2 - Error, location too close to another territory
		 * 			3 - Error, location too far from other territories
		 * 			4 - Error, location overlaps with other territories
		 */
		int locStatus = validateTownLocationConstraints(centerLocation);
		if(locStatus != 0) {
			// Offset error code to fit in this method's return codes
			return locStatus+4;
		}
		
		/* Passed all checks, try to create kingdom */
		
		kingdomMap.put(kingdomName, new KonKingdom(centerLocation, kingdomName, earth));
		
		KonKingdom newKingdom = kingdomMap.get(kingdomName);
		
		if(newKingdom != null) {
			newKingdom.setMonumentTemplate(template);
			// Initialize the capital territory, evaluate status
			// This may fail by internal town checks for gradient, height, etc.
			int initStatus = newKingdom.initCapital();
			
			if(initStatus == 0) {
				// Capital territory initialized successfully, finish kingdom setup
				earth.getTerritoryManager().addAllTerritory(centerLocation.getWorld(),newKingdom.getCapital().getChunkList());
				earth.getMapHandler().drawUpdateTerritory(newKingdom.getCapital());
				if(isAdmin) {
					// This kingdom is operated by admins only
					newKingdom.setIsAdminOperated(true);
					// Make the kingdom open for joining by default
					newKingdom.setIsOpen(true);
				} else {
					// This kingdom is operated by players
					newKingdom.setIsAdminOperated(false);
					// Withdraw cost
					if(totalCost > 0 && EarthPlugin.withdrawPlayer(bukkitPlayer, totalCost)) {
			            earth.getAccomplishmentManager().modifyPlayerStat(master,KonStatsType.FAVOR,(int)totalCost);
					}
					// Assign player to kingdom
					int assignStatus = assignPlayerKingdom(bukkitPlayer.getUniqueId(), kingdomName, true);
					if(assignStatus != 0) {
						ChatUtil.printDebug("Failed to assign player "+bukkitPlayer.getName()+" to created kingdom "+kingdomName+", status code "+assignStatus);
					}
					// Make player master of kingdom
					newKingdom.forceMaster(bukkitPlayer.getUniqueId());
					// Make player lord of capital
					newKingdom.getCapital().setPlayerLord(bukkitPlayer);
				}
				// Update border particles
				earth.getTerritoryManager().updatePlayerBorderParticles(newKingdom.getCapital().getCenterLoc());
				newKingdom.getCapital().updateBarPlayers();
			} else {
				// Failed to pass all init checks, remove the kingdom
				ChatUtil.printDebug("Kingdom capital init failed, error code: "+initStatus);
				newKingdom.removeCapital();
				kingdomMap.remove(kingdomName);
				newKingdom = null;
				/*
				 * 			1 - error, monument did not paste correctly
				 * 			2 - error, bad town height
	 			 * 			3 - error, bad chunks
				 * 			4 - error, too much air below town
	 			 * 			5 - error, too much water below town
	 			 * 			6 - error, containers below monument
	 			 * 		    7 - error, monument template is invalid
				 * 		  	11 - error, monument invalid
				 * 	  		12 - error, monument gradient
				 * 	  		13 - error, monument bad location (bedrock, outside gradient)
				 */
				return 10+initStatus;
			}
		} else {
			return -1;
		}
		return 0;
	}
	
	/**
	 * Deletes a kingdom (plus towns) and exiles all members.
	 * 
	 * @param name - The name of the kingdom to delete
	 * @return True when the deletion was successful, else false
	 */
	public boolean removeKingdom(String name) {
		if(kingdomMap.containsKey(name)) {
			KonKingdom kingdom = kingdomMap.get(name);
			// Clear all membership
			kingdom.clearMaster();
			kingdom.clearMembers();
			// Manually exile all members
			//TODO: Keep an eye on this. 
			// The allPlayers map should contain online KonPlayers as well.
			// Modifying a KonOfflinePlayer from the allPlayers map should reflect on any KonPlayers in
			// the onlinePlayer map too.
			for(KonOfflinePlayer member : earth.getPlayerManager().getAllPlayersInKingdom(kingdom)) {
				String playerName = member.getOfflineBukkitPlayer().getName();
				member.setKingdom(getBarbarians());
				member.setExileKingdom(getBarbarians());
				member.setBarbarian(true);
				if(member instanceof KonPlayer) {
					ChatUtil.printDebug("Removing online KonPlayer "+playerName+" to barbarian.");
					KonPlayer onlinePlayer = (KonPlayer)member;
					// Online-only updates
					onlinePlayer.getPlayerPrefix().setEnable(false);
					earth.updateNamePackets(onlinePlayer);
					ChatUtil.sendNotice(onlinePlayer.getBukkitPlayer(),MessagePath.GENERIC_NOTICE_FORCE_BARBARIAN.getMessage());
				} else {
					ChatUtil.printDebug("Removing offline KonOfflinePlayer "+playerName+" to barbarian.");
				}
				earth.getDatabaseThread().getDatabase().setOfflinePlayer(member); // push to database
			}
			// Remove all towns
			for(KonTown town : kingdom.getTowns()) {
				removeTown(town.getName(),kingdom.getName());
			}
			// Clear all relationships
			kingdom.clearActiveRelations();
			kingdom.clearRelationRequests();
			for(KonKingdom otherKingdom : kingdomMap.values()) {
				otherKingdom.removeActiveRelation(kingdom);
				otherKingdom.removeRelationRequest(kingdom);
			}
			// Delete the kingdom
			kingdom = null;
			KonKingdom oldKingdom = kingdomMap.remove(name);
			if(oldKingdom != null) {
				// Remove capital
				oldKingdom.removeCapital();
				earth.getTerritoryManager().removeAllTerritory(oldKingdom.getCapital().getWorld(),oldKingdom.getCapital().getChunkList().keySet());
				earth.getMapHandler().drawRemoveTerritory(oldKingdom.getCapital());
				oldKingdom = null;
				// Update particle borders of everyone
				// All kingdom's towns and capital were removed, so just update everyone's border particles.
				for(KonPlayer player : earth.getPlayerManager().getPlayersOnline()) {
					earth.getTerritoryManager().updatePlayerBorderParticles(player);
				}
				ChatUtil.printDebug("Removed Kingdom "+name);
				return true;
			}
			ChatUtil.printDebug("Failed to remove null Kingdom "+name);
		}
		ChatUtil.printDebug("Failed to remove unknown Kingdom "+name);
		return false;
	}
	
	/**
	 * Renames a kingdom
	 * @param oldName		The original name of the kingdom
	 * @param newName		The new name for the kingdom
	 * @param player		The player performing the name change, can be null
	 * @param ignoreCost	Bypass cost checks when true
	 * @return Status code
	 * 			0 - Success
	 * 			1 - Unknown kingdom
	 * 			2 - Invalid new name
	 * 			3 - Not enough favor
	 */
	public int renameKingdom(String oldName, String newName, KonPlayer player, boolean ignoreCost) {
		
		if(!isKingdom(oldName)) {
			return 1;
		}
				
		if(earth.validateNameConstraints(newName) != 0) {
			return 2;
		}
		
		// Check cost
		if(!ignoreCost && costRename > 0 && player != null && EarthPlugin.getBalance(player.getBukkitPlayer()) < costRename) {
            return 3;
    	}
		
		// Perform rename
		KonKingdom oldKingdom = getKingdom(oldName);
		for (KonTown town : oldKingdom.getTowns()) {
			earth.getMapHandler().drawRemoveTerritory(town);
		}
		earth.getMapHandler().drawRemoveTerritory(oldKingdom.getCapital());
		getKingdom(oldName).setName(newName);
		KonKingdom kingdom = kingdomMap.remove(oldName);
		kingdomMap.put(newName, kingdom);
		kingdom.getCapital().updateName();
		earth.getMapHandler().drawUpdateTerritory(kingdom.getCapital());
		for (KonTown town : kingdom.getTowns()) {
			earth.getMapHandler().drawUpdateTerritory(town);
		}
		earth.getDatabaseThread().getDatabase().setOfflinePlayers(earth.getPlayerManager().getAllPlayersInKingdom(kingdom));
		
		// Withdraw cost
		if(!ignoreCost && costRename > 0 && player != null && EarthPlugin.withdrawPlayer(player.getBukkitPlayer(), costRename)) {
            earth.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)costRename);
		}
		return 0;
	}

	// Returns true when a kingdom is full, either by absolute limit or relative difference to smallest
	public boolean isKingdomMembershipFull(KonKingdom kingdom) {
		int targetKingdomPlayerCount = earth.getPlayerManager().getAllPlayersInKingdom(kingdom).size();
		// Check if max_player_diff is disabled, or if the desired kingdom is within the max diff
		int config_max_player_diff = earth.getCore().getInt(CorePath.KINGDOMS_MAX_PLAYER_DIFF.getPath(),0);
		if(config_max_player_diff > 0) {
			// Find the smallest kingdom, compare player count to this kingdom's
			int smallestKingdomPlayerCount = earth.getPlayerManager().getAllPlayersInKingdom(getSmallestKingdomName()).size();
			if (targetKingdomPlayerCount > smallestKingdomPlayerCount + config_max_player_diff) {
				return true;
			}
		}
		// Check if max_player_limit is disabled, or if the kingdom has absolute max players
		int config_max_player_limit = earth.getCore().getInt(CorePath.KINGDOMS_MAX_PLAYER_LIMIT.getPath(),0);
		if (config_max_player_limit > 0) {
			// Check if the player count is at or above absolute max
			if (targetKingdomPlayerCount >= config_max_player_limit) {
				return true;
			}
		}
		// Passed all checks, kingdom is not full
		return false;
	}

	/**
	 * Checks whether an online player may join the given kingdom,
	 * using common checks. This method is intended for these scenarios:
	 * - Player sending join request (closed kingdom) or trying to join (open kingdom).
	 * - Player accepting a join invite.
	 * The player must be online.
	 * @param player - The online player that is attempting to join
	 * @param kingdom - The target kingdom for joining
	 * @return Status code  0 - success
	 * 						1 - error, player is already a member
	 * 						2 - error, player is a kingdom master of another kingdom
	 * 						3 - error, join cooldown has not expired yet
	 * 						4 - error, no permission to join
	 * 						5 - error, cannot switch kingdoms
	 * 						6 - error, kingdom is at relative/absolute member limit
	 */
	public int isPlayerJoinKingdomAllowed(KonPlayer player, KonKingdom kingdom) {
		UUID id = player.getBukkitPlayer().getUniqueId();
		// Check that player is not a member
		if(kingdom.equals(player.getKingdom()) || kingdom.isMember(id)) {
			return 1;
		}
		// Check for player that's already a kingdom master
		for(KonKingdom otherKingdom : getKingdoms()) {
			if(otherKingdom.isMaster(id)) {
				return 2;
			}
		}
		// Check for cool-down
		if(isPlayerJoinCooldown(player.getBukkitPlayer())) {
			return 3;
		}
		// Check for permission (online only)
		boolean isPerKingdomJoin = earth.getCore().getBoolean(CorePath.KINGDOMS_PER_KINGDOM_JOIN_PERMISSIONS.getPath(),false);
		String permission = "earth.join."+kingdom.getName().toLowerCase();
		if(isPerKingdomJoin && !player.getBukkitPlayer().hasPermission(permission)) {
			return 4;
		}
		// Check switching criteria
		boolean isAllowSwitch = earth.getCore().getBoolean(CorePath.KINGDOMS_ALLOW_EXILE_SWITCH.getPath(),false);
		if(!isAllowSwitch && !kingdom.equals(player.getExileKingdom()) && !getBarbarians().equals(player.getExileKingdom())) {
			return 5;
		}
		// Check if max_player_diff is disabled, or if the desired kingdom is within the max diff (both online & offline)
		// Also checks for maximum absolute limit
		if(isKingdomMembershipFull(kingdom)) {
			return 6;
		}
		// Finished all checks
		return 0;
	}

	/*
	 * =================================================
	 * Player Assignment Methods
	 * =================================================
	 */

	/*
	 * 1. Assign player to kingdom
	 * 		Player can be barbarian or kingdom member, but NOT master
	 * 2. Exile player to barbarians
	 * 		Player MUST be kingdom member, but NOT master
	 * 
	 * Players have two states: Barbarian or Kingdom Member.
	 * When a player wants to join a kingdom, the assignment method handles any 
	 * previous kingdom and joins the player to the new one.
	 * When a player wants to just leave a kingdom and have no kingdom, they become 
	 * barbarian by the exile method.
	 * 
	 * Players that are kingdom masters cannot exile/become barbarians.
	 * If the player is initiating the exile, they must first transfer master or disband.
	 * If an admin or earth initiates a forced exile, then master is auto
	 * transferred to another member, or if no members exist, disband the kingdom.
	 * 
	 * Similarly, players that are kingdom masters cannot join other kingdoms.
	 * If the player tries to join another kingdom, they must first transfer master or disband.
	 * If an admin or earth forces the player to join, then master is auto
	 * transferred to another member, or if no members exist, disband the kingdom.
	 */
	
	
	/**
	 * Primary method for adding a player member to a kingdom.
	 * Assign a player to a kingdom.
	 * Optionally checks for join permissions based on Earth configuration.
	 * Optionally enforces maximum kingdom membership difference based on Earth configuration.
	 * Applies join cool-down.
	 * 
	 * @param id The player to assign, by UUID
	 * @param kingdomName The kingdom name, case-sensitive
	 * @param force Ignore permission and max membership limits when true
	 * @return status
	 * 				0	- success
	 *  			1 	- kingdom name does not exist
	 *  			2 	- the kingdom is full (config option max_player_diff and max_player_limit)
	 *  			3 	- missing permission
	 *  			4 	- cancelled
	 *  			5 	- joining is denied, player is already member
	 *  			6	- joining is denied, player is a kingdom master
	 *  			7	- joining is denied, failed switch criteria
	 *  			8   - joining is denied, cool-down
	 *  			9 	- Unknown player ID
	 *             -1 	- internal error
	 */
	public int assignPlayerKingdom(UUID id, String kingdomName, boolean force) {
		/*
		 * Constraints for joining a kingdom
		 * 
		 * Player may or may not be a barbarian.
		 * Player must not already be a member of target kingdom.
		 * Player must not be the master of another kingdom, they have to transfer master or disband kingdom first.
		 * Player can join a kingdom if...
		 * 		ExileKingdom is Barbarians (new player)
		 * 		ExileKingdom is the target kingdom (returning from exile)
		 * 		AllowSwitch flag is true
		 * (Optional) Player has permission for the kingdom to join
		 * (Optional) The target kingdom is below max player diff count
		 * 
		 * Joining a new kingdom must ensure the following...
		 * 		Removes player membership from previous kingdom
		 * 		Adds player membership to new kingdom
		 * 		Sets their new ExileKingdom to the target kingdom
		 * 		Removes old town residencies and updates territory bars
		 * 		Updates name packets for the player
		 * 		Updates dynmap label for old & new capitals
		 * 		Updates the smallest kingdom
		 * 		Updates offline protections
		 * 		Updates border particles for player
		 * 		
		 */
    	
		// Verify kingdom exists
		if(!isKingdom(kingdomName)) return 1;
		// Determine offline/online player
		// When an online player is found, modify their KonPlayer object.
		// When an offline player is found, update their KonOfflinePlayer object and push to the database.
		KonOfflinePlayer offlinePlayer = null;
		KonPlayer onlinePlayer = null;
		boolean isOnline = true;
		onlinePlayer = earth.getPlayerManager().getPlayerFromID(id);
		if(onlinePlayer == null) {
			// No online player found, check offline players
			offlinePlayer = earth.getPlayerManager().getOfflinePlayerFromID(id);
			if(offlinePlayer == null) {
				// No player found for the given UUID
				return 9;
			}
			isOnline = false;
		}
		// At this point, either onlinePlayer or offlinePlayer is null
		// isOnline = true -> onlinePlayer is not null
		// isOnline = false -> offlinePlayer is not null
		
		KonKingdom joinKingdom = getKingdom(kingdomName);
		
		// Check for existing membership (both online & offline)
		KonKingdom playerKingdom = isOnline ? onlinePlayer.getKingdom() : offlinePlayer.getKingdom();
		KonKingdom playerExileKingdom = isOnline ? onlinePlayer.getExileKingdom() : offlinePlayer.getExileKingdom();
		if(joinKingdom.equals(playerKingdom) || joinKingdom.isMember(id)) {
			return 5;
		}
		// These checks may be bypassed when forced
		if(!force) {
			// Check for player that's already a kingdom master
			for(KonKingdom kingdom : getKingdoms()) {
				if(kingdom.isMaster(id)) {
					return 6;
				}
			}
			// Check for cool-down
			boolean isCooldown = isOnline ? isPlayerJoinCooldown(onlinePlayer.getBukkitPlayer()) : isPlayerJoinCooldown(offlinePlayer.getOfflineBukkitPlayer());
			if(isCooldown) {
				return 8;
			}
			// Check for permission (online only)
			boolean isPerKingdomJoin = earth.getCore().getBoolean(CorePath.KINGDOMS_PER_KINGDOM_JOIN_PERMISSIONS.getPath(),false);
			if(isOnline && isPerKingdomJoin) {
				String permission = "earth.join."+joinKingdom.getName().toLowerCase();
				if(!onlinePlayer.getBukkitPlayer().hasPermission(permission)) {
					return 3;
				}
			}
			// Check switching criteria
			boolean isAllowSwitch = earth.getCore().getBoolean(CorePath.KINGDOMS_ALLOW_EXILE_SWITCH.getPath(),false);
			if(!isAllowSwitch && !joinKingdom.equals(playerExileKingdom) && !getBarbarians().equals(playerExileKingdom)) {
				return 7;
			}
			// Check if max_player_diff is disabled, or if the desired kingdom is within the max diff (both online & offline)
			if(isKingdomMembershipFull(joinKingdom)) {
				return 2;
			}
			// Fire event (online only)
			if(isOnline) {
				EarthPlayerKingdomEvent invokeEvent = new EarthPlayerKingdomEvent(earth, onlinePlayer, joinKingdom, onlinePlayer.getExileKingdom());
				Earth.callEarthEvent(invokeEvent);
				if(invokeEvent.isCancelled()) {
					return 4;
				}
			}
		}
		
		/* Passed all checks */
		// playerKingdom could be Barbarians
		
		// Remove any barbarian camps
		earth.getCampManager().removeCamp(id.toString());

		// Only perform membership updates on created kingdoms
		if(playerKingdom.isCreated()) {
			// Remove membership from old kingdom
			String oldKingdomName = playerKingdom.getName();
			boolean removeStatus;
			// The player could be master here
			// Potentially transfer master or disband old kingdom when force = true
			if(isOnline) {
				removeStatus = removeKingdomMember(onlinePlayer, playerKingdom, force);
			} else {
				removeStatus = removeKingdomMember(offlinePlayer, playerKingdom, force);
			}
			if(removeStatus) {
				if(isOnline) {
					ChatUtil.sendNotice(onlinePlayer.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_NOTICE_REMOVE_MEMBER.getMessage(oldKingdomName));
				}
	    	} else {
	    		// Something went very wrong, the prior checks should have avoided this
	    		ChatUtil.printDebug("Failed to remove member "+ id +" from kingdom "+playerKingdom.getName());
	    		return -1;
	    	}
		}

		// Add membership to new kingdom
    	boolean memberStatus = joinKingdom.addMember(id, false);
    	if(!memberStatus) {
    		ChatUtil.printDebug("Failed to add member "+ id +" to kingdom "+joinKingdom.getName());
    		return -1;
    	}
    	
    	if(isOnline) {
    		onlinePlayer.setKingdom(joinKingdom);
    		onlinePlayer.setExileKingdom(joinKingdom);
    		onlinePlayer.setBarbarian(false);
    		// Refresh any territory bars
    		KonTerritory territory = earth.getTerritoryManager().getChunkTerritory(onlinePlayer.getBukkitPlayer().getLocation());
    		if(territory != null && territory instanceof KonBarDisplayer) {
    			((KonBarDisplayer)territory).removeBarPlayer(onlinePlayer);
    			((KonBarDisplayer)territory).addBarPlayer(onlinePlayer);
    		}
        	// Updates
        	updateKingdomOfflineProtection();
        	earth.updateNamePackets(onlinePlayer);
        	earth.getTerritoryManager().updatePlayerBorderParticles(onlinePlayer);
        	// Apply cool-down
        	if(!force) {
        		applyPlayerJoinCooldown(onlinePlayer.getBukkitPlayer());
        	}
    	} else {
    		offlinePlayer.setKingdom(joinKingdom);
    		offlinePlayer.setExileKingdom(joinKingdom);
    		offlinePlayer.setBarbarian(false);
    		earth.getDatabaseThread().getDatabase().setOfflinePlayer(offlinePlayer);
    	}
    	
    	earth.getMapHandler().drawLabel(joinKingdom.getCapital());
    	updateSmallestKingdom();
    	
		return 0;
	}
	
	/**
	 * Exiles a player to be a Barbarian.
	 * Sets their exileKingdom value to their current Kingdom.
	 * (Optionally) Teleports to a random Wild location.
	 * (Optionally) Removes all stats and disables prefix.
	 * (Optionally) Removes all favor.
	 * (Optionally) Resets their exileKingdom to Barbarians, making them look like a new player to Earth.
	 * Applies exile cool-down timer.
	 * Method call setBedSpawnLocation() is known to be deprecated.
	 * 
	 * @param id The UUID of the player who is being exiled
	 * @param teleport Teleport the player based on Earth configuration when true
	 * @param clearStats Remove all player stats and prefix when true
	 * @param isFull Perform a full exile such that the player has no exile kingdom, like they just joined the server
	 * @param force Ignore most checks when true
	 * @return status
	 * 				0	- success
	 * 				1	- Player is already a barbarian
	 * 				4	- cancelled by event
	 * 				6	- Exile denied, player is a kingdom master
	 * 				8   - Cool-down remaining
	 * 				9   - Unknown player ID
	 *             -1 	- internal error
	 */
	@SuppressWarnings("deprecation")
	public int exilePlayerBarbarian(UUID id, boolean teleport, boolean clearStats, boolean isFull, boolean force) {
		
		// Determine offline/online player
		KonOfflinePlayer offlinePlayer = null;
		KonPlayer onlinePlayer = null;
		boolean isOnline = false;
		onlinePlayer = earth.getPlayerManager().getPlayerFromID(id);
		if(onlinePlayer == null) {
			// No online player found, check offline players
			offlinePlayer = earth.getPlayerManager().getOfflinePlayerFromID(id);
			if(offlinePlayer == null) {
				// No player found for the given UUID
				return 9;
			}
		} else {
			// Found an online player
			isOnline = true;
		}
		// Check for barbarian player or default kingdom
		KonKingdom oldKingdom = isOnline ? onlinePlayer.getKingdom() : offlinePlayer.getKingdom();
		String oldKingdomName = oldKingdom.getName();
		boolean isPlayerBarbarian = isOnline ? onlinePlayer.isBarbarian() : offlinePlayer.isBarbarian();
		if(isPlayerBarbarian || !oldKingdom.isCreated()) {
    		return 1;
    	}
		// These checks are bypassed when forced
		if(!force) {
			// Check for player that's already a kingdom master
			for(KonKingdom kingdom : getKingdoms()) {
				if(kingdom.isMaster(id)) {
					return 6;
				}
			}
			// Check for cool-down
			if(isOnline && isPlayerExileCooldown(onlinePlayer.getBukkitPlayer())) {
				return 8;
			}
			// Fire event (online and offline)
			EarthPlayerExileEvent invokeEvent = new EarthPlayerExileEvent(earth, offlinePlayer, oldKingdom);
			Earth.callEarthEvent(invokeEvent);
			if(invokeEvent.isCancelled()) {
				return 4;
			}
		}

		/* At this point, oldKingdom should be a player-created kingdom and most checks passed */
    	
		// Remove membership from old kingdom
    	// Now, the player should be in a created kingdom with a membership.
    	boolean removeStatus;
		// The player could be master here
		// Potentially transfer master or disband old kingdom
		if(isOnline) {
			removeStatus = removeKingdomMember(onlinePlayer, oldKingdom, force);
		} else {
			removeStatus = removeKingdomMember(offlinePlayer, oldKingdom, force);
		}
		if(!removeStatus) {
    		// Something went very wrong, the prior checks should have avoided this
    		ChatUtil.printDebug("Failed to remove member "+id.toString()+" from kingdom "+oldKingdom.getName());
    		return -1;
    	}

    	// Try to teleport the player (online only)
		if(isOnline) {
			World playerWorld = onlinePlayer.getBukkitPlayer().getLocation().getWorld();
			if (teleport && playerWorld != null && earth.isWorldValid(playerWorld)) {
				boolean doWorldSpawn = earth.getCore().getBoolean(CorePath.EXILE_TELEPORT_WORLD_SPAWN.getPath(), false);
				boolean doWildTeleport = earth.getCore().getBoolean(CorePath.EXILE_TELEPORT_WILD.getPath(), true);
				Location exileLoc = null;
				if (doWorldSpawn) {
					exileLoc = playerWorld.getSpawnLocation();
				} else if (doWildTeleport) {
					exileLoc = earth.getRandomWildLocation(playerWorld);
				} else {
					ChatUtil.printDebug("Teleport for player " + onlinePlayer.getBukkitPlayer().getName() + " on exile is disabled.");
				}
				if (exileLoc != null) {
					earth.telePlayerLocation(onlinePlayer.getBukkitPlayer(), exileLoc);
					onlinePlayer.getBukkitPlayer().setBedSpawnLocation(exileLoc, true);
				} else {
					ChatUtil.printDebug("Could not teleport player " + onlinePlayer.getBukkitPlayer().getName() + " on exile, disabled or null location.");
				}
			}
		}
    	
    	// Clear stats and prefix
    	boolean doRemoveStats = earth.getCore().getBoolean(CorePath.EXILE_REMOVE_STATS.getPath(), true);
    	if(doRemoveStats && clearStats) {
    		if(isOnline) {
    			onlinePlayer.getPlayerStats().clearStats();
    			onlinePlayer.getPlayerPrefix().setEnable(false);
				ChatUtil.sendNotice(onlinePlayer.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_NOTICE_EXILE_STATS.getMessage());
    		} else {
    			KonStats stats = earth.getDatabaseThread().getDatabase().pullPlayerStats(offlinePlayer.getOfflineBukkitPlayer());
    			stats.clearStats();
    			earth.getDatabaseThread().getDatabase().pushPlayerStats(offlinePlayer.getOfflineBukkitPlayer(), stats);
    			//TODO: KR Add means to clear prefix in database
    		}
    	}
    	
    	// Remove favor
    	boolean doRemoveFavor = earth.getCore().getBoolean(CorePath.EXILE_REMOVE_FAVOR.getPath(), true);
    	OfflinePlayer offlineBukkitPlayer = isOnline ? onlinePlayer.getOfflineBukkitPlayer() : offlinePlayer.getOfflineBukkitPlayer();
    	if(doRemoveFavor) {
			double balance = EarthPlugin.getBalance(offlineBukkitPlayer);
			EarthPlugin.withdrawPlayer(offlineBukkitPlayer, balance);
			if(isOnline) {
				ChatUtil.sendNotice(onlinePlayer.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_NOTICE_EXILE_FAVOR.getMessage());
			}
		}

		// Determine the exile kingdom
		KonKingdom exileKingdom = getBarbarians();
		if (!isFull && isKingdom(oldKingdomName)) {
			exileKingdom = oldKingdom;
		}

		// Update player info
    	if(isOnline) {
    		// Force into global chat mode
    		onlinePlayer.setIsGlobalChat(true);
        	// Make into barbarian
			onlinePlayer.setBarbarian(true);
			onlinePlayer.setExileKingdom(exileKingdom);
    		onlinePlayer.setKingdom(getBarbarians());
    		// Territory updates at player's location
    		KonTerritory territory = earth.getTerritoryManager().getChunkTerritory(onlinePlayer.getBukkitPlayer().getLocation());
    		if(territory instanceof KonBarDisplayer) {
				// Update display bars
				KonBarDisplayer barDisplayer = (KonBarDisplayer)territory;
				barDisplayer.removeBarPlayer(onlinePlayer);
				barDisplayer.addBarPlayer(onlinePlayer);
    		}
			if(territory instanceof KonTown) {
				// Send a raid alert (barbarians are always a threat to towns)
				KonTown town = (KonTown)territory;
				town.sendRaidAlert();
			}
    		// General Updates
    		earth.updateNamePackets(onlinePlayer);
    		earth.getTerritoryManager().updatePlayerBorderParticles(onlinePlayer);
    		// Cool-down
    		if(!force) {
    			applyPlayerExileCooldown(onlinePlayer.getBukkitPlayer());
    		}
			ChatUtil.sendNotice(onlinePlayer.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_NOTICE_EXILE.getMessage(oldKingdomName));
    	} else {
    		offlinePlayer.setKingdom(getBarbarians());
    		offlinePlayer.setExileKingdom(exileKingdom);
    		offlinePlayer.setBarbarian(true);
    		earth.getDatabaseThread().getDatabase().setOfflinePlayer(offlinePlayer);
    	}
    	
    	// Common updates
    	earth.getMapHandler().drawLabel(getKingdom(oldKingdomName).getCapital());
    	updateSmallestKingdom();
    	updateKingdomOfflineProtection();
    	
    	return 0;
	}
	
	/*
	 * ===================================
	 * Menu Membership Methods
	 * ===================================
	 * These methods are used by menu icons for membership management.
	 * Players requesting/invited to join, kicking members, promotions/demotions, etc.
	 */

	/**
	 * Online Player requests to join the kingdom to the officers.
	 * This method is meant to be called via the GUI menu.
	 * When the kingdom is closed, a join request is sent to officers.
	 * When open, the player instantly joins.
	 * @param player - The online player requesting to join the kingdom
	 * @param kingdom - The target kingdom that the player requests to join
	 */
	public boolean menuJoinKingdomRequest(KonPlayer player, KonKingdom kingdom) {
		if(kingdom == null) return false;
		UUID id = player.getBukkitPlayer().getUniqueId();
		// Player can be barbarians, or members of other kingdoms, when requesting to join a kingdom.

		// Check for join/leave property flags, only for created kingdoms (not barbarians)
		if((kingdom.isCreated() && !kingdom.isJoinable()) || (player.getKingdom().isCreated() && !player.getKingdom().isLeaveable())) {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}

		// Pre-check for allowable join conditions
		// These checks are made again within assignPlayerKingdom, but we need to notify the player here of any errors
		int joinCheckStatus = isPlayerJoinKingdomAllowed(player, kingdom);
		if(joinCheckStatus != 0) {
			switch(joinCheckStatus) {
				case 1:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_MEMBER.getMessage());
					break;
				case 2:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_MASTER.getMessage());
					break;
				case 3:
					String remainCooldown = getJoinCooldownRemainingTimeFormat(player.getBukkitPlayer(),ChatColor.RED);
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_COOLDOWN.getMessage(remainCooldown));
					break;
				case 4:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_PERMISSION.getMessage());
					break;
				case 5:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_SWITCH.getMessage());
					break;
				case 6:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_LIMIT.getMessage(kingdom.getName()));
					break;
				default:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
					break;
			}
			return false;
		}
		// Join logic
		if(kingdom.isOpen() || kingdom.isJoinInviteValid(id)) {
			// There is already a valid invite, assign the player to the kingdom
			int status = assignPlayerKingdom(id,kingdom.getName(),false);
			kingdom.removeJoinRequest(id);
			if(status != 0) {
				// Any errors here should have already been caught by the pre-check above
				ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
				return false;
			}
			// Broadcast successful join
			ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_JOIN.getMessage(player.getBukkitPlayer().getName(),kingdom.getName()));
		} else if(!kingdom.isJoinRequestValid(id)){
			// Request to join if not already requested
			kingdom.addJoinRequest(id, false);
			ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_NOTICE_REQUEST_SENT.getMessage(kingdom.getName()));
			broadcastOfficers(kingdom, MessagePath.COMMAND_KINGDOM_NOTICE_REQUEST_RECEIVED.getMessage());
		} else {
			// A join request has already been sent, do nothing
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_REQUEST_EXISTS.getMessage(kingdom.getName()));
			return false;
		}
		return true;
	}
	
	/**
	 * Officer approves or denies join request of (offline) player.
	 * Check to ensure target player is not already a member.
	 * @param player - The officer player responding to the request
	 * @param requester - The offline player target of the response
	 * @param kingdom - The target kingdom that player wants to join
	 * @param resp - True to approve, false to reject request
	 * @return True when request response was successful, else false when target is already in this kingdom.
	 */
	public boolean menuRespondKingdomRequest(KonPlayer player, KonOfflinePlayer requester, KonKingdom kingdom, boolean resp) {
		if(kingdom == null) return true;
		OfflinePlayer offlineBukkitPlayer = requester.getOfflineBukkitPlayer();
		UUID id = offlineBukkitPlayer.getUniqueId();

		// Check for valid request
		if(!kingdom.isJoinRequestValid(id)) {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_FAILED.getMessage());
			return false;
		}
		kingdom.removeJoinRequest(id);
		// Is the request denied?
		if(!resp){
			// Notify officer
			ChatUtil.sendNotice(player.getBukkitPlayer(),MessagePath.COMMAND_KINGDOM_NOTICE_REQUEST_DECLINED.getMessage(offlineBukkitPlayer.getName()));
			// Notify requester
			if(offlineBukkitPlayer.isOnline()) {
				ChatUtil.sendError((Player)offlineBukkitPlayer, MessagePath.COMMAND_KINGDOM_ERROR_REQUEST_DENY.getMessage(kingdom.getName()));
			}
			return false;
		}
		// Check the requester is not already a kingdom member
		if(kingdom.isMember(id)) {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_INVITE_MEMBER.getMessage(offlineBukkitPlayer.getName()));
			return false;
		}
		// Check for join/leave property flags, only for created kingdoms (not barbarians)
		if((kingdom.isCreated() && !kingdom.isJoinable()) || (requester.getKingdom().isCreated() && !requester.getKingdom().isLeaveable())) {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}

		int status = assignPlayerKingdom(id,kingdom.getName(),false);
		if(status != 0) {
			switch(status) {
				case 2:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_LIMIT.getMessage(kingdom.getName()));
					break;
				case 3:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_PERMISSION.getMessage());
					break;
				case 6:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_MASTER.getMessage());
					break;
				case 7:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_SWITCH.getMessage());
					break;
				case 8:
					String remainCooldown = getJoinCooldownRemainingTimeFormat(offlineBukkitPlayer,ChatColor.RED);
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_COOLDOWN.getMessage(remainCooldown));
					break;
				default:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
					break;
			}
			return false;
		}
		// Broadcast successful join
		ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_JOIN.getMessage(offlineBukkitPlayer.getName(),kingdom.getName()));
		return true;
	}
	
	/**
	 * Officer invites (offline) player to join.
	 * Kingdom must be closed to send invites.
	 * Allow invites to players already in another kingdom.
	 * Inviting a player who has already requested to join always lets them join.
	 * @param player - The officer doing the invite
	 * @param invitee - The offline player that the officer invites to join their kingdom
	 * @param kingdom - The target kingdom for the join invite
	 * @return True when the invite was successful
	 */
	public boolean joinKingdomInvite(KonPlayer player, KonOfflinePlayer invitee, KonKingdom kingdom) {
		if(kingdom == null) return false;
		OfflinePlayer offlineBukkitPlayer = invitee.getOfflineBukkitPlayer();
		UUID id = offlineBukkitPlayer.getUniqueId();

		// Check for join/leave property flags, only for created kingdoms (not barbarians)
		if((kingdom.isCreated() && !kingdom.isJoinable()) || (invitee.getKingdom().isCreated() && !invitee.getKingdom().isLeaveable())) {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}
		// Cannot invite a member of this kingdom
		if(kingdom.isMember(id)) {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_INVITE_MEMBER.getMessage(offlineBukkitPlayer.getName()));
			return false;
		}
		// Cannot invite members to an open kingdom
		if(kingdom.isOpen()) {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_INVITE_OPEN.getMessage());
			return false;
		}
		
		if(kingdom.isJoinRequestValid(id)) {
			// There is already a valid request, add the player to the kingdom
			int status = assignPlayerKingdom(id,kingdom.getName(),false);
			kingdom.removeJoinRequest(id);
			if(status != 0) {
				switch(status) {
					case 2:
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_LIMIT.getMessage(kingdom.getName()));
						break;
					case 3:
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_PERMISSION.getMessage());
						break;
					case 6:
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_MASTER.getMessage());
						break;
					case 7:
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_SWITCH.getMessage());
						break;
					case 8:
						String remainCooldown = getJoinCooldownRemainingTimeFormat(offlineBukkitPlayer,ChatColor.RED);
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_COOLDOWN.getMessage(remainCooldown));
						break;
					default:
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
						break;
				}
				return false;
			}
			// Broadcast successful join
			ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_JOIN.getMessage(offlineBukkitPlayer.getName(),kingdom.getName()));
		} else if(!kingdom.isJoinInviteValid(id)) {
			// Invite to join if not already invited
			kingdom.addJoinRequest(id, true);
			if(offlineBukkitPlayer.isOnline()) {
				ChatUtil.sendNotice((Player)offlineBukkitPlayer, MessagePath.COMMAND_KINGDOM_NOTICE_INVITE_RECEIVED.getMessage());
			}
		} else {
			// The invite has already been sent, still pending
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_INVITE_EXISTS.getMessage(offlineBukkitPlayer.getName()));
			return false;
		}
		return true;
	}
	
	/**
	 * Player accepts or declines join invite
	 * @param player - The online player responding to the invite to join
	 * @param kingdom - The target kingdom that the player is responding to
	 * @param resp - True to accept and join kingdom, false to decline invite
	 */
	public boolean menuRespondKingdomInvite(KonPlayer player, KonKingdom kingdom, boolean resp) {
		if(kingdom == null) return false;
		UUID id = player.getBukkitPlayer().getUniqueId();

		// Check for valid invitation to join
		if(!kingdom.isJoinInviteValid(id)) {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_FAILED.getMessage());
			return false;
		}
		kingdom.removeJoinRequest(id);
		if(!resp){
			// Denied join request
			ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_NOTICE_INVITE_DECLINED.getMessage(kingdom.getName()));
			return false;
		}
		// Check for join/leave property flags, only for created kingdoms (not barbarians)
		if((kingdom.isCreated() && !kingdom.isJoinable()) || (player.getKingdom().isCreated() && !player.getKingdom().isLeaveable())) {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}

		int status = assignPlayerKingdom(id,kingdom.getName(),false);
		if(status != 0) {
			switch(status) {
				case 2:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_LIMIT.getMessage(kingdom.getName()));
					break;
				case 3:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_PERMISSION.getMessage());
					break;
				case 5:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_MEMBER.getMessage());
					break;
				case 6:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_MASTER.getMessage());
					break;
				case 7:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_SWITCH.getMessage());
					break;
				case 8:
					String remainCooldown = getJoinCooldownRemainingTimeFormat(player.getBukkitPlayer(),ChatColor.RED);
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_COOLDOWN.getMessage(remainCooldown));
					break;
				default:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
					break;
			}
			return false;
		}
		// Broadcast successful join
		ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_JOIN.getMessage(player.getBukkitPlayer().getName(),kingdom.getName()));
		return true;
	}
	
	// Effectively just a wrapper for exile for online players by their choice.
	// This method is called when a player chooses the exile button in the kingdom menu.
	public boolean menuExileKingdom(KonPlayer player) {
		UUID id = player.getBukkitPlayer().getUniqueId();
		// Check for leave property flag
		if(!player.getKingdom().isLeaveable()) {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}
		// Attempt to exile player, sends notice
		int status = exilePlayerBarbarian(id,true,true,false,false);
		if(status == 0) {
			return true;
		} else {
			switch(status) {
				case 1:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
					break;
				case 4:
					// Cancelled by event, do nothing
					break;
				case 6:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_KICK_MASTER.getMessage());
					break;
				case 8:
					String remainCooldown = getExileCooldownRemainingTimeFormat(player.getBukkitPlayer(), ChatColor.RED);
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_EXILE_COOLDOWN.getMessage(remainCooldown));
					break;
				default:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
					break;
			}
			return false;
		}
	}
	
	// Wrapper for exile for offline players
	// Used by officers to remove players from their kingdom
	public boolean kickKingdomMember(KonPlayer player, OfflinePlayer member) {
		// Assume that the member is in the same kingdom as the player
		// Check for leave property flag
		if(!player.getKingdom().isLeaveable()) {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}
		int status = exilePlayerBarbarian(member.getUniqueId(),true,false,false,false);
		if(status == 0) {
			return true;
		} else {
			switch (status) {
				case 1:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_KICK_BARBARIAN.getMessage());
					break;
				case 4:
					// Cancelled by event, do nothing
					break;
				case 6:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_KICK_MASTER.getMessage());
					break;
				case 8:
					String remainCooldown = getExileCooldownRemainingTimeFormat(member, ChatColor.RED);
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_EXILE_COOLDOWN.getMessage(remainCooldown));
					break;
				default:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
					break;
			}
			return false;
		}
	}
	
	/**
	 * Primary method for removing a player from a kingdom
	 * Removes a kingdom member and handles master transfers when forced.
	 * When force is false, remove the player membership and set to barbarian if player is not master.
	 * When force is true, remove player and attempt to transfer master if applicable.
	 * If player is master, attempts to transfer master to officers first, then members.
	 * If no other members than master, removes the kingdom.
	 * If the kingdom is an admin kingdom, do not remove it.
	 * 
	 * @param player - The player to remove
	 * @param kingdom - the kingdom to remove the player from
	 * @param force - Whether to remove masters and transfer master or not
	 */
	private boolean removeKingdomMember(KonOfflinePlayer player, KonKingdom kingdom, boolean force) {
		if(kingdom == null || !kingdom.isCreated()) return false;
		UUID playerID = player.getOfflineBukkitPlayer().getUniqueId();
		String playerName = player.getOfflineBukkitPlayer().getName();
		String kingdomName = kingdom.getName();
		boolean isPlayerMaster = kingdom.isMaster(playerID);

		// Check for master while not forced
		if(isPlayerMaster && !force) {
			ChatUtil.printDebug("Failed to remove master player "+playerName+" from kingdom "+kingdomName+", force = false");
			return false;
		}

		// First, try to transfer master to another player (or remove the kingdom) when not admin operated
		if(isPlayerMaster && !kingdom.isAdminOperated()) {
			ChatUtil.printDebug("Attempting to transfer master in kingdom "+kingdomName+", force = true");
			// Player is master, transfer if possible
			List<OfflinePlayer> officers = kingdom.getPlayerOfficersOnly();
			if(!officers.isEmpty()) {
				// Make the first officer into the master
				OfflinePlayer newMaster = officers.get(0);
				boolean officerToMasterStatus = kingdom.setMaster(newMaster.getUniqueId());
				if(officerToMasterStatus) {
					// Master was successfully transferred
					broadcastMembers(kingdom,MessagePath.COMMAND_KINGDOM_BROADCAST_TRANSFER.getMessage(newMaster.getName(),kingdomName));
					ChatUtil.printDebug("Transferred master to officer player "+newMaster.getName()+" in kingdom "+kingdomName);
				} else {
					ChatUtil.printDebug("Could not transfer master to officer player "+newMaster.getName()+" in kingdom "+kingdomName);
					return false;
				}
			} else {
				// There are no officers
				List<OfflinePlayer> members = kingdom.getPlayerMembersOnly();
				if(!members.isEmpty()) {
					// Make the first member into the master
					OfflinePlayer newMaster = members.get(0);
					boolean memberToMasterStatus = kingdom.setMaster(newMaster.getUniqueId());
					if(memberToMasterStatus) {
						// Master was successfully transferred
						broadcastMembers(kingdom,MessagePath.COMMAND_KINGDOM_BROADCAST_TRANSFER.getMessage(newMaster.getName(),kingdomName));
						ChatUtil.printDebug("Transferred master to member player "+newMaster.getName()+" in kingdom "+kingdomName);
					} else {
						ChatUtil.printDebug("Could not transfer master to member player "+newMaster.getName()+" in kingdom "+kingdomName);
						return false;
					}
				} else {
					// There are no members to transfer master to, remove the kingdom
					// This will also exile all players to barbarians, including the given ID
					String name = kingdom.getName();
					if(removeKingdom(name)) {
						ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_DISBAND.getMessage(name));
						ChatUtil.printDebug("Successfully removed all players from kingdom "+kingdomName+", and removed the kingdom");
						return true;
					} else {
						ChatUtil.printDebug("Failed to remove kingdom "+kingdomName+", no officers or members");
						return false;
					}
				}
			}
		}
		// Extra check for master in admin kingdom (shouldn't usually happen)
		if(kingdom.isMaster(playerID) && kingdom.isAdminOperated()) {
			// Clear the master
			kingdom.clearMaster();
		}

		// Second, attempt to remove the player from the kingdom
		boolean result;
		if(kingdom.isMember(playerID)) {
			// The player is a member, remove them
			result = kingdom.removeMember(playerID);
		} else {
			// The player is NOT a member already
			result = true;
		}
		if(result) {
			// Successfully removed membership, set barbarian
			player.setKingdom(getBarbarians());
			player.setExileKingdom(kingdom);
			player.setBarbarian(true);
			// Push updates to database
			earth.getDatabaseThread().getDatabase().setOfflinePlayer(player);
			ChatUtil.printDebug("Successfully removed player "+playerName+" from kingdom "+kingdomName);
		} else {
			// Failed to remove
			ChatUtil.printDebug("Failed to remove player "+playerName+" from kingdom "+kingdomName);
			return false;
		}

		return true;
	}

	// Get all kingdoms that have invited the player to join
	public List<KonKingdom> getInviteKingdoms(KonOfflinePlayer player) {
		List<KonKingdom> result = new ArrayList<>();
		for(KonKingdom kingdom : kingdomMap.values()) {
			if(!player.getKingdom().equals(kingdom) && kingdom.isJoinInviteValid(player.getOfflineBukkitPlayer().getUniqueId())) {
				result.add(kingdom);
			}
		}
		return result;
	}

	public List<String> getInviteKingdomNames(KonOfflinePlayer player) {
		List<String> result = new ArrayList<>();
		for(KonKingdom kingdom : kingdomMap.values()) {
			if(!player.getKingdom().equals(kingdom) && kingdom.isJoinInviteValid(player.getOfflineBukkitPlayer().getUniqueId())) {
				result.add(kingdom.getName());
			}
		}
		return result;
	}

	// Get all towns within the player's kingdom that have invited the player to join as a resident
	public List<KonTown> getInviteTowns(KonOfflinePlayer player) {
		List<KonTown> result = new ArrayList<>();
		for(KonTown town : player.getKingdom().getCapitalTowns()) {
			if(!town.isPlayerResident(player.getOfflineBukkitPlayer()) && town.isJoinInviteValid(player.getOfflineBukkitPlayer().getUniqueId())) {
				result.add(town);
			}
		}
		return result;
	}

	public List<String> getInviteTownNames(KonOfflinePlayer player) {
		List<String> result = new ArrayList<>();
		for(KonTown town : player.getKingdom().getCapitalTowns()) {
			if(!town.isPlayerResident(player.getOfflineBukkitPlayer()) && town.isJoinInviteValid(player.getOfflineBukkitPlayer().getUniqueId())) {
				result.add(town.getName());
			}
		}
		return result;
	}

	// Get all towns within the player's kingdom that have requests to join by other players
	public List<KonTown> getRequestTowns(KonOfflinePlayer player) {
		List<KonTown> result = new ArrayList<>();
		for(KonTown town : getManageTowns(player)) {
			if(town.getJoinRequests().size() > 0) {
				result.add(town);
			}
		}
		return result;
	}

	public List<String> getRequestTownNames(KonOfflinePlayer player) {
		List<String> result = new ArrayList<>();
		for(KonTown town : getManageTowns(player)) {
			if(town.getJoinRequests().size() > 0) {
				result.add(town.getName());
			}
		}
		return result;
	}

	// Get all towns within the player's kingdom, of which the player is a knight or lord
	public List<KonTown> getManageTowns(KonOfflinePlayer player) {
		List<KonTown> result = new ArrayList<>();
		for(KonTown town : player.getKingdom().getCapitalTowns()) {
			if(town.isPlayerKnight(player.getOfflineBukkitPlayer())) {
				result.add(town);
			}
		}
		return result;
	}
	
	public boolean menuPromoteOfficer(OfflinePlayer player, KonKingdom kingdom) {
		UUID id = player.getUniqueId();
		// Check for player that's already an officer
		if(kingdom.isOfficer(id)) return false;
		// Attempt to promote
		if(kingdom.setOfficer(id, true)) {
			broadcastMembers(kingdom, MessagePath.COMMAND_KINGDOM_BROADCAST_PROMOTE.getMessage(player.getName(),kingdom.getName()));
			return true;
		}
		return false;
	}
	
	public boolean menuDemoteOfficer(OfflinePlayer player, KonKingdom kingdom) {
		UUID id = player.getUniqueId();
		// Check for player that's already a member
		if(!kingdom.isOfficer(id)) return false;
		// Check for master
		if(kingdom.isMaster(id)) return false;
		// Attempt to demote
		if(kingdom.setOfficer(id, false)) {
			broadcastMembers(kingdom,MessagePath.COMMAND_KINGDOM_BROADCAST_DEMOTE.getMessage(player.getName(),kingdom.getName()));
			return true;
		}
		return false;
	}
	
	public boolean menuTransferMaster(OfflinePlayer master, KonKingdom kingdom, KonPlayer sender) {
		UUID id = master.getUniqueId();
		if(kingdom.isMember(id) && kingdom.setMaster(id)) {
			ChatUtil.sendNotice(sender.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_NOTICE_TRANSFER_MASTER.getMessage(master.getName(),kingdom.getName()));
			broadcastMembers(kingdom,MessagePath.COMMAND_KINGDOM_BROADCAST_TRANSFER.getMessage(master.getName(),kingdom.getName()));
			return true;
		} else {
			ChatUtil.sendError(sender.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}
	}
	
	private void broadcastMembers(KonKingdom kingdom, String message) {
		if(kingdom == null)return;
		for(OfflinePlayer offlinePlayer : kingdom.getPlayerMembers()) {
			if(offlinePlayer.isOnline()) {
				Player player = (Player)offlinePlayer;
				ChatUtil.sendNotice(player, message);
			}
		}
	}
	
	private void broadcastOfficers(KonKingdom kingdom, String message) {
		if(kingdom == null) return;
		for(OfflinePlayer offlinePlayer : kingdom.getPlayerOfficers()) {
			if(offlinePlayer.isOnline()) {
				Player player = (Player)offlinePlayer;
				ChatUtil.sendNotice(player, message);
			}
		}
	}

	public void menuToggleKingdomOpen(KonKingdom kingdom, KonPlayer player) {
		if(kingdom == null || !kingdom.isCreated())return;
		if(kingdom.isOpen()) {
			kingdom.setIsOpen(false);
			ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_NOTICE_CLOSED.getMessage());
		} else {
			kingdom.setIsOpen(true);
			ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_NOTICE_OPEN.getMessage());
		}
	}
	
	public boolean menuDisbandKingdom(KonKingdom kingdom, KonPlayer player) {
		// Cannot disband: a null kingdom; a non-created (barbarians, neutrals) kingdom; an admin kingdom
		if(kingdom == null || !kingdom.isCreated() || kingdom.isAdminOperated()) return false;
		String kingdomName = kingdom.getName();
		boolean status = removeKingdom(kingdomName);
		if(status) {
			ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_DISBAND.getMessage(kingdomName));
			// Update stats
			earth.getAccomplishmentManager().modifyPlayerStat(player, KonStatsType.KINGDOMS, -1);
			return true;
		} else {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return false;
		}
	}
	
	// The primary way to change a kingdom's template, from the kingdom GUI menu
	public boolean menuChangeKingdomTemplate(KonKingdom kingdom, KonMonumentTemplate template, KonPlayer player, boolean ignoreCost) {
		if(kingdom == null || !kingdom.isCreated()) return false;
		Player bukkitPlayer = player.getBukkitPlayer();
		// Verify template is valid
		if(template == null || !template.isValid()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_FAILED.getMessage());
			return false;
		}
		// Check template is not the current choice
		if(kingdom.getMonumentTemplate().equals(template)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_KINGDOM_ERROR_TEMPLATE_CHOSEN.getMessage());
			return false;
		}
		// Check capital is not under attack
		if(kingdom.getCapital().isAttacked()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_KINGDOM_ERROR_TEMPLATE_ATTACK.getMessage());
			return false;
		}
		// Check cost
		boolean hasTemplate = kingdom.hasMonumentTemplate();
		double totalCost = costTemplate + template.getCost();
		if(!ignoreCost && hasTemplate && totalCost > 0 && EarthPlugin.getBalance(bukkitPlayer) < totalCost) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(totalCost));
			return false;
    	}
		// Update template
		kingdom.updateMonumentTemplate(template);
		// Withdraw cost
		if(!ignoreCost && hasTemplate && totalCost > 0 && EarthPlugin.withdrawPlayer(bukkitPlayer, totalCost)) {
            earth.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)totalCost);
		}
		// Success
		ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_NOTICE_TEMPLATE.getMessage(template.getName()));
		return true;
	}

	/**
	 * Sets a town's trade specialization to a villager profession.
	 *
	 * @param town The town being modified
	 * @param profession The new trade specialization of the town
	 * @param payPlayer The player paying for the change
	 * @param messageSender The recipient of status messages
	 * @param isAdmin Whether to ignore payment
	 * @return True when the change was successful, else false
	 */
	public boolean menuChangeTownSpecialization(KonTown town, Villager.Profession profession, KonPlayer payPlayer, CommandSender messageSender, boolean isAdmin) {
		if (town == null || profession == null) return false;
		double costSpecial = earth.getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SPECIALIZE.getPath());
		// Check cost
		if(costSpecial > 0 && !isAdmin && payPlayer != null) {
			if(EarthPlugin.getBalance(payPlayer.getBukkitPlayer()) < costSpecial) {
				ChatUtil.sendError(messageSender, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(costSpecial));
				return false;
			}
		}
		town.setSpecialization(profession);
		// Withdraw cost
		if(costSpecial > 0 && !isAdmin && payPlayer != null) {
			if(EarthPlugin.withdrawPlayer(payPlayer.getBukkitPlayer(), costSpecial)) {
				earth.getAccomplishmentManager().modifyPlayerStat(payPlayer,KonStatsType.FAVOR,(int)costSpecial);
			}
		}
		String professionName = CompatibilityUtil.getProfessionName(profession);
		ChatUtil.sendNotice(messageSender, MessagePath.COMMAND_TOWN_NOTICE_SPECIALIZE.getMessage(town.getName(),professionName));
		return true;
	}
	// Alternate
	public boolean menuChangeTownSpecialization(KonTown town, Villager.Profession profession, KonPlayer player, boolean isAdmin) {
		if (player == null) return false;
		return menuChangeTownSpecialization(town, profession, player, player.getBukkitPlayer(), isAdmin);
	}
	
	/*
	 * =================================================
	 * Relationship Methods
	 * =================================================
	 */

	/**
	 * This method attempts to update the diplomatic states of two kingdoms.
	 * It handles validation of state changes, instant state changes and diplomatic requests.
	 * A peaceful kingdom (from flag property) is always in the peace relation with other kingdoms.
	 * @param kingdom		The source kingdom initiating the change (ours)
	 * @param otherKingdom	The target kingdom in question (theirs)
	 * @param relation		The new diplomatic state to change to
	 * @param payPlayer		The player performing the change and making payments
	 * @param messageSender The user receiving status messages
	 * @param isAdmin		Whether the player is an admin (to bypass some checks)
	 * @return				True when the change was successful, else false when the change could not be processed
	 */
	public boolean menuChangeKingdomRelation(@Nullable KonKingdom kingdom, @Nullable  KonKingdom otherKingdom, @NotNull EarthDiplomacyType relation, KonPlayer payPlayer, CommandSender messageSender, boolean isAdmin) {
		if(kingdom == null || otherKingdom == null) return false;

		// Check for peaceful kingdom flag
		if(kingdom.isPeaceful() || otherKingdom.isPeaceful()) {
			// Cannot change relation for peaceful kingdoms, revert to default
			ChatUtil.printDebug("Tried to change relation of peaceful kingdom(s), reverting to default.");
			kingdom.removeActiveRelation(otherKingdom);
			otherKingdom.removeActiveRelation(kingdom);
			ChatUtil.sendError(messageSender,MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return false;
		}

		// Check for self relation
		if(kingdom.equals(otherKingdom)) {
			ChatUtil.sendError(messageSender,MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}

		// Check cost
		double costRelation = getRelationCost(relation);
		if(costRelation > 0 && !isAdmin && payPlayer != null) {
			if(EarthPlugin.getBalance(payPlayer.getBukkitPlayer()) < costRelation) {
				ChatUtil.sendError(messageSender, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(costRelation));
                return false;
			}
    	}

		// Our current state with the other kingdom
		EarthDiplomacyType ourActiveState = kingdom.getActiveRelation(otherKingdom);
		// Their current request of us (if any)
		EarthDiplomacyType ourRequestState = kingdom.getRelationRequest(otherKingdom);
		// Their current state with us
		EarthDiplomacyType theirActiveState = otherKingdom.getActiveRelation(kingdom);
		// Our current diplomatic request of them (if any)
		EarthDiplomacyType theirRequestState = otherKingdom.getRelationRequest(kingdom);

		ChatUtil.printDebug("Attempting to change kingdom "+kingdom.getName()+" relation to "+otherKingdom.getName()+": "+relation.toString());
		ChatUtil.printDebug("Our states ["+ourActiveState+","+ourRequestState+"]; Their states ["+theirActiveState+","+theirRequestState+"]");

		// Check for valid relation change given current states
		if(!isValidRelationChoice(kingdom, otherKingdom, relation)) {
			ChatUtil.sendError(messageSender, MessagePath.COMMAND_KINGDOM_ERROR_DIPLOMACY_INVALID.getMessage());
			return false;
		}

		// Verify allowable current state combination
		if(!ourActiveState.equals(theirActiveState)) {
			// There is a relation mismatch, revert to default
			ChatUtil.printDebug("Found invalid state combination, reverting to default.");
			kingdom.removeActiveRelation(otherKingdom);
			otherKingdom.removeActiveRelation(kingdom);
			ChatUtil.sendError(messageSender,MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return false;
		}

		// Does any change to war by one side instantly force the other into war?
		boolean isInstantWar = earth.getCore().getBoolean(CorePath.KINGDOMS_INSTANT_WAR.getPath(), false);
		// Does a change from war to peace by one side instantly force the other into peace?
		boolean isInstantPeace = earth.getCore().getBoolean(CorePath.KINGDOMS_INSTANT_PEACE.getPath(), false);
		// Does a declaration of war also include the target kingdom's allies?
		boolean isDefensePact = earth.getCore().getBoolean(CorePath.KINGDOMS_ALLY_DEFENSE_PACT.getPath(), false);

		/* At this point, the active states are valid and the change request can be processed */

		// Relationship transition logic
		if(isAdmin) {
			// Instantly force relation always for admins
			setJointActiveRelation(kingdom,otherKingdom,relation);
			// Broadcast change
			switch (relation) {
				case PEACE:
					ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_PEACE.getMessage(kingdom.getName(), otherKingdom.getName()));
					break;
				case TRADE:
					ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_TRADE.getMessage(kingdom.getName(), otherKingdom.getName()));
					break;
				case ALLIANCE:
					ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_ALLY.getMessage(kingdom.getName(), otherKingdom.getName()));
					break;
				case WAR:
					ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_WAR.getMessage(kingdom.getName(), otherKingdom.getName()));
					break;
				default:
					break;
			}
		} else {
			// Follow relation transition logic for regular players
			switch (relation) {
				case PEACE:
					// Our kingdom wants to make peace
					if (ourActiveState.equals(EarthDiplomacyType.WAR)) {
						// We are enemies, try to make peace
						if (isInstantPeace) {
							// Instantly change to active peace, force them to peace too
							setJointActiveRelation(kingdom, otherKingdom, relation);
							ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_PEACE.getMessage(kingdom.getName(), otherKingdom.getName()));
						} else {
							// Request peace from them
							if (kingdom.hasRelationRequest(otherKingdom) && kingdom.getRelationRequest(otherKingdom).equals(relation)) {
								// Both sides request peace, change to active peace
								setJointActiveRelation(kingdom, otherKingdom, relation);
								ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_PEACE.getMessage(kingdom.getName(), otherKingdom.getName()));
							} else {
								// We request peace, still at war
								if (otherKingdom.hasRelationRequest(kingdom) && otherKingdom.getRelationRequest(kingdom).equals(EarthDiplomacyType.PEACE)) {
									ChatUtil.sendError(messageSender, MessagePath.COMMAND_KINGDOM_ERROR_DIPLOMACY_EXISTS.getMessage());
									return false;
								}
								otherKingdom.setRelationRequest(kingdom, relation);
								ChatUtil.sendNotice(messageSender, MessagePath.COMMAND_KINGDOM_NOTICE_DIPLOMACY_SENT.getMessage(otherKingdom.getName()));
								broadcastMembers(otherKingdom, MessagePath.COMMAND_KINGDOM_BROADCAST_PEACE_REQUEST.getMessage(kingdom.getName()));
							}
						}
					} else {
						// We are not enemies
						if (ourActiveState.equals(EarthDiplomacyType.ALLIANCE)) {
							// We are allies, instantly break alliance and set both to peace
							setJointActiveRelation(kingdom, otherKingdom, relation);
							ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_NO_ALLY.getMessage(kingdom.getName(), otherKingdom.getName()));
						} else if (ourActiveState.equals(EarthDiplomacyType.TRADE)) {
							// We are trade partners, instantly go back to peace
							setJointActiveRelation(kingdom, otherKingdom, relation);
							ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_NO_TRADE.getMessage(kingdom.getName(), otherKingdom.getName()));
						}
					}
					break;
				case TRADE:
					// Our kingdom wants to trade with the other
					if (ourActiveState.equals(EarthDiplomacyType.PEACE)) {
						// We are at peace, request to trade
						if (kingdom.hasRelationRequest(otherKingdom) && kingdom.getRelationRequest(otherKingdom).equals(relation)) {
							// Both sides request trade, change to active trading
							setJointActiveRelation(kingdom, otherKingdom, relation);
							ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_TRADE.getMessage(kingdom.getName(), otherKingdom.getName()));
						} else {
							// We request trade
							if (otherKingdom.hasRelationRequest(kingdom) && otherKingdom.getRelationRequest(kingdom).equals(EarthDiplomacyType.TRADE)) {
								ChatUtil.sendError(messageSender, MessagePath.COMMAND_KINGDOM_ERROR_DIPLOMACY_EXISTS.getMessage());
								return false;
							}
							otherKingdom.setRelationRequest(kingdom, relation);
							ChatUtil.sendNotice(messageSender, MessagePath.COMMAND_KINGDOM_NOTICE_DIPLOMACY_SENT.getMessage(otherKingdom.getName()));
							broadcastMembers(otherKingdom, MessagePath.COMMAND_KINGDOM_BROADCAST_TRADE_REQUEST.getMessage(kingdom.getName()));
						}
					} else if (ourActiveState.equals(EarthDiplomacyType.ALLIANCE)) {
						// We are allies, instantly break alliance and set both to trade
						removeJointActiveRelation(kingdom, otherKingdom);
						ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_NO_ALLY.getMessage(kingdom.getName(), otherKingdom.getName()));
					}
					break;
				case ALLIANCE:
					// Our kingdom wants to ally the other
					if (kingdom.hasRelationRequest(otherKingdom) && kingdom.getRelationRequest(otherKingdom).equals(relation)) {
						// Both sides request allied, change to active alliance
						setJointActiveRelation(kingdom, otherKingdom, relation);
						ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_ALLY.getMessage(kingdom.getName(), otherKingdom.getName()));
					} else {
						// We request allied
						if (otherKingdom.hasRelationRequest(kingdom) && otherKingdom.getRelationRequest(kingdom).equals(EarthDiplomacyType.ALLIANCE)) {
							ChatUtil.sendError(messageSender, MessagePath.COMMAND_KINGDOM_ERROR_DIPLOMACY_EXISTS.getMessage());
							return false;
						}
						otherKingdom.setRelationRequest(kingdom, relation);
						ChatUtil.sendNotice(messageSender, MessagePath.COMMAND_KINGDOM_NOTICE_DIPLOMACY_SENT.getMessage(otherKingdom.getName()));
						broadcastMembers(otherKingdom, MessagePath.COMMAND_KINGDOM_BROADCAST_ALLY_REQUEST.getMessage(kingdom.getName()));
					}
					break;
				case WAR:
					// Our kingdom wants to declare war
					boolean isWarDeclared = false;
					if (isInstantWar) {
						// Instantly change to active enemy, force them to enemy too
						setJointActiveRelation(kingdom, otherKingdom, relation);
						ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_WAR.getMessage(kingdom.getName(), otherKingdom.getName()));
						isWarDeclared = true;
					} else {
						// Request war
						// Check if they already request enemy
						if (kingdom.hasRelationRequest(otherKingdom) && kingdom.getRelationRequest(otherKingdom).equals(relation)) {
							// Both sides request enemy, change to active war
							setJointActiveRelation(kingdom, otherKingdom, relation);
							ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_WAR.getMessage(kingdom.getName(), otherKingdom.getName()));
							isWarDeclared = true;
						} else {
							// We request enemy
							if (otherKingdom.hasRelationRequest(kingdom) && otherKingdom.getRelationRequest(kingdom).equals(EarthDiplomacyType.WAR)) {
								ChatUtil.sendError(messageSender, MessagePath.COMMAND_KINGDOM_ERROR_DIPLOMACY_EXISTS.getMessage());
								return false;
							}
							otherKingdom.setRelationRequest(kingdom, relation);
							ChatUtil.sendNotice(messageSender, MessagePath.COMMAND_KINGDOM_NOTICE_DIPLOMACY_SENT.getMessage(otherKingdom.getName()));
							broadcastMembers(otherKingdom, MessagePath.COMMAND_KINGDOM_BROADCAST_WAR_REQUEST.getMessage(kingdom.getName()));
						}
					}
					// Check for defense pact with allies
					if (isWarDeclared && isDefensePact) {
						// Set war with all allies of target kingdom
						for (KonKingdom otherAlly : otherKingdom.getActiveRelationKingdoms(EarthDiplomacyType.ALLIANCE)) {
							if (!otherAlly.equals(kingdom)) {
								// Declare war on their allies
								setJointActiveRelation(kingdom, otherAlly, relation);
								ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_WAR.getMessage(kingdom.getName(), otherAlly.getName()));
							}
						}
					}
					break;
				default:
					break;
			}
		}
		
		// Debug messaging
		// Our current state with the other kingdom
		EarthDiplomacyType ourNewActiveState = kingdom.getActiveRelation(otherKingdom);
		// Their current request of us (if any)
		EarthDiplomacyType ourNewRequestState = kingdom.getRelationRequest(otherKingdom);
		// Their current state with us
		EarthDiplomacyType theirNewActiveState = otherKingdom.getActiveRelation(kingdom);
		// Our current diplomatic request of them (if any)
		EarthDiplomacyType theirNewRequestState = otherKingdom.getRelationRequest(kingdom);
		ChatUtil.printDebug("Finished; Our states ["+ourNewActiveState+","+ourNewRequestState+"]; Their states ["+theirNewActiveState+","+theirNewRequestState+"]");

		// Update all kingdoms & players for new relationship (when it changes)
		// 1. Update all player name colors
		// 2. Update all border particles
		// 3. Update all territory display bars
		if(!ourActiveState.equals(ourNewActiveState)) {
			earth.updateNamePackets(kingdom);
			earth.getTerritoryManager().updatePlayerBorderParticles(kingdom);
			earth.getTerritoryManager().updateTownDisplayBars(kingdom);
			refreshTownNerfs(kingdom);
		}
		if(!theirActiveState.equals(theirNewActiveState)) {
			earth.updateNamePackets(otherKingdom);
			earth.getTerritoryManager().updatePlayerBorderParticles(otherKingdom);
			earth.getTerritoryManager().updateTownDisplayBars(otherKingdom);
			refreshTownNerfs(otherKingdom);
		}

		// Withdraw cost
		if(costRelation > 0 && !isAdmin && payPlayer != null) {
            if(EarthPlugin.withdrawPlayer(payPlayer.getBukkitPlayer(), costRelation)) {
            	earth.getAccomplishmentManager().modifyPlayerStat(payPlayer,KonStatsType.FAVOR,(int)costRelation);
            }
		}

		return true;
	}

	public boolean menuChangeKingdomRelation(@Nullable KonKingdom kingdom, @Nullable  KonKingdom otherKingdom, @NotNull EarthDiplomacyType relation, KonPlayer player, boolean isAdmin) {
		if (player == null) return false;
		return menuChangeKingdomRelation(kingdom, otherKingdom, relation, player, player.getBukkitPlayer(), isAdmin);
	}
	
	public boolean isValidRelationChoice(@NotNull KonKingdom kingdom1,@NotNull KonKingdom kingdom2,@NotNull EarthDiplomacyType relation) {
		EarthDiplomacyType ourRelation = kingdom1.getActiveRelation(kingdom2);
		if(ourRelation.equals(relation)) {
			// Cannot change to existing active relation
			return false;
		} else if(ourRelation.equals(EarthDiplomacyType.WAR) && !relation.equals(EarthDiplomacyType.PEACE)) {
			// Can only change to peace when currently enemy
			return false;
		} else if(ourRelation.equals(EarthDiplomacyType.ALLIANCE) && relation.equals(EarthDiplomacyType.WAR)) {
			// Cannot change to war when currently Allied
			return false;
		}
		return true;
	}

	public double getRelationCost(@NotNull EarthDiplomacyType relation) {
		switch(relation) {
			case WAR:
				return costDiplomacyWar;
			case PEACE:
				return costDiplomacyPeace;
			case TRADE:
				return costDiplomacyTrade;
			case ALLIANCE:
				return costDiplomacyAlliance;
			default:
				return 0;
		}
	}
	
	private void setJointActiveRelation(@NotNull KonKingdom kingdom1,@NotNull KonKingdom kingdom2,@NotNull EarthDiplomacyType relation) {
		// Force both kingdoms to have the given active relationship
		// Remove any relation requests
		kingdom1.setActiveRelation(kingdom2, relation);
		kingdom1.removeRelationRequest(kingdom2);
		kingdom2.setActiveRelation(kingdom1, relation);
		kingdom2.removeRelationRequest(kingdom1);
	}
	
	private void removeJointActiveRelation(@NotNull KonKingdom kingdom1,@NotNull KonKingdom kingdom2) {
		kingdom1.removeActiveRelation(kingdom2);
		kingdom1.removeRelationRequest(kingdom2);
		kingdom2.removeActiveRelation(kingdom1);
		kingdom2.removeRelationRequest(kingdom1);
	}

	/**
	 * Get the shared diplomatic type of two kingdoms.
	 * Performs error checking to ensure both kingdoms have the same active relation.
	 * If there is a mismatch, they are reset to default (peace).
	 * @param kingdom1 The reference kingdom
	 * @param kingdom2 The target kingdom
	 * @return The diplomatic type
	 */
	public EarthDiplomacyType getDiplomacy(@NotNull EarthKingdom kingdom1, @NotNull EarthKingdom kingdom2) {
		// Default kingdoms are always at war (barbarian & neutral)
		if(!kingdom1.isCreated() || !kingdom2.isCreated()) {
			return EarthDiplomacyType.WAR;
		}
		// Evaluate two created kingdoms
		if(kingdom1.getActiveRelation(kingdom2).equals(kingdom2.getActiveRelation(kingdom1))) {
			// Both kingdoms have a matching active relation
			return kingdom1.getActiveRelation(kingdom2);
		} else {
			// There is a relation mismatch, revert to default
			if(kingdom1 instanceof KonKingdom) {
				((KonKingdom)kingdom1).removeActiveRelation(kingdom2);
			}
			if(kingdom2 instanceof KonKingdom) {
				((KonKingdom)kingdom2).removeActiveRelation(kingdom1);
			}
			return EarthDiplomacyType.getDefault();
		}
	}

	public boolean isKingdomWar(@NotNull EarthKingdom kingdom1, @NotNull EarthKingdom kingdom2) {
		return getDiplomacy(kingdom1, kingdom2).equals(EarthDiplomacyType.WAR);
	}

	public boolean isKingdomPeace(@NotNull EarthKingdom kingdom1, @NotNull EarthKingdom kingdom2) {
		return getDiplomacy(kingdom1, kingdom2).equals(EarthDiplomacyType.PEACE);
	}

	public boolean isKingdomTrade(@NotNull EarthKingdom kingdom1, @NotNull EarthKingdom kingdom2) {
		return getDiplomacy(kingdom1, kingdom2).equals(EarthDiplomacyType.TRADE);
	}

	public boolean isKingdomAlliance(@NotNull EarthKingdom kingdom1, @NotNull EarthKingdom kingdom2) {
		return getDiplomacy(kingdom1, kingdom2).equals(EarthDiplomacyType.ALLIANCE);
	}
	
	public EarthRelationshipType getRelationRole(@Nullable EarthKingdom displayKingdom, @Nullable EarthKingdom contextKingdom) {
    	if(displayKingdom == null || contextKingdom == null) {
    		ChatUtil.printDebug("Failed to evaluate relation of null kingdom");
    		return EarthRelationshipType.NEUTRAL;
    	}
		EarthRelationshipType result;
    	if(contextKingdom.equals(getBarbarians())) {
    		result = EarthRelationshipType.BARBARIAN;
		} else if(contextKingdom.equals(getNeutrals())) {
    		result = EarthRelationshipType.NEUTRAL;
		} else {
			if(displayKingdom.equals(contextKingdom)) {
				result = EarthRelationshipType.FRIENDLY;
    		} else if (displayKingdom.equals(getBarbarians())) {
    			result = EarthRelationshipType.ENEMY;
    		} else {
    			if(isKingdomWar(displayKingdom, contextKingdom)) {
    				result = EarthRelationshipType.ENEMY;
				} else if(isKingdomAlliance(displayKingdom, contextKingdom)) {
					result = EarthRelationshipType.ALLY;
				} else if(isKingdomTrade(displayKingdom, contextKingdom)) {
					result = EarthRelationshipType.TRADE;
				} else if(isKingdomPeace(displayKingdom, contextKingdom)) {
					result = EarthRelationshipType.PEACEFUL;
				} else {
					result = EarthRelationshipType.NEUTRAL;
				}
    		}
		}
    	return result;
    }

    public boolean isPlayerEnemy(@NotNull KonOfflinePlayer offlinePlayer,@NotNull KonKingdom kingdom) {
    	return getRelationRole(offlinePlayer.getKingdom(),kingdom).equals(EarthRelationshipType.ENEMY);
    }

	public boolean isPlayerAlly(@NotNull KonOfflinePlayer offlinePlayer,@NotNull KonKingdom kingdom) {
		return getRelationRole(offlinePlayer.getKingdom(),kingdom).equals(EarthRelationshipType.ALLY);
	}
    
    public boolean isPlayerFriendly(@NotNull KonOfflinePlayer offlinePlayer,@NotNull KonKingdom kingdom) {
    	return getRelationRole(offlinePlayer.getKingdom(),kingdom).equals(EarthRelationshipType.FRIENDLY);
    }

	public boolean isPlayerTrade(@NotNull KonOfflinePlayer offlinePlayer,@NotNull KonKingdom kingdom) {
		return getRelationRole(offlinePlayer.getKingdom(),kingdom).equals(EarthRelationshipType.TRADE);
	}

	public boolean isPlayerPeace(@NotNull KonOfflinePlayer offlinePlayer,@NotNull KonKingdom kingdom) {
		return getRelationRole(offlinePlayer.getKingdom(),kingdom).equals(EarthRelationshipType.PEACEFUL);
	}

	public boolean isPlayerForeign(@NotNull KonOfflinePlayer offlinePlayer,@NotNull KonKingdom kingdom) {
		return !getRelationRole(offlinePlayer.getKingdom(),kingdom).equals(EarthRelationshipType.FRIENDLY);
	}

	public boolean isKingdomBarbarian(@NotNull KonKingdom kingdom) {
		return kingdom.equals(barbarians);
	}

	public boolean isKingdomNeutral(@NotNull KonKingdom kingdom) {
		return kingdom.equals(neutrals);
	}

	/*
	 * =================================================
	 * Town General Methods
	 * =================================================
	 */
	
	
	/**
	 * createTown - Primary method for adding a town
	 * @param loc - location of town center
	 * @param name - name of town
	 * @param kingdomName - name of kingdom that the town belongs to
	 * @return  0 - success
	 * 			1 - error, chunk claim conflict
	 * 			2 - error, bad monument placement
	 * 			3 - error, bad name
	 * 			4 - error, invalid monument template
	 * 			5 - error, bad town placement, invalid world
	 * 			6 - error, bad town placement, too close
	 * 			7 - error, bad town placement, too far
	 * 		   11 - error, town init fail, bad monument paste
	 *   	   12 - error, town init fail, bad town height
	 *		   13 - error, town init fail, bad chunks
	 * 		   14 - error, town init fail, too much air below town
	 * 		   15 - error, town init fail, too much water below town
	 * 		   16 - error, town init fail, containers below monument
	 * 		   17 - error, town init fail, invalid monument template
	 * 		   21 - error, town init fail, invalid monument
	 * 		   22 - error, town init fail, bad monument gradient
	 * 		   23 - error, town init fail, monument bad location (bedrock, outside gradient)
	 */
	public int createTown(@NotNull Location loc, String name, String kingdomName) {
		ChatUtil.printDebug("Attempting to add new town "+name+" for kingdom "+kingdomName);
		
		// Verify name
		if(earth.validateNameConstraints(name) != 0) {
			ChatUtil.printDebug("Town name "+name+" failed name validation");
			return 3;
		}
		if(!isKingdom(kingdomName)) {
			ChatUtil.printDebug("Kingdom name "+kingdomName+" failed name validation");
			return 3;
		}
		
		// Verify position
		int locStatus = validateTownLocationConstraints(loc);
		// Map original error codes
		/*
		 * 			0 - Success
		 * 			1 - Error, location is in an invalid world
		 * 			2 - Error, location too close to another territory
		 * 			3 - Error, location too far from other territories
		 * 			4 - Error, location overlaps with other territories
		 */
		if(locStatus == 1) {
			return 5;
		} else if(locStatus == 2) {
			return 6;
		} else if(locStatus == 3) {
			return 7;
		} else if(locStatus == 4) {
			return 1;
		}
		
		// Verify valid monument template
		if(!getKingdom(kingdomName).isMonumentTemplateValid()) {
			ChatUtil.printDebug("Failed to create town with invalid or blanked monument template.");
			return 4;
		}	
			
		/* Passed checks, start to create town */
		
		// Modify location to max Y at given X,Z
		Point point = HelperUtil.toPoint(loc);
		int xLocal = loc.getBlockX() - (point.x*16);
		int zLocal = loc.getBlockZ() - (point.y*16);
		Chunk chunk = loc.getChunk();
		int yFloor = chunk.getChunkSnapshot(true, false, false).getHighestBlockYAt(xLocal, zLocal);
		while((chunk.getBlock(xLocal, yFloor, zLocal).isPassable() || !chunk.getBlock(xLocal, yFloor, zLocal).getType().isOccluding()) && yFloor > 0) {
			yFloor--;
		}
		loc.setY(yFloor+1);
		// Attempt to add the town
		if(getKingdom(kingdomName).addTown(loc, name)) {
			// Attempt to initialize the town
			int initStatus = getKingdom(kingdomName).initTown(name);
			if(initStatus == 0) {
				KonTown newTown = getKingdom(kingdomName).getTown(name);
				// When a town is added successfully, update the chunk cache
				earth.getTerritoryManager().addAllTerritory(loc.getWorld(),newTown.getChunkList());
				earth.getMapHandler().drawUpdateTerritory(newTown);
				// Update territory bar
				newTown.updateBarPlayers();
				// Update border particles
				earth.getTerritoryManager().updatePlayerBorderParticles(newTown.getCenterLoc());
				return 0;
			} else {
				// Remove town if init fails, exit code 10+
				getKingdom(kingdomName).removeTown(name);
				return 10+initStatus;
			}
		} else {
			return 3;
		}
	}
	
	/**
	 * Primary method for removing a town
	 * @param name - name of town
	 * @param kingdomName - name of kingdom that the town belongs to
	 * @return true on success, false on failure
	 */
	public boolean removeTown(String name, String kingdomName) {
		if(!isKingdom(kingdomName) || !getKingdom(kingdomName).hasTown(name))return false;
		KonKingdom kingdom = getKingdom(kingdomName);
		KonTown town = kingdom.getTown(name);
		ArrayList<Point> townPoints = new ArrayList<>(town.getChunkList().keySet());
		ArrayList<OfflinePlayer> residentPlayers = town.getPlayerResidents();
		ArrayList<KonPlayer> nearbyPlayers = earth.getPlayerManager().getPlayersNearTerritory(town);
		clearAllTownNerfs(town);
		clearAllTownHearts(town);
		if(!kingdom.removeTown(name)) return false;
		// When a town is removed successfully, update the chunk cache
		earth.getTerritoryManager().removeAllTerritory(town.getWorld(),townPoints);
		// Update border particles
		for(KonPlayer player : nearbyPlayers) {
			earth.getTerritoryManager().updatePlayerBorderParticles(player);
		}
		// Update memberships
		for(OfflinePlayer resident : residentPlayers) {
			if (resident.isOnline()) {
				KonPlayer onlineResident = earth.getPlayerManager().getPlayer((Player)resident);
				if (onlineResident != null) {
					updatePlayerMembershipStats(onlineResident);
				}
			}
		}
		// Update maps
		earth.getMapHandler().drawRemoveTerritory(town);
		earth.getMapHandler().drawLabel(town.getKingdom().getCapital());
		// Update shops
		earth.getShopHandler().deleteShopsInPoints(townPoints,town.getWorld());
		return true;
	}

	/**
	 * Primary method for renaming a town
	 * @param oldName - old name of town
	 * @param newName - new name of town
	 * @param kingdomName - name of kingdom town belongs to
	 * @return true on success, false on failure
	 */
	public boolean renameTown(String oldName, String newName, String kingdomName) {
		if(!isKingdom(kingdomName) || !getKingdom(kingdomName).hasTown(oldName))return false;
		KonKingdom kingdom = getKingdom(kingdomName);
		earth.getMapHandler().drawRemoveTerritory(kingdom.getTown(oldName));
		boolean success = kingdom.renameTown(oldName, newName);
		if(!success) return false;
		KonTown town = kingdom.getTown(newName);
		if (town != null) {
			earth.getMapHandler().drawUpdateTerritory(town);
		}
		return true;
	}
	
	/**
	 * Primary method for transferring ownership of a town between kingdoms
	 * @param name - Town name
	 * @param oldKingdomName - Kingdom name of conquered Town
	 * @param conquerPlayerArg - Player who initiated the conquering
	 * @return the captured town
	 */
	@Nullable
	public EarthTown captureTownForPlayer(String name, String oldKingdomName,@Nullable EarthPlayer conquerPlayerArg) {
		if(conquerPlayerArg == null) return null;
		if(!(conquerPlayerArg instanceof KonPlayer)) return null;
		KonPlayer conquerPlayer = (KonPlayer)conquerPlayerArg;
		
		if(conquerPlayer.isBarbarian()) return null;
		KonTown town = captureTown(name, oldKingdomName, conquerPlayer.getKingdom());
		if(town != null) {
			town.setPlayerLord(conquerPlayer.getOfflineBukkitPlayer());
			return town;
		}
		return null;
	}

	@Nullable
	public KonTown captureTown(String name, String oldKingdomName,@NotNull KonKingdom conquerKingdom) {
		if(!conquerKingdom.isMonumentTemplateValid()) {
			ChatUtil.printDebug("Failed to capture town for kingdom with invalid template: "+conquerKingdom.getName());
			return null;
		}
		boolean captureUpgrades = earth.getCore().getBoolean(CorePath.TOWNS_CAPTURE_UPGRADES.getPath(),true);
		if(isKingdom(oldKingdomName) && getKingdom(oldKingdomName).hasTown(name)) {
			earth.getMapHandler().drawRemoveTerritory(getKingdom(oldKingdomName).getTown(name));
			getKingdom(oldKingdomName).getTown(name).purgeResidents();
			getKingdom(oldKingdomName).getTown(name).clearShieldsArmors();
			getKingdom(oldKingdomName).getTown(name).setKingdom(conquerKingdom);
			conquerKingdom.addTownConquer(name, getKingdom(oldKingdomName).removeTownConquer(name));
			conquerKingdom.getTown(name).refreshMonument();
			refreshTownNerfs(conquerKingdom.getTown(name));
			refreshTownHearts(conquerKingdom.getTown(name));
			conquerKingdom.getTown(name).updateBarPlayers();
			if(captureUpgrades) {
				earth.getUpgradeManager().updateTownDisabledUpgrades(conquerKingdom.getTown(name));
			} else {
				conquerKingdom.getTown(name).clearUpgrades();
			}
			conquerKingdom.getTown(name).clearPlots();
			earth.getMapHandler().drawUpdateTerritory(conquerKingdom.getTown(name));
			earth.getMapHandler().drawLabel(getKingdom(oldKingdomName).getCapital());
			earth.getMapHandler().drawLabel(conquerKingdom.getCapital());
			earth.getShopHandler().deleteShopsInPoints(conquerKingdom.getTown(name).getChunkList().keySet(),conquerKingdom.getTown(name).getWorld());
			return conquerKingdom.getTown(name);
		}
		return null;
	}
	
	/**
	 * Primary method for transferring ownership of a capital between kingdoms
	 * @param oldKingdomName - Kingdom name of conquered Town
	 * @param conquerPlayerArg - Player who initiated the conquering
	 * @return EarthTown if all names exist, else null
	 */
	@Nullable
	public EarthTown captureCapitalForPlayer(String oldKingdomName,@Nullable EarthPlayer conquerPlayerArg) {
		if(conquerPlayerArg == null) {
			return null;
		}
		if(!(conquerPlayerArg instanceof KonPlayer)) {
			return null;
		}
		KonPlayer conquerPlayer = (KonPlayer)conquerPlayerArg;
		
		if(conquerPlayer.isBarbarian()) {
			return null;
		}
		KonTown town = captureCapital(oldKingdomName, conquerPlayer.getKingdom());
		if(town != null) {
			town.setPlayerLord(conquerPlayer.getOfflineBukkitPlayer());
			return town;
		}
		return null;
	}

	@Nullable
	public KonTown captureCapital(String oldKingdomName, @NotNull KonKingdom conquerKingdom) {
		if(!conquerKingdom.isMonumentTemplateValid()) {
			ChatUtil.printDebug("Failed to capture capital for kingdom with invalid template: "+conquerKingdom.getName());
			return null;
		}
		boolean captureUpgrades = earth.getCore().getBoolean(CorePath.TOWNS_CAPTURE_UPGRADES.getPath(),true);
		if(isKingdom(oldKingdomName)) {
			// Capture the capital of the old kingdom as a new town for the conquering kingdom.
			Location townLoc = getKingdom(oldKingdomName).getCapital().getCenterLoc();
			String townKingdom = conquerKingdom.getName();
			Set<Point> townLand = getKingdom(oldKingdomName).getCapital().getChunkPoints();
			Map<KonPropertyFlag,Boolean> townProperties = getKingdom(oldKingdomName).getCapital().getAllProperties();
			Map<EarthUpgrade,Integer> townUpgrades = getKingdom(oldKingdomName).getCapital().getUpgrades();
			// Remove the old kingdom
			removeKingdom(oldKingdomName);
			// Validate town name
			int nameStatus = earth.validateNameConstraints(oldKingdomName);
			if(nameStatus != 0) {
				ChatUtil.printDebug("Failed to name captured capital to "+ oldKingdomName +", validation failed with status code "+nameStatus);
				return null;
			}
			// Create a new town
			int status = createTown(townLoc, oldKingdomName,townKingdom);
			if(status == 0) {
				// Successfully created new town, update it
				KonTown town = conquerKingdom.getTown(oldKingdomName);
				// Add land
				town.addPoints(townLand);
				// Update territory cache
				earth.getTerritoryManager().addAllTerritory(town.getWorld(),town.getChunkList());
				// Add properties
            	for(KonPropertyFlag flag : townProperties.keySet()) {
            		town.setPropertyValue(flag, townProperties.get(flag));
            	}
            	// Add upgrades
            	if(captureUpgrades) {
	            	for(EarthUpgrade upgrade : townUpgrades.keySet()) {
	            		town.addUpgrade(upgrade, townUpgrades.get(upgrade));
	            	}
	            	// Update upgrade status
	            	earth.getUpgradeManager().updateTownDisabledUpgrades(town);
            	}
            	// Update display bar, nerfs, etc
            	refreshTownNerfs(town);
    			refreshTownHearts(town);
				town.updateBarPlayers();
				earth.getMapHandler().drawUpdateTerritory(town);
				earth.getMapHandler().drawLabel(conquerKingdom.getCapital());
				earth.getShopHandler().deleteShopsInPoints(town.getChunkList().keySet(),town.getWorld());
				return town;
			} else {
				ChatUtil.printDebug("Failed to create new town over captured capital, status code "+status);
				return null;
			}
		}
		return null;
	}

	public boolean menuDestroyTown(KonTown town, KonPlayer player) {
		// Cannot destroy: a null town
		if(town == null) return false;
		// Destroy the town (cannot be capital)
		if (town.getTerritoryType().equals(EarthTerritoryType.CAPITAL)) {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}
		// Try to remove
		String townName = town.getName();
		if (removeTown(townName, town.getKingdom().getName())) {
			// Successfully removed the town
			ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_DESTROY.getMessage(townName));
		} else {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return false;
		}
		return true;
	}

	/*
	 * =================================================
	 * Town Management Methods
	 * =================================================
	 */

	private void broadcastResidents(KonTown town, String message) {
		if(town == null)return;
		for(OfflinePlayer offlinePlayer : town.getPlayerResidents()) {
			if(offlinePlayer.isOnline()) {
				Player player = (Player)offlinePlayer;
				ChatUtil.sendNotice(player, message);
			}
		}
	}

	private void broadcastKnights(KonTown town, String message) {
		if(town == null) return;
		for(OfflinePlayer offlinePlayer : town.getPlayerKnights()) {
			if(offlinePlayer.isOnline()) {
				Player player = (Player)offlinePlayer;
				ChatUtil.sendNotice(player, message);
			}
		}
	}

	// A player requests to join a town from the menu
	public boolean menuJoinTownRequest(KonPlayer player, KonTown town) {
		Player bukkitPlayer = player.getBukkitPlayer();
		if(player.isBarbarian()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
			return false;
		}
		if(town.isPlayerResident(bukkitPlayer)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_JOIN_MEMBER.getMessage());
			return false;
		}
		// Check for join property flag
		if(!town.isJoinable()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}
		// Check for maximum resident limit
		int numMaxResidents = earth.getCore().getInt(CorePath.TOWNS_MAX_RESIDENT_LIMIT.getPath(),0);
		if(numMaxResidents > 0 && town.getNumResidents() >= numMaxResidents) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_JOIN_LIMIT.getMessage(town.getName()));
			return false;
		}
		UUID myId = bukkitPlayer.getUniqueId();
		if(town.isOpen() || town.isJoinInviteValid(myId)) {
			// There is an existing invite to join, or the town is open, add the player as a resident
			town.removeJoinRequest(myId);
			// Add the player as a resident
			if(town.addPlayerResident(bukkitPlayer,false)) {
				for(OfflinePlayer resident : town.getPlayerResidents()) {
					if(resident.isOnline()) {
						ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_NEW_RESIDENT.getMessage(bukkitPlayer.getName(),town.getName()));
					}
				}
				updatePlayerMembershipStats(player);
			}
		} else {
			if(town.isJoinRequestValid(myId)) {
				// There is already an existing join request
				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_REQUEST_EXISTS.getMessage(town.getName()));
				return false;
			}
			if(town.getNumResidents() == 0) {
				// The town is empty, make the player into the lord
				town.setPlayerLord(bukkitPlayer);
				town.removeJoinRequest(myId);
				updatePlayerMembershipStats(player);
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_NEW_RESIDENT.getMessage(bukkitPlayer.getName(),town.getName()));
			} else {
				// Create a new join request
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_REQUEST_SENT.getMessage(town.getName()));
				town.addJoinRequest(myId, false);
				broadcastKnights(town, MessagePath.COMMAND_TOWN_NOTICE_REQUEST_RECEIVED.getMessage());
			}
		}
		return true;
	}

	// A player leaves a town from the menu
	public boolean menuLeaveTown(KonPlayer player, KonTown town) {
		Player bukkitPlayer = player.getBukkitPlayer();
		if(player.isBarbarian()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
			return false;
		}
		if(!town.isPlayerResident(bukkitPlayer)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_FAILED.getMessage());
			return false;
		}
		// Check for leave property flag
		if(!town.isLeaveable()) {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}
		// Remove the player as a resident
		if(town.removePlayerResident(bukkitPlayer)) {
			for(OfflinePlayer resident : town.getPlayerResidents()) {
				if(resident.isOnline()) {
					ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_LEAVE_RESIDENT.getMessage(bukkitPlayer.getName(),town.getName()));
				}
			}
			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_LEAVE_RESIDENT.getMessage(bukkitPlayer.getName(),town.getName()));
			updatePlayerMembershipStats(player);
		} else {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_FAILED.getMessage());
			return false;
		}
		return true;
	}

	// A player responds to an invite to join a town from the menu
	public boolean menuRespondTownInvite(KonPlayer player, KonTown town, boolean resp) {
		Player bukkitPlayer = player.getBukkitPlayer();
		UUID myId = bukkitPlayer.getUniqueId();
		if(!town.isJoinInviteValid(myId)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PLAYER.getMessage());
			return false;
		}
		town.removeJoinRequest(myId);
		if(player.isBarbarian()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
			return false;
		}
		if(town.isPlayerResident(bukkitPlayer)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_JOIN_MEMBER.getMessage(town.getName()));
			return false;
		}
		// Check for join property flag
		if(!town.isJoinable()) {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}
		// Check for maximum resident limit
		int numMaxResidents = earth.getCore().getInt(CorePath.TOWNS_MAX_RESIDENT_LIMIT.getPath(),0);
		if(numMaxResidents > 0 && town.getNumResidents() >= numMaxResidents) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_JOIN_LIMIT.getMessage(town.getName()));
			return false;
		}
		if(resp) {
			// Accept the invite
			if(town.addPlayerResident(bukkitPlayer,false)) {
				for(OfflinePlayer resident : town.getPlayerResidents()) {
					if(resident.isOnline()) {
						ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_NEW_RESIDENT.getMessage(bukkitPlayer.getName(),town.getName()));
					}
				}
				updatePlayerMembershipStats(player);
			}
		} else {
			// Decline the invite
			ChatUtil.sendNotice(bukkitPlayer,MessagePath.COMMAND_TOWN_NOTICE_INVITE_DECLINED.getMessage(town.getName()));
		}
		return true;
	}

	// A town lord/knight responds to a join request by the given player
	public boolean menuRespondTownRequest(KonPlayer sender, OfflinePlayer requester, KonTown town, boolean resp) {
		Player bukkitPlayer = sender.getBukkitPlayer();
		UUID id = requester.getUniqueId();
		if(!town.isJoinRequestValid(id)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PLAYER.getMessage());
			return false;
		}
		if(!town.isPlayerLord(bukkitPlayer) && !town.isPlayerKnight(bukkitPlayer)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}
		town.removeJoinRequest(id);
		// Check for join property flag
		if(!town.isJoinable()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}
		// Check for maximum resident limit
		int numMaxResidents = earth.getCore().getInt(CorePath.TOWNS_MAX_RESIDENT_LIMIT.getPath(),0);
		if(numMaxResidents > 0 && town.getNumResidents() >= numMaxResidents) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_JOIN_LIMIT.getMessage(town.getName()));
			return false;
		}
		KonOfflinePlayer offlinePlayer = earth.getPlayerManager().getOfflinePlayer(requester);
		if(offlinePlayer == null) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(requester.getName()));
			return false;
		}
		if(!offlinePlayer.getKingdom().equals(town.getKingdom())) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
			return false;
		}
		if(town.isPlayerResident(requester)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_INVITE_MEMBER.getMessage(requester.getName()));
			return false;
		}
		KonPlayer onlinePlayer = earth.getPlayerManager().getPlayerFromID(id);
		if(resp) {
			// Accepted the request
			if(town.addPlayerResident(requester,false)) {
				for(OfflinePlayer resident : town.getPlayerResidents()) {
					if(resident.isOnline()) {
						ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_NEW_RESIDENT.getMessage(requester.getName(),town.getName()));
					}
				}
				if(onlinePlayer != null) {
					updatePlayerMembershipStats(onlinePlayer);
				}
			}
		} else {
			// Denied the request
			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_REQUEST_DECLINED.getMessage(requester.getName()));
			if(onlinePlayer != null) {
				ChatUtil.sendError(onlinePlayer.getBukkitPlayer(), MessagePath.COMMAND_TOWN_ERROR_REQUEST_DENY.getMessage(town.getName()));
			}
		}
		return true;
	}

	// A town lord/knight invites (offline) player to join
	public void addTownPlayer(KonPlayer sender, KonOfflinePlayer player, KonTown town) {
		Player bukkitPlayer = sender.getBukkitPlayer();
		if(!town.isPlayerLord(bukkitPlayer) && !town.isPlayerKnight(bukkitPlayer)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return;
		}
		if(player == null) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return;
		}
		// Check for join property flag
		if(!town.isJoinable()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return;
		}
		// Check for maximum resident limit
		int numMaxResidents = earth.getCore().getInt(CorePath.TOWNS_MAX_RESIDENT_LIMIT.getPath(),0);
		if(numMaxResidents > 0 && town.getNumResidents() >= numMaxResidents) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_JOIN_LIMIT.getMessage(town.getName()));
			return;
		}
		OfflinePlayer offlinePlayer = player.getOfflineBukkitPlayer();
		UUID id = offlinePlayer.getUniqueId();
		if(!player.getKingdom().equals(town.getKingdom())) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
			return;
		}
		// Cannot invite a resident of this town
		if(town.isPlayerResident(offlinePlayer)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_INVITE_MEMBER.getMessage(offlinePlayer.getName()));
			return;
		}
		// Cannot invite to an open town
		if(town.isOpen()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_INVITE_OPEN.getMessage());
			return;
		}
		KonPlayer onlinePlayer = earth.getPlayerManager().getPlayerFromID(id);

		// Check for join request and add as resident
		// Otherwise create join invite
		if(town.isJoinInviteValid(id)) {
			// There is already a join invite for this player
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_INVITE_EXISTS.getMessage(offlinePlayer.getName()));
		} else {
			// Check for join request
			if(town.isJoinRequestValid(id)) {
				// This player has already requested to join, add them
				town.removeJoinRequest(id);
				// Add the player as a resident
				if(town.addPlayerResident(offlinePlayer,false)) {
					for(OfflinePlayer resident : town.getPlayerResidents()) {
						if(resident.isOnline()) {
							ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_NEW_RESIDENT.getMessage(offlinePlayer.getName(),town.getName()));
						}
					}
					if(onlinePlayer != null) {
						updatePlayerMembershipStats(onlinePlayer);
					}
				}
			} else {
				// Create a new join invite
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_INVITE_SENT.getMessage(offlinePlayer.getName(),town.getName()));
				town.addJoinRequest(id, true);
				if(onlinePlayer != null) {
					ChatUtil.sendNotice(onlinePlayer.getBukkitPlayer(), MessagePath.COMMAND_TOWN_NOTICE_INVITE_RECEIVED.getMessage());
				}
			}
		}
	}

	// A town lord/knight kicks (offline) player (with /town kick command)
	public void kickTownPlayer(KonPlayer sender, KonOfflinePlayer player, KonTown town) {
		Player bukkitPlayer = sender.getBukkitPlayer();
		if(!town.isPlayerLord(bukkitPlayer) && !town.isPlayerKnight(bukkitPlayer)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return;
		}
		if(player == null) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return;
		}
		// Check for leave property flag
		if(!town.isLeaveable()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return;
		}
		OfflinePlayer offlinePlayer = player.getOfflineBukkitPlayer();
		// Check for existing resident
		if(!town.isPlayerResident(offlinePlayer)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_KICK_FAIL.getMessage(offlinePlayer.getName(),town.getName()));
			return;
		}
		// Prevent kicking the town lord
		if(town.isPlayerLord(player.getOfflineBukkitPlayer())) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return;
		}
		UUID id = offlinePlayer.getUniqueId();
		KonPlayer onlinePlayer = earth.getPlayerManager().getPlayerFromID(offlinePlayer.getUniqueId());
		// Attempt to kick current resident
		if(town.removePlayerResident(offlinePlayer)) {
			town.removeJoinRequest(id);
			for(OfflinePlayer resident : town.getPlayerResidents()) {
				if(resident.isOnline()) {
					ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_KICK_RESIDENT.getMessage(offlinePlayer.getName(),town.getName()));
				}
			}
			if(onlinePlayer != null) {
				ChatUtil.sendError(onlinePlayer.getBukkitPlayer(), MessagePath.COMMAND_TOWN_ERROR_KICKED.getMessage(town.getName()));
				updatePlayerMembershipStats(onlinePlayer);
			}
		} else {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_KICK_FAIL.getMessage(offlinePlayer.getName(),town.getName()));
		}
	}

	// A town lord/knight promotes or demotes a resident
	public boolean menuPromoteDemoteTownKnight(KonPlayer sender, OfflinePlayer member, KonTown town, boolean action) {
		Player bukkitPlayer = sender.getBukkitPlayer();
		UUID id = member.getUniqueId();
		if(!town.isPlayerLord(bukkitPlayer)) {
			// Only town lord can promote & demote
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}
		KonOfflinePlayer offlinePlayer = earth.getPlayerManager().getOfflinePlayer(member);
		if(offlinePlayer == null) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(member.getName()));
			return false;
		}
		if(!offlinePlayer.getKingdom().equals(town.getKingdom())) {
			// Target player must be a kingdom member
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
			return false;
		}
		if(id.equals(bukkitPlayer.getUniqueId()) || town.isLord(id)) {
			// Target player cannot be the town lord, or the sender
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}
		if(!town.isPlayerResident(member)) {
			// Target player must be a town resident
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_KNIGHT_RESIDENT.getMessage());
			return false;
		}
		// Set resident's elite status
		KonPlayer onlinePlayer = earth.getPlayerManager().getPlayerFromID(id);
		if(action) {
			// Action is true, try to promote
			if(town.isPlayerKnight(member)) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_PROMOTE_KNIGHT.getMessage(member.getName()));
				return false;
			}
			town.setPlayerKnight(member, true);
			for(OfflinePlayer resident : town.getPlayerResidents()) {
				if(resident.isOnline()) {
					ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_KNIGHT_SET.getMessage(member.getName(),town.getName()));
				}
			}
		} else {
			// Action is false, try to demote
			if(!town.isPlayerKnight(member)) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_DEMOTE_RESIDENT.getMessage(member.getName()));
				return false;
			}
			town.setPlayerKnight(offlinePlayer.getOfflineBukkitPlayer(), false);
			for(OfflinePlayer resident : town.getPlayerResidents()) {
				if(resident.isOnline()) {
					ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_KNIGHT_CLEAR.getMessage(member.getName(),town.getName()));
				}
			}
		}
		if(onlinePlayer != null) {
			updatePlayerMembershipStats(onlinePlayer);
		}
		return true;
	}

	// A town lord or admin is transferring town lordship to another member
	public boolean menuTransferTownLord(KonPlayer sender, OfflinePlayer member, KonTown town, boolean isAdmin) {
		// In this method, the sender must be the current town lord, or there is no valid lord, or an admin is transferring.
		// The member player will be the new town lord.
		Player bukkitPlayer = sender.getBukkitPlayer();
		UUID id = member.getUniqueId();
		if(town.isLordValid()) {
			// Lord exists, only permit access to owner
			if(!isAdmin && !town.isPlayerLord(bukkitPlayer)) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
				return false;
			}
		}
		KonOfflinePlayer offlinePlayer = earth.getPlayerManager().getOfflinePlayer(member);
		if(offlinePlayer == null) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(member.getName()));
			return false;
		}
		// Check for enemy new lord
		if(!offlinePlayer.getKingdom().equals(town.getKingdom())) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
			return false;
		}
		// Check for member resident
		if(!town.isPlayerResident(member)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}
		// Check for self new lord
		if(id.equals(bukkitPlayer.getUniqueId())) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}
		// Transfer lordship
		town.setPlayerLord(member);
		for(OfflinePlayer resident : town.getPlayerResidents()) {
			if(resident.isOnline()) {
				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_LORD_SUCCESS.getMessage(town.getName(),member.getName()));
			}
		}
		KonPlayer onlinePlayer = earth.getPlayerManager().getPlayerFromID(id);
		if(onlinePlayer != null) {
			updatePlayerMembershipStats(onlinePlayer);
		}
		return true;
	}

	/**
	 * An online player tries to become lord of an abandoned town.
	 * This also works for capitals.
	 * @param player The player attempting to claim lordship
	 * @param town The town that has no lord
	 */
	public void lordTownTakeover(KonPlayer player, KonTown town) {
		Player bukkitPlayer = player.getBukkitPlayer();
		// Verify conditions for takeover
		if(town.isLordValid() || !town.isJoinable()) {
			// Lord exists or town cannot be joined, cannot take over town
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return;
		}
		// Lord does not exist
		if(!town.isOpen()) {
			// Check player roles in closed towns only, allow any member to claim lordship in open towns
			if(!town.getPlayerKnights().isEmpty()) {
				// Elite residents exist, permit access to them
				if(!town.isPlayerKnight(bukkitPlayer)) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
					return;
				}
			} else {
				// No Elite residents exist
				if(!town.getPlayerResidents().isEmpty()) {
					// Residents for this town exist, permit access to them
					if(!town.isPlayerResident(bukkitPlayer)) {
						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
						return;
					}
				} else {
					// No residents exist, permit access to any Kingdom member
					if(!town.getKingdom().equals(player.getKingdom())) {
						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_ENEMY_TOWN.getMessage());
						return;
					}
				}
			}
		}

		// At this point, the player may take over the town as lord
		town.setPlayerLord(bukkitPlayer);
		town.removeJoinRequest(bukkitPlayer.getUniqueId());
		for(OfflinePlayer resident : town.getPlayerResidents()) {
			if(resident.isOnline()) {
				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_LORD_CLAIM.getMessage(bukkitPlayer.getName(),town.getName()));
			}
		}
		updatePlayerMembershipStats(player);
	}

	/*
	 * More methods... (a lot more)
	 */
	
	public String getSmallestKingdomName() {
		String smallestName = "";
		for(KonKingdom kingdom : kingdomMap.values()) {
			if(kingdom.isSmallest()) {
				smallestName = kingdom.getName();
			}
		}
		return smallestName;
	}
	
	public void updateSmallestKingdom() {
		KonKingdom smallestKingdom = null;
		int smallestSize = Integer.MAX_VALUE;
		for(KonKingdom kingdom : kingdomMap.values()) {
			kingdom.setSmallest(false);
			int size = earth.getPlayerManager().getAllPlayersInKingdom(kingdom.getName()).size();
			if(size < smallestSize) {
				smallestKingdom = kingdom;
				smallestSize = size;
			}
		}
		if(smallestKingdom == null) {
			ChatUtil.printDebug("Failed to update smallest kingdom, got null!");
			return;
		}
		smallestKingdom.setSmallest(true);
		ChatUtil.printDebug("Updated smallest kingdom: "+smallestKingdom.getName());
	}
	
	// This can return null!
	public KonTerritory getClosestEnemyTown(Location loc, KonKingdom friendlyKingdom) {
		KonTerritory closestTerritory = null;
		int minDistance = Integer.MAX_VALUE;
		for(KonKingdom kingdom : kingdomMap.values()) {
			if(!kingdom.equals(friendlyKingdom)) {
				for(KonTown town : kingdom.getTowns()) {
					int townDist = HelperUtil.chunkDistance(loc, town.getCenterLoc());
					if(townDist != -1 && townDist < minDistance) {
						minDistance = townDist;
						closestTerritory = town;
					}
				}
			}
		}
		return closestTerritory;
	}
	
	public ArrayList<String> getKingdomNames() {
		return new ArrayList<>(kingdomMap.keySet());
	}
	
	public ArrayList<KonKingdom> getKingdoms() {
		return new ArrayList<>(kingdomMap.values());
	}
	
	public KonKingdom getKingdom(String name) {
		if(kingdomMap.containsKey(name)) {
			return kingdomMap.get(name);
		} else if(name.equalsIgnoreCase("barbarians")){
			return barbarians;
		} else {
			// Check for case-insensitive name
			for(String kingdomName : kingdomMap.keySet()) {
				if(name.equalsIgnoreCase(kingdomName)) {
					return kingdomMap.get(kingdomName);
				}
			}
		}
		// Could not find a kingdom by name, return barbarians
		ChatUtil.printDebug("KingdomManager couldn't get Kingdom! "+name);
		return barbarians;
	}
	
	public boolean isKingdom(String name) {
		// Check for exact String key match
		if(kingdomMap.containsKey(name)) {
			return true;
		} else {
			// Check for case-insensitive name
			for(String kingdomName : kingdomMap.keySet()) {
				if(name.equalsIgnoreCase(kingdomName)) {
					return true;
				}
			}
		}
		// Could not find a kingdom by name
		return false;
	}

	@NotNull
	public ArrayList<String> getTownNames() {
		ArrayList<String> names = new ArrayList<>();
		for(KonKingdom kingdom : kingdomMap.values()) {
			names.addAll(kingdom.getTownNames());
		}
		return names;
	}

	@Nullable
	public KonTown getTown(String name) {
		KonTown result = null;
		for(KonKingdom kingdom : kingdomMap.values()) {
			if(kingdom.hasTown(name)) {
				result = kingdom.getTown(name);
			}
		}
		return result;
	}

	public boolean isTown(String name) {
		for(KonKingdom kingdom : kingdomMap.values()) {
			if(kingdom.hasTown(name)) {
				return true;
			}
		}
		return false;
	}

	@Nullable
	public KonCapital getCapital(String name) {
		KonCapital result = null;
		for(KonKingdom kingdom : kingdomMap.values()) {
			if(kingdom.hasCapital(name)) {
				result = kingdom.getCapital();
			}
		}
		return result;
	}
	
	public boolean isCapital(String name) {
		for(KonKingdom kingdom : kingdomMap.values()) {
			if(kingdom.hasCapital(name)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Finds the town with a matching name, or the kingdom capital with a matching kingdom name.
	 * @param name The name of the town or kingdom capital
	 * @return Town or capital
	 */
	@Nullable
	public KonTown getTownCapital(String name) {
		for(KonKingdom kingdom : kingdomMap.values()) {
			if(kingdom.hasCapital(name)) {
				return kingdom.getCapital();
			} else if(kingdom.hasTown(name)) {
				return kingdom.getTown(name);
			}
		}
		return null;
	}
	
	public KonKingdom getBarbarians() {
		return barbarians;
	}
	
	public KonKingdom getNeutrals() {
		return neutrals;
	}
	
	public void removeAllRabbits() {
		for(KonKingdom kingdom : getKingdoms()) {
			for(KonTown town : kingdom.getCapitalTowns()) {
				town.removeRabbit();
			}
		}
	}
	
	public void reloadMonumentsForTemplate(KonMonumentTemplate template) {
		for(KonKingdom kingdom : getKingdoms()) {
			if(kingdom.isMonumentTemplateValid() && kingdom.getMonumentTemplate().equals(template)) {
				kingdom.reloadLoadedTownMonuments();
			}
		}
	}

	public boolean isTownOptionFeatureEnabled(@NotNull KonTownOption option) {
		if (option.equals(KonTownOption.ALLIED_BUILDING)) {
			return earth.getCore().getBoolean(CorePath.KINGDOMS_ALLY_BUILD.getPath(), false);
		}
		return true;
	}
	
	/**
	 * Updates a town option based on action
	 * @param option - The option to change
	 * @param town - The town to update
	 * @param bukkitPlayer - the player who is performing the action
	 * @return true if action was successful
	 */
	public boolean changeTownOption(KonTownOption option, KonTown town, Player bukkitPlayer) {
		boolean result;
		boolean currentValue = town.getTownOption(option);
		if (currentValue) {
			// Set to false
			result = setTownOption(option, town, bukkitPlayer, false);
		} else {
			// Set to true
			result = setTownOption(option, town, bukkitPlayer, true);
		}
		return result;
	}

	public boolean setTownOption(KonTownOption option, KonTown town, CommandSender messageSender, boolean value) {
		// Check for disabled options
		if (!isTownOptionFeatureEnabled(option)) {
			// Cannot set this option
			ChatUtil.sendError(messageSender, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
			return false;
		}
		// Set option
		boolean result = town.setTownOption(option, value);
		// Display messages
		String statusMessage = "";
		switch(option) {
			case OPEN:
				if(!value) {
					// Close the town
					statusMessage = MessagePath.COMMAND_TOWN_NOTICE_CLOSE.getMessage(town.getName());
				} else {
					// Open the town
					statusMessage = MessagePath.COMMAND_TOWN_NOTICE_OPEN.getMessage(town.getName());
				}
				break;
			case PLOTS_ONLY:
				if(!value) {
					// Disable plot only mode
					statusMessage = MessagePath.COMMAND_TOWN_NOTICE_PLOT_DISABLE.getMessage(town.getName());
				} else {
					// Enable plot only mode
					statusMessage = MessagePath.COMMAND_TOWN_NOTICE_PLOT_ENABLE.getMessage(town.getName());
				}
				break;
			case ALLIED_BUILDING:
				if(!value) {
					// Disable allied building
					statusMessage = MessagePath.COMMAND_TOWN_NOTICE_ALLIED_BUILDING_DISABLE.getMessage(town.getName());
				} else {
					// Enable allied building
					statusMessage = MessagePath.COMMAND_TOWN_NOTICE_ALLIED_BUILDING_ENABLE.getMessage(town.getName());
				}
				break;
			case FRIENDLY_REDSTONE:
				if(!value) {
					// Disable friendly redstone
					statusMessage = MessagePath.COMMAND_TOWN_NOTICE_FRIENDLY_REDSTONE_DISABLE.getMessage(town.getName());
				} else {
					// Enable friendly redstone
					statusMessage = MessagePath.COMMAND_TOWN_NOTICE_FRIENDLY_REDSTONE_ENABLE.getMessage(town.getName());
				}
				break;
			case ENEMY_REDSTONE:
				if(!value) {
					// Disable enemy redstone
					statusMessage = MessagePath.COMMAND_TOWN_NOTICE_REDSTONE_DISABLE.getMessage(town.getName());
				} else {
					// Enable enemy redstone
					statusMessage = MessagePath.COMMAND_TOWN_NOTICE_REDSTONE_ENABLE.getMessage(town.getName());
				}
				break;
			case GOLEM_OFFENSE:
				if(!value) {
					// Disable golem offense
					statusMessage = MessagePath.COMMAND_TOWN_NOTICE_GOLEM_DISABLE.getMessage(town.getName());
				} else {
					// Enable golem offense
					statusMessage = MessagePath.COMMAND_TOWN_NOTICE_GOLEM_ENABLE.getMessage(town.getName());
				}
				break;
			default:
				statusMessage = MessagePath.GENERIC_ERROR_INTERNAL.getMessage();
				break;
		}
		boolean isSenderResident = false;
		for(OfflinePlayer resident : town.getPlayerResidents()) {
			if(resident.isOnline()) {
				ChatUtil.sendNotice((Player) resident, statusMessage);
			}
			if(resident.equals(messageSender)) {
				isSenderResident = true;
			}
		}
		if(!isSenderResident) {
			ChatUtil.sendNotice(messageSender, statusMessage);
		}
		return result;
	}
	
	private void makeTownNerfs() {
		townNerfs.put(CompatibilityUtil.getMiningFatigue(), 0);
	}
	
	public void applyTownNerf(KonPlayer player, KonTown town) {
		if(!player.isAdminBypassActive()) {
			for(PotionEffectType pot : townNerfs.keySet()) {
				int upgradeLevel = earth.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.FATIGUE);
				int upgradedPotLevel = townNerfs.get(pot) + upgradeLevel;
				player.getBukkitPlayer().addPotionEffect(new PotionEffect(pot, 9999999, upgradedPotLevel));
			}
		}
	}
	
	public void clearTownNerf(KonPlayer player) {
		for(PotionEffectType pot : townNerfs.keySet()) {
			player.getBukkitPlayer().removePotionEffect(pot);
		}
	}
	
	public void clearAllTownNerfs(KonTown town) {
		for(KonPlayer player : earth.getPlayerManager().getPlayersOnline()) {
			if(town.isLocInside(player.getBukkitPlayer().getLocation())) {
				clearTownNerf(player);
			}
		}
	}
	
	public void refreshTownNerfs(KonTown town) {
		for(KonPlayer player : earth.getPlayerManager().getPlayersOnline()) {
			if(town.isLocInside(player.getBukkitPlayer().getLocation())) {
				if(isPlayerEnemy(player, town.getKingdom())) {
					applyTownNerf(player, town);
				} else {
					clearTownNerf(player);
				}
			}
		}
	}

	public void refreshTownNerfs(KonKingdom kingdom) {
		for(KonTown town : kingdom.getCapitalTowns()) {
			refreshTownNerfs(town);
		}
	}
	
	public boolean isTownNerf(PotionEffectType pot) {
		return townNerfs.containsKey(pot);
	}
	
	public void applyTownHearts(KonPlayer player, KonTown town) {
		if(player.getKingdom().equals(town.getKingdom())) {
			String modName = Earth.healthModName;
			NamespacedKey modKey = earth.healthModKey;
			int upgradeLevel = earth.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.HEALTH);
			if(upgradeLevel >= 1) {
				double modifier = (upgradeLevel*2);
				CompatibilityUtil.applyHealthModifier(player.getBukkitPlayer(),modifier,modName,modKey);
			} else {
				CompatibilityUtil.removeHealthModifier(player.getBukkitPlayer(),modName,modKey);
			}
		}
	}
	
	public void clearTownHearts(KonPlayer player) {
		String modName = Earth.healthModName;
		NamespacedKey modKey = earth.healthModKey;
		CompatibilityUtil.removeHealthModifier(player.getBukkitPlayer(),modName,modKey);
	}
	
	public void clearAllTownHearts(KonTown town) {
		for(KonPlayer player : earth.getPlayerManager().getPlayersOnline()) {
			if(town.isLocInside(player.getBukkitPlayer().getLocation())) {
				clearTownHearts(player);
			}
		}
	}
	
	public void refreshTownHearts(KonTown town) {
		for(KonPlayer player : earth.getPlayerManager().getPlayersOnline()) {
			if(town.isLocInside(player.getBukkitPlayer().getLocation())) {
				clearTownHearts(player);
				if(player.getKingdom().equals(town.getKingdom())) {
					applyTownHearts(player, town);
				}
			}
		}
	}

	public void updateKingdomOfflineProtection() {
		updateKingdomOfflineProtection(false);
	}

	public void updateKingdomOfflineProtection(boolean force) {
		boolean isOfflineProtectedEnabled = earth.getCore().getBoolean(CorePath.KINGDOMS_NO_ENEMY_EDIT_OFFLINE.getPath(),true);
		int offlineProtectedWarmupSeconds = earth.getCore().getInt(CorePath.KINGDOMS_NO_ENEMY_EDIT_OFFLINE_WARMUP.getPath(),0);
		int offlineProtectedMinimumPlayers = earth.getCore().getInt(CorePath.KINGDOMS_NO_ENEMY_EDIT_OFFLINE_MINIMUM.getPath(),0);
		int warmupMinutes = offlineProtectedWarmupSeconds / 60;
		int warmupSeconds = offlineProtectedWarmupSeconds % 60;
		String warmupTime = String.format("%d:%02d", warmupMinutes, warmupSeconds);
		// For each kingdom, start protection warmup timer if no players are online, else ensure the timer is stopped
		for(KonKingdom kingdom : getKingdoms()) {
			// Kingdom Offline Protection
			if(isOfflineProtectedEnabled) {
				ArrayList<KonPlayer> onlineKingdomPlayers = earth.getPlayerManager().getPlayersInKingdom(kingdom.getName());
				int numOnlineKingdomPlayers = onlineKingdomPlayers.size();
				Timer protectedWarmupTimer = kingdom.getProtectedWarmupTimer();
				if ((offlineProtectedMinimumPlayers <= 0 && onlineKingdomPlayers.isEmpty()) || (numOnlineKingdomPlayers < offlineProtectedMinimumPlayers)) {
					if (offlineProtectedWarmupSeconds > 0 && protectedWarmupTimer.getTime() == -1 && !kingdom.isOfflineProtected()) {
						// start timer
						ChatUtil.printDebug("Starting kingdom protection warmup timer for " + offlineProtectedWarmupSeconds + " seconds: " + kingdom.getName());
						protectedWarmupTimer.stopTimer();
						protectedWarmupTimer.setTime(offlineProtectedWarmupSeconds);
						protectedWarmupTimer.startTimer();
						ChatUtil.sendBroadcast(ChatColor.LIGHT_PURPLE + MessagePath.PROTECTION_NOTICE_KINGDOM_WARMUP.getMessage(kingdom.getName(), warmupTime));
					}
				} else {
					// stop timer, clear protection
					kingdom.setOfflineProtected(false);
					protectedWarmupTimer.stopTimer();
				}
			}
			// Town Watch Protection
			ArrayList<String> protectedTownNames = new ArrayList<>();
			for(KonTown town : kingdom.getCapitalTowns()) {
				if (town.updateProtection(force)) {
					protectedTownNames.add(town.getName());
				}
			}
			if (!protectedTownNames.isEmpty() && !force) {
				// Broadcast protection info
				ChatUtil.sendBroadcast(ChatColor.LIGHT_PURPLE + MessagePath.PROTECTION_NOTICE_TOWN_WARMUP.getMessage(kingdom.getName(), warmupTime, HelperUtil.formatCommaSeparatedList(protectedTownNames)));
			}
		}
	}
	
	public int getPlayerLordships(KonOfflinePlayer player) {
		int count = 0;
		for(KonTown town : player.getKingdom().getCapitalTowns()) {
			if(town.isPlayerLord(player.getOfflineBukkitPlayer())) {
				count = count + 1;
			}
		}
		return count;
	}

	public int getPlayerResidencies(KonOfflinePlayer player) {
		int count = 0;
		for(KonTown town : player.getKingdom().getCapitalTowns()) {
			if(town.isPlayerResident(player.getOfflineBukkitPlayer())) {
				count++;
			}
		}
		return count;
	}
	
	public List<KonTown> getPlayerLordshipTowns(EarthOfflinePlayer player) {
		List<KonTown> towns = new ArrayList<>();
		if(player.getKingdom() instanceof KonKingdom) {
			KonKingdom kingdom = (KonKingdom)player.getKingdom();
			for(KonTown town : kingdom.getCapitalTowns()) {
				if(town.isPlayerLord(player.getOfflineBukkitPlayer())) {
					towns.add(town);
				}
			}
		}
		return towns;
	}

	@NotNull
	public List<KonTown> getPlayerKnightTowns(EarthOfflinePlayer player) {
		List<KonTown> towns = new ArrayList<>();
		if(player.getKingdom() instanceof KonKingdom) {
			KonKingdom kingdom = (KonKingdom)player.getKingdom();
			for(KonTown town : kingdom.getCapitalTowns()) {
				if(town.isPlayerKnight(player.getOfflineBukkitPlayer()) && !town.isPlayerLord(player.getOfflineBukkitPlayer())) {
					towns.add(town);
				}
			}
		}
		return towns;
	}

	@NotNull
	public List<KonTown> getPlayerResidenceTowns(EarthOfflinePlayer player) {
		List<KonTown> towns = new ArrayList<>();
		if(player.getKingdom() instanceof KonKingdom) {
			KonKingdom kingdom = (KonKingdom)player.getKingdom();
			for(KonTown town : kingdom.getCapitalTowns()) {
				if(town.isPlayerResident(player.getOfflineBukkitPlayer())) {
					towns.add(town);
				}
			}
		}
		return towns;
	}

	/**
	 * Check every kingdom and town and ensure the player has
	 * valid memberships.
	 * @param player The player to check
	 */
	public void checkPlayerMemberships(KonPlayer player) {
		KonKingdom playerKingdom = player.getKingdom();
		Player bukkitPlayer = player.getBukkitPlayer();
		UUID id = bukkitPlayer.getUniqueId();
		for(KonKingdom kingdom : getKingdoms()) {
			// Skip player's kingdom
			if(kingdom.equals(playerKingdom)) {
				continue;
			}
			// Ensure player is not a member of other kingdoms
			if(kingdom.isMember(id)) {
				if(kingdom.removeMember(id)) {
					ChatUtil.printDebug("Cleaned kingdom membership of "+kingdom.getName()+" for player "+bukkitPlayer.getName());
				} else {
					ChatUtil.printDebug("FAILED to clean kingdom membership of "+kingdom.getName()+" for player "+bukkitPlayer.getName());
				}
			}
			// Ensure player is not a resident of other kingdom towns
			for(KonTown town : kingdom.getCapitalTowns()) {
				if(town.isPlayerResident(bukkitPlayer)) {
					if(town.removePlayerResident(bukkitPlayer)) {
						ChatUtil.printDebug("Cleaned town residency of "+town.getName()+" in kingdom "+kingdom.getName()+" for player "+bukkitPlayer.getName());
					} else {
						ChatUtil.printDebug("FAILED to clean town residency of "+town.getName()+" in kingdom "+kingdom.getName()+" for player "+bukkitPlayer.getName());
					}
				}
			}
		}
	}
	
	public void updatePlayerMembershipStats(KonPlayer player) {
		int countLord = 0;
		int countElite = 0;
		int countResident = 0;
		for(KonTown town : player.getKingdom().getCapitalTowns()) {
			if(town.isPlayerLord(player.getOfflineBukkitPlayer())) {
				countLord = countLord + 1;
			} else if(town.isPlayerKnight(player.getOfflineBukkitPlayer())) {
				countElite = countElite + 1;
			} else if(town.isPlayerResident(player.getOfflineBukkitPlayer())) {
				countResident = countResident + 1;
			}
		}
		player.getPlayerStats().setStat(KonStatsType.LORDS, countLord);
		player.getPlayerStats().setStat(KonStatsType.KNIGHTS, countElite);
		player.getPlayerStats().setStat(KonStatsType.RESIDENTS, countResident);
	}

	@NotNull
	public KonLeaderboard getKingdomLeaderboard(KonKingdom kingdom) {
		KonLeaderboard leaderboard = new KonLeaderboard();
		if(kingdom.equals(barbarians)) return leaderboard;
		// Determine scores for all players within towns
		HashMap<OfflinePlayer,KonPlayerScoreAttributes> memberScores = new HashMap<>();
		int numTownLords;
		int numTownKnights;
		int numTownResidents;
		int numLordLand;
		int numKnightLand;
		int numResidentLand;
		for(KonTown town : kingdom.getCapitalTowns()) {
			for(OfflinePlayer offlinePlayer : town.getPlayerResidents()) {
				numTownLords = 0;
				numTownKnights = 0;
				numTownResidents = 0;
				numLordLand = 0;
				numKnightLand = 0;
				numResidentLand = 0;
				if(town.isPlayerLord(offlinePlayer)) {
					numTownLords++;
					numLordLand += town.getChunkList().size();
				} else if(town.isPlayerKnight(offlinePlayer)) {
					numTownKnights++;
					numKnightLand += town.getChunkList().size();
				} else {
					numTownResidents++;
					numResidentLand += town.getChunkList().size();
				}
				if(memberScores.containsKey(offlinePlayer)) {
					memberScores.get(offlinePlayer).addAttribute(KonPlayerScoreAttribute.TOWN_LORDS, numTownLords);
					memberScores.get(offlinePlayer).addAttribute(KonPlayerScoreAttribute.TOWN_KNIGHTS, numTownKnights);
					memberScores.get(offlinePlayer).addAttribute(KonPlayerScoreAttribute.TOWN_RESIDENTS, numTownResidents);
					memberScores.get(offlinePlayer).addAttribute(KonPlayerScoreAttribute.LAND_LORDS, numLordLand);
					memberScores.get(offlinePlayer).addAttribute(KonPlayerScoreAttribute.LAND_KNIGHTS, numKnightLand);
					memberScores.get(offlinePlayer).addAttribute(KonPlayerScoreAttribute.LAND_RESIDENTS, numResidentLand);
				} else {
					KonOfflinePlayer validPlayer = earth.getPlayerManager().getOfflinePlayer(offlinePlayer);
					if(validPlayer != null && validPlayer.getKingdom().equals(kingdom)) {
						KonPlayerScoreAttributes newMemberAttributes = new KonPlayerScoreAttributes();
						newMemberAttributes.setAttribute(KonPlayerScoreAttribute.TOWN_LORDS, numTownLords);
						newMemberAttributes.setAttribute(KonPlayerScoreAttribute.TOWN_KNIGHTS, numTownKnights);
						newMemberAttributes.setAttribute(KonPlayerScoreAttribute.TOWN_RESIDENTS, numTownResidents);
						newMemberAttributes.setAttribute(KonPlayerScoreAttribute.LAND_LORDS, numLordLand);
						newMemberAttributes.setAttribute(KonPlayerScoreAttribute.LAND_KNIGHTS, numKnightLand);
						newMemberAttributes.setAttribute(KonPlayerScoreAttribute.LAND_RESIDENTS, numResidentLand);
						memberScores.put(offlinePlayer, newMemberAttributes);
					}
				}
			}
		}
		// Find top scoring members
		final int NUM_TOP_PLAYERS = 9;
		int topScore = 0;
		OfflinePlayer topPlayer = null;
		for(int i = 0;i<NUM_TOP_PLAYERS;i++) {
			if(!memberScores.isEmpty()) {
				for(OfflinePlayer offlinePlayer : memberScores.keySet()) {
					int playerScore = memberScores.get(offlinePlayer).getScore();
					if(playerScore > topScore) {
						topScore = playerScore;
						topPlayer = offlinePlayer;
					}
				}
				if(topPlayer != null) {
					leaderboard.addEntry(topPlayer,topScore);
					//ChatUtil.printDebug("Found leaderboard member "+topPlayer.getName()+", score: "+topScore);
					memberScores.remove(topPlayer);
					topScore = 0;
					topPlayer = null;
				}
			}
		}
		return leaderboard;
	}

	@NotNull
	public KonKingdomScoreAttributes getKingdomScoreAttributes(KonKingdom kingdom) {
		KonKingdomScoreAttributes scoreAttributes = new KonKingdomScoreAttributes();
		//if(!kingdom.equals(barbarians) && !kingdom.isPeaceful()) {
		if(kingdom.isCreated() && !kingdom.isPeaceful()) {
			// Gather Kingdom metrics
			int numKingdomTowns = kingdom.getTowns().size();
	    	ArrayList<KonOfflinePlayer> allPlayersInKingdom = earth.getPlayerManager().getAllPlayersInKingdom(kingdom.getName());
	    	int numAllKingdomPlayers = allPlayersInKingdom.size();
	    	int numKingdomLand = 0;
	    	for(KonTown town : kingdom.getCapitalTowns()) {
	    		numKingdomLand += town.getChunkList().size();
	    	}
	    	int numKingdomFavor = 0;
	    	for(KonOfflinePlayer kingdomPlayer : allPlayersInKingdom) {
	    		numKingdomFavor += (int) EarthPlugin.getBalance(kingdomPlayer.getOfflineBukkitPlayer());
	    	}
	    	// Gather favor costs
	    	int cost_settle = (int)earth.getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SETTLE.getPath());
	    	int cost_claim = (int)earth.getCore().getDouble(CorePath.FAVOR_COST_CLAIM.getPath());
	    	// Set attributes
	    	scoreAttributes.setAttributeWeight(KonKingdomScoreAttribute.TOWNS, cost_settle+2);
	    	scoreAttributes.setAttributeWeight(KonKingdomScoreAttribute.LAND, cost_claim+1);
	    	scoreAttributes.setAttribute(KonKingdomScoreAttribute.TOWNS, numKingdomTowns);
	    	scoreAttributes.setAttribute(KonKingdomScoreAttribute.LAND, numKingdomLand);
	    	scoreAttributes.setAttribute(KonKingdomScoreAttribute.FAVOR, numKingdomFavor);
	    	scoreAttributes.setAttribute(KonKingdomScoreAttribute.POPULATION, numAllKingdomPlayers);
		}
		return scoreAttributes;
	}
	
	public int getKingdomScore(KonKingdom kingdom) {
		return getKingdomScoreAttributes(kingdom).getScore();
	}

	@NotNull
	public KonPlayerScoreAttributes getPlayerScoreAttributes(KonOfflinePlayer offlinePlayer) {
		KonPlayerScoreAttributes scoreAttributes = new KonPlayerScoreAttributes();
		if(!offlinePlayer.isBarbarian() && !offlinePlayer.getKingdom().isPeaceful()) {
			// Gather player metrics
			int numTownLords = 0;
			int numTownKnights = 0;
			int numTownResidents = 0;
			int numLordLand = 0;
			int numKnightLand = 0;
			int numResidentLand = 0;
			for(KonTown town : offlinePlayer.getKingdom().getCapitalTowns()) {
				if(town.isPlayerLord(offlinePlayer.getOfflineBukkitPlayer())) {
					numTownLords++;
					numLordLand += town.getChunkList().size();
				} else if(town.isPlayerKnight(offlinePlayer.getOfflineBukkitPlayer())) {
					numTownKnights++;
					numKnightLand += town.getChunkList().size();
				} else if(town.isPlayerResident(offlinePlayer.getOfflineBukkitPlayer())) {
					numTownResidents++;
					numResidentLand += town.getChunkList().size();
				}
			}
			scoreAttributes.setAttribute(KonPlayerScoreAttribute.TOWN_LORDS, numTownLords);
			scoreAttributes.setAttribute(KonPlayerScoreAttribute.TOWN_KNIGHTS, numTownKnights);
			scoreAttributes.setAttribute(KonPlayerScoreAttribute.TOWN_RESIDENTS, numTownResidents);
			scoreAttributes.setAttribute(KonPlayerScoreAttribute.LAND_LORDS, numLordLand);
			scoreAttributes.setAttribute(KonPlayerScoreAttribute.LAND_KNIGHTS, numKnightLand);
			scoreAttributes.setAttribute(KonPlayerScoreAttribute.LAND_RESIDENTS, numResidentLand);
		}
		return scoreAttributes;
	}
	
	public int getPlayerScore(KonOfflinePlayer offlinePlayer) {
		return getPlayerScoreAttributes(offlinePlayer).getScore();
	}

	public void updateAllTownDisabledUpgrades() {
		for(KonKingdom kingdom : getKingdoms()) {
			// Capital
			earth.getUpgradeManager().updateTownDisabledUpgrades(kingdom.getCapital());
			// Towns
			for(KonTown town : kingdom.getTowns()) {
				earth.getUpgradeManager().updateTownDisabledUpgrades(town);
			}
		}
	}
	
	// Returns true when the player is still in cool-down, else false
	public boolean isPlayerJoinCooldown(OfflinePlayer player) {
		// Check if player is currently in cool-down.
		// If cool-down has expired, remove it.
		UUID id = player.getUniqueId();
		if(!joinPlayerCooldowns.containsKey(id))return false;
		long endSeconds = joinPlayerCooldowns.get(id);
		Date now = new Date();
		if(now.after(new Date(endSeconds*1000))) {
			// The cool-down has expired
			joinPlayerCooldowns.remove(id);
			return false;
		}

		return true;
	}
	
	public void applyPlayerJoinCooldown(OfflinePlayer player) {
		if(joinCooldownSeconds <= 0)return;
		UUID id = player.getUniqueId();
		int endTimeSeconds = (int)((new Date()).getTime()/1000) + joinCooldownSeconds;
		joinPlayerCooldowns.put(id, endTimeSeconds);
	}
	
	public int getJoinCooldownRemainingSeconds(OfflinePlayer player) {
		UUID id = player.getUniqueId();
		if(!joinPlayerCooldowns.containsKey(id))return 0;
		int endSeconds = joinPlayerCooldowns.get(id);
		Date now = new Date();
		if(now.after(new Date((long)endSeconds*1000))){
			return 0;
		}
		return endSeconds - (int)(now.getTime()/1000);
	}

	public String getJoinCooldownRemainingTimeFormat(OfflinePlayer player, ChatColor color) {
		String remainCooldown = "";
		int cooldown = getJoinCooldownRemainingSeconds(player);
		if(cooldown > 0) {
			remainCooldown = HelperUtil.getTimeFormat(cooldown, color);
		}
		return remainCooldown;
	}
	
	// Returns true when the player is still in cool-down, else false
	public boolean isPlayerExileCooldown(OfflinePlayer player) {
		// Check if player is currently in cool-down.
		// If cool-down has expired, remove it.
		UUID id = player.getUniqueId();
		if(!exilePlayerCooldowns.containsKey(id))return false;
		long endSeconds = exilePlayerCooldowns.get(id);
		Date now = new Date();
		if(now.after(new Date(endSeconds*1000))) {
			// The cooldown has expired
			exilePlayerCooldowns.remove(id);
			return false;
		}
		// The player is still in cool-down
		return true;
	}
	
	public void applyPlayerExileCooldown(OfflinePlayer player) {
		if(exileCooldownSeconds <= 0)return;
		UUID id = player.getUniqueId();
		int endTimeSeconds = (int)((new Date()).getTime()/1000) + exileCooldownSeconds;
		exilePlayerCooldowns.put(id, endTimeSeconds);
	}
	
	public int getExileCooldownRemainingSeconds(OfflinePlayer player) {
		UUID id = player.getUniqueId();
		if(!exilePlayerCooldowns.containsKey(id))return 0;
		int endSeconds = exilePlayerCooldowns.get(id);
		Date now = new Date();
		if(now.after(new Date((long)endSeconds*1000))){
			return 0;
		}
		return endSeconds - (int)(now.getTime()/1000);
	}

	public String getExileCooldownRemainingTimeFormat(OfflinePlayer player, ChatColor color) {
		String remainCooldown = "";
		int cooldown = getExileCooldownRemainingSeconds(player);
		if(cooldown > 0) {
			remainCooldown = HelperUtil.getTimeFormat(cooldown, color);
		}
		return remainCooldown;
	}
	
	public void loadJoinExileCooldowns() {
		joinCooldownSeconds = earth.getCore().getInt(CorePath.KINGDOMS_JOIN_COOLDOWN.getPath(),0);
		exileCooldownSeconds = earth.getCore().getInt(CorePath.KINGDOMS_EXILE_COOLDOWN.getPath(),0);
		// Check any existing cool-downs, ensure less than config values.
		// Join cool-down map
		if(!joinPlayerCooldowns.isEmpty()) {
			HashMap<UUID,Integer> updateJoinMap = new HashMap<>();
			if(joinCooldownSeconds > 0) {
				int nowSeconds = (int)((new Date()).getTime()/1000);
				int maxEndSeconds = nowSeconds + joinCooldownSeconds;
				for(UUID u : joinPlayerCooldowns.keySet()) {
					int endSeconds = joinPlayerCooldowns.get(u);
					// Existing cool-down is longer than new config setting, shorten it.
					// Existing cool-down is under config, preserve it.
					updateJoinMap.put(u, Math.min(endSeconds, maxEndSeconds));
				}
			}
			joinPlayerCooldowns.clear();
			joinPlayerCooldowns.putAll(updateJoinMap);
		}
		// Exile cool-down map
		if(!exilePlayerCooldowns.isEmpty()) {
			HashMap<UUID,Integer> updateExileMap = new HashMap<>();
			if(exileCooldownSeconds > 0) {
				int nowSeconds = (int)((new Date()).getTime()/1000);
				int maxEndSeconds = nowSeconds + exileCooldownSeconds;
				for(UUID u : exilePlayerCooldowns.keySet()) {
					int endSeconds = exilePlayerCooldowns.get(u);
					// Existing cool-down is longer than new config setting, shorten it.
					// Existing cool-down is under config, preserve it.
					updateExileMap.put(u, Math.min(endSeconds, maxEndSeconds));
				}
			}
			exilePlayerCooldowns.clear();
			exilePlayerCooldowns.putAll(updateExileMap);
		}
	}
	
	public Material getTownCriticalBlock() {
		return townCriticalBlock;
	}
	
	public int getMaxCriticalHits() {
		return maxCriticalHits;
	}

	/**
	 * Loads the custom monument critical block from core.yml
	 */
	public void loadCriticalBlocks() {
		maxCriticalHits = earth.getCore().getInt(CorePath.MONUMENTS_DESTROY_AMOUNT.getPath(),12);
		String townCriticalBlockTypeName = earth.getCore().getString(CorePath.MONUMENTS_CRITICAL_BLOCK.getPath(),"");
		try {
			townCriticalBlock = Material.valueOf(townCriticalBlockTypeName);
		} catch(IllegalArgumentException e) {
			String message = "Invalid monument critical block \""+townCriticalBlockTypeName+"\" given in core.monuments.critical_block, using default OBSIDIAN";
    		ChatUtil.printConsoleError(message);
    		earth.opStatusMessages.add(message);
		}
	}
	
	public boolean isArmorValid(Material mat) {
		boolean result = false;
		if(isArmorBlockWhitelist) {
			result = armorBlocks.contains(mat);
		} else {
			result = !armorBlocks.contains(mat);
		}
		return result;
	}
	
	public void loadArmorBlacklist() {
		armorBlocks.clear();
		// True = list is a whitelist, False = list is a blacklist (Default false)
		isArmorBlockWhitelist = earth.getCore().getBoolean(CorePath.TOWNS_ARMOR_BLACKLIST_REVERSE.getPath(),false);
		boolean isListEnabled = earth.getCore().getBoolean(CorePath.TOWNS_ARMOR_BLACKLIST_ENABLE.getPath(),false);
		if(isListEnabled) {
			List<String> armorList = earth.getCore().getStringList(CorePath.TOWNS_ARMOR_BLACKLIST.getPath());
			Material mat;
			for(String entry : armorList) {
				mat = Material.matchMaterial(entry);
				if(mat != null) {
					armorBlocks.add(mat);
				}else{
					ChatUtil.printConsoleWarning("Invalid material \""+entry+"\" in "+CorePath.TOWNS_ARMOR_BLACKLIST.getPath()+", ignoring.");
				}
			}
		}
	}

	/**
	 * This method is meant to run after all kingdoms have been loaded from file
	 */
	private void assignDiplomacyTickets() {
		// Go through all tickets and set relations
		for(DiplomacyTicket ticket : diplomacyTickets) {
			// Get owner kingdom
			KonKingdom kingdom  = getKingdom(ticket.getOwnerKingdomName());
			if(!kingdom.isCreated()) {
				// Must be a created kingdom (not barbarian, neutral) to have relationships
				continue;
			}
			// Get other kingdom
			KonKingdom otherKingdom = getKingdom(ticket.getOtherKingdomName());
			if(!otherKingdom.isCreated()) {
				// Must be a created kingdom (not barbarian, neutral) to have relationships
				continue;
			}
			// Get diplomatic relation
			EarthDiplomacyType relation = ticket.getRelation();
			if(relation == null) {
				continue;
			}
			// Check for active vs request
			if(ticket.isActive()) {
				// Relation is active
				kingdom.setActiveRelation(otherKingdom, relation);
			} else {
				// Relation is request
				kingdom.setRelationRequest(otherKingdom, relation);
			}
		}
		// Finished, clear tickets
		diplomacyTickets.clear();
	}
	
	private void loadKingdoms() {
		FileConfiguration kingdomsConfig = earth.getConfigManager().getConfig("kingdoms");
        if (kingdomsConfig.get("kingdoms") == null) {
        	ChatUtil.printConsoleError("Failed to load any kingdoms from kingdoms.yml! Check file permissions.");
			isKingdomDataNull = true;
            return;
        }

        double x,y,z;
        List<Double> sectionList;
        String worldName;
        String defaultWorldName = earth.getCore().getString(CorePath.WORLD_NAME.getPath(),"world");
		diplomacyTickets.clear();
		ConfigurationSection rootConfig = kingdomsConfig.getConfigurationSection("kingdoms");
		assert rootConfig != null;
        // Count all towns
		int numKingdoms = 0;
        int numTowns = 0;
        for(String kingdomName : rootConfig.getKeys(false)) {
			ConfigurationSection kingdomTownsSection = rootConfig.getConfigurationSection(kingdomName+".towns");
			if(kingdomTownsSection != null) {
				numTowns += kingdomTownsSection.getKeys(false).size();
			}
			numKingdoms++;
        }
        LoadingPrinter loadBar = new LoadingPrinter(numTowns,"Loading "+numKingdoms+" Kingdoms with "+numTowns+" Towns");
        // Load all Kingdoms
        for(String kingdomName : rootConfig.getKeys(false)) {
        	ConfigurationSection kingdomSection = rootConfig.getConfigurationSection(kingdomName);
			assert kingdomSection != null;
        	// Check for center capital field
        	ConfigurationSection capitalSection = kingdomSection.getConfigurationSection("towns.capital");
			if(capitalSection == null) {
				// Error, there is no capital section
				ChatUtil.printConsoleError("Bad data format in kingdoms.yml for kingdom "+kingdomName+", attempting to migrate legacy kingdom to current format.");
				// This kingdom data may be from an older version, attempt to migrate.
				boolean legacyStatus = loadLegacyKingdom(kingdomSection, kingdomName);
				String message;
				if(legacyStatus) {
					// Successfully migrated legacy kingdom
					message = "Kingdom " + kingdomName + " was created in a legacy version of Earth and has been migrated. Review the kingdom setup.";
					ChatUtil.printConsoleAlert(message);
				} else {
					// Failed to load legacy kingdom
					message = "Kingdom " + kingdomName + " was created in a legacy version of Earth and could not be migrated. The kingdom was deleted.";
					ChatUtil.printConsoleError(message);
				}
				earth.opStatusMessages.add(message);
				// Skip to the next kingdom
				continue;
			}
			// Check for loaded world
			worldName = capitalSection.getString("world",defaultWorldName);
			World capitalWorld = Bukkit.getWorld(worldName);
			if(capitalWorld == null) {
				// Error, world is not loaded
				String message = "Failed to load kingdom "+kingdomName+" capital in an unloaded world, "+worldName+". Check plugin load order.";
				ChatUtil.printConsoleError(message);
				earth.opStatusMessages.add(message);
				// Skip to the next kingdom
				continue;
			}
			// Load the kingdom capital
			sectionList = capitalSection.getDoubleList("center");
			x = sectionList.get(0);
			y = sectionList.get(1);
			z = sectionList.get(2);
			Location capitalCenter = new Location(capitalWorld,x,y,z);
			// Add new kingdom
			kingdomMap.put(kingdomName, new KonKingdom(capitalCenter, kingdomName, earth));
			KonKingdom newKingdom = kingdomMap.get(kingdomName);
			// Kingdom Operating Mode
			boolean isAdmin = kingdomSection.getBoolean("admin", false);
			newKingdom.setIsAdminOperated(isAdmin);
			// Kingdom Web Color
			int webColor = kingdomSection.getInt("webcolor", -1);
			newKingdom.setWebColor(webColor);
			// Kingdom Template
			String templateName = kingdomSection.getString("template","");
			if(earth.getSanctuaryManager().isTemplate(templateName)) {
				KonMonumentTemplate template = earth.getSanctuaryManager().getTemplate(templateName);
				newKingdom.setMonumentTemplate(template);
			} else {
				ChatUtil.printConsoleError("Missing monument template \""+templateName+"\" for Kingdom "+kingdomName+" in loaded data file!");
				earth.opStatusMessages.add("Missing monument template for Kingdom "+kingdomName+"! Use \"/k admin kingdom\" to set a monument template for this Kingdom.");
			}
			// Kingdom Properties
			ConfigurationSection kingdomPropertiesSection = kingdomSection.getConfigurationSection("properties");
			if(kingdomPropertiesSection != null) {
				for(String propertyName : kingdomPropertiesSection.getKeys(false)) {
					boolean value = kingdomPropertiesSection.getBoolean(propertyName);
					KonPropertyFlag property = KonPropertyFlag.getFlag(propertyName);
					boolean status = newKingdom.setPropertyValue(property, value);
					if(!status) {
						ChatUtil.printDebug("Failed to set invalid property "+propertyName+" to Kingdom "+kingdomName);
					}
				}
			}
			// Kingdom Membership
			// Set open flag
			boolean isKingdomOpen = kingdomSection.getBoolean("open",false);
			newKingdom.setIsOpen(isKingdomOpen);
			// Assign Master
			String masterUUID = kingdomSection.getString("master","");
			if(!masterUUID.equalsIgnoreCase("")) {
				UUID playerID = HelperUtil.idFromString(masterUUID);
				if(playerID != null) {
					newKingdom.forceMaster(playerID);
				} else {
					ChatUtil.printDebug("Kingdom "+kingdomName+" has a null UUID! Master remains invalid");
				}
			} else {
				ChatUtil.printDebug("Kingdom "+kingdomName+" does not have a stored Master ID");
			}
			// Populate Members
			ConfigurationSection kingdomMembersSection = kingdomSection.getConfigurationSection("members");
			if(kingdomMembersSection != null) {
				for(String residentUUID : kingdomMembersSection.getKeys(false)) {
					boolean isOfficer = kingdomMembersSection.getBoolean(residentUUID, false);
					newKingdom.addMember(UUID.fromString(residentUUID),isOfficer);
				}
			}
			// Add invite requests
			ConfigurationSection kingdomRequestsSection = kingdomSection.getConfigurationSection("requests");
			if(kingdomRequestsSection != null) {
				for(String requestUUID : kingdomRequestsSection.getKeys(false)) {
					boolean type = kingdomRequestsSection.getBoolean(requestUUID, false);
					newKingdom.addJoinRequest(UUID.fromString(requestUUID), type);
				}
			}
			// Kingdom Diplomacy
			// Submit names and relationships to a queue for later assignment, once all kingdoms have been loaded
			ConfigurationSection kingdomDiplomacyActiveSection = kingdomSection.getConfigurationSection("diplomacy_active");
			if(kingdomDiplomacyActiveSection != null) {
				for(String diplomacyKingdomName : kingdomDiplomacyActiveSection.getKeys(false)) {
					String relationName = kingdomDiplomacyActiveSection.getString(diplomacyKingdomName);
					if(relationName == null) {
						continue;
					}
					EarthDiplomacyType relation = EarthDiplomacyType.getDefault();
					try {
						relation = EarthDiplomacyType.valueOf(relationName);
					} catch(Exception e) {
						ChatUtil.printConsoleAlert("Failed to parse active diplomacy relation "+relationName+" between kingdoms "+kingdomName+" and "+diplomacyKingdomName);
					}
					DiplomacyTicket ticket = new DiplomacyTicket(kingdomName, diplomacyKingdomName, relation, true);
					diplomacyTickets.add(ticket);
				}
			}
			ConfigurationSection kingdomDiplomacyRequestSection = kingdomSection.getConfigurationSection("diplomacy_request");
			if(kingdomDiplomacyRequestSection != null) {
				for(String diplomacyKingdomName : kingdomDiplomacyRequestSection.getKeys(false)) {
					String relationName = kingdomDiplomacyRequestSection.getString(diplomacyKingdomName);
					if(relationName == null) {
						continue;
					}
					EarthDiplomacyType relation = EarthDiplomacyType.getDefault();
					try {
						relation = EarthDiplomacyType.valueOf(relationName);
					} catch(Exception e) {
						ChatUtil.printConsoleAlert("Failed to parse request diplomacy relation "+relationName+" between kingdoms "+kingdomName+" and "+diplomacyKingdomName);
					}
					DiplomacyTicket ticket = new DiplomacyTicket(kingdomName, diplomacyKingdomName, relation, false);
					diplomacyTickets.add(ticket);
				}
			}
			// Capital and Town Loading all towns
			ConfigurationSection kingdomTownsSection = kingdomSection.getConfigurationSection("towns");
			if(kingdomTownsSection == null) {
				// Error, there is no town section
				ChatUtil.printDebug("Internal error, null town section in kingdoms.yml for kingdom "+kingdomName);
				// Skip to the next kingdom
				continue;
			}
			for(String townName : kingdomTownsSection.getKeys(false)) {
				boolean isCapital = townName.equals("capital");
				ConfigurationSection townSection = kingdomTownsSection.getConfigurationSection(townName);
				assert townSection != null;
				// Load the town from file
				loadTown(townSection, townName, kingdomName, isCapital);
				// Update loading bar
				loadBar.addProgress(1);
			}
			// Check for invalid template
			if(!newKingdom.isMonumentTemplateValid()) {
				earth.opStatusMessages.add("Kingdom "+kingdomName+" has Towns with invalid Monuments. You must set up a valid Monument Template and restart the server.");
			}
        }
        if(kingdomMap.isEmpty()) {
			ChatUtil.printConsoleAlert("There are no Kingdoms.");
		} else {
			// Some kingdoms exist, assign diplomacy relationships
			assignDiplomacyTickets();
			ChatUtil.printDebug("Loaded Kingdoms");
		}
	}

	/**
	 * Attempts to load a kingdom from the given config and name, using legacy data file formats.
	 * This method creates new sanctuaries and monument templates.
	 * @param kingdomSection
	 * @param kingdomName
	 */
	private boolean loadLegacyKingdom(ConfigurationSection kingdomSection, String kingdomName) {
		// Try to identify legacy data formats
		ConfigurationSection capitalSection = kingdomSection.getConfigurationSection("capital");
		ConfigurationSection monumentSection = kingdomSection.getConfigurationSection("monument");
		ConfigurationSection townsSection = kingdomSection.getConfigurationSection("towns");
		if(capitalSection == null || monumentSection == null || townsSection == null) {
			ChatUtil.printDebug("Failed to load legacy kingdom "+kingdomName+", missing primary sections.");
			return false;
		}
		// Create a new sanctuary based on the legacy capital.
		// Create a new template based on the legacy monument.
		// Load the legacy towns, but make the one with the highest population into the capital.

		// Check for towns
		Set<String> townNames = townsSection.getKeys(false);
		if(townNames.isEmpty()) {
			// Failed to create kingdom with no towns
			ChatUtil.printDebug("Failed to load legacy kingdom "+kingdomName+", no towns");
			return false;
		}
		// Check the world
		String defaultWorldName = earth.getCore().getString(CorePath.WORLD_NAME.getPath(),"world");
		String worldName = capitalSection.getString("world",defaultWorldName);
		World capitalWorld = Bukkit.getWorld(worldName);
		if(capitalWorld == null) {
			ChatUtil.printDebug("Failed to load legacy kingdom "+kingdomName+", null world: "+worldName);
			return false;
		}
		// Gather capital + monument data
		double x,y,z;
		float pitch,yaw;
		List<Double> sectionList;
		sectionList = capitalSection.getDoubleList("spawn");
		x = sectionList.get(0);
		y = sectionList.get(1);
		z = sectionList.get(2);
		if(sectionList.size() > 3) {
			double val = sectionList.get(3);
			pitch = (float)val;
		} else {
			pitch = 0;
		}
		if(sectionList.size() > 4) {
			double val = sectionList.get(4);
			yaw = (float)val;
		} else {
			yaw = 0;
		}
		Location capital_spawn = new Location(capitalWorld,x,y,z,yaw,pitch);
		sectionList = capitalSection.getDoubleList("center");
		x = sectionList.get(0);
		y = sectionList.get(1);
		z = sectionList.get(2);
		Location capital_center = new Location(capitalWorld,x,y,z);
		ArrayList<Point> capital_chunks = new ArrayList<>();
		String chunkStr = capitalSection.getString("chunks");
		if(chunkStr != null) {
			capital_chunks.addAll(HelperUtil.formatStringToPoints(chunkStr));
		}
		sectionList = monumentSection.getDoubleList("travel");
		x = sectionList.get(0);
		y = sectionList.get(1);
		z = sectionList.get(2);
		Location monument_travel = new Location(capitalWorld,x,y,z);
		sectionList = monumentSection.getDoubleList("cornerone");
		x = sectionList.get(0);
		y = sectionList.get(1);
		z = sectionList.get(2);
		Location monument_cornerone = new Location(capitalWorld,x,y,z);
		sectionList = monumentSection.getDoubleList("cornertwo");
		x = sectionList.get(0);
		y = sectionList.get(1);
		z = sectionList.get(2);
		Location monument_cornertwo = new Location(capitalWorld,x,y,z);

		// Create a new sanctuary
		String sanctuaryName = kingdomName+"Sanctuary";
		boolean sanctuaryStatus = earth.getSanctuaryManager().addSanctuary(capital_center,sanctuaryName);
		if(!sanctuaryStatus) {
			// Failed to add new sanctuary
			ChatUtil.printDebug("Failed to load legacy kingdom "+kingdomName+", could not add sanctuary: "+sanctuaryName);
			return false;
		}
		KonSanctuary sanctuary = earth.getSanctuaryManager().getSanctuary(sanctuaryName);
		// Set spawn location
		sanctuary.setSpawn(capital_spawn);
		// Set territory chunks
		sanctuary.addPoints(capital_chunks);
		earth.getTerritoryManager().addAllTerritory(capitalWorld,sanctuary.getChunkList());
		ChatUtil.printConsoleAlert("Created new sanctuary "+sanctuaryName+" from legacy kingdom migration.");

		// Create a new monument template
		double cost = 0;
		// Create  & validate template
		// If it fails validation, it is still loaded into memory, but marked as invalid.
		String templateName = kingdomName+"Template";
		earth.getSanctuaryManager().loadMonumentTemplate(sanctuary, templateName, monument_cornerone, monument_cornertwo, monument_travel, cost);
		ChatUtil.printConsoleAlert("Created new monument template "+templateName+" from legacy kingdom migration.");

		// Identify the town which will become the capital.
		String capitalName = "";
		int capitalPopulation = 0;
		int capitalLand = 0;
		for(String townName : townNames) {
			ConfigurationSection townSection = townsSection.getConfigurationSection(townName);
			assert townSection != null;
			int townPopulation = 0;
			int townLand = 0;
			// Count town lord
			String lordUUID = townSection.getString("lord","");
			if(!lordUUID.equalsIgnoreCase("")) {
				townPopulation++;
			}
			// Count town residents
			ConfigurationSection townResidentsSection = townSection.getConfigurationSection("residents");
			if(townResidentsSection != null) {
				townPopulation += townResidentsSection.getKeys(false).size();
			}
			// Count town land
			String townChunks = townSection.getString("chunks");
			if(townChunks != null) {
				townLand += HelperUtil.formatStringToPoints(townChunks).size();
			}
			// Compare to previous
			ChatUtil.printDebug("Evaluating for capital: "+townName+", "+townLand+", "+townPopulation);
			// First compare by land
			if(townLand > capitalLand) {
				// This town has more land than the previous largest
				capitalPopulation = townPopulation;
				capitalName = townName;
				capitalLand = townLand;
				ChatUtil.printDebug("Chose largest land: "+townName);
			} else if(townLand == capitalLand) {
				// This town has the same land as the previous largest
				// Compare by population
				if(townPopulation >= capitalPopulation) {
					// This town has greater or equal population than the previous largest
					capitalPopulation = townPopulation;
					capitalName = townName;
					capitalLand = townLand;
					ChatUtil.printDebug("Chose largest land, largest pop: "+townName);
				}
			}
		}

		// Create the kingdom using new sanctuary and template
		sectionList = townsSection.getConfigurationSection(capitalName).getDoubleList("center");
		x = sectionList.get(0);
		y = sectionList.get(1);
		z = sectionList.get(2);
		Location capitalCenter = new Location(capitalWorld,x,y,z);
		// Add new kingdom
		kingdomMap.put(kingdomName, new KonKingdom(capitalCenter, kingdomName, earth));
		KonKingdom newKingdom = kingdomMap.get(kingdomName);
		// Kingdom Template
		if(earth.getSanctuaryManager().isTemplate(templateName)) {
			KonMonumentTemplate template = earth.getSanctuaryManager().getTemplate(templateName);
			newKingdom.setMonumentTemplate(template);
		} else {
			ChatUtil.printDebug("Missing template for legacy kingdom "+kingdomName+", could not find template: "+templateName);
		}
		// Set legacy flag
		newKingdom.setIsLegacy(true);
		// Set to open
		newKingdom.setIsOpen(true);
		// Set to admin kingdom
		newKingdom.setIsAdminOperated(true);
		// Load towns
		for(String townName : townsSection.getKeys(false)) {
			boolean isCapital = townName.equals(capitalName);
			ConfigurationSection townSection = townsSection.getConfigurationSection(townName);
			assert townSection != null;
			// Load the town from file
			loadTown(townSection, townName, kingdomName, isCapital);
		}

		return true;
	}

	/**
	 * Loads a town from data file.
	 * @param townSection
	 * @param townName
	 * @param kingdomName
	 * @param isCapital
	 */
	private void loadTown(ConfigurationSection townSection, String townName, String kingdomName, boolean isCapital) {
		double x,y,z;
		float pitch,yaw;
		List<Double> sectionList;
		boolean isShieldsEnabled = earth.getCore().getBoolean(CorePath.TOWNS_ENABLE_SHIELDS.getPath(),false);
		boolean isArmorsEnabled = earth.getCore().getBoolean(CorePath.TOWNS_ENABLE_ARMOR.getPath(),false);
		String defaultWorldName = earth.getCore().getString(CorePath.WORLD_NAME.getPath(),"world");
		// Check for loaded town world
		String worldName = townSection.getString("world",defaultWorldName);
		World townWorld = Bukkit.getServer().getWorld(worldName);
		if(townWorld == null) {
			// Error, town world is not loaded
			String message = "Failed to load town "+townName+" in an unloaded world, "+worldName+". Check plugin load order.";
			ChatUtil.printConsoleError(message);
			earth.opStatusMessages.add(message);
			// Skip to the next town
			return;
		}
		// Load the town
		int base = townSection.getInt("base");
		sectionList = townSection.getDoubleList("spawn");
		x = sectionList.get(0);
		y = sectionList.get(1);
		z = sectionList.get(2);
		if(sectionList.size() > 3) {
			double val = sectionList.get(3);
			pitch = (float)val;
		} else {
			pitch = 0;
		}
		if(sectionList.size() > 4) {
			double val = sectionList.get(4);
			yaw = (float)val;
		} else {
			yaw = 0;
		}
		Location townSpawn = new Location(townWorld,x,y,z,yaw,pitch);
		sectionList = townSection.getDoubleList("center");
		x = sectionList.get(0);
		y = sectionList.get(1);
		z = sectionList.get(2);
		Location townCenter = new Location(townWorld,x,y,z);
		// Create Town/Capital
		KonTown town;
		if(isCapital) {
			town = kingdomMap.get(kingdomName).getCapital();
		} else {
			kingdomMap.get(kingdomName).addTown(townCenter, townName);
			town = kingdomMap.get(kingdomName).getTown(townName);
		}
		// Town Properties
		ConfigurationSection townPropertiesSection = townSection.getConfigurationSection("properties");
		if(townPropertiesSection != null) {
			for(String propertyName : townPropertiesSection.getKeys(false)) {
				boolean value = townPropertiesSection.getBoolean(propertyName);
				KonPropertyFlag property = KonPropertyFlag.getFlag(propertyName);
				boolean status = town.setPropertyValue(property, value);
				if(!status) {
					ChatUtil.printDebug("Failed to set invalid property "+propertyName+" to Town "+townName);
				}
			}
		}
		// Set spawn point
		town.setSpawn(townSpawn);
		// Setup town monument parameters from template
		int monumentStatus = town.loadMonument(base, kingdomMap.get(kingdomName).getMonumentTemplate());
		if(monumentStatus != 0) {
			ChatUtil.printConsoleError("Failed to load monument for Town "+townName+" in kingdom "+kingdomName+" from invalid template");
		}
		// Add all Town chunk claims
		String chunkStr = townSection.getString("chunks");
		if(chunkStr != null) {
			town.addPoints(HelperUtil.formatStringToPoints(chunkStr));
		}
		// Update territory cache
		earth.getTerritoryManager().addAllTerritory(townWorld,town.getChunkList());
		// Set specialization
		String specializationName = townSection.getString("specialization","NONE");
		Villager.Profession profession = CompatibilityUtil.getProfessionFromName(specializationName);
		if (profession == null) {
			ChatUtil.printConsoleAlert("Failed to parse profession "+specializationName+" for town "+townName);
			profession = Villager.Profession.NONE;
		}
		town.setSpecialization(profession);
		// Set shield
		boolean isShieldActive = townSection.getBoolean("shield",false);
		int shieldTime = townSection.getInt("shield_time",0);
		Date now = new Date();
		if(isShieldsEnabled && isShieldActive && shieldTime > (now.getTime()/1000)) {
			town.activateShield(shieldTime);
		}
		// Set armor
		boolean isArmorActive = townSection.getBoolean("armor",false);
		int armorBlocks = townSection.getInt("armor_blocks",0);
		if(isArmorsEnabled && isArmorActive && armorBlocks > 0) {
			town.activateArmor(armorBlocks);
		}
		// Set open flag
		boolean isOpen = townSection.getBoolean("open",false);
		town.setIsOpen(isOpen);
		// Set plot flag
		boolean isPlotOnly = townSection.getBoolean("plot",false);
		town.setIsPlotOnly(isPlotOnly);
		// Set allied building flags
		boolean isAlliedBuilding = townSection.getBoolean("allied_building",false);
		town.setIsAlliedBuildingAllowed(isAlliedBuilding);
		// Set redstone flags
		boolean isFriendlyRedstone = townSection.getBoolean("friendly_redstone",true);
		town.setIsFriendlyRedstoneAllowed(isFriendlyRedstone);
		boolean isEnemyRedstone = townSection.getBoolean("redstone",false);
		town.setIsEnemyRedstoneAllowed(isEnemyRedstone);
		// Set golem offensive flag
		boolean isGolemOffensive = townSection.getBoolean("golem_offensive",false);
		town.setIsGolemOffensive(isGolemOffensive);
		// Assign Lord
		String lordUUID = townSection.getString("lord","");
		if(!lordUUID.equalsIgnoreCase("")) {
			UUID playerID = HelperUtil.idFromString(lordUUID);
			if(playerID != null) {
				town.setLord(playerID,false);
			} else {
				ChatUtil.printDebug("Town "+townName+" in kingdom "+kingdomName+" has a null UUID! Lord remains invalid");
			}
		} else {
			ChatUtil.printDebug("Town "+townName+" in kingdom "+kingdomName+" does not have a stored Lord ID");
		}
		// Populate Residents
		ConfigurationSection townResidentSection = townSection.getConfigurationSection("residents");
		if(townResidentSection != null) {
			for(String residentUUID : townResidentSection.getKeys(false)) {
				boolean isElite = townSection.getBoolean("residents."+residentUUID);
				town.addPlayerResident(Bukkit.getOfflinePlayer(UUID.fromString(residentUUID)),isElite,false);
			}
		}
		// Add invite requests
		ConfigurationSection townRequestsSection = townSection.getConfigurationSection("requests");
		if(townRequestsSection != null) {
			for(String requestUUID : townRequestsSection.getKeys(false)) {
				boolean type = townSection.getBoolean("requests."+requestUUID);
				town.addJoinRequest(UUID.fromString(requestUUID), type);
			}
		}
		// Add upgrades
		ConfigurationSection townUpgradesSection = townSection.getConfigurationSection("upgrades");
		if(townUpgradesSection != null) {
			for(String upgradeName : townUpgradesSection.getKeys(false)) {
				int level = townSection.getInt("upgrades."+upgradeName);
				KonUpgrade upgrade = KonUpgrade.getUpgrade(upgradeName);
				if(upgrade != null) {
					town.addUpgrade(upgrade, level);
				}
			}
		}
		// Update upgrade status
		earth.getUpgradeManager().updateTownDisabledUpgrades(town);
		// Create plots
		ConfigurationSection townPlotsSection = townSection.getConfigurationSection("plots");
		if(townPlotsSection != null) {
			for(String plotIndex : townPlotsSection.getKeys(false)) {
				String plotStr = townPlotsSection.getString(plotIndex + ".chunks");
				if(plotStr == null) {
					continue;
				}
				HashSet<Point> points = new HashSet<>(HelperUtil.formatStringToPoints(plotStr));
				ArrayList<UUID> users = new ArrayList<>();
				for(String user : townSection.getStringList("plots."+plotIndex+".members")) {
					users.add(UUID.fromString(user));
				}
				KonPlot plot = new KonPlot(points,users);
				if(!earth.getPlotManager().addPlot(town, plot)) {
					ChatUtil.printConsoleError("Failed to add incompatible plot to town "+town.getName());
				}
			}
		}
	}

	/**
	 * This method is intended to be run on plugin enable, after the
	 * KingdomManager and PlayerManager have been initialized.
	 * Updates legacy kingdom memberships from the offline player database.
	 */
	public void loadLegacyKingdomMemberships() {
		// For every kingdom, find offline players with a matching kingdom and add them as members.
		for(KonKingdom kingdom : getKingdoms()) {
			// Only update legacy kingdoms
			if(!kingdom.isLegacy()) {
				// Skip this kingdom
				continue;
			}
			String kingdomName = kingdom.getName();
			ChatUtil.printDebug("Loading memberships for legacy kingdom "+kingdomName);
			// Find players
			for(KonOfflinePlayer member : earth.getPlayerManager().getAllPlayersInKingdom(kingdom)) {
				UUID id = member.getOfflineBukkitPlayer().getUniqueId();
				kingdom.addMember(id,false);
				String playerName = member.getOfflineBukkitPlayer().getName();
				ChatUtil.printDebug("Added membership for player "+playerName+" in legacy kingdom "+kingdomName);
			}
		}
	}
	
	public void saveKingdoms() {
		if(isKingdomDataNull && kingdomMap.isEmpty()) {
			// There was probably an issue loading kingdoms, do not save.
			ChatUtil.printConsoleError("Aborted saving kingdom data because a problem was encountered while loading data from kingdoms.yml");
			return;
		}
		FileConfiguration kingdomsConfig = earth.getConfigManager().getConfig("kingdoms");
		kingdomsConfig.set("kingdoms", null); // reset kingdoms config
		ConfigurationSection root = kingdomsConfig.createSection("kingdoms");
		for(KonKingdom kingdom : kingdomMap.values()) {
			ConfigurationSection kingdomSection = root.createSection(kingdom.getName());
			// Kingdom Template Name
			kingdomSection.set("template",kingdom.getMonumentTemplateName());
			// Kingdom Operating Mode
			kingdomSection.set("admin",kingdom.isAdminOperated());
			// Kingdom Web Color
			kingdomSection.set("webcolor",kingdom.getWebColor());
			// Kingdom Properties
			ConfigurationSection kingdomPropertiesSection = kingdomSection.createSection("properties");
			for(KonPropertyFlag flag : KonPropertyFlag.values()) {
				if(kingdom.hasPropertyValue(flag)) {
					kingdomPropertiesSection.set(flag.toString(), kingdom.getPropertyValue(flag));
				}
			}
			// Kingdom Membership
			kingdomSection.set("open", kingdom.isOpen());
			kingdomSection.set("master", "");
			ConfigurationSection kingdomMemberSection = kingdomSection.createSection("members");
			for(OfflinePlayer member : kingdom.getPlayerMembers()) {
				String uuid = member.getUniqueId().toString();
				if(kingdom.isMaster(member.getUniqueId())) {
					kingdomSection.set("master", uuid);
				} else if(kingdom.isOfficer(member.getUniqueId())) {
					kingdomMemberSection.set(uuid, true);
				} else {
					kingdomMemberSection.set(uuid, false);
				}
			}
			ConfigurationSection kingdomRequestsSection = kingdomSection.createSection("requests");
			for(OfflinePlayer requestee : kingdom.getJoinRequests()) {
				String uuid = requestee.getUniqueId().toString();
				kingdomRequestsSection.set(uuid, false);
			}
			for(OfflinePlayer invitee : kingdom.getJoinInvites()) {
				String uuid = invitee.getUniqueId().toString();
				kingdomRequestsSection.set(uuid, true);
			}
			// Kingdom Diplomacy
			ConfigurationSection kingdomRelationActiveSection = kingdomSection.createSection("diplomacy_active");
			for(EarthKingdom diplomacyKingdom : kingdom.getActiveRelationKingdoms()) {
				kingdomRelationActiveSection.set(diplomacyKingdom.getName(), kingdom.getActiveRelation(diplomacyKingdom).toString());
			}
			ConfigurationSection kingdomRelationRequestSection = kingdomSection.createSection("diplomacy_request");
			for(EarthKingdom diplomacyKingdom : kingdom.getRelationRequestKingdoms()) {
				kingdomRelationRequestSection.set(diplomacyKingdom.getName(), kingdom.getRelationRequest(diplomacyKingdom).toString());
			}
			// Towns + Capital
			List<KonTown> allTowns = new ArrayList<>();
			allTowns.add(kingdom.getCapital());
			allTowns.addAll(kingdom.getTowns());
			ConfigurationSection townsSection = kingdomSection.createSection("towns");
			for(KonTown town : allTowns) {
				ConfigurationSection townInstanceSection;
				if(town.getTerritoryType().equals(EarthTerritoryType.CAPITAL)) {
					townInstanceSection = townsSection.createSection("capital");
				} else {
					townInstanceSection = townsSection.createSection(town.getName());
				}
				// Town Properties
				ConfigurationSection townPropertiesSection = townInstanceSection.createSection("properties");
				for(KonPropertyFlag flag : KonPropertyFlag.values()) {
					if(town.hasPropertyValue(flag)) {
						townPropertiesSection.set(flag.toString(), town.getPropertyValue(flag));
					}
				}
            	townInstanceSection.set("world", town.getWorld().getName());
            	townInstanceSection.set("base", town.getMonument().getBaseY());
            	townInstanceSection.set("spawn", new int[] {town.getSpawnLoc().getBlockX(),
						town.getSpawnLoc().getBlockY(),
						town.getSpawnLoc().getBlockZ(),
						   									(int) town.getSpawnLoc().getPitch(),
						   									(int) town.getSpawnLoc().getYaw()});
            	townInstanceSection.set("center", new int[] {town.getCenterLoc().getBlockX(),
						town.getCenterLoc().getBlockY(),
						town.getCenterLoc().getBlockZ()});
                townInstanceSection.set("chunks", HelperUtil.formatPointsToString(town.getChunkList().keySet()));
				townInstanceSection.set("specialization", town.getSpecializationName());
				townInstanceSection.set("open", town.isOpen());
                townInstanceSection.set("plot", town.isPlotOnly());
				townInstanceSection.set("allied_building", town.isAlliedBuildingAllowed());
				townInstanceSection.set("friendly_redstone", town.isFriendlyRedstoneAllowed());
                townInstanceSection.set("redstone", town.isEnemyRedstoneAllowed());
                townInstanceSection.set("golem_offensive", town.isGolemOffensive());
                townInstanceSection.set("shield", town.isShielded());
                townInstanceSection.set("shield_time", town.getShieldEndTime());
                townInstanceSection.set("armor", town.isArmored());
                townInstanceSection.set("armor_blocks", town.getArmorBlocks());
                townInstanceSection.set("lord", "");
                ConfigurationSection townInstanceResidentSection = townInstanceSection.createSection("residents");
                for(OfflinePlayer resident : town.getPlayerResidents()) {
                	String uuid = resident.getUniqueId().toString();
                	if(town.isPlayerLord(resident)) {
                		townInstanceSection.set("lord", uuid);
                	} else if(town.isPlayerKnight(resident)) {
                		townInstanceResidentSection.set(uuid, true);
                	} else {
                		townInstanceResidentSection.set(uuid, false);
                	}
                }
                ConfigurationSection townInstanceRequestsSection = townInstanceSection.createSection("requests");
                for(OfflinePlayer requestee : town.getJoinRequests()) {
                	String uuid = requestee.getUniqueId().toString();
                	townInstanceRequestsSection.set(uuid, false);
                }
                for(OfflinePlayer invitee : town.getJoinInvites()) {
                	String uuid = invitee.getUniqueId().toString();
                	townInstanceRequestsSection.set(uuid, true);
                }
                ConfigurationSection townInstanceUpgradeSection = townInstanceSection.createSection("upgrades");
                for(KonUpgrade upgrade : KonUpgrade.values()) {
                	int level = town.getRawUpgradeLevel(upgrade);
                	if(level > 0) {
                		townInstanceUpgradeSection.set(upgrade.toString(), level);
                	}
                }
                ConfigurationSection townInstancePlotSection = townInstanceSection.createSection("plots");
                int plotIndex = 0;
                for(KonPlot plot : town.getPlots()) {
                	ConfigurationSection plotInstanceSection = townInstancePlotSection.createSection("plot_"+plotIndex);
                	plotInstanceSection.set("chunks", HelperUtil.formatPointsToString(plot.getPoints()));
                	plotInstanceSection.set("members",plot.getUserStrings());
                	plotIndex++;
                }
            }
		}
		if(kingdomMap.isEmpty()) {
			ChatUtil.printDebug("No Kingdoms to save!");
		} else {
			ChatUtil.printDebug("Saved Kingdoms");
		}
	}
	
}
