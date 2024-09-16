package com.github.ssquadteam.earth.listener;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.EarthPlugin;
import com.github.ssquadteam.earth.api.model.EarthRelationshipType;
import com.github.ssquadteam.earth.manager.KingdomManager;
import com.github.ssquadteam.earth.manager.TerritoryManager;
import com.github.ssquadteam.earth.model.*;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;

public class HangingListener implements Listener {

	private final Earth earth;
	private final KingdomManager kingdomManager;
	private final TerritoryManager territoryManager;
	
	public HangingListener(EarthPlugin plugin) {
		this.earth = plugin.getEarthInstance();
		this.kingdomManager = earth.getKingdomManager();
		this.territoryManager = earth.getTerritoryManager();
	}
	
	@EventHandler(priority = EventPriority.HIGH)
    public void onHangingPlace(HangingPlaceEvent event) {
		if(event.isCancelled()) return;
		if(earth.isWorldIgnored(event.getBlock().getWorld())) return;
		Player bukkitPlayer = event.getPlayer();
		if(bukkitPlayer == null) return;
		KonPlayer player = earth.getPlayerManager().getPlayer(event.getPlayer());
		if(player == null) return;
		Location placeLoc = event.getEntity().getLocation();
		//ChatUtil.printDebug("EVENT player hanging placement of "+event.getEntity().getType());
		if(!player.isAdminBypassActive() && territoryManager.isChunkClaimed(placeLoc)) {
			KonTerritory territory = territoryManager.getChunkTerritory(placeLoc);
			// Property Flag Holders
			if(territory instanceof KonPropertyFlagHolder) {
				KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territory;
				if(flagHolder.hasPropertyValue(KonPropertyFlag.BUILD)) {
					if(!flagHolder.getPropertyValue(KonPropertyFlag.BUILD)) {
						ChatUtil.sendKonPriorityTitle(player, "", Earth.blockedFlagColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
						event.setCancelled(true);
						return;
					}
				}
			}
			// Specific territory protections
			if(territory instanceof KonTown) {
				KonTown town = (KonTown) territory;
				// Check for placement inside of town's monument
				if(town.getMonument().isLocInside(placeLoc)) {
					//ChatUtil.printDebug("EVENT: Hanging placed inside of monument");
					ChatUtil.sendKonPriorityTitle(player, "", Earth.blockedProtectionColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
					event.setCancelled(true);
					return;
				}
				// Prevent non-friendlies (including enemies) and friendly non-residents
				EarthRelationshipType playerRole = kingdomManager.getRelationRole(player.getKingdom(), territory.getKingdom());
				if(!playerRole.equals(EarthRelationshipType.FRIENDLY) || (!town.isOpen() && !town.isPlayerResident(player.getOfflineBukkitPlayer()))) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_NOT_RESIDENT.getMessage(territory.getName()));
					event.setCancelled(true);
					return;
				}
			} else if(territory instanceof KonRuin) {
				// Always prevent
				ChatUtil.sendKonPriorityTitle(player, "", Earth.blockedProtectionColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
				event.setCancelled(true);
				return;
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
    public void onHangingBreakPlayer(HangingBreakByEntityEvent event) {
		// Handle hanging breaks by players
		if(event.isCancelled()) return;
		if(earth.isWorldIgnored(event.getEntity().getWorld())) return;
		if(!(event.getRemover() instanceof Player)) return;
		Player bukkitPlayer = (Player)event.getRemover();
		KonPlayer player = earth.getPlayerManager().getPlayer(bukkitPlayer);
		if(player == null) return;
		Location brakeLoc = event.getEntity().getLocation();
		//ChatUtil.printDebug("EVENT player hanging break of "+event.getEntity().getType());
		if(!player.isAdminBypassActive() && territoryManager.isChunkClaimed(brakeLoc)) {
			KonTerritory territory = territoryManager.getChunkTerritory(brakeLoc);
			// Property Flag Holders
			if(territory instanceof KonPropertyFlagHolder) {
				KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territory;
				if(flagHolder.hasPropertyValue(KonPropertyFlag.BUILD)) {
					if(!flagHolder.getPropertyValue(KonPropertyFlag.BUILD)) {
						ChatUtil.sendKonPriorityTitle(player, "", Earth.blockedFlagColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
						event.setCancelled(true);
						return;
					}
				}
			}
			// Specific territory protections
			if(territory instanceof KonTown) {
				KonTown town = (KonTown) territory;
				// Check for break inside of town's monument
				if(town.getMonument().isLocInside(brakeLoc)) {
					ChatUtil.printDebug("EVENT: Hanging broke inside of monument");
					ChatUtil.sendKonPriorityTitle(player, "", Earth.blockedProtectionColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
					event.setCancelled(true);
					return;
				}
				// Prevent non-friendlies (including enemies) and friendly non-residents
				EarthRelationshipType playerRole = kingdomManager.getRelationRole(player.getKingdom(), territory.getKingdom());
				if(!playerRole.equals(EarthRelationshipType.FRIENDLY) || (!town.isOpen() && !town.isPlayerResident(player.getOfflineBukkitPlayer()))) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_NOT_RESIDENT.getMessage(territory.getName()));
					event.setCancelled(true);
					return;
				}
			} else if(territory instanceof KonRuin) {
				// Always prevent
				ChatUtil.sendKonPriorityTitle(player, "", Earth.blockedProtectionColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
				event.setCancelled(true);
				return;
			}
		}
    }

	@EventHandler(priority = EventPriority.MONITOR)
	public void onHangingBreakMonument(HangingBreakByEntityEvent event) {
		// Handle hanging breaks during monument changes
		Location brakeLoc = event.getEntity().getLocation();
		if(!territoryManager.isChunkClaimed(brakeLoc)) return;
		KonTerritory territory = territoryManager.getChunkTerritory(brakeLoc);
		// Check for break inside of town monument
		if(territory instanceof KonTown) {
			KonTown town = (KonTown) territory;
			if(town.getMonument().isItemDropsDisabled() && town.getMonument().isLocInside(brakeLoc)) {
				event.getEntity().remove();
			}
		}

	}
}
