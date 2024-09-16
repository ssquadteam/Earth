package com.github.ssquadteam.earth.display.icon;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.api.model.EarthDiplomacyType;
import com.github.ssquadteam.earth.utility.CompatibilityUtil;
import com.github.ssquadteam.earth.utility.Labeler;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class DiplomacyIcon implements MenuIcon {

	private final EarthDiplomacyType relation;
	private final List<String> lore;
	private final int index;
	private final boolean isClickable;
	
	public DiplomacyIcon(EarthDiplomacyType relation, List<String> lore, int index, boolean isClickable) {
		this.relation = relation;
		this.lore = lore;
		this.index = index;
		this.isClickable = isClickable;
	}
	
	public EarthDiplomacyType getRelation() {
		return relation;
	}
	
	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return Labeler.lookup(relation);
	}

	@Override
	public ItemStack getItem() {
		List<String> itemLore = new ArrayList<>(lore);
		String nameColor = ""+ChatColor.GOLD;
		switch(relation) {
			case WAR:
				nameColor = Earth.enemyColor2;
				break;
			case PEACE:
				nameColor = Earth.peacefulColor2;
				break;
			case TRADE:
				nameColor = Earth.tradeColor2;
				break;
			case ALLIANCE:
				nameColor = Earth.alliedColor2;
				break;
			default:
				break;
		}
		String name = nameColor+getName();
		return CompatibilityUtil.buildItem(relation.getIcon(), name, itemLore);
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}

}
