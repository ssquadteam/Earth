package com.github.ssquadteam.earth.listener;

import com.Acrobot.ChestShop.Events.PreShopCreationEvent;
import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import com.github.ssquadteam.earth.EarthPlugin;
import com.github.ssquadteam.earth.shop.ShopHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChestShopListener implements Listener {

    private final ShopHandler shopHandler;

    public ChestShopListener(EarthPlugin plugin) {
        this.shopHandler = plugin.getEarthInstance().getShopHandler();
    }

    /**
     * Allow players to create shops only in friendly towns or their camp
     */
    @EventHandler()
    public void onShopPreCreate(PreShopCreationEvent event) {
        boolean status = shopHandler.onShopCreate(event.getSign().getLocation(), event.getPlayer());
        if(!status) {
            event.setCancelled(true);
            event.setOutcome(PreShopCreationEvent.CreationOutcome.NO_PERMISSION_FOR_TERRAIN);
        }
    }

    /**
     * Protect shops from being used by enemies
     */
    @EventHandler()
    public void onShopPurchase(PreTransactionEvent event) {
        boolean status = shopHandler.onShopUse(event.getSign().getLocation(), event.getClient());
        if(!status) {
            event.setCancelled(true);
        }
    }
}
