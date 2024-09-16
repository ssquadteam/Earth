package com.github.ssquadteam.earth.display.icon;

import com.github.ssquadteam.earth.api.model.EarthTerritoryType;
import com.github.ssquadteam.earth.manager.DisplayManager;
import com.github.ssquadteam.earth.model.KonTown;
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

public class TownIcon implements MenuIcon {

	private final KonTown town;
	private final String contextColor;
	private final List<String> lore;
	private final int index;
	private final boolean isClickable;
	private final ItemStack item;

	private final String alertColor = DisplayManager.alertFormat;
	private final String propertyColor = DisplayManager.propertyFormat;
	private final String loreColor = DisplayManager.loreFormat;
	private final String valueColor = DisplayManager.valueFormat;
	
	public TownIcon(KonTown town, String contextColor, List<String> lore, int index, boolean isClickable) {
		this.town = town;
		this.contextColor = contextColor;
		this.isClickable = isClickable;
		this.lore = lore;
		this.index = index;
		this.item = initItem();
	}
	
	private ItemStack initItem() {
		// Determine material
		Material material = Material.OBSIDIAN;
		if(town.isAttacked()) {
			material = Material.RED_WOOL;
		} else if(town.isArmored()) {
			material = Material.STONE_BRICKS;
		}
		// Add applicable labels
		boolean isProtected = false;
		List<String> loreList = new ArrayList<>();
		// Alerts
		if(town.getTerritoryType().equals(EarthTerritoryType.CAPITAL) &&
				town.getKingdom().isCapitalImmune()) {
			loreList.add(alertColor+MessagePath.LABEL_IMMUNITY.getMessage());
		}
		if(!town.isLordValid()) {
			loreList.add(alertColor+MessagePath.LABEL_NO_LORD.getMessage());
		}
		if(town.isAttacked()) {
			loreList.add(alertColor+MessagePath.PROTECTION_NOTICE_ATTACKED.getMessage());
		}
		// Properties
		if(town.getTerritoryType().equals(EarthTerritoryType.CAPITAL)) {
			loreList.add(propertyColor+MessagePath.TERRITORY_CAPITAL.getMessage());
		} else {
			loreList.add(propertyColor+MessagePath.TERRITORY_TOWN.getMessage());
		}
		if(town.isOpen()) {
			loreList.add(propertyColor+MessagePath.LABEL_OPEN.getMessage());
		}
		if(town.isArmored()) {
			loreList.add(propertyColor+MessagePath.LABEL_ARMOR.getMessage());
		}
		if(town.isShielded()) {
			isProtected = true;
			loreList.add(propertyColor+MessagePath.LABEL_SHIELD.getMessage());
		}
		// Lore
		loreList.add(loreColor+MessagePath.LABEL_POPULATION.getMessage() + ": " + valueColor + town.getNumResidents());
		loreList.add(loreColor+MessagePath.LABEL_LAND.getMessage() + ": " + valueColor + town.getChunkList().size());
		loreList.addAll(lore);
		String name = contextColor+town.getName();
		return CompatibilityUtil.buildItem(material, name, loreList, isProtected);
	}
	
	public KonTown getTown() {
		return town;
	}
	
	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return town.getName();
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
