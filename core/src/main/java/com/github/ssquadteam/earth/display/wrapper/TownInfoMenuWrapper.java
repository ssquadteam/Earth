package com.github.ssquadteam.earth.display.wrapper;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.api.model.EarthTerritoryType;
import com.github.ssquadteam.earth.display.icon.InfoIcon;
import com.github.ssquadteam.earth.display.icon.KingdomIcon;
import com.github.ssquadteam.earth.display.icon.MenuIcon;
import com.github.ssquadteam.earth.display.icon.PlayerIcon;
import com.github.ssquadteam.earth.display.icon.PlayerIcon.PlayerIconAction;
import com.github.ssquadteam.earth.manager.DisplayManager;
import com.github.ssquadteam.earth.model.*;
import com.github.ssquadteam.earth.utility.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class TownInfoMenuWrapper extends MenuWrapper {

	private final KonTown infoTown;
	private final KonPlayer observer;
	
	public TownInfoMenuWrapper(Earth earth, KonTown infoTown, KonPlayer observer) {
		super(earth);
		this.infoTown = infoTown;
		this.observer = observer;
	}

	@Override
	public void constructMenu() {

		String pageLabel;
 		List<String> loreList;
 		InfoIcon info;
		int pageTotal;
		int pageNum = 0;
		
		String kingdomColor = getEarth().getDisplaySecondaryColor(observer, infoTown);
 		String titleColor = DisplayManager.titleFormat;
		String propertyColor = DisplayManager.propertyFormat;
		String loreColor = DisplayManager.loreFormat;
		String valueColor = DisplayManager.valueFormat;
		String hintColor = DisplayManager.hintFormat;
		
		List<OfflinePlayer> townKnights = new ArrayList<>();
		List<OfflinePlayer> townResidents = new ArrayList<>();
		for(OfflinePlayer resident : infoTown.getPlayerResidents()) {
			if(!infoTown.isPlayerLord(resident)) {
				if(infoTown.isPlayerKnight(resident)) {
					townKnights.add(resident);
				} else {
					townResidents.add(resident);
				}
			}
		}

 		// Page 0
		pageLabel = titleColor+MessagePath.COMMAND_INFO_NOTICE_TOWN_HEADER.getMessage(infoTown.getName());
		getMenu().addPage(pageNum, 1, pageLabel);

		/* Kingdom Info Icon (1) */
		loreList = new ArrayList<>();
    	loreList.add(hintColor+MessagePath.MENU_SCORE_HINT.getMessage());
    	KingdomIcon kingdom = new KingdomIcon(infoTown.getKingdom(),kingdomColor,loreList,1,true);
    	getMenu().getPage(pageNum).addIcon(kingdom);

		/* Lord Player Info Icon (2) */
		loreList = new ArrayList<>();
		if(infoTown.isLordValid()) {
			OfflinePlayer lordPlayer = infoTown.getPlayerLord();
			loreList.add(propertyColor+MessagePath.LABEL_LORD.getMessage());
			loreList.add(hintColor+MessagePath.MENU_SCORE_HINT.getMessage());
			PlayerIcon playerInfo = new PlayerIcon(kingdomColor+lordPlayer.getName(),loreList,lordPlayer,2,true,PlayerIconAction.DISPLAY_INFO);
			getMenu().getPage(pageNum).addIcon(playerInfo);
		} else {
			loreList.addAll(HelperUtil.stringPaginate(MessagePath.COMMAND_TOWN_NOTICE_NO_LORD.getMessage(infoTown.getName(), infoTown.getTravelName()), ChatColor.RED));
			info = new InfoIcon(kingdomColor+MessagePath.LABEL_LORD.getMessage(),loreList,Material.BARRIER,2,false);
			getMenu().getPage(pageNum).addIcon(info);
		}

		/* Specialization Info Icon (4) */
		if (getEarth().getKingdomManager().getIsDiscountEnable()) {
			loreList = new ArrayList<>();
			loreList.add(valueColor + infoTown.getSpecializationName());
			loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_INFO_SPECIAL.getMessage(), loreColor));
			Material specialMat = CompatibilityUtil.getProfessionMaterial(infoTown.getSpecialization());
			info = new InfoIcon(kingdomColor + MessagePath.LABEL_SPECIALIZATION.getMessage(), loreList, specialMat, 4, false);
			getMenu().getPage(pageNum).addIcon(info);
		}

		/* Properties Info Icon (5) */
    	String isProtected = DisplayManager.boolean2Symbol((infoTown.isCaptureDisabled() || infoTown.getKingdom().isOfflineProtected() || infoTown.isTownWatchProtected()));
    	String isAttacked = DisplayManager.boolean2Symbol(infoTown.isAttacked());
    	String isShielded = DisplayManager.boolean2Symbol(infoTown.isShielded());
    	String isArmored = DisplayManager.boolean2Symbol(infoTown.isArmored());
    	String isPeaceful = DisplayManager.boolean2Symbol(infoTown.getKingdom().isPeaceful());
		String isImmune = DisplayManager.boolean2Symbol(infoTown.getKingdom().isCapitalImmune());
    	loreList = new ArrayList<>();
    	if(infoTown.getTerritoryType().equals(EarthTerritoryType.CAPITAL)) {
			loreList.add(loreColor+MessagePath.LABEL_IMMUNITY.getMessage()+": "+isImmune);
		}
		loreList.add(loreColor+MessagePath.PROTECTION_NOTICE_ATTACKED.getMessage()+": "+isAttacked);
		loreList.add(loreColor+MessagePath.LABEL_PROTECTED.getMessage()+": "+isProtected);
		loreList.add(loreColor+MessagePath.LABEL_SHIELD.getMessage()+": "+isShielded);
		loreList.add(loreColor+MessagePath.LABEL_ARMOR.getMessage()+": "+isArmored);
    	loreList.add(loreColor+MessagePath.LABEL_PEACEFUL.getMessage()+": "+isPeaceful);
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_PROPERTIES.getMessage(), loreList, Material.PAPER, 5, false);
    	getMenu().getPage(pageNum).addIcon(info);

		/* Options Info Icon (6) */
		String isOpen = DisplayManager.boolean2Symbol(infoTown.isOpen());
    	String isPlotOnly = DisplayManager.boolean2Symbol(infoTown.isPlotOnly());
		String isFriendlyRedstone = DisplayManager.boolean2Symbol(infoTown.isFriendlyRedstoneAllowed());
		String isRedstone = DisplayManager.boolean2Symbol(infoTown.isEnemyRedstoneAllowed());
    	String isGolemOffense = DisplayManager.boolean2Symbol(infoTown.isGolemOffensive());
		loreList = new ArrayList<>();
		loreList.add(loreColor+MessagePath.LABEL_OPEN.getMessage()+": "+isOpen);
		loreList.add(loreColor+MessagePath.LABEL_PLOT.getMessage()+": "+isPlotOnly);
		loreList.add(loreColor+MessagePath.LABEL_FRIENDLY_REDSTONE.getMessage()+": "+isFriendlyRedstone);
		loreList.add(loreColor+MessagePath.LABEL_ENEMY_REDSTONE.getMessage()+": "+isRedstone);
    	loreList.add(loreColor+MessagePath.LABEL_GOLEM_OFFENSE.getMessage()+": "+isGolemOffense);
		boolean isAlliedBuildingEnable = getEarth().getCore().getBoolean(CorePath.KINGDOMS_ALLY_BUILD.getPath(),false);
		if(isAlliedBuildingEnable) {
			String isAlliedBuildingAllowed = DisplayManager.boolean2Symbol(infoTown.isAlliedBuildingAllowed());
			loreList.add(loreColor+MessagePath.LABEL_ALLIED_BUILDING.getMessage()+": "+isAlliedBuildingAllowed);
		}
		info = new InfoIcon(kingdomColor+MessagePath.LABEL_OPTIONS.getMessage(), loreList, Material.OAK_SIGN, 6, false);
		getMenu().getPage(pageNum).addIcon(info);

		/* Stats Info Icon (7) */
		int maxCriticalHits = getEarth().getCore().getInt(CorePath.MONUMENTS_DESTROY_AMOUNT.getPath());
		int townHealth = maxCriticalHits - infoTown.getMonument().getCriticalHits();
		loreList = new ArrayList<>();
		loreList.add(loreColor+MessagePath.LABEL_HEALTH.getMessage()+": "+valueColor+townHealth+"/"+maxCriticalHits);
		loreList.add(loreColor+MessagePath.LABEL_LAND.getMessage()+": "+valueColor+infoTown.getChunkList().size());
		loreList.add(loreColor+MessagePath.LABEL_POPULATION.getMessage()+": "+valueColor+infoTown.getNumResidents());
		info = new InfoIcon(kingdomColor+MessagePath.LABEL_STATS.getMessage(), loreList, Material.BELL, 7, false);
		getMenu().getPage(pageNum).addIcon(info);

		/* Flags Info Icon (8) */
		loreList = new ArrayList<>();
		for(KonPropertyFlag flag : KonPropertyFlag.values()) {
			if(infoTown.hasPropertyValue(flag)) {
				String flagDisplaySymbol = DisplayManager.boolean2Symbol(infoTown.getPropertyValue(flag));
				loreList.add(loreColor+flag.getName()+": "+flagDisplaySymbol);
			}
		}
		InfoIcon propertyInfo = new InfoIcon(kingdomColor+MessagePath.LABEL_FLAGS.getMessage(), loreList, Material.REDSTONE_TORCH, 8, false);
		getMenu().getPage(pageNum).addIcon(propertyInfo);
		pageNum++;

    	// Page 1
		if (getEarth().getUpgradeManager().isEnabled()) {
			pageLabel = titleColor + MessagePath.LABEL_UPGRADES.getMessage();
			getMenu().addPage(pageNum, 1, pageLabel);
			int index = 0;
			for (KonUpgrade upgrade : KonUpgrade.values()) {
				int currentLevel = infoTown.getRawUpgradeLevel(upgrade);
				if (currentLevel > 0) {
					String formattedUpgrade = ChatColor.LIGHT_PURPLE + upgrade.getDescription() + " " + currentLevel;
					int level = currentLevel;
					boolean isDisabled = false;
					if (infoTown.isUpgradeDisabled(upgrade)) {
						isDisabled = true;
						int reducedLevel = infoTown.getUpgradeLevel(upgrade);
						level = reducedLevel;
						if (reducedLevel > 0) {
							formattedUpgrade = ChatColor.LIGHT_PURPLE + upgrade.getDescription() + " " + ChatColor.GRAY + reducedLevel;
						} else {
							formattedUpgrade = ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + upgrade.getDescription() + " " + reducedLevel;
						}
					}
					loreList = new ArrayList<>();
					for (String line : HelperUtil.stringPaginate(upgrade.getLevelDescription(level))) {
						loreList.add(ChatColor.YELLOW + line);
					}
					if (isDisabled) {
						for (String line : HelperUtil.stringPaginate(MessagePath.UPGRADE_DISABLED.getMessage())) {
							loreList.add(ChatColor.RED + line);
						}
					}
					// Create info icon with upgrade info
					info = new InfoIcon(formattedUpgrade, loreList, upgrade.getIcon(), index, false);
					getMenu().getPage(pageNum).addIcon(info);
					index++;
				}
			}
			pageNum++;
		}

		// Page 2+
		pageTotal = getTotalPages(townKnights.size());
		ListIterator<OfflinePlayer> knightIter = townKnights.listIterator();
		for(int i = 0; i < pageTotal; i++) {
			int numPageRows = getNumPageRows(townKnights.size(), i);
			pageLabel = titleColor+MessagePath.LABEL_KNIGHTS.getMessage()+" "+(i+1)+"/"+pageTotal;
			getMenu().addPage(pageNum, numPageRows, pageLabel);
			int slotIndex = 0;
			while(slotIndex < MAX_ICONS_PER_PAGE && knightIter.hasNext()) {
				/* Player Icon (n) */
				OfflinePlayer currentKnight = knightIter.next();
				loreList = new ArrayList<>();
				loreList.add(propertyColor+MessagePath.LABEL_KNIGHT.getMessage());
		    	loreList.add(hintColor+MessagePath.MENU_SCORE_HINT.getMessage());
		    	PlayerIcon player = new PlayerIcon(kingdomColor+currentKnight.getName(),loreList,currentKnight,slotIndex,true,PlayerIconAction.DISPLAY_INFO);
		    	getMenu().getPage(pageNum).addIcon(player);
				slotIndex++;
			}
			pageNum++;
		}
		
		// Page 3+
		pageTotal = getTotalPages(townResidents.size());
		ListIterator<OfflinePlayer> residentIter = townResidents.listIterator();
		for(int i = 0; i < pageTotal; i++) {
			int numPageRows = getNumPageRows(townResidents.size(), i);
			pageLabel = titleColor+MessagePath.LABEL_RESIDENTS.getMessage()+" "+(i+1)+"/"+pageTotal;
			getMenu().addPage(pageNum, numPageRows, pageLabel);
			int slotIndex = 0;
			while(slotIndex < MAX_ICONS_PER_PAGE && residentIter.hasNext()) {
				/* Player Icon (n) */
				OfflinePlayer currentResident = residentIter.next();
				loreList = new ArrayList<>();
				loreList.add(propertyColor+MessagePath.LABEL_RESIDENT.getMessage());
		    	loreList.add(hintColor+MessagePath.MENU_SCORE_HINT.getMessage());
		    	PlayerIcon player = new PlayerIcon(kingdomColor+currentResident.getName(),loreList,currentResident,slotIndex,true,PlayerIconAction.DISPLAY_INFO);
		    	getMenu().getPage(pageNum).addIcon(player);
				slotIndex++;
			}
			pageNum++;
		}
		
		getMenu().refreshNavigationButtons();
		getMenu().setPageIndex(0);
	}

	@Override
	public void onIconClick(KonPlayer clickPlayer, MenuIcon clickedIcon) {
		Player bukkitPlayer = clickPlayer.getBukkitPlayer();
		if(clickedIcon instanceof InfoIcon) {
			// Info Icons close the GUI and print their info in chat
			InfoIcon icon = (InfoIcon)clickedIcon;
			ChatUtil.sendNotice(bukkitPlayer, icon.getInfo());
		} else if(clickedIcon instanceof KingdomIcon) {
			// Kingdom Icons open a new kingdom info menu for the associated player
			KingdomIcon icon = (KingdomIcon)clickedIcon;
			getEarth().getDisplayManager().displayKingdomInfoMenu(clickPlayer,icon.getKingdom());
		} else if(clickedIcon instanceof PlayerIcon) {
			// Player Head Icons open a new info menu for the associated player
			PlayerIcon icon = (PlayerIcon)clickedIcon;
			KonOfflinePlayer offlinePlayer = getEarth().getPlayerManager().getOfflinePlayer(icon.getOfflinePlayer());
			if(offlinePlayer != null && icon.getAction().equals(PlayerIconAction.DISPLAY_INFO)) {
				getEarth().getDisplayManager().displayPlayerInfoMenu(clickPlayer, offlinePlayer);
			} else {
				ChatUtil.sendError(clickPlayer.getBukkitPlayer(),MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
				ChatUtil.printConsoleWarning("Failed to open info menu for unknown player "+icon.getOfflinePlayer().getName()+". Check your SQL database settings in core.yml, the database connection may have been lost or corrupted.");
			}
		}
	}

}
