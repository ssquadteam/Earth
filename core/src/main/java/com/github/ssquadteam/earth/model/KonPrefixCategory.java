package com.github.ssquadteam.earth.model;

import org.bukkit.Material;

import com.github.ssquadteam.earth.utility.MessagePath;

public enum KonPrefixCategory {

	ROYALTY			(MessagePath.PREFIX_CATEGORY_ROYALTY.getMessage(), 		Material.ORANGE_WOOL),
	CLERGY			(MessagePath.PREFIX_CATEGORY_CLERGY.getMessage(), 		Material.WHITE_WOOL),
	NOBILITY		(MessagePath.PREFIX_CATEGORY_NOBILITY.getMessage(), 	Material.PURPLE_WOOL),
	TRADESMAN		(MessagePath.PREFIX_CATEGORY_TRADESMAN.getMessage(), 	Material.YELLOW_WOOL),
	MILITARY		(MessagePath.PREFIX_CATEGORY_MILITARY.getMessage(), 	Material.RED_WOOL),
	FARMING			(MessagePath.PREFIX_CATEGORY_FARMING.getMessage(), 		Material.GREEN_WOOL),
	COOKING			(MessagePath.PREFIX_CATEGORY_COOKING.getMessage(), 		Material.PINK_WOOL),
	FISHING			(MessagePath.PREFIX_CATEGORY_FISHING.getMessage(), 		Material.CYAN_WOOL),
	JOKING			(MessagePath.PREFIX_CATEGORY_JOKING.getMessage(), 		Material.BROWN_WOOL);

	private final String title;
	private final Material displayMaterial;
	KonPrefixCategory(String title, Material mat) {
		this.title = title;
		this.displayMaterial = mat;
	}
	
	public String getTitle() {
		return title;
	}
	
	public Material getMaterial() {
		return displayMaterial;
	}
	
}
