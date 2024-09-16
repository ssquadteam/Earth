package com.github.ssquadteam.earth.listener;

import com.earth2me.essentials.ITarget;
import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.model.*;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.CorePath;
import com.github.ssquadteam.earth.utility.MessagePath;
import net.ess3.api.IUser;
import net.ess3.api.events.AfkStatusChangeEvent;
import net.ess3.api.events.teleport.PreTeleportEvent;
import net.essentialsx.api.v2.events.HomeModifyEvent;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class EssentialsXListener implements Listener {

    private final Earth earth;

    public EssentialsXListener(Earth earth) {
        this.earth = earth;
    }

    /**
     * Prevent players from modifying their home locations in specific territories
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onHomeModify(HomeModifyEvent event) {
        if (event.isCancelled()) return;
        // Get home location
        Location newHomeLoc = event.getNewLocation();
        // Check if home is deleted (null)
        if (newHomeLoc == null) return;
        // Get player
        IUser homeOwner = event.getHomeOwner();
        if (homeOwner == null) return;
        KonPlayer player = earth.getPlayerManager().getPlayer(homeOwner.getBase());
        if (player == null) return;
        // Check territory
        boolean isWildHomeEnabled = earth.getCore().getBoolean(CorePath.INTEGRATION_ESSENTIALSX_OPTIONS_ENABLE_WILD_HOMES.getPath(),true);
        if (earth.getTerritoryManager().isChunkClaimed(newHomeLoc)) {
            // Home is in a claimed territory
            KonTerritory territory = earth.getTerritoryManager().getChunkTerritory(newHomeLoc);
            assert territory != null;
            if (territory instanceof KonCamp) {
                // For camps, check if player is the owner
                KonCamp camp = (KonCamp)territory;
                if (!camp.isPlayerOwner(player.getBukkitPlayer())) {
                    // Homes cannot be made in barbarian camps except by the owner
                    ChatUtil.printDebug("Cancelled EssentialsX home update in camp by non-owner");
                    ChatUtil.sendError(player, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
                    event.setCancelled(true);
                }
            } else {
                // For all other territories, check for friendly relation
                if (!earth.getKingdomManager().isPlayerFriendly(player,territory.getKingdom())) {
                    // Homes cannot be made in non-friendly territory
                    ChatUtil.printDebug("Cancelled EssentialsX home update in non-friendly territory");
                    ChatUtil.sendError(player, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
                    event.setCancelled(true);
                }
            }
        } else {
            // Home is in the wild
            if (!isWildHomeEnabled) {
                // Homes cannot be made in the wild, cancel
                ChatUtil.printDebug("Cancelled EssentialsX home update in the wild");
                ChatUtil.sendError(player, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
                event.setCancelled(true);
            }
        }
    }

    /**
     * Prevent players from teleporting in specific territories and situations, similar to protections for Earth's travel command.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPreTeleport(PreTeleportEvent event) {
        if (event.isCancelled()) return;
        // Check configuration setting
        boolean isTeleportProtectionEnabled = earth.getCore().getBoolean(CorePath.INTEGRATION_ESSENTIALSX_OPTIONS_ENABLE_TELEPORT_PROTECTION.getPath(),true);
        if (!isTeleportProtectionEnabled) return;

        // Get destination
        ITarget teleportTarget = event.getTarget();
        if (teleportTarget == null) return;
        Location teleportDestination = teleportTarget.getLocation();

        // Get player
        IUser teleportUser = event.getTeleportee();
        if (teleportUser == null) return;
        Location teleportOrigin = teleportUser.getBase().getLocation();
        KonPlayer player = earth.getPlayerManager().getPlayer(teleportUser.getBase());
        if (player == null) return;

        /*
         * Summary of teleport protections:
         * - Cannot tp when player is combat tagged (optional)
         * - Cannot tp into territory with TRAVEL property flag disabled
         * - Cannot tp into or out of enemy territory (enemy kingdom towns/capital, barbarian camps)
         *      - Barbarian players are enemy with all other kingdoms
         *      - Kingdom members at war with other kingdoms are enemies, and barbarian players are enemies
         */

        // Check for combat tagged player
        boolean isCombatTagEnabled = earth.getCore().getBoolean(CorePath.COMBAT_PREVENT_COMMAND_ON_DAMAGE.getPath(),true);
        if (isCombatTagEnabled && player.isCombatTagged()) {
            // Player is combat tagged, cancel teleport
            ChatUtil.printDebug("Cancelled EssentialsX teleport for combat tagged player");
            ChatUtil.sendError(player, MessagePath.PROTECTION_ERROR_TAG_BLOCKED.getMessage());
            event.setCancelled(true);
            return;
        }

        // Get destination territory
        if (earth.getTerritoryManager().isChunkClaimed(teleportDestination)) {
            // Teleport destination is in a claimed territory
            KonTerritory territory = earth.getTerritoryManager().getChunkTerritory(teleportDestination);
            assert territory != null;
            // Check property flags
            if (territory instanceof KonPropertyFlagHolder) {
                KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territory;
                if (flagHolder.hasPropertyValue(KonPropertyFlag.TRAVEL)) {
                    if (!flagHolder.getPropertyValue(KonPropertyFlag.TRAVEL)) {
                        ChatUtil.printDebug("Cancelled EssentialsX teleport into territory with TRAVEL flag = false");
                        ChatUtil.sendError(player, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
                        event.setCancelled(true);
                        return;
                    }
                }
            }
            // Check relationship
            if (earth.getKingdomManager().isPlayerEnemy(player,territory.getKingdom())) {
                // Cannot teleport into enemy territory
                ChatUtil.printDebug("Cancelled EssentialsX teleport into enemy territory");
                ChatUtil.sendError(player, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
                event.setCancelled(true);
                return;
            }
        }

        // Get origin territory
        if (earth.getTerritoryManager().isChunkClaimed(teleportOrigin)) {
            // Teleport origin is in a claimed territory
            KonTerritory territory = earth.getTerritoryManager().getChunkTerritory(teleportOrigin);
            assert territory != null;
            // Check relationship
            if (earth.getKingdomManager().isPlayerEnemy(player,territory.getKingdom())) {
                // Cannot teleport out of an enemy territory
                ChatUtil.printDebug("Cancelled EssentialsX teleport out of enemy territory");
                ChatUtil.sendError(player, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Track when players go AFK
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onAfkChange(AfkStatusChangeEvent event) {
        if (event.isCancelled()) return;
        // Get player
        IUser afkUser = event.getAffected();
        if (afkUser == null) return;
        KonPlayer player = earth.getPlayerManager().getPlayer(afkUser.getBase());
        if (player == null) return;
        // Update player's AFK status
        player.setIsAfk(event.getValue());
    }

}
