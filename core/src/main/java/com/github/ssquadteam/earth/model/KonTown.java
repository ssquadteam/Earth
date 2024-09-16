package com.github.ssquadteam.earth.model;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.api.model.EarthRelationshipType;
import com.github.ssquadteam.earth.api.model.EarthTerritoryType;
import com.github.ssquadteam.earth.api.model.EarthTown;
import com.github.ssquadteam.earth.api.model.EarthUpgrade;
import com.github.ssquadteam.earth.utility.Timer;
import com.github.ssquadteam.earth.utility.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.*;


/**
 * KonTown - Represents a Town Territory, has a center chunk monument
 * @author squadsteam
 * @prerequisites	The Town's Kingdom must have a valid Monument Template
 */
public class KonTown extends KonTerritory implements EarthTown, KonBarDisplayer, KonPropertyFlagHolder, Timeable {
	
	private final KonMonument monument;
	private final Timer monumentTimer;
	private final Timer captureTimer;
	private final Timer raidAlertTimer;
	private final Timer shieldTimer;
	private final Timer protectedWarmupTimer;
	private final HashMap<UUID, Timer> playerTravelTimers;
	private boolean isCaptureDisabled;
	private boolean isRaidAlertDisabled;
	private final BossBar monumentBarFriendlies;
	private final BossBar monumentBarWar;
	private final BossBar monumentBarPeace;
	private final BossBar monumentBarTrade;
	private final BossBar monumentBarAlliance;
	private UUID lord;
	private final HashMap<UUID,Boolean> residents;
	private final RequestKeeper joinRequestKeeper;
	private final HashMap<KonTownOption,Boolean> townOptions;
	private boolean isAttacked;
	private boolean isShielded;
	private boolean isArmored;
	private boolean isTownWatchProtected;
	private int shieldEndTimeSeconds;
	private int shieldNowTimeSeconds;
	private int armorTotalBlocks;
	private int armorCurrentBlocks;
	private double armorProgress;
	private final ArrayList<UUID> defenders;
	private final HashMap<EarthUpgrade,Integer> upgrades;
	private final HashMap<EarthUpgrade,Integer> disabledUpgrades;
	private final KonTownRabbit rabbit;
	private final HashMap<Point,KonPlot> plots;
	private final Map<KonPropertyFlag,Boolean> properties;
	private Villager.Profession specialization;
	
	public KonTown(Location loc, String name, KonKingdom kingdom, Earth earth) {
		super(loc, name, kingdom, earth);
		// Assume there is a valid Monument Template
		this.monument = new KonMonument(loc);
		this.monumentTimer = new Timer(this);
		this.captureTimer = new Timer(this);
		this.raidAlertTimer = new Timer(this);
		this.shieldTimer = new Timer(this);
		this.protectedWarmupTimer = new Timer(this);
		this.playerTravelTimers = new HashMap<>();
		this.isCaptureDisabled = false;
		this.isRaidAlertDisabled = false;
		// Display bars
		this.monumentBarFriendlies = Bukkit.getServer().createBossBar(Earth.friendColor2+name, ChatUtil.mapBarColor(Earth.friendColor1), BarStyle.SOLID);
		this.monumentBarFriendlies.setVisible(true);
		this.monumentBarFriendlies.setProgress(1.0);
		this.monumentBarWar = Bukkit.getServer().createBossBar(Earth.enemyColor2+name, ChatUtil.mapBarColor(Earth.enemyColor1), BarStyle.SOLID);
		this.monumentBarWar.setVisible(true);
		this.monumentBarWar.setProgress(1.0);
		this.monumentBarPeace = Bukkit.getServer().createBossBar(Earth.peacefulColor2+name, ChatUtil.mapBarColor(Earth.peacefulColor1), BarStyle.SOLID);
		this.monumentBarPeace.setVisible(true);
		this.monumentBarPeace.setProgress(1.0);
		this.monumentBarTrade = Bukkit.getServer().createBossBar(Earth.tradeColor2 +name, ChatUtil.mapBarColor(Earth.tradeColor1), BarStyle.SOLID);
		this.monumentBarTrade.setVisible(true);
		this.monumentBarTrade.setProgress(1.0);
		this.monumentBarAlliance = Bukkit.getServer().createBossBar(Earth.alliedColor2+name, ChatUtil.mapBarColor(Earth.alliedColor1), BarStyle.SOLID);
		this.monumentBarAlliance.setVisible(true);
		this.monumentBarAlliance.setProgress(1.0);
		// Other stuff
		this.lord = null; // init with no lord
		this.residents = new HashMap<>();
		this.joinRequestKeeper = new RequestKeeper();
		this.isAttacked = false;
		this.isShielded = false;
		this.isArmored = false;
		this.isTownWatchProtected = false;
		this.shieldEndTimeSeconds = 0;
		this.shieldNowTimeSeconds = 0;
		this.armorTotalBlocks = 0;
		this.armorCurrentBlocks = 0;
		this.armorProgress = 0.0;
		this.defenders = new ArrayList<>();
		this.upgrades = new HashMap<>();
		this.disabledUpgrades = new HashMap<>();
		this.rabbit = new KonTownRabbit(getSpawnLoc());
		this.plots = new HashMap<>();
		this.townOptions = new HashMap<>();
		initOptions();
		this.properties = new HashMap<>();
		initProperties();
		this.specialization = Villager.Profession.NONE;
	}

	private void initOptions() {
		townOptions.clear();
		for (KonTownOption option : KonTownOption.values()) {
			townOptions.put(option,option.getDefaultValue());
		}
	}

	public static List<KonPropertyFlag> getProperties() {
		List<KonPropertyFlag> result = new ArrayList<>();
		result.add(KonPropertyFlag.CAPTURE);
		result.add(KonPropertyFlag.CLAIM);
		result.add(KonPropertyFlag.UNCLAIM);
		result.add(KonPropertyFlag.UPGRADE);
		result.add(KonPropertyFlag.PLOTS);
		result.add(KonPropertyFlag.TRAVEL);
		result.add(KonPropertyFlag.PVP);
		result.add(KonPropertyFlag.PVE);
		result.add(KonPropertyFlag.BUILD);
		result.add(KonPropertyFlag.USE);
		result.add(KonPropertyFlag.CHEST);
		result.add(KonPropertyFlag.MOBS);
		result.add(KonPropertyFlag.PORTALS);
		result.add(KonPropertyFlag.JOIN);
		result.add(KonPropertyFlag.LEAVE);
		result.add(KonPropertyFlag.PROMOTE);
		result.add(KonPropertyFlag.DEMOTE);
		result.add(KonPropertyFlag.TRANSFER);
		result.add(KonPropertyFlag.SHOP);
		return result;
	}

	@Override
	public void initProperties() {
		properties.clear();
		for (KonPropertyFlag flag : getProperties()) {
			properties.put(flag, getEarth().getConfigManager().getConfig("properties").getBoolean("properties.towns."+flag.toString().toLowerCase()));
		}
	}

	public boolean isJoinable() {
		return getPropertyValue(KonPropertyFlag.JOIN);
	}

	public boolean isLeaveable() {
		return getPropertyValue(KonPropertyFlag.LEAVE);
	}

	public boolean isPromoteable() {
		return getPropertyValue(KonPropertyFlag.PROMOTE);
	}

	public boolean isDemoteable() {
		return getPropertyValue(KonPropertyFlag.DEMOTE);
	}

	public boolean isTransferable() {
		return getPropertyValue(KonPropertyFlag.TRANSFER);
	}


	@Override
	public boolean setPropertyValue(KonPropertyFlag property, boolean value) {
		boolean result = false;
		if(properties.containsKey(property)) {
			properties.put(property, value);
			result = true;
		}
		return result;
	}

	@Override
	public boolean getPropertyValue(KonPropertyFlag property) {
		boolean result = false;
		if(properties.containsKey(property)) {
			result = properties.get(property);
		}
		return result;
	}
	
	@Override
	public boolean hasPropertyValue(KonPropertyFlag property) {
		return properties.containsKey(property);
	}

	@Override
	public Map<KonPropertyFlag, Boolean> getAllProperties() {
		return new HashMap<>(properties);
	}

	public void setSpecialization(Villager.Profession newSpec) {
		specialization = newSpec;
	}

	public Villager.Profession getSpecialization() {
		return specialization;
	}

	public String getSpecializationName() {
		return CompatibilityUtil.getProfessionName(specialization);
	}

	public void applyTradeDiscounts(KonPlayer player, Inventory inv) {
		/*
		 * Ensure inventory is from a not-null merchant
		 * Get all merchant trades in the inventory and apply special price
		 */
		boolean isDiscountEnabled = getEarth().getKingdomManager().getIsDiscountEnable();
		if(!isDiscountEnabled) {
			return;
		}
		if(inv != null && inv.getType().equals(InventoryType.MERCHANT) && inv instanceof MerchantInventory) {
			MerchantInventory merch = (MerchantInventory)inv;
			if(merch.getHolder() != null && merch.getHolder() instanceof Villager) {
				// The inventory belongs to a valid merchant villager entity

				// Check that the merchant is of the correct specialized profession
				Villager host = (Villager)merch.getHolder();
				if(CompatibilityUtil.isProfessionEqual(host.getProfession(),this.getSpecialization())) {
					double discountPercent = getEarth().getKingdomManager().getDiscountPercent();
					boolean isDiscountStack = getEarth().getKingdomManager().getIsDiscountStack();
					if(discountPercent > 0) {
						// Try to use 1.18 API first, catch missing method error and then try to use version handler
						// Proceed with discounts for the valid villager's profession
						double priceAdj = (double)discountPercent/100;
						boolean doNotification = false;
						try {
							// Get and set special price with API methods
							int amount = 0;
							int discount = 0;
							Merchant tradeHost = merch.getMerchant();
							List<MerchantRecipe> tradeListDiscounted = new ArrayList<>();
							for(MerchantRecipe trade : tradeHost.getRecipes()) {
								ChatUtil.printDebug("Found trade for "+trade.getResult().getType().toString()+" with price mult "+trade.getPriceMultiplier()+
										", special "+trade.getSpecialPrice()+", uses "+trade.getUses()+", max "+trade.getMaxUses());
								List<ItemStack> ingredientList = trade.getIngredients();
								for(ItemStack ingredient : ingredientList) {
									ChatUtil.printDebug("  Has ingredient "+ingredient.getType().toString()+", amount: "+ingredient.getAmount());
								}
								if(!ingredientList.isEmpty()) {
									amount = ingredientList.get(0).getAmount();
									discount = (int)(amount*priceAdj*-1);
									if(isDiscountStack) {
										discount += trade.getSpecialPrice();
									}
									trade.setSpecialPrice(discount);
									ChatUtil.printDebug("  Applied special price "+discount);
								}
								tradeListDiscounted.add(trade);
							}
							tradeHost.setRecipes(tradeListDiscounted);
							doNotification = true;
						} catch (NoSuchMethodError compatibility) {
							ChatUtil.printDebug("Attempting to use version handler to apply trade discounts...");
							if(getEarth().isVersionHandlerEnabled()) {
								getEarth().getVersionHandler().applyTradeDiscount(priceAdj, isDiscountStack, merch);
								doNotification = true;
							} else {
								ChatUtil.printDebug("Version handler is not available.");
							}
						}
						// Notify player
						if(doNotification) {
							Earth.playDiscountSound(player.getBukkitPlayer());
							String discountStr = ""+discountPercent;
							ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_NOTICE_DISCOUNT.getMessage(getKingdom().getName(),discountStr));
						}
					}
				}
			}
		}
	}

	/**
	 * Adds a chunk to the chunkMap and checks to make sure its within the max distance from town center
	 * @param point - the chunk to add
	 * @return true if chunk is within max distance from town center, else false
	 */
	@Override
	public boolean addChunk(Point point) {
		boolean testResult = testChunk(point);
		if(testResult) {
			addPoint(point);
		}
		return testResult;
	}
	
	@Override
	public boolean testChunk(Point point) {
		// Tests a chunk to make sure it is within the max town size limit
		Point centerChunk = HelperUtil.toPoint(getCenterLoc());
		int maxChunkRange = getEarth().getCore().getInt(CorePath.TOWNS_MAX_SIZE.getPath());
		if(maxChunkRange < 0) {
			maxChunkRange = 0;
		}
		int initRange = getEarth().getCore().getInt(CorePath.TOWNS_INIT_RADIUS.getPath());
		if(initRange > 0 && maxChunkRange < initRange) {
			maxChunkRange = initRange;
		}
		if(maxChunkRange >= 1) {
			int distX = (int)Math.abs(point.getX() - centerChunk.getX());
			int distY = (int)Math.abs(point.getY() - centerChunk.getY());
			if(distX > (maxChunkRange-1) || distY > (maxChunkRange-1)) {
				ChatUtil.printDebug("Failed to test chunk in territory "+getName()+", too far");
				return false;
			}
		}
		return true;
	}

	/**
	 * initClaim - initializes the Monument, pastes the template and claims chunks.
	 * @return  0 - success
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
	@Override
	public int initClaim() {
		
		if(!getKingdom().isMonumentTemplateValid()) {
			return 7;
		}
		
		// Verify monument template and chunk gradient and initialize travelPoint and baseY coordinate
		int flatness = getEarth().getCore().getInt(CorePath.TOWNS_SETTLE_CHECK_FLATNESS.getPath(),3);
		int monumentStatus = monument.initialize(getKingdom().getMonumentTemplate(), getCenterLoc(), flatness);
		if(monumentStatus != 0) {
			ChatUtil.printDebug("Town init failed: monument did not initialize correctly");
			return 10 + monumentStatus;
		}
		
		// Verify monument paste Y level is within config min/max range
		int config_min_y = getEarth().getCore().getInt(CorePath.TOWNS_MIN_SETTLE_HEIGHT.getPath());
		int config_max_y = getEarth().getCore().getInt(CorePath.TOWNS_MAX_SETTLE_HEIGHT.getPath());
		if(config_min_y < 0) {
			config_min_y = 0;
		}
		if(config_min_y != 0 && monument.getBaseY() < config_min_y) {
			ChatUtil.printDebug("Town init failed: "+monument.getBaseY()+" less than min limit "+config_min_y);
			return 2;
		}
		if(config_max_y > getWorld().getMaxHeight()) {
			config_max_y = getWorld().getMaxHeight();
		}
		if(config_max_y != 0 && monument.getBaseY() > config_max_y) {
			ChatUtil.printDebug("Town init failed: "+monument.getBaseY()+" greater than max limit "+config_max_y);
			return 2;
		}
		int world_min_y = getWorld().getMinHeight();
		int world_max_y = getWorld().getMaxHeight();
		if(monument.getBaseY() <= world_min_y) {
			ChatUtil.printDebug("Town init failed: "+monument.getBaseY()+" less than world min limit "+world_min_y);
			return 2;
		}
		if(monument.getBaseY() >= world_max_y) {
			ChatUtil.printDebug("Town init failed: "+monument.getBaseY()+" greater than world max limit "+world_max_y);
			return 2;
		}
		
		// Verify monument template will not pass the height limit
		if(monument.getBaseY() + getKingdom().getMonumentTemplate().getHeight() + 1 >= getWorld().getMaxHeight()) {
			ChatUtil.printDebug("Town init failed: too high");
			return 2;
		}
		
		// Iterate over every block in the chunk to check for bad conditions
		int countWaterBlocks = 0;
		int countAirBlocks = 0;
		int countContainers = 0;
		int baseDepth = getEarth().getCore().getInt(CorePath.TOWNS_SETTLE_CHECKS_DEPTH.getPath(),0);
		int yMin = 0;
		int yMax = monument.getBaseY();
		if(baseDepth > 0) {
			yMin = monument.getBaseY() - baseDepth;
		}
		if(yMin < getWorld().getMinHeight()) {
			yMin = getWorld().getMinHeight();
		}
		if(yMax > getWorld().getMaxHeight()-1) {
			yMax = getWorld().getMaxHeight()-1;
		}
		for (int x = 0; x <= 15; x++) {
            for (int y = getWorld().getMinHeight(); y <= getWorld().getMaxHeight()-1; y++) {
                for (int z = 0; z <= 15; z++) {
                    Block currentBlock = getWorld().getChunkAt(getCenterLoc()).getBlock(x, y, z);
                    // Checks for blocks within min max monument Y levels
                    if(y >= yMin && y <= yMax) {
						if(currentBlock.getType().equals(Material.WATER)) {
							countWaterBlocks++;
						} else if(currentBlock.getType().equals(Material.AIR)) {
                    		countAirBlocks++;
                        } else if(currentBlock.getState() instanceof Container) {
                        	countContainers++;
                        }
                    }
                }
            }
        }
		
		// Verify there is not too much air below the base Y location, no more than 10 layers of 16x16 blocks.
		if(baseDepth > 0 && countAirBlocks > 2560) {
			ChatUtil.printDebug("Town init failed: too much air below monument");
			return 4;
		}
		
		// Verify there is not too much water in this chunk, no more than 10 layers of 16x16 blocks.
		if(countWaterBlocks > 2560) {
			ChatUtil.printDebug("Town init failed: too much water in the chunk");
			return 5;
		}
		
		// Verify there are not too many containers below the monument
		if(baseDepth > 0 && countContainers > 0) {
			ChatUtil.printDebug("Town init failed: too many containers in the chunk");
			return 6;
		}
        
		// Add chunks around the monument chunk
		int radius = getEarth().getCore().getInt(CorePath.TOWNS_INIT_RADIUS.getPath());
		if(radius < 1) {
			radius = 1;
		}
		if(!addChunks(HelperUtil.getAreaPoints(getCenterLoc(), radius))) {
			ChatUtil.printDebug("Town init failed: problem adding some chunks");
			return 3;
		}
		
		// Copy Monument template blocks into town center chunk
		if(!pasteMonumentFromTemplate(getKingdom().getMonumentTemplate())) {
			ChatUtil.printDebug("Town init failed: monument did not paste correctly");
			return 1;
		}

		// Validate Monument
		monument.setIsValid(true);
		
		// Set spawn point to monument travel point
		setSpawn(monument.getTravelPoint());
		
		return 0;
	}

	/**
	 * Pastes the current monument template over the town monument.
	 * Also clears air above the monument up to the tallest template height.
	 * @param template The monument template to paste.
	 * @return True when the paste was sucessfull, else false.
	 */
	public boolean pasteMonumentFromTemplate(KonMonumentTemplate template) {
		if(template == null || !template.isValid()) {
			return false;
		}
		//Date start = new Date();
		monument.setIsItemDropsDisabled(true);
		monument.setIsDamageDisabled(true);
		
		int topBlockX = Math.max(template.getCornerOne().getBlockX(), template.getCornerTwo().getBlockX());
        int topBlockY = Math.max(template.getCornerOne().getBlockY(), template.getCornerTwo().getBlockY());
        int topBlockZ = Math.max(template.getCornerOne().getBlockZ(), template.getCornerTwo().getBlockZ());
        int bottomBlockX = Math.min(template.getCornerOne().getBlockX(), template.getCornerTwo().getBlockX());
        int bottomBlockY = Math.min(template.getCornerOne().getBlockY(), template.getCornerTwo().getBlockY());
        int bottomBlockZ = Math.min(template.getCornerOne().getBlockZ(), template.getCornerTwo().getBlockZ());
        
        // Determine minimum Y level of paste chunk below monument Y base
        int monument_y = monument.getBaseY();
        int fill_y;
        int min_fill_y = getCenterLoc().getWorld().getMinHeight();

        Chunk fillChunk = getCenterLoc().getChunk();
        ChunkSnapshot fillChunkSnap = fillChunk.getChunkSnapshot(true,false,false);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
            	fill_y = fillChunkSnap.getHighestBlockYAt(x, z);
            	while((fillChunk.getBlock(x, fill_y, z).isPassable() || !fillChunkSnap.getBlockType(x, fill_y, z).isOccluding()) && fill_y > fillChunk.getWorld().getMinHeight()) {
            		fill_y--;
				}
            	if((x == 0 && z == 0) || fill_y < min_fill_y) {
            		min_fill_y = fill_y;
            	}
            }
        }
        // Lower minimum fill by 1, so that there is always a layer of stone under the monument base.
        min_fill_y--;
        if(min_fill_y < getCenterLoc().getWorld().getMinHeight()) {
        	min_fill_y = getCenterLoc().getWorld().getMinHeight();
        }
        //Date step4 = new Date();
        ChatUtil.printDebug("Pasting monument ("+fillChunk.getX()+","+fillChunk.getZ()+") at base "+monument_y+" found minimum Y level: "+min_fill_y);
        // Fill air between world and monument base
        if(min_fill_y < monument_y) {
	        for (int x = 0; x < 16; x++) {
	            for (int z = 0; z < 16; z++) {
                	for (int y = min_fill_y; y <= monument_y; y++) {
                		fillChunk.getBlock(x, y, z).setType(Material.STONE);
                	}
	            }
	        }
        }
		// Clear any air above the monument, for when there was a previous taller template.
		//TODO This is a brute-force way to do it, there's probably a smarter way to avoid always pasting air.
		int maxHeight = getEarth().getSanctuaryManager().getTallestTemplateHeight();
		int monumentHeight = monument.getHeight();
		if(monumentHeight < maxHeight) {
			// The monument being pasted is shorter than the tallest possible template.
			// Paste air above it just in case the tallest template was used previously.
			int startY = monument.getBaseY()+monumentHeight+1;
			int endY = monument.getBaseY()+maxHeight;
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					for (int y = startY; y <= endY; y++) {
						Block currentBlock = getWorld().getChunkAt(getCenterLoc()).getBlock(x, y, z);
						currentBlock.setType(Material.AIR);
					}
				}
			}
		}
        World templateWorld = template.getCornerOne().getWorld();
        BlockPaster monumentPaster = new BlockPaster(fillChunk,templateWorld,bottomBlockY,monument.getBaseY(),bottomBlockY,topBlockX,topBlockZ,bottomBlockX,bottomBlockZ);
        for (int y = bottomBlockY; y <= topBlockY; y++) {
        	monumentPaster.setY(y);
        	monumentPaster.startPaste();
        }
        monument.setIsItemDropsDisabled(false);
        monument.setIsDamageDisabled(false);
		return true;
	}

	public boolean removeMonumentBlocks() {
		if(!monument.isValid()) {
			ChatUtil.printDebug("Town remove monument failed: Monument is not valid");
			return false;
		}
		int minY = monument.getBaseY();
		int maxY = monument.getBaseY()+monument.getHeight();
		if(minY == 0 || maxY == 0) {
			ChatUtil.printDebug("Town remove monument failed: Base or Height value is 0");
			return false;
		}
		monument.setIsItemDropsDisabled(true);
		for (int x = 0; x <= 15; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = 0; z <= 15; z++) {
                    Block currentBlock = getWorld().getChunkAt(getCenterLoc()).getBlock(x, y, z);
                    if(y == minY) {
                    	currentBlock.setType(Material.DIRT);
                    } else {
                    	currentBlock.setType(Material.AIR);
                    }
                }
            }
        }
		monument.setIsItemDropsDisabled(false);
		return true;
	}
	
	/**
	 * Counts all water blocks in the entire chunk
	 * @return amount of water blocks
	 */
	public int countWaterInChunk() {
		int count = 0;
		for (int x = 0; x <= 15; x++) {
            for (int y = getWorld().getMinHeight(); y <= getWorld().getMaxHeight()-1; y++) {
                for (int z = 0; z <= 15; z++) {
                    Block currentBlock = getWorld().getChunkAt(getCenterLoc()).getBlock(x, y, z);
                    if(currentBlock.getType().equals(Material.WATER)) {
                    	count++;
                    }
                }
            }
        }
		return count;
	}
	
	/**
	 * Count the number of air blocks below the monument base, for height specified by limit
	 * @param limit - The height below the monument base to check for air
	 * @return amount of air blocks
	 */
	public int countAirBelowMonument(int limit) {
		int count = 0;
		int yMin = 0;
		int yMax = monument.getBaseY();
		if(limit > 0) {
			yMin = monument.getBaseY() - limit;
		}
		if(yMin < getWorld().getMinHeight()) {
			yMin = getWorld().getMinHeight();
		}
		if(yMax > getWorld().getMaxHeight()-1) {
			yMax = getWorld().getMaxHeight()-1;
		}
		for (int x = 0; x <= 15; x++) {
            for (int y = yMin; y <= yMax; y++) {
                for (int z = 0; z <= 15; z++) {
                    Block currentBlock = getWorld().getChunkAt(getCenterLoc()).getBlock(x, y, z);
                    if(currentBlock.getType().equals(Material.AIR)) {
                    	count++;
                    }
                }
            }
        }
		return count;
	}

	public boolean isLocInsideCenterChunk(Location loc) {
		Point centerPoint = HelperUtil.toPoint(getCenterLoc());
		Point testPoint = HelperUtil.toPoint(loc);
		return centerPoint.x == testPoint.x && centerPoint.y == testPoint.y;
	}
	
	public boolean isChunkCenter(Chunk chunk) {
		Point centerPoint = HelperUtil.toPoint(getCenterLoc());
		Point testPoint = HelperUtil.toPoint(chunk);
		return centerPoint.x == testPoint.x && centerPoint.y == testPoint.y;
	}
	
	public boolean isLocInsideMonumentProtectionArea(Location loc) {
		Point centerPoint = HelperUtil.toPoint(getCenterLoc());
		Point testPoint = HelperUtil.toPoint(loc);
		int baseDepth = getEarth().getCore().getInt(CorePath.TOWNS_SETTLE_CHECKS_DEPTH.getPath(),0);
		if(baseDepth < 0) {
			baseDepth = 0;
		}
		int bottomLevel = monument.getBaseY() - baseDepth;
		if(bottomLevel < getWorld().getMinHeight()) {
			baseDepth = getWorld().getMinHeight();
		}
		return centerPoint.x == testPoint.x && centerPoint.y == testPoint.y && loc.getBlockY() >= bottomLevel;
	}
	
	/**
	 * Method used for loading a monument on server start
	 * @param base base
	 * @param template the monument template
	 */
	public int loadMonument(int base, KonMonumentTemplate template) {
		int status = 0;
		monument.setBaseY(base);
		if(template != null && template.isValid()) {
			monument.updateFromTemplate(template);
			monument.setIsValid(true);
		} else {
			status = 1;
		}
		return status;
	}

	/**
	 * Updates the monument when the kingdom changes templates
	 */
	public void updateMonumentFromTemplate() {
		// Check that the template is valid
		if(getKingdom().isMonumentTemplateValid()) {
			// Update this town's monument travel point and height based on template
			monument.updateFromTemplate(getKingdom().getMonumentTemplate());
			// Force the monument to be valid
			monument.setIsValid(true);
			// Update the town spawn point
			setSpawn(monument.getTravelPoint());
		} else {
			ChatUtil.printDebug("Failed to update monument from invalid template for town "+getName());
		}
	}

	/**
	 * Reloads a monument from template and pastes it into the town.
	 * Known uses:
	 * - World chunk load
	 * - Post-attack regenerate
	 * - Kingdom template change
	 */
	public boolean reloadMonument() {
		if(!monument.isValid()) {
			ChatUtil.printDebug("Failed to reload invalid monument for town "+getName());
			return false;
		}
		if(!getKingdom().isMonumentTemplateValid()) {
			ChatUtil.printDebug("Failed to reload monument from invalid template for town "+getName());
			return false;
		}
		if(this.isAttacked) {
			ChatUtil.printDebug("Failed to reload monument for attacked town "+getName());
			return false;
		}
		// Update this town's monument travel point and height based on template
		monument.updateFromTemplate(getKingdom().getMonumentTemplate());
		// Paste the template into the town
		pasteMonumentFromTemplate(getKingdom().getMonumentTemplate());
		// Update the town spawn point
		setSpawn(monument.getTravelPoint());
		return true;
	}
	
	/**
	 * Refreshes a monument from template, meant to be used for town capture
	 */
	public void refreshMonument() {
		if(monument.isValid()) {
			removeMonumentBlocks();
			monument.clearCriticalHits();
			monument.updateFromTemplate(getKingdom().getMonumentTemplate());
			setSpawn(monument.getTravelPoint());
			Earth.playTownSettleSound(this.getCenterLoc());
			// Delay paste by 1 tick
			Bukkit.getScheduler().scheduleSyncDelayedTask(getEarth().getPlugin(), new Runnable() {
	            @Override
	            public void run() {
	            	pasteMonumentFromTemplate(getKingdom().getMonumentTemplate());
	            }
	        },1);
		} else {
			ChatUtil.printDebug("Failed to refresh monument from template for town "+getName());
		}
	}
	
	public KonMonument getMonument() {
		return monument;
	}
	
	public boolean isCaptureDisabled() {
		return isCaptureDisabled;
	}
	
	public void setIsCaptureDisabled(boolean val) {
		isCaptureDisabled = val;
	}
	
	public boolean isRaidAlertDisabled() {
		return isRaidAlertDisabled;
	}
	
	public void setIsRaidAlertDisabled(boolean val) {
		isRaidAlertDisabled = val;
	}
	
	public boolean addPlayerTravelCooldown(UUID uuid) {
		if(playerTravelTimers.containsKey(uuid)) {
			return false;
		}
		int travelCooldownSeconds = getEarth().getCore().getInt(CorePath.TOWNS_TRAVEL_COOLDOWN.getPath());
		Timer travelCooldownTimer = new Timer(this);
		travelCooldownTimer.stopTimer();
		travelCooldownTimer.setTime(travelCooldownSeconds);
		travelCooldownTimer.startTimer();
		String name = Bukkit.getOfflinePlayer(uuid).getName();
		playerTravelTimers.put(uuid, travelCooldownTimer);
		ChatUtil.printDebug("Starting player travel timer for "+travelCooldownSeconds+" seconds with taskID "+travelCooldownTimer.getTaskID()+" for player "+name);
		return true;
	}
	
	public boolean isPlayerTravelDisabled(UUID uuid) {
		return playerTravelTimers.containsKey(uuid);
	}
	
	public String getPlayerTravelCooldownString(UUID uuid) {
		String cooldownTime = "";
		if(playerTravelTimers.containsKey(uuid)) {
			cooldownTime = String.format("%02d:%02d", playerTravelTimers.get(uuid).getMinutes(), playerTravelTimers.get(uuid).getSeconds());
		}
		return cooldownTime;
	}
	
	public String getCaptureCooldownString() {
		return String.format("%02d:%02d", captureTimer.getMinutes(), captureTimer.getSeconds());
	}

	/**
	 * Timer is used to regenerate monument on timer end.
	 * Town Timer starts when a monument is damaged in EarthListener.
	 * Town Timer restarts whenever damage is taken.
	 */
	@Override
	public void onEndTimer(int taskID) {
		if(taskID == 0) {
			ChatUtil.printDebug("Town Timer ended with null taskID!");
		} else if(taskID == monumentTimer.getTaskID()) {
			ChatUtil.printDebug("Monument Timer ended with taskID: "+taskID);
			// When a monument timer ends
			monument.clearCriticalHits();
			setAttacked(false,null);
			reloadMonument();
			updateBarTitle();
			getWorld().playSound(getCenterLoc(), Sound.BLOCK_ANVIL_USE, (float)1, (float)0.8);
			for(KonPlayer player : getEarth().getPlayerManager().getPlayersInKingdom(getKingdom().getName())) {
				ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_SAFE.getMessage(getName()), ChatColor.GREEN);
			}
		} else if(taskID == captureTimer.getTaskID()) {
			ChatUtil.printDebug("Capture Timer ended with taskID: "+taskID);
			// When a capture cool-down timer ends
			isCaptureDisabled = false;
		} else if(taskID == raidAlertTimer.getTaskID()) {
			ChatUtil.printDebug("Raid Alert Timer ended with taskID: "+taskID);
			// When a raid alert cool-down timer ends
			isRaidAlertDisabled = false;
		} else if(taskID == shieldTimer.getTaskID()) {
			// When shield loop timer ends (1 second loop)
			Date now = new Date();
			shieldNowTimeSeconds = (int)(now.getTime()/1000);
			if(shieldEndTimeSeconds < shieldNowTimeSeconds) {
				deactivateShield();
			} else {
				//refreshShieldBarTitle();
				updateBarTitle();
			}
		} else if(taskID == protectedWarmupTimer.getTaskID()) {
			ChatUtil.printDebug("Town Watch protection warmup Timer ended with taskID: "+taskID);
			isTownWatchProtected = true;
		} else {
			// Check for timer in player travel cool-down map
			for(UUID uuid : playerTravelTimers.keySet()) {
				if(taskID == playerTravelTimers.get(uuid).getTaskID()) {
					String name = Bukkit.getOfflinePlayer(uuid).getName();
					ChatUtil.printDebug("Travel Cooldown Timer ended for player: "+name);
					playerTravelTimers.remove(uuid);
					return;
				}
			}
			ChatUtil.printDebug("Town Timer ended with unknown taskID: "+taskID);
		}
	}
	
	public Timer getMonumentTimer() {
		return monumentTimer;
	}
	
	public Timer getCaptureTimer() {
		return captureTimer;
	}
	
	public Timer getRaidAlertTimer() {
		return raidAlertTimer;
	}

	public void stopTimers() {
		monumentTimer.stopTimer();
		captureTimer.stopTimer();
		raidAlertTimer.stopTimer();
		shieldTimer.stopTimer();
		playerTravelTimers.clear();
	}
	
	public void updateBarPlayers() {
		monumentBarFriendlies.removeAll();
		monumentBarWar.removeAll();
		monumentBarAlliance.removeAll();
		monumentBarTrade.removeAll();
		monumentBarPeace.removeAll();
		for(KonPlayer player : getEarth().getPlayerManager().getPlayersOnline()) {
			Player bukkitPlayer = player.getBukkitPlayer();
			if(isLocInside(bukkitPlayer.getLocation())) {
				EarthRelationshipType role = getEarth().getKingdomManager().getRelationRole(player.getKingdom(),getKingdom());
				switch(role) {
			    	case ENEMY:
						monumentBarWar.addPlayer(bukkitPlayer);
			    		break;
			    	case FRIENDLY:
			    		monumentBarFriendlies.addPlayer(bukkitPlayer);
			    		break;
			    	case ALLY:
						monumentBarAlliance.addPlayer(bukkitPlayer);
			    		break;
			    	case TRADE:
						monumentBarTrade.addPlayer(bukkitPlayer);
			    		break;
			    	case PEACEFUL:
						monumentBarPeace.addPlayer(bukkitPlayer);
			    		break;
		    		default:
		    			break;
		    	}
			}
		}
	}
	
	public void addBarPlayer(KonPlayer player) {
		EarthRelationshipType role = getEarth().getKingdomManager().getRelationRole(player.getKingdom(),getKingdom());
		switch(role) {
	    	case ENEMY:
	    		monumentBarWar.addPlayer(player.getBukkitPlayer());
	    		break;
	    	case FRIENDLY:
	    		monumentBarFriendlies.addPlayer(player.getBukkitPlayer());
	    		break;
	    	case ALLY:
	    		monumentBarAlliance.addPlayer(player.getBukkitPlayer());
	    		break;
	    	case TRADE:
	    		monumentBarTrade.addPlayer(player.getBukkitPlayer());
	    		break;
	    	case PEACEFUL:
	    		monumentBarPeace.addPlayer(player.getBukkitPlayer());
	    		break;
			default:
				break;
		}
	}
	
	public void removeBarPlayer(KonPlayer player) {
		monumentBarFriendlies.removePlayer(player.getBukkitPlayer());
		monumentBarWar.removePlayer(player.getBukkitPlayer());
		monumentBarAlliance.removePlayer(player.getBukkitPlayer());
		monumentBarTrade.removePlayer(player.getBukkitPlayer());
		monumentBarPeace.removePlayer(player.getBukkitPlayer());
	}
	
	public void removeAllBarPlayers() {
		monumentBarFriendlies.removeAll();
		monumentBarWar.removeAll();
		monumentBarAlliance.removeAll();
		monumentBarTrade.removeAll();
		monumentBarPeace.removeAll();
	}
	
	public void setBarProgress(double prog) {
		monumentBarFriendlies.setProgress(prog);
		monumentBarWar.setProgress(prog);
		monumentBarAlliance.setProgress(prog);
		monumentBarTrade.setProgress(prog);
		monumentBarPeace.setProgress(prog);
	}
	
	public void setAttacked(boolean val, KonPlayer attacker) {
		if(val) {
			if(!isAttacked) {
				// Town is now under attack, update golem targets
				this.isAttacked = true;
				if(attacker != null) {
					updateGolemTargets(attacker,false);
					//TODO: Add rabbit spawn as part of a town upgrade
				}
			}
			// Start Monument regenerate timer for target town when no armor nor shield
			int monumentRegenTimeSeconds = getEarth().getCore().getInt(CorePath.MONUMENTS_DAMAGE_REGEN.getPath());
			monumentTimer.stopTimer();
			monumentTimer.setTime(monumentRegenTimeSeconds);
			monumentTimer.startTimer();

		} else {
			this.isAttacked = false;
			defenders.clear();
			rabbit.remove();
		}
	}
	
	public boolean isAttacked() {
		return isAttacked;
	}

	// Return true when protection warmup starts, else false
	public boolean updateProtection(boolean force) {
		int upgradeLevel = getEarth().getUpgradeManager().getTownUpgradeLevel(this, KonUpgrade.WATCH);
		boolean isTownWatchValid = (upgradeLevel > 0 && getNumResidentsOnline() < upgradeLevel);
		if (!isTownWatchValid) {
			// Town Watch conditions are no longer met
			// Disable protection
			isTownWatchProtected = false;
			protectedWarmupTimer.stopTimer();
		} else if (!isTownWatchProtected) {
			// Town Watch is disabled, and conditions are met
			// Enable protection warmup
			// Use same warmup time as kingdom offline protection
			int offlineProtectedWarmupSeconds = getEarth().getCore().getInt(CorePath.KINGDOMS_NO_ENEMY_EDIT_OFFLINE_WARMUP.getPath(),0);
			if (force || offlineProtectedWarmupSeconds <= 0) {
				// Warmup is disabled, immediately enable protection
				ChatUtil.printDebug("Town Watch protection enabled without warmup in "+getName());
				isTownWatchProtected = true;
				protectedWarmupTimer.stopTimer();
			} else if (protectedWarmupTimer.getTime() == -1) {
				// Timer is not running, and config time is non-zero
				// start warmup timer
				ChatUtil.printDebug("Starting town watch protection warmup timer for " + offlineProtectedWarmupSeconds + " seconds: " + this.getName());
				protectedWarmupTimer.stopTimer();
				protectedWarmupTimer.setTime(offlineProtectedWarmupSeconds);
				protectedWarmupTimer.startTimer();
			}
			return true;
		}
		// Default
		return false;
	}
	
	public void targetRabbitToPlayer(Player player) {
		rabbit.targetTo(player);
	}
	
	public void removeRabbit() {
		rabbit.remove();
	}
	
	public void updateBarTitle() {
		/*
		 * When town block is broken without shield or armor, attacked=true & show critical hit progress.
		 * If shields or armor are added while attacked, preserve attacked state and critical progress in background,
		 * but display armor/shield progress. Refresh regen timer while armor/shield is active when blocks broken.
		 * When shields/armor are depleted, and regen timer has not expired, restore attacked title & critical bar progress.
		 * Otherwise, when regen timer has expired, restore to normal non-attacked title & 1.0 progress.
		 */
		String separator = " - ";
		int remainingSeconds = 0;
		String armor = MessagePath.LABEL_ARMOR.getMessage();
		String shield = MessagePath.LABEL_SHIELD.getMessage();
		String critical = MessagePath.LABEL_CRITICAL_HITS.getMessage();
		
		// Set title conditions
		if(isShielded && isArmored) {
			remainingSeconds = getRemainingShieldTimeSeconds();
			monumentBarFriendlies.setTitle(Earth.friendColor2+getName()+separator+armorCurrentBlocks+" "+armor+" | "+shield+" "+HelperUtil.getTimeFormat(remainingSeconds,Earth.friendColor2));
			monumentBarWar.setTitle(Earth.enemyColor2+getName()+separator+armorCurrentBlocks+" "+armor+" | "+shield+" "+HelperUtil.getTimeFormat(remainingSeconds,Earth.enemyColor2));
			monumentBarAlliance.setTitle(Earth.alliedColor2+getName()+separator+armorCurrentBlocks+" "+armor+" | "+shield+" "+HelperUtil.getTimeFormat(remainingSeconds,Earth.alliedColor2));
			monumentBarTrade.setTitle(Earth.tradeColor2 +getName()+separator+armorCurrentBlocks+" "+armor+" | "+shield+" "+HelperUtil.getTimeFormat(remainingSeconds,Earth.tradeColor2));
			monumentBarPeace.setTitle(Earth.peacefulColor2+getName()+separator+armorCurrentBlocks+" "+armor+" | "+shield+" "+HelperUtil.getTimeFormat(remainingSeconds,Earth.peacefulColor2));
		} else if(isShielded) {
			remainingSeconds = getRemainingShieldTimeSeconds();
			monumentBarFriendlies.setTitle(Earth.friendColor2+getName()+separator+shield+" "+HelperUtil.getTimeFormat(remainingSeconds,Earth.friendColor2));
			monumentBarWar.setTitle(Earth.enemyColor2+getName()+separator+shield+" "+HelperUtil.getTimeFormat(remainingSeconds,Earth.enemyColor2));
			monumentBarAlliance.setTitle(Earth.alliedColor2+getName()+separator+shield+" "+HelperUtil.getTimeFormat(remainingSeconds,Earth.alliedColor2));
			monumentBarTrade.setTitle(Earth.tradeColor2 +getName()+separator+shield+" "+HelperUtil.getTimeFormat(remainingSeconds,Earth.tradeColor2));
			monumentBarPeace.setTitle(Earth.peacefulColor2+getName()+separator+shield+" "+HelperUtil.getTimeFormat(remainingSeconds,Earth.peacefulColor2));
		} else if(isArmored) {
			monumentBarFriendlies.setTitle(Earth.friendColor2+getName()+separator+armorCurrentBlocks+" "+armor);
			monumentBarWar.setTitle(Earth.enemyColor2+getName()+separator+armorCurrentBlocks+" "+armor);
			monumentBarAlliance.setTitle(Earth.alliedColor2+getName()+separator+armorCurrentBlocks+" "+armor);
			monumentBarTrade.setTitle(Earth.tradeColor2 +getName()+separator+armorCurrentBlocks+" "+armor);
			monumentBarPeace.setTitle(Earth.peacefulColor2+getName()+separator+armorCurrentBlocks+" "+armor);
		} else if(isAttacked) {
			monumentBarFriendlies.setTitle(Earth.friendColor2+getName()+separator+critical);
			monumentBarWar.setTitle(Earth.enemyColor2+getName()+separator+critical);
			monumentBarAlliance.setTitle(Earth.alliedColor2+getName()+separator+critical);
			monumentBarTrade.setTitle(Earth.tradeColor2 +getName()+separator+critical);
			monumentBarPeace.setTitle(Earth.peacefulColor2+getName()+separator+critical);
		} else {
			monumentBarFriendlies.setTitle(Earth.friendColor2+getName());
			monumentBarWar.setTitle(Earth.enemyColor2+getName());
			monumentBarAlliance.setTitle(Earth.alliedColor2+getName());
			monumentBarTrade.setTitle(Earth.tradeColor2 +getName());
			monumentBarPeace.setTitle(Earth.peacefulColor2+getName());
		}
		
		// Set progress conditions
		if(isShielded || isArmored) {
			setAllBarStyle(BarStyle.SEGMENTED_10);
			setBarProgress(armorProgress);
		} else if(isAttacked) {
			setAllBarStyle(BarStyle.SOLID);
			// Set bar with critical hits
			int maxCriticalhits = getEarth().getKingdomManager().getMaxCriticalHits();
			double progress = (double)(maxCriticalhits - getMonument().getCriticalHits()) / (double)maxCriticalhits;
			setBarProgress(progress);
		} else {
			setAllBarStyle(BarStyle.SOLID);
			setBarProgress(1.0);
		}
	}
	
	private void setAllBarStyle(BarStyle style) {
		monumentBarFriendlies.setStyle(style);
		monumentBarWar.setStyle(style);
		monumentBarAlliance.setStyle(style);
		monumentBarTrade.setStyle(style);
		monumentBarPeace.setStyle(style);
	}
	
	public boolean addDefender(Player bukkitPlayer) {
		boolean result = false;
		if(!defenders.contains(bukkitPlayer.getUniqueId())) {
			defenders.add(bukkitPlayer.getUniqueId());
			result = true;
		}
		return result;
	}
	
	public void applyGlow(Player bukkitPlayer) {
		boolean isGlowEnabled = getEarth().getCore().getBoolean(CorePath.TOWNS_ENEMY_GLOW.getPath(), true);
		if(isGlowEnabled) {
			bukkitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20*5, 1));
			//ChatUtil.printDebug("Applied glowing to "+bukkitPlayer.getName()+": "+bukkitPlayer.isGlowing());
		}
	}
	
	public void sendRaidAlert() {
		// Attempt to start a raid alert
		if(!isRaidAlertDisabled()) {
			// Alert all players of this town's kingdom
			for(KonPlayer player : getEarth().getPlayerManager().getPlayersInKingdom(getKingdom().getName())) {
				if(!player.isAdminBypassActive() && !player.getBukkitPlayer().getGameMode().equals(GameMode.SPECTATOR)) {
					ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_RAID.getMessage(getName(),getTravelName()),ChatColor.DARK_RED);
					ChatUtil.sendKonPriorityTitle(player, ChatColor.DARK_RED+MessagePath.PROTECTION_NOTICE_RAID_ALERT.getMessage(), ChatColor.DARK_RED+""+getName(), 60, 1, 10);
				}
			}
			// Start Raid Alert disable timer for target town
			int raidAlertTimeSeconds = getEarth().getCore().getInt(CorePath.TOWNS_RAID_ALERT_COOLDOWN.getPath());
			ChatUtil.printDebug("Starting raid alert timer for "+raidAlertTimeSeconds+" seconds in town "+getName());
			Timer raidAlertTimer = getRaidAlertTimer();
			setIsRaidAlertDisabled(true);
			raidAlertTimer.stopTimer();
			raidAlertTimer.setTime(raidAlertTimeSeconds);
			raidAlertTimer.startTimer();
		}
	}
	
	public void updateGolemTargets(KonPlayer triggerPlayer, boolean useTriggerPlayerAsTarget) {
    	// Ignore if town option Golem Offense is disabled and not under attack
    	if(!isAttacked() && !isGolemOffensive()) {
    		return;
    	}
		// Ignore if kingdom property flag is disabled
		if(!getKingdom().getPropertyValue(KonPropertyFlag.GOLEMS)) {
			return;
		}
    	// Command all nearby Iron Golems to target closest enemy player, if enemy exists nearby, else don't change target
    	// Find iron golems within the town max radius
    	// Do not update targets if triggering player is not an enemy of this town
    	boolean isTriggerPlayerEnemy = getEarth().getKingdomManager().isPlayerEnemy(triggerPlayer, getKingdom());
    	boolean isGolemAttackEnemies = getEarth().getCore().getBoolean(CorePath.KINGDOMS_GOLEM_ATTACK_ENEMIES.getPath());
		if(isGolemAttackEnemies && !triggerPlayer.isAdminBypassActive() && !triggerPlayer.getKingdom().equals(getKingdom()) && isTriggerPlayerEnemy) {
			Location centerLoc = getCenterLoc();
			int golumSearchRange = getEarth().getCore().getInt(CorePath.TOWNS_MAX_SIZE.getPath(),1); // chunks
			int radius = 16*16;
			if(golumSearchRange > 1) {
				radius = golumSearchRange*16;
			}
			for(Entity e : centerLoc.getWorld().getNearbyEntities(centerLoc,radius,256,radius,(e) -> e.getType() == EntityType.IRON_GOLEM)) {
				IronGolem golem = (IronGolem)e;
				// Check for golem inside given territory or in wild
				if(isLocInside(golem.getLocation()) || !getEarth().getTerritoryManager().isChunkClaimed(golem.getLocation())) {
					
					// Check for closest enemy player
					boolean isNearbyPlayer = false;
					double minDistance = 99;
					KonPlayer nearestPlayer = null;
					for(Entity p : golem.getNearbyEntities(32,32,32)) {
						if(p instanceof Player) {
							KonPlayer nearbyPlayer = getEarth().getPlayerManager().getPlayer((Player)p);
							if(nearbyPlayer != null && !nearbyPlayer.isAdminBypassActive() && !nearbyPlayer.getKingdom().equals(getKingdom()) && isLocInside(p.getLocation()) &&
									(useTriggerPlayerAsTarget || !nearbyPlayer.equals(triggerPlayer))) {
								// Found nearby player that might be a valid target
								// Check for closest distance, and that the player is an enemy
								boolean isEnemy = getEarth().getKingdomManager().isPlayerEnemy(nearbyPlayer, getKingdom());
								double distance = golem.getLocation().distance(p.getLocation());
								if(distance < minDistance && isEnemy) {
									minDistance = distance;
									isNearbyPlayer = true;
									nearestPlayer = nearbyPlayer;
								}
							}
						}
					}
					
					// Attempt to remove current target
					LivingEntity currentTarget = golem.getTarget();
					//ChatUtil.printDebug("Golem: Evaluating new targets in territory "+territory.getName());
					if(currentTarget != null && currentTarget instanceof Player) {
						KonPlayer previousTargetPlayer = getEarth().getPlayerManager().getPlayer((Player)currentTarget);
						if(previousTargetPlayer != null) {
							previousTargetPlayer.removeMobAttacker(golem);
							//ChatUtil.printDebug("Golem: Removed mob attacker from player "+previousTargetPlayer.getBukkitPlayer().getName());
						}
					} else {
						//ChatUtil.printDebug("Golem: Bad current target");
					}
					
					// Attempt to apply new target, either closest player or default trigger player
					if(isNearbyPlayer) {
						//ChatUtil.printDebug("Golem: Found nearby player "+nearestPlayer.getBukkitPlayer().getName());
						golem.setTarget(nearestPlayer.getBukkitPlayer());
						nearestPlayer.addMobAttacker(golem);
					} else if(useTriggerPlayerAsTarget){
						//ChatUtil.printDebug("Golem: Targeting default player "+triggerPlayer.getBukkitPlayer().getName());
						golem.setTarget(triggerPlayer.getBukkitPlayer());
						triggerPlayer.addMobAttacker(golem);
					}
					
				} else {
					ChatUtil.printDebug("Golem: Not in this territory or wild");
				}
			}
		}
    }

	/*
		Town Options
	 */

	public boolean setTownOption(KonTownOption option, boolean value) {
		if (townOptions.containsKey(option)) {
			// Set valid option
			townOptions.put(option,value);
			return true;
		}
		// No valid option
		return false;
	}

	public boolean getTownOption(KonTownOption option) {
		if (townOptions.containsKey(option)) {
			// Get valid option
			return townOptions.get(option);
		}
		// No valid option
		return false;
	}

	/* Legacy Methods */
	public void setIsOpen(boolean val) {
		townOptions.put(KonTownOption.OPEN,val);
	}
	
	public boolean isOpen() {
		return townOptions.get(KonTownOption.OPEN);
	}

	public void setIsAlliedBuildingAllowed(boolean val) {
		townOptions.put(KonTownOption.ALLIED_BUILDING,val);
	}

	public boolean isAlliedBuildingAllowed() {
		return townOptions.get(KonTownOption.ALLIED_BUILDING);
	}

	public void setIsFriendlyRedstoneAllowed(boolean val) {
		townOptions.put(KonTownOption.FRIENDLY_REDSTONE,val);
	}

	public boolean isFriendlyRedstoneAllowed() {
		return townOptions.get(KonTownOption.FRIENDLY_REDSTONE);
	}

	public void setIsEnemyRedstoneAllowed(boolean val) {
		townOptions.put(KonTownOption.ENEMY_REDSTONE,val);
	}
	
	public boolean isEnemyRedstoneAllowed() {
		return townOptions.get(KonTownOption.ENEMY_REDSTONE);
	}
	
	public void setIsPlotOnly(boolean val) {
		townOptions.put(KonTownOption.PLOTS_ONLY,val);
	}
	
	public boolean isPlotOnly() {
		return townOptions.get(KonTownOption.PLOTS_ONLY);
	}
	
	public void setIsGolemOffensive(boolean val) {
		townOptions.put(KonTownOption.GOLEM_OFFENSE,val);
	}
	
	public boolean isGolemOffensive() {
		return townOptions.get(KonTownOption.GOLEM_OFFENSE);
	}

	/*
	Membership Methods
	 */

	public boolean canClaimLordship(KonPlayer player) {
		boolean result = false;
		if(!isLordValid()) {
			if(isOpen()) {
				result = true;
			} else {
				if(getPlayerResidents().isEmpty()) {
					result = true;
				} else if(isPlayerResident(player.getOfflineBukkitPlayer())) {
					result = true;
				}
			}
		}
		return result;
	}
	
	public void purgeResidents() {
		lord = null;
		residents.clear();
		joinRequestKeeper.clearRequests();
	}
	
	public void setPlayerLord(OfflinePlayer player) {
		setLord(player.getUniqueId());
	}

	public void setLord(UUID id) {
		setLord(id, true);
	}

	public void setLord(UUID id, boolean doUpdates) {
		lord = id;
		residents.put(id,true);
		if (doUpdates) {
			getEarth().getUpgradeManager().updateTownDisabledUpgrades(this);
			getEarth().getMapHandler().drawLabel(this);
		}
	}
	
	public boolean isLord(UUID id) {
		boolean status = false;
		if(lord != null) {
			status = id.equals(lord);
		}
		return status;
	}
	
	public boolean isPlayerLord(OfflinePlayer player) {
		return isLord(player.getUniqueId());
	}

	// Changes resident's knight flag, cannot be lord
	public boolean setPlayerKnight(OfflinePlayer player, boolean val) {
		boolean status = true;
		UUID playerUUID = player.getUniqueId();
		if(residents.containsKey(playerUUID) && !isLord(playerUUID)) {
			residents.put(playerUUID,val);
		} else {
			status = false;
		}
		return status;
	}
	
	// Returns true when player is Lord or Knight
	public boolean isPlayerKnight(OfflinePlayer player) {
		boolean status = true;
		UUID playerUUID = player.getUniqueId();
		if(residents.containsKey(playerUUID) || isLord(playerUUID)) {
			status = residents.get(playerUUID);
		} else {
			status = false;
		}
		return status;
	}

	public boolean addPlayerResident(OfflinePlayer player, boolean isElite) {
		return addPlayerResident(player, isElite, true);
	}
	
	public boolean addPlayerResident(OfflinePlayer player, boolean isElite, boolean doUpdates) {
		boolean status = false;
		UUID playerUUID = player.getUniqueId();
		if(!residents.containsKey(playerUUID)) {
			residents.put(playerUUID,isElite);
			if (doUpdates) {
				getEarth().getUpgradeManager().updateTownDisabledUpgrades(this);
				getEarth().getMapHandler().drawLabel(this);
			}
			status = true;
		}
		return status;
	}
	
	public boolean removePlayerResident(OfflinePlayer player) {
		boolean status = false;
		UUID playerUUID = player.getUniqueId();
		if(residents.containsKey(playerUUID)) {
			residents.remove(playerUUID);
			if(isPlayerLord(player)) {
				lord = null;
			}
			getEarth().getUpgradeManager().updateTownDisabledUpgrades(this);
			getEarth().getMapHandler().drawLabel(this);
			if(residents.isEmpty()) {
				clearShieldsArmors();
			}
			for(KonPlot plot : getPlots()) {
				plot.removeUser(player);
			}
			status = true;
		}
		return status;
	}
	
	public boolean isPlayerResident(OfflinePlayer player) {
		return residents.containsKey(player.getUniqueId());
	}
	
	public boolean isLordValid() {
		return lord != null;
	}
	
	public OfflinePlayer getPlayerLord() {
		if(lord != null) {
			return Bukkit.getOfflinePlayer(lord);
		}
		return null;
	}
	
	public UUID getLord() {
		return lord;
	}
	
	public ArrayList<OfflinePlayer> getPlayerKnights() {
		ArrayList<OfflinePlayer> eliteList = new ArrayList<>();
		for(UUID id : residents.keySet()) {
			if(residents.get(id)) {
				eliteList.add(Bukkit.getOfflinePlayer(id));
			}
		}
		return eliteList;
	}

	public ArrayList<OfflinePlayer> getPlayerKnightsOnly() {
		ArrayList<OfflinePlayer> eliteList = new ArrayList<>();
		for(UUID id : residents.keySet()) {
			if(residents.get(id) && !lord.equals(id)) {
				eliteList.add(Bukkit.getOfflinePlayer(id));
			}
		}
		return eliteList;
	}
	
	public ArrayList<OfflinePlayer> getPlayerResidents() {
		ArrayList<OfflinePlayer> residentList = new ArrayList<>();
		for(UUID id : residents.keySet()) {
			residentList.add(Bukkit.getOfflinePlayer(id));
		}
		return residentList;
	}

	public ArrayList<OfflinePlayer> getPlayerResidentsOnly() {
		ArrayList<OfflinePlayer> residentList = new ArrayList<>();
		for(UUID id : residents.keySet()) {
			if(!residents.get(id)) {
				residentList.add(Bukkit.getOfflinePlayer(id));
			}
		}
		return residentList;
	}

	public String getPlayerRoleName(KonOfflinePlayer offlinePlayer) {
		return getPlayerRoleName(offlinePlayer.getOfflineBukkitPlayer());
	}
	public String getPlayerRoleName(OfflinePlayer offlinePlayer) {
		String result = "";
		if(isLord(offlinePlayer.getUniqueId())) {
			result = MessagePath.LABEL_LORD.getMessage();
		} else if(isPlayerKnight(offlinePlayer)) {
			result = MessagePath.LABEL_KNIGHT.getMessage();
		} else if(isPlayerResident(offlinePlayer)) {
			result = MessagePath.LABEL_RESIDENT.getMessage();
		}
		return result;
	}

	public int getNumLand() {
		return getChunkList().size();
	}

	public int getNumResidents() {
		return residents.size();
	}
	
	public int getNumResidentsOnline() {
		int result = 0;
		for(UUID id : residents.keySet()) {
			if(getEarth().getPlayerManager().isOnlinePlayer(Bukkit.getPlayer(id))) {
				result++;
			}
		}
		return result;
	}
	
	public Map<EarthUpgrade,Integer> getUpgrades() {
		Map<EarthUpgrade,Integer> result = new HashMap<>(upgrades);
		return result;
	}
	
	public void addUpgrade(EarthUpgrade upgrade, int level) {
		upgrades.put(upgrade, level);
	}
	
	public boolean hasUpgrade(EarthUpgrade upgrade) {
		return upgrades.containsKey(upgrade);
	}
	
	public void clearUpgrades() {
		upgrades.clear();
	}
	
	// Returns current upgrade level, modified when disabled
	public int getUpgradeLevel(EarthUpgrade upgrade) {
		int result = 0;
		if(upgrades.containsKey(upgrade)) {
			if(disabledUpgrades.containsKey(upgrade)) {
				result = disabledUpgrades.get(upgrade);
			} else {
				result = upgrades.get(upgrade);
			}
		}
		return result;
	}
	
	public int getRawUpgradeLevel(KonUpgrade upgrade) {
		int result = 0;
		if(upgrades.containsKey(upgrade)) {
			result = upgrades.get(upgrade);
		}
		return result;
	}
	
	public void disableUpgrade(KonUpgrade upgrade, int newLevel) {
		disabledUpgrades.put(upgrade,newLevel);
	}
	
	public boolean allowUpgrade(KonUpgrade upgrade) {
		boolean result = false;
		if(disabledUpgrades.containsKey(upgrade)) {
			disabledUpgrades.remove(upgrade);
			result = true;
		}
		return result;
	}
	
	public boolean isUpgradeDisabled(EarthUpgrade upgrade) {
		return disabledUpgrades.containsKey(upgrade);
	}
	
	// Players who have tried joining but need to be added
	public List<OfflinePlayer> getJoinRequests() {
		return joinRequestKeeper.getJoinRequests();
	}
	
	// Players who have been added but need to join
	public List<OfflinePlayer> getJoinInvites() {
		return joinRequestKeeper.getJoinInvites();
	}
	
	public boolean addJoinRequest(UUID id, Boolean type) {
		return joinRequestKeeper.addJoinRequest(id, type);
	}
	
	// Does the player have an existing request to be added?
	public boolean isJoinRequestValid(UUID id) {
		return joinRequestKeeper.isJoinRequestValid(id);
	}
	
	// Does the player have an existing invite to join?
	public boolean isJoinInviteValid(UUID id) {
		return joinRequestKeeper.isJoinInviteValid(id);
	}
	
	public void removeJoinRequest(UUID id) {
		joinRequestKeeper.removeJoinRequest(id);
	}

	public boolean isTownWatchProtected() {
		return isTownWatchProtected;
	}
	
	public boolean isShielded() {
		return isShielded;
	}
	
	public boolean isArmored() {
		return isArmored;
	}
	
	public void activateShield(int val) {
		if(isShielded) {
			// Already shielded
			shieldEndTimeSeconds = val;
		} else {
			// Activate new shield
			isShielded = true;
			shieldEndTimeSeconds = val;
			Date now = new Date();
			shieldNowTimeSeconds = (int)(now.getTime()/1000);
			shieldTimer.stopTimer();
			shieldTimer.setTime(0);
			shieldTimer.startLoopTimer();
		}
		updateBarTitle();
	}
	
	public void deactivateShield() {
		if(isShielded) {
			playDeactivateSound();
		}
		isShielded = false;
		shieldTimer.stopTimer();
		updateBarTitle();
	}
	
	public void activateArmor(int val) {
		if(!isArmored) {
			// Activate new armor
			isArmored = true;
		}
		armorCurrentBlocks = val;
		armorTotalBlocks = armorCurrentBlocks;
		armorProgress = 1.0;
		updateBarTitle();
	}
	
	public void deactivateArmor() {
		if(isArmored) {
			playDeactivateSound();
		}
		isArmored = false;
		armorCurrentBlocks = 0;
		armorTotalBlocks = 0;
		armorProgress = 0.0;
		updateBarTitle();
	}
	
	public void clearShieldsArmors() {
		deactivateShield();
		deactivateArmor();
	}
	
	public boolean damageArmor(int damage) {
		boolean result = false;
		if(isArmored) {
			armorCurrentBlocks -= damage;
			result = true;
			if(armorCurrentBlocks <= 0) {
				deactivateArmor();
			} else {
				armorProgress = (double)armorCurrentBlocks/armorTotalBlocks;
				updateBarTitle();
			}
		}
		return result;
	}
	
	public int getShieldEndTime() {
		return shieldEndTimeSeconds;
	}
	
	public int getArmorBlocks() {
		return armorCurrentBlocks;
	}
	
	public int getRemainingShieldTimeSeconds() {
		int result = 0;
		if(isShielded && shieldEndTimeSeconds > shieldNowTimeSeconds) {
			result = shieldEndTimeSeconds - shieldNowTimeSeconds;
		}
		return result;
	}
	
	private void playDeactivateSound() {
		getWorld().playSound(getCenterLoc(), Sound.BLOCK_GLASS_BREAK, (float)3.0, (float)0.3);
	}
	
	public void putPlot(KonPlot plot) {
		if(plot == null) {
			ChatUtil.printDebug("Failed to set null plot!");
			return;
		}
		for(Point p : plot.getPoints()) {
			plots.put(p, plot);
		}
	}
	
	public void removePlot(KonPlot plot) {
		if(plot == null) {
			ChatUtil.printDebug("Failed to remove null plot!");
			return;
		}
		for(Point p : plot.getPoints()) {
			plots.remove(p);
		}
	}
	
	public boolean hasPlot(Location loc) {
		return hasPlot(HelperUtil.toPoint(loc), loc.getWorld());
	}
	
	public boolean hasPlot(Point p, World w) {
		boolean result = false;
		if(this.getChunkList().containsKey(p) && w.equals(getWorld())) {
			result = plots.containsKey(p);
		}
		return result;
	}
	
	@Nullable
	public KonPlot getPlot(Location loc) {
		return getPlot(HelperUtil.toPoint(loc), loc.getWorld());
	}
	
	@Nullable
	public KonPlot getPlot(Point p, World w) {
		KonPlot result = null;
		if(this.getChunkList().containsKey(p) && w.equals(getWorld())) {
			result = plots.get(p);
		}
		return result;
	}
	
	public List<KonPlot> getPlots() {
		ArrayList<KonPlot> result = new ArrayList<>();
		for(KonPlot plot : plots.values()) {
			if(!result.contains(plot)) {
				result.add(plot);
			}
		}
		return result;
	}
	
	public void clearPlots() {
		plots.clear();
	}

	@Override
	public EarthTerritoryType getTerritoryType() {
		return EarthTerritoryType.TOWN;
	}
	
}
