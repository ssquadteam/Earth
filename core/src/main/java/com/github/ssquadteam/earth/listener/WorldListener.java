package com.github.ssquadteam.earth.listener;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.EarthPlugin;
import com.github.ssquadteam.earth.model.*;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.HelperUtil;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;

import java.awt.*;

public class WorldListener  implements Listener {

	private final Earth earth;
	
	public WorldListener(EarthPlugin plugin) {
		this.earth = plugin.getEarthInstance();
	}
	
	@EventHandler(priority = EventPriority.HIGH)
    public void onPortalCreate(PortalCreateEvent event) {
		ChatUtil.printDebug("EVENT: Portal is being created in "+event.getWorld().getName());
		if(event.isCancelled()) return;
		if(!earth.isWorldValid(event.getWorld())) return;
		// Check every block associated with the portal
		for(BlockState current_blockState : event.getBlocks()) {
			// Prevent portals from being created inside of Capitals and Monuments in the primary world
			if(earth.getTerritoryManager().isChunkClaimed(current_blockState.getLocation())) {
				KonTerritory territory = earth.getTerritoryManager().getChunkTerritory(current_blockState.getLocation());
				// Property Flag Holders
				if(territory instanceof KonPropertyFlagHolder) {
					KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territory;
					if(flagHolder.hasPropertyValue(KonPropertyFlag.PORTALS)) {
						if(!flagHolder.getPropertyValue(KonPropertyFlag.PORTALS)) {
							ChatUtil.printDebug("EVENT: Portal creation stopped inside of "+territory.getName());
							event.setCancelled(true);
							return;
						}
					}
				}
				// Check for portal inside monument/template
				if(isLocInsideMonument(current_blockState.getLocation())) {
					ChatUtil.printDebug("EVENT: Portal creation stopped inside of monument "+territory.getName());
					event.setCancelled(true);
					return;
				}
			}
		}

	}
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
		if(earth.getTerritoryManager().isChunkClaimed(HelperUtil.toPoint(event.getChunk()), event.getWorld())) {
			KonTerritory territory = earth.getTerritoryManager().getChunkTerritory(HelperUtil.toPoint(event.getChunk()), event.getWorld());
			if(territory instanceof KonTown) {
				KonTown town = (KonTown) territory;
				if(town.isChunkCenter(event.getChunk())) {
					ChatUtil.printDebug("EVENT: Loaded monument chunk of town "+town.getName());
					if(!town.getKingdom().isMonumentTemplateValid()) {
						ChatUtil.printDebug("Could not paste monument in town "+town.getName()+" while monument template is invalid");
					} else if(town.isAttacked()) {
						ChatUtil.printDebug("Could not paste monument in town "+town.getName()+" while under attack");
					} else {
						// Paste current monument template
						boolean status = town.reloadMonument();
						if(!status) {
							ChatUtil.printDebug("Failed to paste invalid monument template!");
						}
					}
				}
			}
		}
		earth.applyQueuedTeleports(event.getChunk());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onChunkUnload(ChunkUnloadEvent event) {
		// Remove Ruin Golems when a chunk with a ruin spawn location unloads
		Point chunkPoint = HelperUtil.toPoint(event.getChunk());
		if(earth.getTerritoryManager().isChunkClaimed(chunkPoint, event.getWorld())) {
			KonTerritory territory = earth.getTerritoryManager().getChunkTerritory(HelperUtil.toPoint(event.getChunk()), event.getWorld());
			if(territory instanceof KonRuin) {
				KonRuin ruin = (KonRuin) territory;
				for(Location spawn : ruin.getSpawnLocations()) {
					Point spawnPoint = HelperUtil.toPoint(spawn);
					if(chunkPoint.equals(spawnPoint)) {
						// Found a ruin's spawn point within the chunk being unloaded
						// Remove the golem associated with the spawn point
						ruin.removeGolem(spawn);
					}
				}
			}
		}

	}
	
	@EventHandler(priority = EventPriority.HIGH)
    public void onStructureGrow(StructureGrowEvent event) {
		if(earth.isWorldIgnored(event.getWorld())) return;
		// Prevent growth if any blocks are within monument
		for(BlockState bState : event.getBlocks()) {
			if(isLocInsideMonument(bState.getLocation())) {
				ChatUtil.printDebug("EVENT: Prevented structure growth within monument");
				event.setCancelled(true);
				return;
			}
		}
	}
	
	/*
	 * Check if the given block is inside any town/capital monument or monument template
	 */
	private boolean isLocInsideMonument(Location loc) {
		if(!earth.getTerritoryManager().isChunkClaimed(loc)) return false;
		KonTerritory territory = earth.getTerritoryManager().getChunkTerritory(loc);
		if(territory instanceof KonSanctuary && ((KonSanctuary) territory).isLocInsideTemplate(loc)) {
			return true;
		}
		return territory instanceof KonTown && ((KonTown) territory).isLocInsideMonumentProtectionArea(loc);
	}

}
