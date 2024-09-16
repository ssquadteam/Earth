package com.github.ssquadteam.earth.listener;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.EarthPlugin;
import com.github.ssquadteam.earth.api.model.EarthRelationshipType;
import com.github.ssquadteam.earth.api.model.EarthTerritoryType;
import com.github.ssquadteam.earth.manager.KingdomManager;
import com.github.ssquadteam.earth.manager.PlayerManager;
import com.github.ssquadteam.earth.model.*;
import com.github.ssquadteam.earth.utility.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.DoubleChest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.SmithingInventory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class InventoryListener implements Listener {

	private final Earth earth;
	private final KingdomManager kingdomManager;
	private final PlayerManager playerManager;
	
	public InventoryListener(EarthPlugin plugin) {
		this.earth = plugin.getEarthInstance();
		this.kingdomManager = earth.getKingdomManager();
		this.playerManager = earth.getPlayerManager();
	}
	
	@EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
		if(event.isCancelled()) return;
		if(earth.isWorldIgnored(event.getInventory().getLocation())) return;
		// Monitor blocks in claimed territory
		// Check for merchant trades in claimed territory of guilds
		Location openLoc = event.getInventory().getLocation();
		
		if(openLoc != null && earth.getTerritoryManager().isChunkClaimed(openLoc)) {
			
			if(!earth.getPlayerManager().isOnlinePlayer((Player)event.getPlayer())) {
				ChatUtil.printDebug("Failed to handle onInventoryOpen for non-existent player");
				return;
			}
			KonPlayer player = playerManager.getPlayer((Player)event.getPlayer());
			// Bypass event restrictions for player in Admin Bypass Mode
			if(!player.isAdminBypassActive()) {
				// Determine type of inventory
				boolean isContainer = event.getInventory().getHolder() instanceof BlockInventoryHolder ||
						event.getInventory().getHolder() instanceof DoubleChest ||
						event.getInventory().getHolder() instanceof Vehicle;
				boolean isMerchant = event.getInventory().getType().equals(InventoryType.MERCHANT) &&
						event.getInventory() instanceof MerchantInventory;
				// Get territory
				KonTerritory territory = earth.getTerritoryManager().getChunkTerritory(openLoc);
				// Property Flag Holders
				if(isContainer && territory instanceof KonPropertyFlagHolder) {
					KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territory;
					if(flagHolder.hasPropertyValue(KonPropertyFlag.CHEST)) {
						if(!flagHolder.getPropertyValue(KonPropertyFlag.CHEST)) {
							ChatUtil.sendKonBlockedFlagTitle(player);
							event.setCancelled(true);
							return;
						}
					}
				}
				EarthRelationshipType territoryRole = kingdomManager.getRelationRole(territory.getKingdom(), player.getKingdom());
				// Town restrictions for inventories
				if(territory instanceof KonTown) {
					KonTown town = (KonTown) territory;
					// Protection checks
					if(isMerchant) {
						// Inventory is a Villager merchant
						// Only friendly, trade and ally players can use merchants
						boolean isMerchantAllowed = territoryRole.equals(EarthRelationshipType.FRIENDLY) || territoryRole.equals(EarthRelationshipType.TRADE) || territoryRole.equals(EarthRelationshipType.ALLY);
						// Prevent opening by enemy and sanction kingdom members
						if(!isMerchantAllowed) {
							ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_MERCHANT.getMessage(town.getKingdom().getName()));
							event.setCancelled(true);
							return;
						}
						// Attempt to modify merchant trades based on town specialization and relationships with player
						town.applyTradeDiscounts(player, event.getInventory());
					} else if(isContainer) {
						// Inventory can hold items, or is a vehicle with storage

						// Prevent inventory openings by non-friendlies
						boolean isEnemyInventoryOpenDenied = earth.getCore().getBoolean(CorePath.KINGDOMS_PROTECT_CONTAINERS_USE.getPath());
						if(isEnemyInventoryOpenDenied && !territoryRole.equals(EarthRelationshipType.FRIENDLY)) {
							ChatUtil.sendKonBlockedProtectionTitle(player);
							event.setCancelled(true);
							return;
						}
						// Notify player when there is no lord
						if(town.canClaimLordship(player)) {
							ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_TOWN_NOTICE_NO_LORD.getMessage(town.getName(),town.getTravelName()));
						}
						// Prevent non-residents in closed towns from opening inventories that can hold items
						// However this still allows them to open inventories like enchantment tables, crafting bench, etc.
						// Protect land and plots
						if(!town.isOpen() && !town.isPlayerResident(player.getOfflineBukkitPlayer())) {
							// Stop all edits by non-resident in closed towns
							ChatUtil.sendError((Player)event.getPlayer(), MessagePath.PROTECTION_ERROR_NOT_RESIDENT.getMessage(territory.getName()));
							event.setCancelled(true);
							return;
						}
						if(town.isOpen() || (!town.isOpen() && town.isPlayerResident(player.getOfflineBukkitPlayer()))) {
							// Check for protections in open towns and closed town residents
							if(earth.getPlotManager().isPlayerPlotProtectContainer(town, openLoc, player.getBukkitPlayer())) {
								// Stop when player edits plot that isn't theirs
								ChatUtil.sendError((Player)event.getPlayer(), MessagePath.PROTECTION_ERROR_NOT_PLOT.getMessage());
								event.setCancelled(true);
								return;
							}
							if(town.isPlotOnly() && !town.isPlayerKnight(player.getOfflineBukkitPlayer()) && !town.hasPlot(openLoc) && !town.getMonument().isLocInside(openLoc)) {
								// Stop when non-elite player edits non-plot land (excluding monument)
								ChatUtil.sendError((Player)event.getPlayer(), MessagePath.PROTECTION_ERROR_ONLY_PLOT.getMessage());
								event.setCancelled(true);
								return;
							}
						}
					}
					// Attempt to put loot into empty chests within the monument
					if(town.getMonument().isLocInside(openLoc) && event.getInventory().getType().equals(InventoryType.CHEST)) {
						if(town.isPlayerLord(player.getOfflineBukkitPlayer()) || town.isPlayerKnight(player.getOfflineBukkitPlayer())) {
							// Update loot with default count as defined in core YML
							boolean result = earth.getLootManager().updateMonumentLoot(event.getInventory(), town);
							ChatUtil.printDebug("Attempted to update loot in town "+territory.getName()+", got "+result);
							if(result) {
								event.getInventory().getLocation().getWorld().playSound(event.getInventory().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, (float)1.0, (float)1.0);
								// Execute custom commands from config
								earth.executeCustomCommand(CustomCommandPath.TOWN_MONUMENT_LOOT_OPEN,player.getBukkitPlayer());
							} else {
								ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_LOOT_LATER.getMessage());
							}
						} else {
							ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
							event.setCancelled(true);
							return;
						}
					}
				}

				// Ruin restrictions for inventories
				if(territory instanceof KonRuin) {
					// Attempt to put loot into empty chests within the ruin
					if(event.getInventory().getType().equals(InventoryType.CHEST)) {
						// Update loot with default count as defined in core YML
						boolean result = earth.getLootManager().updateRuinLoot(event.getInventory(), (KonRuin)territory);
						ChatUtil.printDebug("Attempted to update loot in ruin "+territory.getName()+", got "+result);
						if(result) {
							event.getInventory().getLocation().getWorld().playSound(event.getInventory().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, (float)1.0, (float)1.0);
							// Execute custom commands from config
							earth.executeCustomCommand(CustomCommandPath.RUIN_LOOT_OPEN,player.getBukkitPlayer());
						} else {
							ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_LOOT_CAPTURE.getMessage());
						}
					}
				}
				
				// Prevent all inventory openings except for camp owner and clan members when allowed
				boolean isCampContainersProtected = earth.getCore().getBoolean(CorePath.CAMPS_PROTECT_CONTAINERS.getPath(), true);
				if(territory instanceof KonCamp && isCampContainersProtected) {
					KonCamp camp = (KonCamp) territory;
					boolean isMemberAllowed = earth.getCore().getBoolean(CorePath.CAMPS_CLAN_ALLOW_CONTAINERS.getPath(), false);
					boolean isMember = false;
					if(earth.getCampManager().isCampGrouped(camp)) {
						isMember = earth.getCampManager().getCampGroup(camp).isPlayerMember(player.getBukkitPlayer());
					}
					if(!(isMember && isMemberAllowed) && !player.getBukkitPlayer().getUniqueId().equals(camp.getOwner().getUniqueId())) {
						ChatUtil.sendKonBlockedProtectionTitle(player);
						event.setCancelled(true);
					}
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
		// When a player closes a display menu inventory
		if(earth.getDisplayManager().isNotDisplayMenu(event.getInventory())) return;
		earth.getDisplayManager().onDisplayMenuClose(event.getInventory(), event.getPlayer());
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
    public void onDisplayMenuClick(InventoryClickEvent event) {
		// When a player clicks inside a display menu inventory
		if(earth.getDisplayManager().isNotDisplayMenu(event.getClickedInventory())) return;
		int slot = event.getRawSlot();
		Player bukkitPlayer = (Player) event.getWhoClicked();
		KonPlayer player = earth.getPlayerManager().getPlayer(bukkitPlayer);
		if(player == null || slot >= CompatibilityUtil.getTopInventory(event).getSize()) return;
		event.setResult(Event.Result.DENY);
		event.setCancelled(true);
		if(event.getClick().equals(ClickType.LEFT) || event.getClick().equals(ClickType.RIGHT)) {
			boolean clickType = !event.getClick().equals(ClickType.RIGHT);
			Bukkit.getScheduler().runTask(earth.getPlugin(),
					() -> earth.getDisplayManager().onDisplayMenuClick(player, event.getClickedInventory(), slot, clickType));
		}

	}
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void onNetheriteCraft(InventoryClickEvent event) {
		if(event.isCancelled()) return;
		// Check for picking up netherite items for stats
		if(event.getAction().equals(InventoryAction.PICKUP_ALL) && event.getClickedInventory() instanceof SmithingInventory &&
				!earth.isWorldIgnored(event.getInventory().getLocation())) {
			Material itemType = event.getCurrentItem().getType();
			int slot = event.getRawSlot();
			HumanEntity human = event.getWhoClicked();
			KonPlayer player = playerManager.getPlayer((Player)human);
			if(player != null && (itemType.equals(Material.NETHERITE_AXE) ||
					itemType.equals(Material.NETHERITE_HOE) ||
					itemType.equals(Material.NETHERITE_SHOVEL) ||
					itemType.equals(Material.NETHERITE_PICKAXE) ||
					itemType.equals(Material.NETHERITE_SWORD) ||
					itemType.equals(Material.NETHERITE_HELMET) ||
					itemType.equals(Material.NETHERITE_CHESTPLATE) ||
					itemType.equals(Material.NETHERITE_LEGGINGS) ||
					itemType.equals(Material.NETHERITE_BOOTS))) {
				if(slot == 2) {
					// Player crafted a netherite item
					earth.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.CRAFTED,5);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onLootChestClick(InventoryClickEvent event) {
		// Prevent placing items into loot chests
		if(event.isCancelled()) return;
		if(event.getClickedInventory() == null) return;
		if(!CompatibilityUtil.getTopInventory(event).getType().equals(InventoryType.CHEST)) return;
		Location inventoryLocation = CompatibilityUtil.getTopInventory(event).getLocation();
		if(inventoryLocation == null) return;
		// Check for ignored world
		if(earth.isWorldIgnored(inventoryLocation)) return;
		// Check if the raw slot index is within the chest inventory
		if(event.getRawSlot() < CompatibilityUtil.getTopInventory(event).getSize()) {
			// When clicking in the top (chest) inventory, only allow pickup and shift-click of items into bottom.
			if(event.getAction().equals(InventoryAction.PICKUP_ALL) || event.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY)) {
				//ChatUtil.printDebug("Ignored top inventory");
				return;
			}
		} else {
			// When clicking in the bottom (player) inventory, allow everything but shift-clicks of items into top.
			if(!event.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY)) {
				//ChatUtil.printDebug("Ignored bottom inventory");
				return;
			}
		}
		//ChatUtil.printDebug("Checking for loot chest...");
		// Check for loot chest
		if(isLootChestLocation(inventoryLocation)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onLootChestDrag(InventoryDragEvent event) {
		// Prevent dragging items into loot chests
		if(event.isCancelled()) return;
		if(!CompatibilityUtil.getTopInventory(event).getType().equals(InventoryType.CHEST)) return;
		Location inventoryLocation = event.getInventory().getLocation();
		if(inventoryLocation == null) return;
		// Check for ignored world
		if(earth.isWorldIgnored(inventoryLocation)) return;
		// Check if any raw slot index is within the chest inventory
		boolean isInTop = false;
		for(int slot : event.getRawSlots()) {
			if(slot < CompatibilityUtil.getTopInventory(event).getSize()) {
				isInTop = true;
				break;
			}
		}
		//ChatUtil.printDebug("Inventory dragged in top chest inventory: "+isInTop);
		// When dragging in the top (chest) inventory, do not allow anything.
		// When dragging in the bottom (player) inventory, allow everything.
		if(!isInTop) {
			//ChatUtil.printDebug("Ignored bottom inventory");
			return;
		}
		//ChatUtil.printDebug("Checking for loot chest...");
		// Check for territory at inventory location
		if(isLootChestLocation(inventoryLocation)) {
			event.setCancelled(true);
		}
	}

	private boolean isLootChestLocation(Location loc) {
		// Check for territory at inventory location
		if(!earth.getTerritoryManager().isChunkClaimed(loc)) return false;
		KonTerritory territory = earth.getTerritoryManager().getChunkTerritory(loc);
		// Check for town
		if(territory instanceof KonTown) {
			KonTown town = (KonTown) territory;
			// Check for inventory inside monument
			if(town.getMonument().isLocInside(loc)) {
				// At this point, due to previous checks, the inventory is inside of a town monument
				// and is one of the prohibited actions. Cancel this event.
				//ChatUtil.printDebug("Cancelling player item placement into monument inventory of town "+territory.getName());
				return true;
			}
		} else if(territory instanceof KonRuin) {
			//ChatUtil.printDebug("Cancelling player item placement into ruin inventory of ruin "+territory.getName());
			return true;
		}
		return false;
	}

	@EventHandler(priority = EventPriority.MONITOR)
    public void onCraftItem(CraftItemEvent event) {
		if(earth.isWorldIgnored(event.getInventory().getLocation())) {
			return;
		}
		HumanEntity human = event.getWhoClicked();
		if(event.isCancelled() || !(human instanceof Player)) return;
		if(!earth.getPlayerManager().isOnlinePlayer((Player)human)) {
			ChatUtil.printDebug("Failed to handle onCraftItem for non-existent player");
			return;
		}
		Material recipeMaterial = event.getRecipe().getResult().getType();
		KonPlayer player = earth.getPlayerManager().getPlayer((Player)human);
		if(recipeMaterial.equals(Material.IRON_HELMET) ||
				recipeMaterial.equals(Material.IRON_CHESTPLATE) ||
				recipeMaterial.equals(Material.IRON_LEGGINGS) ||
				recipeMaterial.equals(Material.IRON_BOOTS)) {
			// Player crafted a component of iron armor
			earth.getDirectiveManager().updateDirectiveProgress(player, KonDirective.CRAFT_ARMOR);
			earth.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.CRAFTED,2);
		} else if(recipeMaterial.equals(Material.DIAMOND_HELMET) ||
				recipeMaterial.equals(Material.DIAMOND_CHESTPLATE) ||
				recipeMaterial.equals(Material.DIAMOND_LEGGINGS) ||
				recipeMaterial.equals(Material.DIAMOND_BOOTS)) {
			// Player crafted a component of diamond armor
			earth.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.CRAFTED,3);
		} else if(recipeMaterial.equals(Material.GOLDEN_HELMET) ||
				recipeMaterial.equals(Material.GOLDEN_CHESTPLATE) ||
				recipeMaterial.equals(Material.GOLDEN_LEGGINGS) ||
				recipeMaterial.equals(Material.GOLDEN_BOOTS)) {
			// Player crafted a component of gold armor
			earth.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.CRAFTED,1);
		} else if(recipeMaterial.equals(Material.LEATHER_HELMET) ||
				recipeMaterial.equals(Material.LEATHER_CHESTPLATE) ||
				recipeMaterial.equals(Material.LEATHER_LEGGINGS) ||
				recipeMaterial.equals(Material.LEATHER_BOOTS)) {
			// Player crafted a component of leather armor
			earth.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.CRAFTED,1);
		} else if(recipeMaterial.equals(Material.IRON_AXE) ||
				recipeMaterial.equals(Material.IRON_HOE) ||
				recipeMaterial.equals(Material.IRON_SHOVEL) ||
				recipeMaterial.equals(Material.IRON_PICKAXE) ||
				recipeMaterial.equals(Material.IRON_SWORD)) {
			// Player crafted an iron item
			earth.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.CRAFTED,1);
		} else if(recipeMaterial.equals(Material.DIAMOND_AXE) ||
				recipeMaterial.equals(Material.DIAMOND_HOE) ||
				recipeMaterial.equals(Material.DIAMOND_SHOVEL) ||
				recipeMaterial.equals(Material.DIAMOND_PICKAXE) ||
				recipeMaterial.equals(Material.DIAMOND_SWORD)) {
			// Player crafted a diamond item
			earth.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.CRAFTED,2);
		}
		// Check for edible item
		if(recipeMaterial.isEdible()) {
			earth.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FOOD,event.getCurrentItem().getAmount());
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
		if(earth.isWorldIgnored(event.getBlock().getWorld())) return;
		if(!earth.getPlayerManager().isOnlinePlayer(event.getPlayer())) {
			ChatUtil.printDebug("Failed to handle onFurnaceExtract for non-existent player");
			return;
		}
		KonPlayer player = playerManager.getPlayer(event.getPlayer());
		Material extractMat = event.getItemType();
		int stackSize = event.getItemAmount();
		if(extractMat.toString().toLowerCase().contains("ingot") ||
				extractMat.equals(Material.NETHERITE_SCRAP) ||
				extractMat.equals(Material.BRICK)) {
			earth.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.INGOTS,stackSize);
		} else if(extractMat.isEdible()) {
			earth.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FOOD,stackSize);
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void onEnchantItem(EnchantItemEvent event) {
		if(event.isCancelled())return;
		if(earth.isWorldIgnored(event.getInventory().getLocation())) return;
		if(!earth.getPlayerManager().isOnlinePlayer(event.getEnchanter())) {
			ChatUtil.printDebug("Failed to handle onEnchantItem for non-existent player");
			return;
		}
		KonPlayer player = playerManager.getPlayer(event.getEnchanter());
		earth.getDirectiveManager().updateDirectiveProgress(player, KonDirective.ENCHANT_ITEM);
		earth.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.ENCHANTMENTS,1);
		Location enchantLoc = event.getEnchantBlock().getLocation();
		if(earth.getTerritoryManager().isChunkClaimed(enchantLoc)) {
			KonTerritory territory = earth.getTerritoryManager().getChunkTerritory(enchantLoc);
			if(territory instanceof KonTown) {
				KonTown town = (KonTown)territory;
				int upgradeLevel = earth.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.ENCHANT);
				if(upgradeLevel >= 1) {
					// Add 1 (if possible) to all applied enchantment levels
					Map<Enchantment,Integer> enchantsToAdd = event.getEnchantsToAdd();
					for(Enchantment enchant : enchantsToAdd.keySet()) {
						int enchantLevel = enchantsToAdd.get(enchant);
						if(enchantLevel < enchant.getMaxLevel()) {
							enchantsToAdd.replace(enchant, enchantLevel+1);
						}
					}
				}
			}
		}

	}
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
		if(event.isCancelled())return;
		if(earth.isWorldIgnored(event.getInventory().getLocation())) return;

		Location enchantLoc = event.getEnchantBlock().getLocation();
		if(!earth.getTerritoryManager().isChunkClaimed(enchantLoc)) return;
		KonTerritory territory = earth.getTerritoryManager().getChunkTerritory(enchantLoc);
		if(!(territory instanceof KonTown)) return;
		KonTown town = (KonTown)territory;
		int upgradeLevel = earth.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.ENCHANT);
		if(upgradeLevel < 1)return;

		// Add 1 (if possible) to all enchantment offers
		EnchantmentOffer[] offers = event.getOffers();
		for (EnchantmentOffer offer : offers) {
			if (offer != null) {
				int enchantLevel = offer.getEnchantmentLevel();
				int enchantMaxLevel = offer.getEnchantment().getMaxLevel();
				if (enchantLevel < enchantMaxLevel) {
					offer.setEnchantmentLevel(enchantLevel + 1);
				}
			}
		}

	}

}
