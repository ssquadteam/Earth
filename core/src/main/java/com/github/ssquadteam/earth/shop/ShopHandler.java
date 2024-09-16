package com.github.ssquadteam.earth.shop;

import com.Acrobot.ChestShop.ChestShop;
import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.api.model.EarthTerritoryType;
import com.github.ssquadteam.earth.model.*;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.maxgamer.quickshop.api.QuickShopAPI;
import org.maxgamer.quickshop.api.shop.Shop;

import java.awt.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * This class manages generic chest shops:
 * - Only allow shops to be created within friendly land, and not in the Wild.
 * - Only allow players to trade with shops in friendly, trade or alliance territory (and optionally sanctuary).
 * - Upon town capture, remove all shops within the town land, leaving behind chests with contents.
 */
public class ShopHandler {

    private final Earth earth;

    public ShopHandler(Earth earth) {
        this.earth = earth;
    }

    private void notifyAdminBypass(Player player) {
        // Notify admins about using bypass
        if(player.hasPermission("earth.admin.bypass")) {
            ChatUtil.sendNotice(player,MessagePath.PROTECTION_NOTICE_IGNORE.getMessage());
        }
    }

    /**
     * This method removes all shops within the chunks represented as points.
     * This gets called mostly when a town is captured, or camps are destroyed.
     * Only the shops are destroyed, not the chests + item contents.
     * @param points - The X,Y points that represent chunks (X,Z)
     * @param world - The world that contains the chunks
     */
    public void deleteShopsInPoints(Collection<Point> points, World world) {
        if(points.isEmpty() || world == null) return;

        // QuickShop
        if(earth.getIntegrationManager().getQuickShop().isEnabled()) {
            QuickShopAPI quickshop = earth.getIntegrationManager().getQuickShop().getAPI();
            if(quickshop != null) {
                HashMap<String,Integer> removedShops = new HashMap<>();
                for(Point point : points) {
                    int chunkX = point.x;
                    int chunkZ = point.y;
                    Map<Location, Shop> shopList = quickshop.getShopManager().getShops(world.getName(), chunkX, chunkZ);
                    if(shopList != null) {
                        for(Map.Entry<Location, Shop> entry : shopList.entrySet()) {
                            final OfflinePlayer owner = Bukkit.getOfflinePlayer(entry.getValue().getOwner());
                            entry.getValue().delete();
                            // Record metrics
                            String ownerName = owner.getName();
                            if(removedShops.containsKey(ownerName)) {
                                int previousNum = removedShops.get(ownerName);
                                removedShops.put(ownerName,previousNum+1);
                            } else {
                                removedShops.put(ownerName,1);
                            }
                        }
                    }
                }
                for(String name : removedShops.keySet()) {
                    ChatUtil.printDebug("Deleted "+removedShops.get(name)+" QuickShop chest(s) owned by "+name);
                }
            }
        }

        // ChestShop
        if(earth.getIntegrationManager().getChestShop().isEnabled()) {
            ChestShop chestshop = earth.getIntegrationManager().getChestShop().getAPI();
            if (chestshop != null) {
                int totalRemovedShops = 0;
                // Find signs within the chunks and delete them
                for(Point point : points) {
                    List<Sign> signShops = ChestShopFinder.findShopSigns(world,point.x,point.y);
                    totalRemovedShops += signShops.size();
                    // Break all sign shops
                    for(Sign shop : signShops) {
                        shop.getBlock().breakNaturally();
                    }
                }
                ChatUtil.printDebug("Deleted "+totalRemovedShops+" ChestShop chest(s)");
            }
        }

        // Other shop plugins
    }

    /**
     * Called when a player tries to use (trade with) a shop.
     * @param shopLoc - The location of the chest shop.
     * @param bukkitPlayer - The player attempting to trade.
     * @return True if the trade is allowed, else false to cancel the trade.
     */
    public boolean onShopUse(Location shopLoc, Player bukkitPlayer) {
        if(earth.isWorldIgnored(shopLoc)) return true;
        KonPlayer player = earth.getPlayerManager().getPlayer(bukkitPlayer);
        if(player == null) return true;
        boolean isShopClaimed = earth.getTerritoryManager().isChunkClaimed(shopLoc);
        if(!isShopClaimed) return true;
        KonTerritory territory = earth.getTerritoryManager().getChunkTerritory(shopLoc);
        assert territory != null;

        boolean isRelationAllowed = earth.getKingdomManager().isPlayerFriendly(player,territory.getKingdom()) ||
                earth.getKingdomManager().isPlayerTrade(player,territory.getKingdom()) ||
                earth.getKingdomManager().isPlayerAlly(player,territory.getKingdom()) ||
                territory.getTerritoryType().equals(EarthTerritoryType.SANCTUARY);

        // Bypass all checks for admins in bypass mode
        if(!player.isAdminBypassActive() && !isRelationAllowed) {
            notifyAdminBypass(bukkitPlayer);
            ChatUtil.sendError(bukkitPlayer, MessagePath.SHOP_ERROR_USE_RELATION.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Called when a player tries to create a shop.
     * @param shopLoc - The location of the chest shop.
     * @param bukkitPlayer - The player attempting to create it.
     * @return True if the creation is allowed, else false to cancel the creation.
     */
    public boolean onShopCreate(Location shopLoc, Player bukkitPlayer) {
        if(earth.isWorldIgnored(shopLoc)) return true;
        KonPlayer player = earth.getPlayerManager().getPlayer(bukkitPlayer);
        if(player == null) return true;
        boolean isShopClaimed = earth.getTerritoryManager().isChunkClaimed(shopLoc);
        // Prevent creating any shops in the wild
        if(!isShopClaimed) {
            ChatUtil.sendError(bukkitPlayer, MessagePath.SHOP_ERROR_CREATE_WILD.getMessage());
            return false;
        }
        KonTerritory territory = earth.getTerritoryManager().getChunkTerritory(shopLoc);
        assert territory != null;

        boolean isPropertyAllowed = true;
        if(territory instanceof KonPropertyFlagHolder) {
            KonPropertyFlagHolder holder = (KonPropertyFlagHolder)territory;
            if(holder.hasPropertyValue(KonPropertyFlag.SHOP)) {
                isPropertyAllowed = holder.getPropertyValue(KonPropertyFlag.SHOP);
            }
        }

        // Always prevent shops in monuments and templates
        if(territory instanceof KonSanctuary) {
            // Protect templates
            KonSanctuary sanctuary = (KonSanctuary) territory;
            if (sanctuary.isLocInsideTemplate(shopLoc)) {
                ChatUtil.sendError(bukkitPlayer, MessagePath.SHOP_ERROR_CREATE_MONUMENT.getMessage());
                return false;
            }
        } else if(territory instanceof KonTown) {
            // Protect monuments
            KonTown town = (KonTown) territory;
            if (town.isLocInsideMonumentProtectionArea(shopLoc)) {
                ChatUtil.sendError(bukkitPlayer, MessagePath.SHOP_ERROR_CREATE_MONUMENT.getMessage());
                return false;
            }
        }

        // Check for admin bypass
        if(!player.isAdminBypassActive()) {
            if(!isPropertyAllowed) {
                notifyAdminBypass(bukkitPlayer);
                ChatUtil.sendError(bukkitPlayer, MessagePath.SHOP_ERROR_CREATE_FAIL.getMessage());
                return false;
            }
            if(territory instanceof KonTown) {
                KonTown town = (KonTown) territory;
                if (!earth.getKingdomManager().isPlayerFriendly(player, town.getKingdom())) {
                    notifyAdminBypass(bukkitPlayer);
                    ChatUtil.sendError(bukkitPlayer, MessagePath.SHOP_ERROR_CREATE_TOWN.getMessage());
                    return false;
                }
            } else if(territory instanceof KonCamp && !((KonCamp)territory).isPlayerOwner(bukkitPlayer)) {
                notifyAdminBypass(bukkitPlayer);
                ChatUtil.sendError(bukkitPlayer, MessagePath.SHOP_ERROR_CREATE_CAMP.getMessage());
                return false;
            }
        }

        return true;
    }
}
