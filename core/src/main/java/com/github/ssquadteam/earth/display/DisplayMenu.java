package com.github.ssquadteam.earth.display;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.display.icon.MenuIcon;
import com.github.ssquadteam.earth.display.icon.PlayerIcon;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;

public class DisplayMenu {

	private final Inventory inventory;
	private final HashMap<Integer,MenuIcon> iconMap;
	
	public DisplayMenu (int rows, String label) {
		inventory = Bukkit.createInventory(null, rows*9, label);
		iconMap = new HashMap<>();
	}
	
	public Inventory getInventory() {
        return inventory;
    }
	
	public void addIcon(MenuIcon icon) {
		iconMap.put(icon.getIndex(), icon);
	}
	
	public MenuIcon getIcon(int index) {
		return iconMap.get(index);
	}
	
	public void updateIcons() {
		Bukkit.getScheduler().runTaskAsynchronously(Earth.getInstance().getPlugin(), () -> {
			// Set non-player heads first
			for(MenuIcon icon : iconMap.values()) {
				if(!(icon instanceof PlayerIcon)) {
					inventory.setItem(icon.getIndex(), icon.getItem());
				}
			}
			// Set player heads last
			for(MenuIcon icon : iconMap.values()) {
				if((icon instanceof PlayerIcon)) {
					inventory.setItem(icon.getIndex(), icon.getItem());
				}
			}
		});
	}

}
