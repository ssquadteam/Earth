package com.github.ssquadteam.earth.manager;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.api.manager.EarthPlotManager;
import com.github.ssquadteam.earth.api.model.EarthTown;
import com.github.ssquadteam.earth.model.KonPlot;
import com.github.ssquadteam.earth.model.KonTown;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.CorePath;
import com.github.ssquadteam.earth.utility.HelperUtil;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.awt.*;

public class PlotManager implements EarthPlotManager {

	private final Earth earth;
	private boolean isPlotsEnabled;
	private boolean isAllowBuild;
	private boolean isAllowContainers;
	private boolean isIgnoreKnights;
	private int maxSize;
	
	public PlotManager(Earth earth) {
		this.earth = earth;
		this.isPlotsEnabled = false;
		this.isAllowBuild = false;
		this.isAllowContainers = false;
		this.isIgnoreKnights = false;
		this.maxSize = 16;
	}
	
	public void initialize() {
		isPlotsEnabled = earth.getCore().getBoolean(CorePath.PLOTS_ENABLE.getPath(),false);
		isAllowBuild = earth.getCore().getBoolean(CorePath.PLOTS_ALLOW_BUILD.getPath(),false);
		isAllowContainers = earth.getCore().getBoolean(CorePath.PLOTS_ALLOW_CONTAINERS.getPath(),false);
		isIgnoreKnights = earth.getCore().getBoolean(CorePath.PLOTS_IGNORE_KNIGHTS.getPath(),false);
		maxSize = earth.getCore().getInt(CorePath.PLOTS_MAX_SIZE.getPath(),16);
		ChatUtil.printDebug("Plot Manager is ready, enabled: "+isPlotsEnabled);
	}
	
	public boolean isEnabled() {
		return isPlotsEnabled;
	}
	
	public boolean isBuildAllowed() {
		return isAllowBuild;
	}
	
	public boolean isContainerAllowed() {
		return isAllowContainers;
	}
	
	public boolean isKnightIgnored() {
		return isIgnoreKnights;
	}
	
	public int getMaxSize() {
		return maxSize;
	}
	
	public void removePlotPoint(KonTown town, Point point, World world) {
		if(!town.hasPlot(point, world))return;
		KonPlot plot = town.getPlot(point,world).clone();
		if(plot == null) return;
		town.removePlot(plot);
		plot.removePoint(point);
		if(!plot.getPoints().isEmpty()) {
			town.putPlot(plot);
		}


	}
	
	public boolean addPlot(KonTown town, KonPlot plot) {
		boolean enabled = earth.getCore().getBoolean(CorePath.PLOTS_ENABLE.getPath(),false);
		if(!enabled) return true;
		// Verify plot points exist within town
		for(Point p : plot.getPoints()) {
			if(!town.getChunkList().containsKey(p)) {
				return false;
			}
		}
		// Verify plot users are town residents
		for(OfflinePlayer p : plot.getUserOfflinePlayers()) {
			if(!town.isPlayerResident(p)) {
				return false;
			}
		}
		//ChatUtil.printDebug("Adding plot to town "+town.getName());
		// Add the plot to the town
		town.putPlot(plot);

		return true;
	}
	
	/**
	 * Check if the plot at the given location for the given town is protected from the given player.
	 * Town lords can always build in all plots.
	 * @param townArg - Town to check
	 * @param loc - Location to check
	 * @param player - player to check
	 * @return True when the player is not allowed to edit the plot at loc in town.
	 */
	public boolean isPlayerPlotProtectBuild(EarthTown townArg, Location loc, Player player) {
		if(isBuildAllowed())return false;
		return isPlayerPlotProtect(townArg,loc,player);
	}
	
	/**
	 * Check if the plot at the given location for the given town is protected from the given player.
	 * Town lords can always access containers in all plots.
	 * @param townArg - Town to check
	 * @param loc - Location to check
	 * @param player - player to check
	 * @return True when the player is not allowed to access containers in the plot at loc in town.
	 */
	public boolean isPlayerPlotProtectContainer(EarthTown townArg, Location loc, Player player) {
		if(isContainerAllowed())return false;
		return isPlayerPlotProtect(townArg,loc,player);
	}

	private boolean isPlayerPlotProtect(EarthTown townArg, Location loc, Player player) {
		if(!(townArg instanceof KonTown)) return false;

		KonTown town = (KonTown) townArg;
		if(!town.hasPlot(loc))return false;
		if(town.getPlot(loc).hasUser(player))return false;

		if(isKnightIgnored()) {
			return !town.isPlayerKnight(player);
		} else {
			return !town.isPlayerLord(player);
		}
	}
	
}
