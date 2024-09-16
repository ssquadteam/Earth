package com.github.ssquadteam.earth.display.wrapper;

import com.github.ssquadteam.earth.model.KonKingdom;
import com.github.ssquadteam.earth.model.KonTown;
import org.bukkit.inventory.Inventory;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.display.DisplayMenu;
import com.github.ssquadteam.earth.display.PagedMenu;
import com.github.ssquadteam.earth.display.icon.MenuIcon;
import com.github.ssquadteam.earth.model.KonPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

public abstract class MenuWrapper {
	
	private final PagedMenu menu;
	private final Earth earth;

	protected final Comparator<KonTown> townComparator;
	protected final Comparator<KonKingdom> kingdomComparator;
	protected final int MAX_ICONS_PER_PAGE 		= 45;
	
	public MenuWrapper(Earth earth) {
		this.earth = earth;
		this.menu = new PagedMenu();

		// Sorts towns by land, then population
		this.townComparator = (townOne, townTwo) -> {
			int result = 0;
			int g1Land = townOne.getChunkList().size();
			int g2Land = townTwo.getChunkList().size();
			if(g1Land < g2Land) {
				result = 1;
			} else if(g1Land > g2Land) {
				result = -1;
			} else {
				int g1Pop = townOne.getNumResidents();
				int g2Pop = townTwo.getNumResidents();
				if(g1Pop < g2Pop) {
					result = 1;
				} else if(g1Pop > g2Pop) {
					result = -1;
				}
			}
			return result;
		};

		// sort by towns, then population
		this.kingdomComparator = (kingdomOne, kingdomTwo) -> {
			int result = 0;
			int g1Land = kingdomOne.getNumLand();
			int g2Land = kingdomTwo.getNumLand();
			if(g1Land < g2Land) {
				result = 1;
			} else if(g1Land > g2Land) {
				result = -1;
			} else {
				int g1Pop = kingdomOne.getNumMembers();
				int g2Pop = kingdomTwo.getNumMembers();
				if(g1Pop < g2Pop) {
					result = 1;
				} else if(g1Pop > g2Pop) {
					result = -1;
				}
			}
			return result;
		};
	}
	
	public Earth getEarth() {
		return earth;
	}
	
	public PagedMenu getMenu() {
		return menu;
	}

	@Nullable
	public Inventory getCurrentInventory() {
		DisplayMenu currentPage = menu.getCurrentPage();
		if(currentPage != null) {
			return currentPage.getInventory();
		} else {
			return null;
		}
	}
	
	public abstract void constructMenu();
	
	public abstract void onIconClick(KonPlayer clickPlayer, MenuIcon clickedIcon);

	protected int getTotalPages(int numElements) {
		int pageTotal = (int)Math.ceil(((double)numElements)/MAX_ICONS_PER_PAGE);
		pageTotal = Math.max(pageTotal,1);
		return pageTotal;
	}

	protected int getNumPageRows(int numElements, int pageIndex) {
		int numPageRows = (int)Math.ceil(((double)(numElements - pageIndex*MAX_ICONS_PER_PAGE))/9);
		numPageRows = Math.max(numPageRows,1);
		numPageRows = Math.min(numPageRows,5);
		return numPageRows;
	}
}
