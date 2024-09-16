package com.github.ssquadteam.earth.display.icon;

import com.github.ssquadteam.earth.utility.CompatibilityUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class InfoIcon implements MenuIcon{
	
	private final String name;
	private final List<String> lore;
	private final Material mat;
	private String info;
	private final int index;
	private final ItemStack item;
	private final boolean isClickable;
	
	public InfoIcon(String name, List<String> lore, Material mat, int index, boolean isClickable) {
		this.name = name;
		this.lore = lore;
		this.mat = mat;
		this.info = "";
		this.index = index;
		this.isClickable = isClickable;
		this.item = initItem();
	}

	private ItemStack initItem() {
		String name = getName();
		return CompatibilityUtil.buildItem(mat, name, lore);
	}
	
	public void setInfo(String info) {
		this.info = info;
	}
	
	public String getInfo() {
		return info;
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
		return item;
	}
	
	@Override
	public boolean isClickable() {
		return isClickable;
	}
	
}