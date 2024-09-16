package com.github.ssquadteam.earth.display.wrapper;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.EarthPlugin;
import com.github.ssquadteam.earth.api.model.EarthDiplomacyType;
import com.github.ssquadteam.earth.display.icon.*;
import com.github.ssquadteam.earth.display.icon.PlayerIcon.PlayerIconAction;
import com.github.ssquadteam.earth.manager.DisplayManager;
import com.github.ssquadteam.earth.model.*;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.Labeler;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

public class KingdomInfoMenuWrapper extends MenuWrapper {

	private final KonKingdom infoKingdom;
	private final KonPlayer observer;
	
	public KingdomInfoMenuWrapper(Earth earth, KonKingdom infoKingdom, KonPlayer observer) {
		super(earth);
		this.infoKingdom = infoKingdom;
		this.observer = observer;
	}

	@Override
	public void constructMenu() {

		EarthDiplomacyType currentDiplomacy = getEarth().getKingdomManager().getDiplomacy(observer.getKingdom(),infoKingdom);
		String kingdomColor = getEarth().getDisplaySecondaryColor(observer.getKingdom(), infoKingdom);
		String titleColor = DisplayManager.titleFormat;
		String loreColor = DisplayManager.loreFormat;
		String valueColor = DisplayManager.valueFormat;
		String hintColor = DisplayManager.hintFormat;
		String propertyColor = DisplayManager.propertyFormat;
		String alertColor = DisplayManager.alertFormat;
		
		String pageLabel;
 		List<String> loreList;
 		MenuIcon info;
		int slotIndex;
		int pageIndex = 0;
		int pageRows = 1;
		int pageTotal = 1;

		// Limit which icons appear if the kingdom is not created (e.g. a default Neutrals or Barbarian)

		OfflinePlayer masterPlayer = infoKingdom.getPlayerMaster();
 		List<OfflinePlayer> allKingdomMembers = new ArrayList<>();
		if(infoKingdom.isCreated()) {
			if(masterPlayer != null) {
				allKingdomMembers.add(masterPlayer);
			}
			allKingdomMembers.addAll(infoKingdom.getPlayerOfficersOnly());
			allKingdomMembers.addAll(infoKingdom.getPlayerMembersOnly());
		} else {
			allKingdomMembers.addAll(getEarth().getPlayerManager().getAllBukkitPlayersInKingdom(infoKingdom));
		}

 		// Page 0
		pageLabel = titleColor+MessagePath.COMMAND_INFO_NOTICE_KINGDOM_HEADER.getMessage(infoKingdom.getName());
		getMenu().addPage(pageIndex, pageRows, pageLabel);

		/* Capital Info Icon (0) */
		if(infoKingdom.isCreated()) {
			KonCapital capital = infoKingdom.getCapital();
			loreList = new ArrayList<>();
			loreList.add(hintColor + MessagePath.MENU_SCORE_HINT.getMessage());
			TownIcon capitalIcon = new TownIcon(capital, kingdomColor, loreList, 0, true);
			getMenu().getPage(pageIndex).addIcon(capitalIcon);
		}

		/* Master Player Info Icon (1) */
		if(infoKingdom.isCreated()) {
			loreList = new ArrayList<>();
			if (infoKingdom.isMasterValid()) {
				loreList.add(propertyColor + MessagePath.LABEL_MASTER.getMessage());
				loreList.add(hintColor + MessagePath.MENU_SCORE_HINT.getMessage());
				assert masterPlayer != null;
				PlayerIcon playerInfo = new PlayerIcon(kingdomColor + masterPlayer.getName(), loreList, masterPlayer, 1, true, PlayerIconAction.DISPLAY_INFO);
				getMenu().getPage(pageIndex).addIcon(playerInfo);
			} else {
				info = new InfoIcon(ChatColor.DARK_RED + MessagePath.LABEL_MASTER.getMessage(), loreList, Material.BARRIER, 1, false);
				getMenu().getPage(pageIndex).addIcon(info);
			}
		}

		/* Relationship Icon (3) */
		loreList = new ArrayList<>();
		if(observer.getKingdom().equals(infoKingdom)) {
			loreList.add(loreColor+MessagePath.DIPLOMACY_SELF.getMessage());
		} else {
			String diplomacyState = Labeler.lookup(currentDiplomacy);
			loreList.add(loreColor+MessagePath.LABEL_DIPLOMACY.getMessage()+": "+valueColor+diplomacyState);
			if(observer.getKingdom().hasRelationRequest(infoKingdom)) {
				// They have sent a valid diplomacy change request to us
				String ourRequestStatus = Labeler.lookup(observer.getKingdom().getRelationRequest(infoKingdom));
				loreList.add(alertColor+MessagePath.MENU_KINGDOM_THEY_REQUESTED.getMessage()+": "+valueColor+ourRequestStatus);
			}
			if(infoKingdom.hasRelationRequest(observer.getKingdom())) {
				// We have sent a valid diplomacy change request to them
				String theirRequestStatus = Labeler.lookup(infoKingdom.getRelationRequest(observer.getKingdom()));
				loreList.add(alertColor+MessagePath.MENU_KINGDOM_WE_REQUESTED.getMessage()+": "+valueColor+theirRequestStatus);
			}
		}
		InfoIcon relationIcon = new InfoIcon(kingdomColor+MessagePath.MENU_KINGDOM_RELATION.getMessage(),loreList,Material.GOLDEN_SWORD,3,false);
		getMenu().getPage(pageIndex).addIcon(relationIcon);

		/* Member Info Icon (4) */
		int numKingdomPlayers = getEarth().getPlayerManager().getPlayersInKingdom(infoKingdom).size();
    	int numAllKingdomPlayers = allKingdomMembers.size();
    	int numKingdomOfficers = infoKingdom.getPlayerOfficersOnly().size();
    	loreList = new ArrayList<>();
    	loreList.add(loreColor+MessagePath.LABEL_ONLINE_PLAYERS.getMessage()+": "+valueColor+numKingdomPlayers);
    	loreList.add(loreColor+MessagePath.LABEL_TOTAL_PLAYERS.getMessage()+": "+valueColor+numAllKingdomPlayers);
    	loreList.add(loreColor+MessagePath.LABEL_OFFICERS.getMessage()+": "+valueColor+numKingdomOfficers);
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_PLAYERS.getMessage(), loreList, Material.NAME_TAG, 4, false);
    	getMenu().getPage(pageIndex).addIcon(info);

		/* Flags Info Icon (5) */
		loreList = new ArrayList<>();
		for(KonPropertyFlag flag : KonPropertyFlag.values()) {
			if(infoKingdom.hasPropertyValue(flag)) {
				String flagDisplaySymbol = DisplayManager.boolean2Symbol(infoKingdom.getPropertyValue(flag));
				loreList.add(loreColor+flag.getName()+": "+flagDisplaySymbol);
			}
		}
		InfoIcon propertyInfo = new InfoIcon(kingdomColor+MessagePath.LABEL_FLAGS.getMessage(), loreList, Material.REDSTONE_TORCH, 5, false);
		getMenu().getPage(0).addIcon(propertyInfo);

    	/* Properties Info Icon (6) */
    	String isSmallest = DisplayManager.boolean2Symbol(infoKingdom.isSmallest());
    	String isProtected = DisplayManager.boolean2Symbol(infoKingdom.isOfflineProtected());
    	String isOpen = DisplayManager.boolean2Symbol(infoKingdom.isOpen());
    	String isAdminOperated = DisplayManager.boolean2Symbol(infoKingdom.isAdminOperated());
    	loreList = new ArrayList<>();
		loreList.add(loreColor+MessagePath.LABEL_ADMIN_KINGDOM.getMessage()+": "+isAdminOperated);
		loreList.add(loreColor+MessagePath.LABEL_PROTECTED.getMessage()+": "+isProtected);
		loreList.add(loreColor+MessagePath.LABEL_OPEN.getMessage()+": "+isOpen);
		loreList.add(loreColor+MessagePath.LABEL_SMALLEST.getMessage()+": "+isSmallest);
		info = new InfoIcon(kingdomColor+MessagePath.LABEL_PROPERTIES.getMessage(), loreList, Material.PAPER, 6, false);
    	getMenu().getPage(pageIndex).addIcon(info);

		/* Stats Info Icon (7) */
		ArrayList<KonOfflinePlayer> allPlayersInKingdom = getEarth().getPlayerManager().getAllPlayersInKingdom(infoKingdom);
		int numKingdomFavor = 0;
		for(KonOfflinePlayer kingdomPlayer : allPlayersInKingdom) {
			numKingdomFavor += (int) EarthPlugin.getBalance(kingdomPlayer.getOfflineBukkitPlayer());
		}
		int numKingdomTowns = infoKingdom.getTowns().size();
		int numKingdomLand = 0;
		for(KonTown town : infoKingdom.getCapitalTowns()) {
			numKingdomLand += town.getNumLand();
		}
		loreList = new ArrayList<>();
		loreList.add(loreColor+MessagePath.LABEL_FAVOR.getMessage()+": "+valueColor+numKingdomFavor);
		loreList.add(loreColor+MessagePath.LABEL_TOWNS.getMessage()+": "+valueColor+numKingdomTowns);
		loreList.add(loreColor+MessagePath.LABEL_LAND.getMessage()+": "+valueColor+numKingdomLand);
		info = new InfoIcon(kingdomColor+MessagePath.LABEL_INFORMATION.getMessage(), loreList, Material.ENDER_EYE, 7, false);
		getMenu().getPage(pageIndex).addIcon(info);

		/* Template Info Icon (8) */
		if(infoKingdom.isCreated()) {
			loreList = new ArrayList<>();
			if (infoKingdom.hasMonumentTemplate()) {
				KonMonumentTemplate template = infoKingdom.getMonumentTemplate();
				info = new TemplateIcon(template,kingdomColor,loreList,8,false);
			} else {
				info = new InfoIcon(ChatColor.RED+MessagePath.LABEL_INVALID.getMessage(), loreList, Material.BARRIER, 8, false);
			}
			getMenu().getPage(pageIndex).addIcon(info);
		}
		pageIndex++;

    	// Pages for created kingdoms
		if(infoKingdom.isCreated()) {

			/* Enemy Kingdoms */
			List<KonKingdom> enemyKingdoms = infoKingdom.getActiveRelationKingdoms(EarthDiplomacyType.WAR);
			enemyKingdoms.sort(kingdomComparator);
			pageTotal = getTotalPages(enemyKingdoms.size());
			ListIterator<KonKingdom> enemyIterator = enemyKingdoms.listIterator();
			for (int i = 0; i < pageTotal; i++) {
				int numPageRows = getNumPageRows(enemyKingdoms.size(),i);
				pageLabel = titleColor + MessagePath.DIPLOMACY_WAR.getMessage() + " " + (i + 1) + "/" + pageTotal;
				getMenu().addPage(pageIndex, numPageRows, pageLabel);
				slotIndex = 0;
				while (slotIndex < MAX_ICONS_PER_PAGE && enemyIterator.hasNext()) {
					/* Kingdom Icon (n) */
					KonKingdom currentKingdom = enemyIterator.next();
					loreList = new ArrayList<>();
					loreList.add(hintColor + MessagePath.MENU_SCORE_HINT.getMessage());
					KingdomIcon kingdomIcon = new KingdomIcon(currentKingdom,Earth.enemyColor2,loreList,slotIndex,true);
					getMenu().getPage(pageIndex).addIcon(kingdomIcon);
					slotIndex++;
				}
				pageIndex++;
			}

			/* Allied Kingdoms */
			List<KonKingdom> allyKingdoms = infoKingdom.getActiveRelationKingdoms(EarthDiplomacyType.ALLIANCE);
			allyKingdoms.sort(kingdomComparator);
			pageTotal = getTotalPages(allyKingdoms.size());
			ListIterator<KonKingdom> allyIterator = allyKingdoms.listIterator();
			for (int i = 0; i < pageTotal; i++) {
				int numPageRows = getNumPageRows(allyKingdoms.size(),i);
				pageLabel = titleColor + MessagePath.DIPLOMACY_ALLIANCE.getMessage() + " " + (i + 1) + "/" + pageTotal;
				getMenu().addPage(pageIndex, numPageRows, pageLabel);
				slotIndex = 0;
				while (slotIndex < MAX_ICONS_PER_PAGE && allyIterator.hasNext()) {
					/* Kingdom Icon (n) */
					KonKingdom currentKingdom = allyIterator.next();
					loreList = new ArrayList<>();
					loreList.add(hintColor + MessagePath.MENU_SCORE_HINT.getMessage());
					KingdomIcon kingdomIcon = new KingdomIcon(currentKingdom,Earth.alliedColor2,loreList,slotIndex,true);
					getMenu().getPage(pageIndex).addIcon(kingdomIcon);
					slotIndex++;
				}
				pageIndex++;
			}

			/* Trade Kingdoms */
			List<KonKingdom> tradeKingdoms = infoKingdom.getActiveRelationKingdoms(EarthDiplomacyType.TRADE);
			tradeKingdoms.sort(kingdomComparator);
			pageTotal = getTotalPages(tradeKingdoms.size());
			ListIterator<KonKingdom> tradeIterator = tradeKingdoms.listIterator();
			for (int i = 0; i < pageTotal; i++) {
				int numPageRows = getNumPageRows(tradeKingdoms.size(),i);
				pageLabel = titleColor + MessagePath.DIPLOMACY_TRADE.getMessage() + " " + (i + 1) + "/" + pageTotal;
				getMenu().addPage(pageIndex, numPageRows, pageLabel);
				slotIndex = 0;
				while (slotIndex < MAX_ICONS_PER_PAGE && tradeIterator.hasNext()) {
					/* Kingdom Icon (n) */
					KonKingdom currentKingdom = tradeIterator.next();
					loreList = new ArrayList<>();
					loreList.add(hintColor + MessagePath.MENU_SCORE_HINT.getMessage());
					KingdomIcon kingdomIcon = new KingdomIcon(currentKingdom,Earth.tradeColor2,loreList,slotIndex,true);
					getMenu().getPage(pageIndex).addIcon(kingdomIcon);
					slotIndex++;
				}
				pageIndex++;
			}

			/* Town List */
			List<KonTown> kingdomTowns = infoKingdom.getTowns();
			kingdomTowns.sort(townComparator);
			pageTotal = getTotalPages(kingdomTowns.size());
			ListIterator<KonTown> townIter = kingdomTowns.listIterator();
			for (int i = 0; i < pageTotal; i++) {
				int numPageRows = getNumPageRows(kingdomTowns.size(),i);
				pageLabel = titleColor + MessagePath.LABEL_TOWNS.getMessage() + " " + (i + 1) + "/" + pageTotal;
				getMenu().addPage(pageIndex, numPageRows, pageLabel);
				slotIndex = 0;
				while (slotIndex < MAX_ICONS_PER_PAGE && townIter.hasNext()) {
					/* Town Icon (n) */
					KonTown currentTown = townIter.next();
					loreList = new ArrayList<>();
					loreList.add(hintColor + MessagePath.MENU_SCORE_HINT.getMessage());
					TownIcon townIcon = new TownIcon(currentTown, kingdomColor, loreList, slotIndex, true);
					getMenu().getPage(pageIndex).addIcon(townIcon);
					slotIndex++;
				}
				pageIndex++;
			}
		}

		// Page 2+
		// All kingdom members
		pageTotal = (int)Math.ceil(((double)allKingdomMembers.size())/MAX_ICONS_PER_PAGE);
		if(pageTotal == 0) {
			pageTotal = 1;
		}
		ListIterator<OfflinePlayer> memberIter = allKingdomMembers.listIterator();
		for(int i = 0; i < pageTotal; i++) {
			int numPageRows = (int)Math.ceil(((double)(allKingdomMembers.size() - i*MAX_ICONS_PER_PAGE))/9);
			if(numPageRows < 1) {
				numPageRows = 1;
			} else if(numPageRows > 5) {
				numPageRows = 5;
			}
			pageLabel = titleColor+MessagePath.LABEL_PLAYERS.getMessage()+" "+(i+1)+"/"+pageTotal;
			getMenu().addPage(pageIndex, numPageRows, pageLabel);
			slotIndex = 0;
			while(slotIndex < MAX_ICONS_PER_PAGE && memberIter.hasNext()) {
				/* Player Icon (n) */
				OfflinePlayer currentMember = memberIter.next();
				if(currentMember != null) {
					loreList = new ArrayList<>();
					if(infoKingdom.isCreated()) {
						loreList.add(propertyColor+infoKingdom.getPlayerRoleName(currentMember));
					}
					loreList.add(hintColor+MessagePath.MENU_SCORE_HINT.getMessage());
					PlayerIcon player = new PlayerIcon(kingdomColor+currentMember.getName(),loreList,currentMember,slotIndex,true,PlayerIconAction.DISPLAY_INFO);
					getMenu().getPage(pageIndex).addIcon(player);
					slotIndex++;
				}
			}
			pageIndex++;
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
		} else if(clickedIcon instanceof TownIcon) {
			// Town Icons open a new town info menu for the associated player
			TownIcon icon = (TownIcon)clickedIcon;
			getEarth().getDisplayManager().displayTownInfoMenu(clickPlayer,icon.getTown());
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
		} else if(clickedIcon instanceof KingdomIcon) {
			// Kingdom Icons open a new kingdom info menu for the associated player
			KingdomIcon icon = (KingdomIcon)clickedIcon;
			getEarth().getDisplayManager().displayKingdomInfoMenu(clickPlayer,icon.getKingdom());
		}
	}

}
