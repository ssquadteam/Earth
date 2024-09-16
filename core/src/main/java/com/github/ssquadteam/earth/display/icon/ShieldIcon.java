package com.github.ssquadteam.earth.display.icon;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.manager.DisplayManager;
import com.github.ssquadteam.earth.model.KonShield;
import com.github.ssquadteam.earth.utility.CompatibilityUtil;
import com.github.ssquadteam.earth.utility.HelperUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ShieldIcon implements MenuIcon {

	private final KonShield shield;
	private final boolean isAvailable;
	private final int population;
	private final int land;
	private final int index;
	ItemStack item;

	private final String loreColor = DisplayManager.loreFormat;
	private final String valueColor = DisplayManager.valueFormat;
	private final String hintColor = DisplayManager.hintFormat;
	
	public ShieldIcon(KonShield shield, boolean isAvailable, int population, int land, int index) {
		this.shield = shield;
		this.isAvailable = isAvailable;
		this.population = population;
		this.land = land;
		this.index = index;
		this.item = initItem();
	}
	
	private ItemStack initItem() {
		Material material = Material.SHIELD;
		if(!isAvailable) {
			material = Material.IRON_BARS;
		}
		int totalCost = shield.getCost() + (shield.getCostPerResident()*population) + (shield.getCostPerLand()*land);
		List<String> loreList = new ArrayList<>();
		loreList.add(HelperUtil.getTimeFormat(shield.getDurationSeconds(), ChatColor.DARK_AQUA));
    	loreList.add(loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+totalCost);
    	if(isAvailable) {
    		loreList.add(hintColor+MessagePath.MENU_SHIELD_HINT.getMessage());
    	}
		String name = ChatColor.GOLD+shield.getId()+" "+MessagePath.LABEL_SHIELD.getMessage();
		return CompatibilityUtil.buildItem(material, name, loreList, true);
	}
	
	public KonShield getShield() {
		return shield;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return shield.getId();
	}

	@Override
	public ItemStack getItem() {
		return item;
	}

	@Override
	public boolean isClickable() {
		return isAvailable;
	}
}
