package com.github.ssquadteam.earth.display.icon;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.command.CommandType;
import com.github.ssquadteam.earth.manager.DisplayManager;
import com.github.ssquadteam.earth.utility.CompatibilityUtil;
import com.github.ssquadteam.earth.utility.HelperUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CommandIcon implements MenuIcon{

	private final CommandType command;
	private final int cost;
	private final int cost_incr;
	private final int index;
	private final ItemStack item;

	private final String loreColor = DisplayManager.loreFormat;
	private final String valueColor = DisplayManager.valueFormat;

	public CommandIcon(CommandType command, int cost, int cost_incr, int index) {
		this.command = command;
		this.cost = cost;
		this.cost_incr = cost_incr;
		this.index = index;
		this.item = initItem();
	}

	private ItemStack initItem() {
		List<String> loreList = new ArrayList<>();
		if(cost > 0) {
			loreList.add(loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+cost);
		}
		if(cost_incr > 0) {
			loreList.add(loreColor+MessagePath.LABEL_INCREMENT_COST.getMessage()+": "+valueColor+cost_incr);
		}
		loreList.addAll(HelperUtil.stringPaginate(command.description(),loreColor));
		String name = ChatColor.GOLD+getName();
		return CompatibilityUtil.buildItem(command.iconMaterial(), name, loreList);
	}
	
	public CommandType getCommand() {
		return command;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return command.toString();
	}

	@Override
	public ItemStack getItem() {
		return item;
	}

	@Override
	public boolean isClickable() {
		return true;
	}

}