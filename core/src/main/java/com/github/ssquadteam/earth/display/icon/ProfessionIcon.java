package com.github.ssquadteam.earth.display.icon;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.utility.CompatibilityUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ProfessionIcon implements MenuIcon {

	private final String name;
	private final List<String> lore;
	private final Villager.Profession profession;
	private final int index;
	private final boolean isClickable;
	
	public ProfessionIcon(List<String> lore, Villager.Profession profession, int index, boolean isClickable) {
		this.name = ChatColor.GOLD+CompatibilityUtil.getProfessionName(profession);
		this.lore = lore;
		this.profession = profession;
		this.index = index;
		this.isClickable = isClickable;
	}
	
	public Villager.Profession getProfession() {
		return profession;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ItemStack getItem() {
		return CompatibilityUtil.buildItem(CompatibilityUtil.getProfessionMaterial(profession), getName(), lore);
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}
}
