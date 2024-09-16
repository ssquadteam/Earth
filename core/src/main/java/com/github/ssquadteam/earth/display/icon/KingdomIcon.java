package com.github.ssquadteam.earth.display.icon;

import com.github.ssquadteam.earth.manager.DisplayManager;
import com.github.ssquadteam.earth.model.KonKingdom;
import com.github.ssquadteam.earth.utility.CompatibilityUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class KingdomIcon implements MenuIcon {

	private final KonKingdom kingdom;
	private final String contextColor;
	private final List<String> lore;
	private final int index;
	private final ItemStack item;
	private final boolean isClickable;

	private final String propertyColor = DisplayManager.propertyFormat;
	private final String loreColor = DisplayManager.loreFormat;
	private final String valueColor = DisplayManager.valueFormat;

	public KingdomIcon(KonKingdom kingdom, String contextColor, List<String> lore, int index, boolean isClickable) {
		this.kingdom = kingdom;
		this.contextColor = contextColor;
		this.lore = lore;
		this.index = index;
		this.isClickable = isClickable;
		this.item = initItem();
	}
	
	private ItemStack initItem() {
		// Determine material
		Material material = Material.DIAMOND_HELMET;
		if(kingdom.isAdminOperated()) {
			material = Material.GOLDEN_HELMET;
		}
		// Add applicable labels
		boolean isProtected = false;
		List<String> loreList = new ArrayList<>();
		if(kingdom.isAdminOperated()) {
			loreList.add(propertyColor+MessagePath.LABEL_ADMIN_KINGDOM.getMessage());
		} else {
			loreList.add(propertyColor+MessagePath.LABEL_KINGDOM.getMessage());
		}
		if(kingdom.isPeaceful()) {
			loreList.add(propertyColor+MessagePath.LABEL_PEACEFUL.getMessage());
		}
		if(kingdom.isOpen()) {
			loreList.add(propertyColor+MessagePath.LABEL_OPEN.getMessage());
		}
		if(kingdom.isOfflineProtected()) {
			isProtected = true;
			loreList.add(propertyColor+MessagePath.LABEL_PROTECTED.getMessage());
		}
		loreList.add(loreColor+MessagePath.LABEL_TOWNS.getMessage()+": "+valueColor+kingdom.getNumTowns());
		loreList.add(loreColor+MessagePath.LABEL_MEMBERS.getMessage()+": "+valueColor+kingdom.getNumMembers());
		loreList.addAll(lore);
		String name = contextColor+kingdom.getName();
		return CompatibilityUtil.buildItem(material, name, loreList, isProtected);
	}
	
	public KonKingdom getKingdom() {
		return kingdom;
	}
	
	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		String name = kingdom.getName();
		if(name.equalsIgnoreCase("barbarians")) {
			name = MessagePath.LABEL_BARBARIANS.getMessage();
		}
		return name;
	}

	@Override
	public ItemStack getItem() {
		return item;
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}

}
