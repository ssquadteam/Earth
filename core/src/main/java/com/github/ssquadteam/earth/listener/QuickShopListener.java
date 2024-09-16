package com.github.ssquadteam.earth.listener;

import com.github.ssquadteam.earth.EarthPlugin;
import com.github.ssquadteam.earth.shop.ShopHandler;
import com.github.ssquadteam.earth.utility.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.maxgamer.quickshop.api.event.PlayerShopClickEvent;
import org.maxgamer.quickshop.api.event.ShopPreCreateEvent;
import org.maxgamer.quickshop.api.event.ShopPurchaseEvent;

public class QuickShopListener implements Listener {

	private final ShopHandler shopHandler;
	
	public QuickShopListener(EarthPlugin plugin) {
		this.shopHandler = plugin.getEarthInstance().getShopHandler();
	}

	/**
	 * Allow players to create shops only in friendly towns or their camp
	 */
	@EventHandler()
    public void onShopPreCreate(ShopPreCreateEvent event) {
		boolean status = shopHandler.onShopCreate(event.getLocation(), event.getPlayer());
		if(!status) {
			event.setCancelled(true);
		}
	}
	
	/**
	 * Protect shops from being used by enemies
	 */
	@EventHandler()
    public void onShopPurchase(ShopPurchaseEvent event) {
		Player purchaser = Bukkit.getPlayer(event.getPurchaser());
		boolean status = shopHandler.onShopUse(event.getShop().getLocation(), purchaser);
		if(!status) {
			event.setCancelled(true);
		}
	}

	@EventHandler()
	public void onShopClick(PlayerShopClickEvent event) {
		boolean status = shopHandler.onShopUse(event.getShop().getLocation(), event.getPlayer());
		if(!status) {
			event.setCancelled(true);
		}
	}
	
}
