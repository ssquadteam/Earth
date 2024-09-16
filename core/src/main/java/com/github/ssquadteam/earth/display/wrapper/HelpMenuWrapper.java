package com.github.ssquadteam.earth.display.wrapper;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.command.CommandType;
import com.github.ssquadteam.earth.command.admin.AdminCommandType;
import com.github.ssquadteam.earth.display.icon.AdminCommandIcon;
import com.github.ssquadteam.earth.display.icon.CommandIcon;
import com.github.ssquadteam.earth.display.icon.InfoIcon;
import com.github.ssquadteam.earth.display.icon.MenuIcon;
import com.github.ssquadteam.earth.manager.DisplayManager;
import com.github.ssquadteam.earth.model.KonPlayer;
import com.github.ssquadteam.earth.utility.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HelpMenuWrapper extends MenuWrapper {

	private final Player observer;

	public HelpMenuWrapper(Earth earth, Player observer) {
		super(earth);
		this.observer = observer;
	}
	
	@Override
	public void constructMenu() {

		int cost;
		int cost_incr;

		double cost_spy = getEarth().getCore().getDouble(CorePath.FAVOR_COST_SPY.getPath(),0.0);
		double cost_settle = getEarth().getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SETTLE.getPath(),0.0);
		double cost_settle_incr = getEarth().getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SETTLE_INCREMENT.getPath(),0.0);
		double cost_claim = getEarth().getCore().getDouble(CorePath.FAVOR_COST_CLAIM.getPath(),0.0);
		double cost_travel = getEarth().getCore().getDouble(CorePath.FAVOR_COST_TRAVEL.getPath(),0.0);
    	String communityLink = getEarth().getCore().getString(CorePath.COMMUNITY_LINK.getPath(),"");

		String titleColor = DisplayManager.titleFormat;

		String pageLabel;
		List<String> loreList;
		MenuIcon icon;
		int slotIndex;
		int pageIndex;
		int pageRows;

		// Page 0 - Player Commands
		pageLabel = titleColor+MessagePath.MENU_HELP_TITLE.getMessage();
		pageRows = (int)Math.ceil(((double)(CommandType.values().length))/9);
		pageIndex = 0;
		slotIndex = 0;
		getMenu().addPage(pageIndex, pageRows, pageLabel);
    	// Add command icons
		for(CommandType cmd : CommandType.values()) {
			switch (cmd) {
				case SPY:
					cost = (int)cost_spy;
					cost_incr = 0;
					break;
				case SETTLE:
					cost = (int)cost_settle;
					cost_incr = (int)cost_settle_incr;
					break;
				case CLAIM:
					cost = (int)cost_claim;
					cost_incr = 0;
					break;
				case TRAVEL:
					cost = (int)cost_travel;
					cost_incr = 0;
					break;
				default:
					cost = 0;
					cost_incr = 0;
					break;
			}
			icon = new CommandIcon(cmd, cost, cost_incr, slotIndex);
			getMenu().getPage(pageIndex).addIcon(icon);
			slotIndex++;
		}
		// Add info icons
		loreList = new ArrayList<>();
		InfoIcon info = new InfoIcon(MessagePath.MENU_HELP_COMMUNITY.getMessage(), loreList, Material.MINECART, slotIndex, true);
		info.setInfo(ChatColor.GOLD+MessagePath.MENU_HELP_HINT.getMessage()+": "+ChatColor.LIGHT_PURPLE+ChatColor.UNDERLINE+communityLink);
		getMenu().getPage(pageIndex).addIcon(info);
		pageIndex++;

		// Page 1 - Admin Commands (conditional)
		// First, find all admin commands that the observer has permission to use
		List<AdminCommandType> allowedCmdList = new ArrayList<>();
		for(AdminCommandType adminCmd : AdminCommandType.values()) {
			if(observer.hasPermission(adminCmd.permission())) {
				allowedCmdList.add(adminCmd);
			}
		}
		// Create admin page if there are any allowed commands
		if(!allowedCmdList.isEmpty()) {
			pageLabel = titleColor+MessagePath.MENU_HELP_ADMIN.getMessage();
			pageRows = (int)Math.ceil(((double)(allowedCmdList.size()))/9);
			slotIndex = 0;
			getMenu().addPage(pageIndex, pageRows, pageLabel);
			// Add command icons
			for(AdminCommandType adminCmd : allowedCmdList) {
				icon = new AdminCommandIcon(adminCmd, slotIndex);
				getMenu().getPage(pageIndex).addIcon(icon);
				slotIndex++;
			}
			pageIndex++;
		}

		// Page 2 - Tips
		pageLabel = titleColor+MessagePath.MENU_HELP_TIPS.getMessage();
		pageRows = 1;
		slotIndex = 0;
		getMenu().addPage(pageIndex, pageRows, pageLabel);
		String[] tips = {
				MessagePath.MENU_HELP_TIP_1.getMessage(),
				MessagePath.MENU_HELP_TIP_2.getMessage(),
				MessagePath.MENU_HELP_TIP_3.getMessage(),
				MessagePath.MENU_HELP_TIP_4.getMessage(),
				MessagePath.MENU_HELP_TIP_5.getMessage(),
				MessagePath.MENU_HELP_TIP_6.getMessage(),
				MessagePath.MENU_HELP_TIP_7.getMessage(),
				MessagePath.MENU_HELP_TIP_8.getMessage(),
				MessagePath.MENU_HELP_TIP_9.getMessage()
		};
		Material[] iconMaterials = {
				Material.ORANGE_BANNER,
				Material.YELLOW_BANNER,
				Material.LIME_BANNER,
				Material.GREEN_BANNER,
				Material.CYAN_BANNER,
				Material.LIGHT_BLUE_BANNER,
				Material.BLUE_BANNER,
				Material.PURPLE_BANNER,
				Material.MAGENTA_BANNER
		};
		for(int i = 0; i < 9; i++) {
			String tip = tips[i];
			Material iconMaterial = iconMaterials[i];
			loreList = new ArrayList<>();
			loreList.addAll(HelperUtil.stringPaginate(tip,ChatColor.LIGHT_PURPLE));
			icon = new InfoIcon(MessagePath.LABEL_INFORMATION.getMessage(), loreList, iconMaterial, slotIndex, false);
			getMenu().getPage(pageIndex).addIcon(icon);
			slotIndex++;
		}

		getMenu().refreshNavigationButtons();
		getMenu().setPageIndex(0);
	}

	@Override
	public void onIconClick(KonPlayer clickPlayer, MenuIcon clickedIcon) {
		Player bukkitPlayer = clickPlayer.getBukkitPlayer();
		if(clickedIcon instanceof CommandIcon) {
			// Command Icons close the GUI and print a command in chat
			CommandIcon icon = (CommandIcon)clickedIcon;
			for (String usageLine : icon.getCommand().argumentUsage()) {
				ChatUtil.sendNotice(bukkitPlayer, usageLine);
			}
		} else if(clickedIcon instanceof AdminCommandIcon) {
			// Admin Command Icons close the GUI and print a command in chat
			AdminCommandIcon icon = (AdminCommandIcon)clickedIcon;
			for (String usageLine : icon.getCommand().argumentUsage()) {
				ChatUtil.sendNotice(bukkitPlayer, usageLine);
			}
		} else if(clickedIcon instanceof InfoIcon) {
			// Info Icons close the GUI and print their info in chat
			InfoIcon icon = (InfoIcon)clickedIcon;
			ChatUtil.sendNotice(bukkitPlayer, icon.getInfo());
		}
	}

}
