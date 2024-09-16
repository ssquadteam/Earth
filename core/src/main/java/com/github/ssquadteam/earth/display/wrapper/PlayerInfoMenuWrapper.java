package com.github.ssquadteam.earth.display.wrapper;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.EarthPlugin;
import com.github.ssquadteam.earth.display.icon.*;
import com.github.ssquadteam.earth.display.icon.PlayerIcon.PlayerIconAction;
import com.github.ssquadteam.earth.manager.DisplayManager;
import com.github.ssquadteam.earth.model.KonOfflinePlayer;
import com.github.ssquadteam.earth.model.KonPlayer;
import com.github.ssquadteam.earth.model.KonTown;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.HelperUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

public class PlayerInfoMenuWrapper extends MenuWrapper {

	private final KonOfflinePlayer infoPlayer;
	private final KonPlayer observer;
	
	public PlayerInfoMenuWrapper(Earth earth, KonOfflinePlayer infoPlayer, KonPlayer observer) {
		super(earth);
		this.infoPlayer = infoPlayer;
		this.observer = observer;
	}

	@Override
	public void constructMenu() {

 		String kingdomColor = getEarth().getDisplaySecondaryColor(observer, infoPlayer);
		String titleColor = DisplayManager.titleFormat;
		String loreColor = DisplayManager.loreFormat;
		String valueColor = DisplayManager.valueFormat;
		String hintColor = DisplayManager.hintFormat;

		String pageLabel;
		List<String> loreList;
 		
 		// Page 0
		pageLabel = titleColor+MessagePath.LABEL_PLAYER.getMessage()+" "+infoPlayer.getOfflineBukkitPlayer().getName();
		getMenu().addPage(0, 1, pageLabel);

		/* Kingdom Icon (2) */
		loreList = new ArrayList<>();
    	loreList.add(hintColor+MessagePath.MENU_SCORE_HINT.getMessage());
    	KingdomIcon kingdom = new KingdomIcon(infoPlayer.getKingdom(),kingdomColor,loreList,2,true);
    	getMenu().getPage(0).addIcon(kingdom);
		
		/* Player Score Icon (3) */
		int score = getEarth().getKingdomManager().getPlayerScore(infoPlayer);
		loreList = new ArrayList<>();
		loreList.add(loreColor+MessagePath.MENU_SCORE_PLAYER_SCORE.getMessage()+": "+valueColor+score);
		loreList.add(hintColor+MessagePath.MENU_SCORE_HINT.getMessage());
		PlayerIcon playerScore = new PlayerIcon(kingdomColor+infoPlayer.getOfflineBukkitPlayer().getName(),loreList,infoPlayer.getOfflineBukkitPlayer(),3,true,PlayerIconAction.DISPLAY_SCORE);
		getMenu().getPage(0).addIcon(playerScore);

		/* General Info Icon (5) */
		String balanceF = String.format("%.2f",EarthPlugin.getBalance(infoPlayer.getOfflineBukkitPlayer()));
		int numResidentTowns = getEarth().getKingdomManager().getPlayerResidencies(infoPlayer);
		String kingdomName = infoPlayer.getKingdom().getName();
		loreList = new ArrayList<>();
		loreList.add(loreColor + MessagePath.LABEL_KINGDOM.getMessage() + ": " + valueColor + kingdomName);
		if(infoPlayer.getKingdom().isCreated()) {
			String kingdomRole = infoPlayer.getKingdom().getPlayerRoleName(infoPlayer);
			loreList.add(loreColor + MessagePath.LABEL_KINGDOM_ROLE.getMessage() + ": " + valueColor + kingdomRole);
		}
		loreList.add(loreColor + MessagePath.LABEL_FAVOR.getMessage() + ": " + valueColor + balanceF);
		loreList.add(loreColor + MessagePath.LABEL_RESIDENCIES.getMessage() + ": " + valueColor + numResidentTowns);
		InfoIcon info = new InfoIcon(kingdomColor+MessagePath.LABEL_INFORMATION.getMessage(), loreList, Material.ENDER_EYE, 5, false);
		getMenu().getPage(0).addIcon(info);

		/* Camp Icon (6) */
		if(infoPlayer.isBarbarian()) {
			loreList = new ArrayList<>();
			if(getEarth().getCampManager().isCampSet(infoPlayer)) {
				for(String line : HelperUtil.stringPaginate(MessagePath.COMMAND_INFO_NOTICE_CAMP_PLACED.getMessage())) {
					loreList.add(loreColor+line);
				}
			} else {
				for(String line : HelperUtil.stringPaginate(MessagePath.COMMAND_INFO_NOTICE_CAMP_MISSING.getMessage())) {
					loreList.add(loreColor+line);
				}
			}
			InfoIcon campInfo = new InfoIcon(kingdomColor+MessagePath.LABEL_CAMP.getMessage(), loreList, Material.YELLOW_BED, 6, false);
			getMenu().getPage(0).addIcon(campInfo);
		}

		// Page 1+
		List<KonTown> playerTowns = sortedTowns(infoPlayer);
		final int MAX_ICONS_PER_PAGE = 45;
		int pageTotal = (int)Math.ceil(((double)playerTowns.size())/MAX_ICONS_PER_PAGE);
		if(pageTotal == 0) {
			pageTotal = 1;
		}
		int pageNum = 1;
		ListIterator<KonTown> townIter = playerTowns.listIterator();
		for(int i = 0; i < pageTotal; i++) {
			int numPageRows = (int)Math.ceil(((double)(playerTowns.size() - i*MAX_ICONS_PER_PAGE))/9);
			if(numPageRows < 1) {
				numPageRows = 1;
			} else if(numPageRows > 5) {
				numPageRows = 5;
			}
			pageLabel = titleColor+MessagePath.LABEL_RESIDENCIES.getMessage()+" "+(i+1)+"/"+pageTotal;
			getMenu().addPage(pageNum, numPageRows, pageLabel);
			int slotIndex = 0;
			while(slotIndex < MAX_ICONS_PER_PAGE && townIter.hasNext()) {
				/* Town Icon (n) */
				KonTown currentTown = townIter.next();
				loreList = new ArrayList<>();
				loreList.add(currentTown.getPlayerRoleName(infoPlayer));
		    	loreList.add(hintColor+MessagePath.MENU_SCORE_HINT.getMessage());
		    	TownIcon town = new TownIcon(currentTown,kingdomColor,loreList,slotIndex,true);
		    	getMenu().getPage(pageNum).addIcon(town);
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
		} else if(clickedIcon instanceof PlayerIcon) {
			// Player Head Icons open a new score menu for the associated player
			PlayerIcon icon = (PlayerIcon)clickedIcon;
			KonOfflinePlayer offlinePlayer = getEarth().getPlayerManager().getOfflinePlayer(icon.getOfflinePlayer());
			if(offlinePlayer != null && icon.getAction().equals(PlayerIconAction.DISPLAY_SCORE)) {
				getEarth().getDisplayManager().displayScoreMenu(clickPlayer, offlinePlayer);
			} else {
				ChatUtil.sendError(clickPlayer.getBukkitPlayer(),MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
				ChatUtil.printConsoleWarning("Failed to open score menu for unknown player "+icon.getOfflinePlayer().getName()+". Check your SQL database settings in core.yml, the database connection may have been lost or corrupted.");
			}
		} else if(clickedIcon instanceof KingdomIcon) {
			// Kingdom Icons open a new kingdom info menu for the associated player
			KingdomIcon icon = (KingdomIcon)clickedIcon;
			getEarth().getDisplayManager().displayKingdomInfoMenu(clickPlayer,icon.getKingdom());
		} else if(clickedIcon instanceof TownIcon) {
			// Town Icons open a new town info menu for the associated player
			TownIcon icon = (TownIcon)clickedIcon;
			getEarth().getDisplayManager().displayTownInfoMenu(clickPlayer,icon.getTown());
		}
	}
	
	// Sort player town list by Lord, Knight, Resident, and then by population, and then by size
 	private List<KonTown> sortedTowns(KonOfflinePlayer player) {
 		List<KonTown> sortedTowns = new ArrayList<>();
 		// Determine town group lists
 		List<KonTown> lordTowns = new ArrayList<>();
 		List<KonTown> knightTowns = new ArrayList<>();
 		List<KonTown> residentTowns = new ArrayList<>();
 		for(KonTown town : getEarth().getKingdomManager().getPlayerResidenceTowns(player)) {
 			if(town.isPlayerLord(player.getOfflineBukkitPlayer())) {
 				lordTowns.add(town);
 			} else if(town.isPlayerKnight(player.getOfflineBukkitPlayer())) {
 				knightTowns.add(town);
 			} else {
 				residentTowns.add(town);
 			}
 		}
 		lordTowns.sort(townComparator);
 		knightTowns.sort(townComparator);
 		residentTowns.sort(townComparator);
 		// Add sorted towns to result list
 		sortedTowns.addAll(lordTowns);
 		sortedTowns.addAll(knightTowns);
 		sortedTowns.addAll(residentTowns);
 		
 		return sortedTowns;
 	}

}
