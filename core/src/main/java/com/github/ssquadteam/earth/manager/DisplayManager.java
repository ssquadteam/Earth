package com.github.ssquadteam.earth.manager;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.display.*;
import com.github.ssquadteam.earth.display.icon.MenuIcon;
import com.github.ssquadteam.earth.display.wrapper.*;
import com.github.ssquadteam.earth.model.*;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.CorePath;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;

public class DisplayManager {

	private final Earth earth;
	private final HashMap<Inventory, MenuWrapper> pagedMenus;
	private final HashMap<Inventory, ViewableMenu> stateMenus;
	private final HashSet<Player> playerViewerCache;
	
	public static String titleFormat 		= ""+ChatColor.BLACK;
	public static String loreFormat 		= ""+ChatColor.YELLOW;
	public static String valueFormat 		= ""+ChatColor.AQUA;
	public static String hintFormat 		= ""+ChatColor.GOLD+ChatColor.UNDERLINE+"\u21D2"; // ⇒
	public static String propertyFormat 	= ""+ChatColor.LIGHT_PURPLE+ChatColor.ITALIC+"\u25C6"; // ◆
	public static String alertFormat 		= ""+ChatColor.RED+ChatColor.ITALIC+"\u26A0"; // ⚠
	
	public DisplayManager(Earth earth) {
		this.earth = earth;
		this.pagedMenus = new HashMap<>();
		this.stateMenus = new HashMap<>();
		this.playerViewerCache = new HashSet<>();
	}
	
	public void initialize() {
		ChatUtil.printDebug("Display Manager is ready");
	}
	
	/*
	 * Common menu methods
	 */
	
	public boolean isPlayerViewingMenu(@Nullable Player player) {
		if(player == null) return false;
		return playerViewerCache.contains(player);
	}
	
	public boolean isNotDisplayMenu(@Nullable Inventory inv) {
		if(inv == null) return true;
		return !pagedMenus.containsKey(inv) && !stateMenus.containsKey(inv);
	}
	
	public void onDisplayMenuClick(KonPlayer clickPlayer, Inventory inv, int slot, boolean clickType) {
		if(inv == null) return;
		Player bukkitPlayer = clickPlayer.getBukkitPlayer();
		try {
			// Switch pages and handle navigation button clicks
			if(pagedMenus.containsKey(inv)) {
				MenuWrapper wrapper = pagedMenus.get(inv);
				if(wrapper == null) {
					return;
				}
				PagedMenu clickMenu = wrapper.getMenu();
				if(clickMenu == null) {
					return;
				}
				DisplayMenu currentPage = clickMenu.getCurrentPage();
				if(currentPage == null) {
					return;
				}
				MenuIcon clickedIcon = currentPage.getIcon(slot);
				if(clickedIcon == null || !clickedIcon.isClickable()) {
					return;
				}
				playMenuClickSound(bukkitPlayer);
				int nextIndex = clickMenu.getCurrentNextSlot();
				int closeIndex = clickMenu.getCurrentCloseSlot();
				int backIndex = clickMenu.getCurrentBackSlot();
				playerViewerCache.add(bukkitPlayer);
				pagedMenus.remove(inv);
				// Handle click context
				if(slot == nextIndex) {
					// Change paged view to next
					clickMenu.nextPageIndex();
					clickMenu.refreshCurrentPage();
					bukkitPlayer.openInventory(wrapper.getCurrentInventory());
	            	pagedMenus.put(wrapper.getCurrentInventory(), wrapper);
				} else if(slot == closeIndex) {
					// Close paged view
					bukkitPlayer.closeInventory();
	            	playerViewerCache.remove(bukkitPlayer);
				} else if(slot == backIndex) {
					// Change paged view to previous
					clickMenu.previousPageIndex();
					clickMenu.refreshCurrentPage();
					bukkitPlayer.openInventory(clickMenu.getCurrentPage().getInventory());
	            	pagedMenus.put(wrapper.getCurrentInventory(), wrapper);
				} else {
					// Clicked non-navigation slot, clickable icon
					// An icon will either open another menu or do nothing
					wrapper.onIconClick(clickPlayer, clickedIcon);
					bukkitPlayer.closeInventory();
	            	playerViewerCache.remove(bukkitPlayer);
				}
			} else if(stateMenus.containsKey(inv)) {
				// Handle menu navigation and states
				// Every clickable icon in a menu view will update the state and refresh the open inventory
				ViewableMenu clickMenu = stateMenus.get(inv);
				DisplayMenu currentView = clickMenu.getCurrentView();
				if(currentView == null || !currentView.getInventory().equals(inv)) {
					ChatUtil.printDebug("State menu view is not current!");
					return;
				}
				MenuIcon clickedIcon = currentView.getIcon(slot);
				if(clickedIcon == null || !clickedIcon.isClickable()) {
					return;
				}
				playMenuClickSound(bukkitPlayer);
				// Update plot menu state
				DisplayMenu updateView = clickMenu.updateState(slot, clickType);
				// Update inventory view
				stateMenus.remove(inv);
				playerViewerCache.add(bukkitPlayer);
				if(updateView != null) {
					// Refresh displayed inventory view
					bukkitPlayer.openInventory(updateView.getInventory());
	            	stateMenus.put(updateView.getInventory(), clickMenu);
				} else {
					// Close inventory view
					bukkitPlayer.closeInventory();
	            	playerViewerCache.remove(bukkitPlayer);
				}
			}
		} catch(Exception | Error e) {
			// Close inventory view
			bukkitPlayer.closeInventory();
        	playerViewerCache.remove(bukkitPlayer);
        	// Display exception
        	ChatUtil.printConsoleError("Failed to handle menu click, report this as a bug to the plugin author!");
        	e.printStackTrace();
		}
	}
	
	public void onDisplayMenuClose(Inventory inv, HumanEntity owner) {
		pagedMenus.remove(inv);
		stateMenus.remove(inv);
		if(owner instanceof Player) {
			playerViewerCache.remove((Player)owner);
		}
	}
	
	private void showMenuWrapper(Player bukkitPlayer, MenuWrapper wrapper) {
		pagedMenus.put(wrapper.getCurrentInventory(), wrapper);
		Bukkit.getScheduler().scheduleSyncDelayedTask(earth.getPlugin(), () -> {
			bukkitPlayer.openInventory(wrapper.getCurrentInventory());
			playerViewerCache.add(bukkitPlayer);
		});
	}
	
	/*
	 * ===============================================
	 * Help Menu
	 * ===============================================
	 */
	public void displayHelpMenu(Player bukkitPlayer) {
		playMenuOpenSound(bukkitPlayer);
		// Create menu
		HelpMenuWrapper wrapper = new HelpMenuWrapper(earth, bukkitPlayer);
		wrapper.constructMenu();
		// Display menu
		showMenuWrapper(bukkitPlayer,wrapper);
	}

	/*
	 * ===============================================
	 * Score Menu
	 * ===============================================
	 */
 	public void displayScoreMenu(KonPlayer displayPlayer, KonOfflinePlayer scorePlayer) {
		if (displayPlayer == null) return;
		Player bukkitPlayer = displayPlayer.getBukkitPlayer();
		playMenuOpenSound(bukkitPlayer);
		// Create menu
		ScoreMenuWrapper wrapper = new ScoreMenuWrapper(earth, scorePlayer, displayPlayer);
		wrapper.constructMenu();
		// Display menu
		showMenuWrapper(bukkitPlayer,wrapper);
	}
	
 	/*
	 * ===============================================
	 * Info Menus
	 * ===============================================
	 */
 	// Player Info
 	public void displayPlayerInfoMenu(KonPlayer displayPlayer, KonOfflinePlayer infoPlayer) {
		if (displayPlayer == null) return;
		Player bukkitPlayer = displayPlayer.getBukkitPlayer();
		playMenuOpenSound(displayPlayer.getBukkitPlayer());
		// Create menu
		PlayerInfoMenuWrapper wrapper = new PlayerInfoMenuWrapper(earth, infoPlayer, displayPlayer);
		wrapper.constructMenu();
		// Display menu
		showMenuWrapper(bukkitPlayer,wrapper);
 	}
 	
 	// Kingdom Info
  	public void displayKingdomInfoMenu(KonPlayer displayPlayer, KonKingdom infoKingdom) {
		if (displayPlayer == null) return;
		Player bukkitPlayer = displayPlayer.getBukkitPlayer();
		playMenuOpenSound(displayPlayer.getBukkitPlayer());
		// Create menu
		KingdomInfoMenuWrapper wrapper = new KingdomInfoMenuWrapper(earth, infoKingdom, displayPlayer);
		wrapper.constructMenu();
		// Display menu
		showMenuWrapper(bukkitPlayer,wrapper);
  	}
 	
  	// Town Info
   	public void displayTownInfoMenu(KonPlayer displayPlayer, KonTown infoTown) {
		if (displayPlayer == null) return;
		Player bukkitPlayer = displayPlayer.getBukkitPlayer();
		playMenuOpenSound(displayPlayer.getBukkitPlayer());
		// Create menu
		TownInfoMenuWrapper wrapper = new TownInfoMenuWrapper(earth, infoTown, displayPlayer);
		wrapper.constructMenu();
		// Display menu
		showMenuWrapper(bukkitPlayer,wrapper);
   	}

	// Monument Template Info
	public void displayTemplateInfoMenu(KonPlayer displayPlayer) {
		if (displayPlayer == null) return;
		Player bukkitPlayer = displayPlayer.getBukkitPlayer();
		playMenuOpenSound(displayPlayer.getBukkitPlayer());
		// Create menu
		MonumentTemplateInfoMenuWrapper wrapper = new MonumentTemplateInfoMenuWrapper(earth);
		wrapper.constructMenu();
		// Display menu
		showMenuWrapper(bukkitPlayer,wrapper);
	}

	// Sanctuary Info
	public void displaySanctuaryInfoMenu(KonPlayer displayPlayer, KonSanctuary infoSanctuary) {
		if (displayPlayer == null) return;
		Player bukkitPlayer = displayPlayer.getBukkitPlayer();
		playMenuOpenSound(displayPlayer.getBukkitPlayer());
		// Create menu
		SanctuaryInfoMenuWrapper wrapper = new SanctuaryInfoMenuWrapper(earth, infoSanctuary);
		wrapper.constructMenu();
		// Display menu
		showMenuWrapper(bukkitPlayer, wrapper);
	}

	// Ruin Info
	public void displayRuinInfoMenu(KonPlayer displayPlayer, KonRuin infoRuin) {
		if (displayPlayer == null) return;
		Player bukkitPlayer = displayPlayer.getBukkitPlayer();
		playMenuOpenSound(displayPlayer.getBukkitPlayer());
		// Create menu
		RuinInfoMenuWrapper wrapper = new RuinInfoMenuWrapper(earth, infoRuin);
		wrapper.constructMenu();
		// Display menu
		showMenuWrapper(bukkitPlayer,wrapper);
	}
   	
   	/*
	 * ===============================================
	 * Prefix Menu
	 * ===============================================
	 */
   	public void displayPrefixMenu(KonPlayer displayPlayer) {
		if (displayPlayer == null) return;
		Player bukkitPlayer = displayPlayer.getBukkitPlayer();
		playMenuOpenSound(displayPlayer.getBukkitPlayer());
		// Create menu
		PrefixMenuWrapper wrapper = new PrefixMenuWrapper(earth, displayPlayer);
		wrapper.constructMenu();
		// Display menu
		showMenuWrapper(bukkitPlayer,wrapper);
   	}
   	
   	/*
	 * ===============================================
	 * Kingdom Menu
	 * ===============================================
	 */
   	public void displayKingdomMenu(KonPlayer displayPlayer, KonKingdom kingdom, boolean isAdmin) {
		if (displayPlayer == null) return;
		playMenuOpenSound(displayPlayer.getBukkitPlayer());
		KingdomMenu newMenu = new KingdomMenu(earth, displayPlayer, kingdom, isAdmin);
		stateMenus.put(newMenu.getCurrentView().getInventory(), newMenu);
		// Schedule delayed task to display inventory to player
		Bukkit.getScheduler().scheduleSyncDelayedTask(earth.getPlugin(), () -> displayPlayer.getBukkitPlayer().openInventory(newMenu.getCurrentView().getInventory()),1);
	}

	/*
	 * ===============================================
	 * Town Menus
	 * ===============================================
	 */
	public void displayTownMenu(KonPlayer displayPlayer) {
		if (displayPlayer == null) return;
		playMenuOpenSound(displayPlayer.getBukkitPlayer());
		TownMenu newMenu = new TownMenu(earth, displayPlayer);
		stateMenus.put(newMenu.getCurrentView().getInventory(), newMenu);
		// Schedule delayed task to display inventory to player
		Bukkit.getScheduler().scheduleSyncDelayedTask(earth.getPlugin(), () -> displayPlayer.getBukkitPlayer().openInventory(newMenu.getCurrentView().getInventory()),1);
	}

	public void displayTownManagementMenu(KonPlayer displayPlayer, KonTown town, boolean isAdmin) {
		if (displayPlayer == null) return;
		playMenuOpenSound(displayPlayer.getBukkitPlayer());
		TownManagementMenu newMenu = new TownManagementMenu(earth, displayPlayer, town, isAdmin);
		stateMenus.put(newMenu.getCurrentView().getInventory(), newMenu);
		// Schedule delayed task to display inventory to player
		Bukkit.getScheduler().scheduleSyncDelayedTask(earth.getPlugin(), () -> displayPlayer.getBukkitPlayer().openInventory(newMenu.getCurrentView().getInventory()),1);
	}

	public void displayTownPlotMenu(Player bukkitPlayer, KonTown town) {
		// Verify plots are enabled
		boolean isPlotsEnabled = earth.getPlotManager().isEnabled();
		if(!isPlotsEnabled) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
		} else {
			// Open the menu
			playMenuOpenSound(bukkitPlayer);
			int maxSize = earth.getPlotManager().getMaxSize();
			PlotMenu newMenu = new PlotMenu(town, bukkitPlayer, maxSize);
			stateMenus.put(newMenu.getCurrentView().getInventory(), newMenu);
			// Schedule delayed task to display inventory to player
			Bukkit.getScheduler().scheduleSyncDelayedTask(earth.getPlugin(),
					() -> bukkitPlayer.openInventory(newMenu.getCurrentView().getInventory()),1);
		}
	}
   	
   	/*
	 * Helper methods
	 */
 	
   	public static String boolean2Symbol(boolean val) {
	   if(val)return ChatColor.DARK_GREEN+""+ChatColor.BOLD+ "\u2713";
	   return ChatColor.DARK_RED+""+ChatColor.BOLD+ "\u274C";
 	}
 	
   	public static String boolean2Lang(boolean val) {
	   if(val) return MessagePath.LABEL_TRUE.getMessage();
	   return MessagePath.LABEL_FALSE.getMessage();
 	}
 	
   	private void playMenuClickSound(Player bukkitPlayer) {
 		bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, (float)1.0, (float)0.8);
 	}
 	
   	private void playMenuOpenSound(Player bukkitPlayer) {
 		bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, (float)1.0, (float)1.4);
 	}
}
