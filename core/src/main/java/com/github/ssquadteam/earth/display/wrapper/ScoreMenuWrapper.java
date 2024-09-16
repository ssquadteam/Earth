package com.github.ssquadteam.earth.display.wrapper;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.display.icon.InfoIcon;
import com.github.ssquadteam.earth.display.icon.KingdomIcon;
import com.github.ssquadteam.earth.display.icon.MenuIcon;
import com.github.ssquadteam.earth.display.icon.PlayerIcon;
import com.github.ssquadteam.earth.display.icon.PlayerIcon.PlayerIconAction;
import com.github.ssquadteam.earth.manager.DisplayManager;
import com.github.ssquadteam.earth.model.*;
import com.github.ssquadteam.earth.model.KonKingdomScoreAttributes.KonKingdomScoreAttribute;
import com.github.ssquadteam.earth.model.KonPlayerScoreAttributes.KonPlayerScoreAttribute;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class ScoreMenuWrapper extends MenuWrapper {
	
	private final KonOfflinePlayer scorePlayer;
	private final KonPlayer observer;
	
	public ScoreMenuWrapper(Earth earth, KonOfflinePlayer scorePlayer, KonPlayer observer) {
		super(earth);
		this.scorePlayer = scorePlayer;
		this.observer = observer;
	}

	@Override
	public void constructMenu() {
		KonPlayerScoreAttributes playerScoreAttributes = getEarth().getKingdomManager().getPlayerScoreAttributes(scorePlayer);
		KonKingdomScoreAttributes kingdomScoreAttributes = getEarth().getKingdomManager().getKingdomScoreAttributes(scorePlayer.getKingdom());
		int playerScore = playerScoreAttributes.getScore();
		int kingdomScore = kingdomScoreAttributes.getScore();
		String pageLabel;
		int i;
		InfoIcon info;

		String kingdomColor = getEarth().getDisplaySecondaryColor(observer, scorePlayer);
		String titleColor = DisplayManager.titleFormat;
		String loreColor = DisplayManager.loreFormat;
		String valueColor = DisplayManager.valueFormat;
		String hintColor = DisplayManager.hintFormat;
		
		// Page 0
		pageLabel = titleColor+scorePlayer.getOfflineBukkitPlayer().getName()+" "+MessagePath.LABEL_SCORE.getMessage()+": "+playerScore;
		getMenu().addPage(0, 1, pageLabel);
		PlayerIcon playerInfo = new PlayerIcon(kingdomColor+scorePlayer.getOfflineBukkitPlayer().getName(),Arrays.asList(loreColor+MessagePath.LABEL_INFORMATION.getMessage(), hintColor+MessagePath.MENU_SCORE_HINT.getMessage()),scorePlayer.getOfflineBukkitPlayer(),1,true,PlayerIconAction.DISPLAY_INFO);
		getMenu().getPage(0).addIcon(playerInfo);
		info = new InfoIcon(kingdomColor+MessagePath.MENU_SCORE_TOWN_1.getMessage(), Arrays.asList(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+playerScoreAttributes.getAttributeValue(KonPlayerScoreAttribute.TOWN_LORDS), loreColor+MessagePath.LABEL_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+playerScoreAttributes.getAttributeScore(KonPlayerScoreAttribute.TOWN_LORDS)), Material.PURPLE_CONCRETE, 2, false);
		getMenu().getPage(0).addIcon(info);
		info = new InfoIcon(kingdomColor+MessagePath.MENU_SCORE_LAND_1.getMessage(), Arrays.asList(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+playerScoreAttributes.getAttributeValue(KonPlayerScoreAttribute.LAND_LORDS), loreColor+MessagePath.LABEL_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+playerScoreAttributes.getAttributeScore(KonPlayerScoreAttribute.LAND_LORDS)), Material.PURPLE_CARPET, 3, false);
		getMenu().getPage(0).addIcon(info);
		info = new InfoIcon(kingdomColor+MessagePath.MENU_SCORE_TOWN_2.getMessage(), Arrays.asList(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+playerScoreAttributes.getAttributeValue(KonPlayerScoreAttribute.TOWN_KNIGHTS), loreColor+MessagePath.LABEL_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+playerScoreAttributes.getAttributeScore(KonPlayerScoreAttribute.TOWN_KNIGHTS)), Material.BLUE_CONCRETE, 4, false);
		getMenu().getPage(0).addIcon(info);
		info = new InfoIcon(kingdomColor+MessagePath.MENU_SCORE_LAND_2.getMessage(), Arrays.asList(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+playerScoreAttributes.getAttributeValue(KonPlayerScoreAttribute.LAND_KNIGHTS), loreColor+MessagePath.LABEL_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+playerScoreAttributes.getAttributeScore(KonPlayerScoreAttribute.LAND_KNIGHTS)), Material.BLUE_CARPET, 5, false);
		getMenu().getPage(0).addIcon(info);
		info = new InfoIcon(kingdomColor+MessagePath.MENU_SCORE_TOWN_3.getMessage(), Arrays.asList(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+playerScoreAttributes.getAttributeValue(KonPlayerScoreAttribute.TOWN_RESIDENTS), loreColor+MessagePath.LABEL_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+playerScoreAttributes.getAttributeScore(KonPlayerScoreAttribute.TOWN_RESIDENTS)), Material.WHITE_CONCRETE, 6, false);
		getMenu().getPage(0).addIcon(info);
		info = new InfoIcon(kingdomColor+MessagePath.MENU_SCORE_LAND_3.getMessage(), Arrays.asList(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+playerScoreAttributes.getAttributeValue(KonPlayerScoreAttribute.LAND_RESIDENTS), loreColor+MessagePath.LABEL_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+playerScoreAttributes.getAttributeScore(KonPlayerScoreAttribute.LAND_RESIDENTS)), Material.WHITE_CARPET, 7, false);
		getMenu().getPage(0).addIcon(info);
		// Page 1
		pageLabel = titleColor+scorePlayer.getKingdom().getName()+" "+MessagePath.LABEL_SCORE.getMessage()+": "+kingdomScore;
		getMenu().addPage(1, 1, pageLabel);
		KingdomIcon kingdomInfo = new KingdomIcon(scorePlayer.getKingdom(),kingdomColor,Arrays.asList(loreColor+MessagePath.LABEL_INFORMATION.getMessage(), hintColor+MessagePath.MENU_SCORE_HINT.getMessage()),2,false);
		getMenu().getPage(1).addIcon(kingdomInfo);
		info = new InfoIcon(kingdomColor+MessagePath.MENU_SCORE_KINGDOM_TOWNS.getMessage(), Arrays.asList(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+kingdomScoreAttributes.getAttributeValue(KonKingdomScoreAttribute.TOWNS), loreColor+MessagePath.LABEL_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+kingdomScoreAttributes.getAttributeScore(KonKingdomScoreAttribute.TOWNS)), Material.OBSIDIAN, 3, false);
		getMenu().getPage(1).addIcon(info);
		info = new InfoIcon(kingdomColor+MessagePath.MENU_SCORE_KINGDOM_LAND.getMessage(), Arrays.asList(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+kingdomScoreAttributes.getAttributeValue(KonKingdomScoreAttribute.LAND), loreColor+MessagePath.LABEL_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+kingdomScoreAttributes.getAttributeScore(KonKingdomScoreAttribute.LAND)), Material.GRASS_BLOCK, 4, false);
		getMenu().getPage(1).addIcon(info);
		info = new InfoIcon(kingdomColor+MessagePath.MENU_SCORE_KINGDOM_FAVOR.getMessage(), Arrays.asList(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+kingdomScoreAttributes.getAttributeValue(KonKingdomScoreAttribute.FAVOR), loreColor+MessagePath.LABEL_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+kingdomScoreAttributes.getAttributeScore(KonKingdomScoreAttribute.FAVOR)), Material.GOLD_BLOCK, 5, false);
		getMenu().getPage(1).addIcon(info);
		info = new InfoIcon(kingdomColor+MessagePath.MENU_SCORE_KINGDOM_POPULATION.getMessage(), Arrays.asList(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+kingdomScoreAttributes.getAttributeValue(KonKingdomScoreAttribute.POPULATION), loreColor+MessagePath.LABEL_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+kingdomScoreAttributes.getAttributeScore(KonKingdomScoreAttribute.POPULATION)), Material.PLAYER_HEAD, 6, false);
		getMenu().getPage(1).addIcon(info);
		// Page 2
		pageLabel = titleColor+scorePlayer.getOfflineBukkitPlayer().getName()+" "+MessagePath.LABEL_STATS.getMessage();
		getMenu().addPage(2, 3, pageLabel);
		KonPlayer player = getEarth().getPlayerManager().getPlayerFromName(scorePlayer.getOfflineBukkitPlayer().getName());
		KonStats stats;
    	if(player == null) {
    		// Use offline player, pull stats from DB
    		stats = getEarth().getDatabaseThread().getDatabase().pullPlayerStats(scorePlayer.getOfflineBukkitPlayer());
    	} else {
    		// Use online player's active stats
    		stats = player.getPlayerStats();
		}
    	i = 0;
    	int statValue;
    	for(KonStatsType stat : KonStatsType.values()) {
    		statValue = stats.getStat(stat);
    		info = new InfoIcon(ChatColor.GOLD+stat.displayName(),Arrays.asList(loreColor+stat.description(),valueColor+""+statValue),stat.getMaterial(),i,false);
    		getMenu().getPage(2).addIcon(info);
    		i++;
    	}
		// Page 3
		pageLabel = titleColor+scorePlayer.getKingdom().getName()+" "+MessagePath.LABEL_LEADERBOARD.getMessage();
		getMenu().addPage(3, 1, pageLabel);
		KonLeaderboard leaderboard = getEarth().getKingdomManager().getKingdomLeaderboard(scorePlayer.getKingdom());
		if(!leaderboard.isEmpty()) {
			int numEntries = 9;
			if(leaderboard.getSize() < numEntries) {
				numEntries = leaderboard.getSize();
			}
			for(int n = 0;n<numEntries;n++) {
				int rank = n + 1;
				PlayerIcon leader = new PlayerIcon(ChatColor.GOLD+"#"+rank+" "+kingdomColor+leaderboard.getName(n),Arrays.asList(loreColor+MessagePath.MENU_SCORE_PLAYER_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+leaderboard.getScore(n),hintColor+MessagePath.MENU_SCORE_HINT.getMessage()),leaderboard.getOfflinePlayer(n),n,true,PlayerIconAction.DISPLAY_SCORE);
				getMenu().getPage(3).addIcon(leader);
			}
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
			// Player Head Icons open a new info menu for the associated player
			PlayerIcon icon = (PlayerIcon)clickedIcon;
			KonOfflinePlayer offlinePlayer = getEarth().getPlayerManager().getOfflinePlayer(icon.getOfflinePlayer());
			if(offlinePlayer != null) {
				switch(icon.getAction()) {
					case DISPLAY_INFO:
						getEarth().getDisplayManager().displayPlayerInfoMenu(clickPlayer, offlinePlayer);
						break;
					case DISPLAY_SCORE:
						getEarth().getDisplayManager().displayScoreMenu(clickPlayer, offlinePlayer);
						break;
					default:
						break;
				}
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
