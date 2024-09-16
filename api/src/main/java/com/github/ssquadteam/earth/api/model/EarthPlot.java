package com.github.ssquadteam.earth.api.model;

import com.github.ssquadteam.earth.api.EarthAPI;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.awt.*;
import java.util.List;

/**
 * A plot of land within a town, with player members (users) and land (points).
 * 
 * @author squadsteam
 *
 */
public interface EarthPlot {

	/**
	 * Checks whether the given point is a part of this plot.
	 * Use the {@link EarthAPI#toPoint(Location loc) toPoint} method to convert from location to point easily.
	 * Make sure to check that the world matches also.
	 * 
	 * @param point The point to check
	 * @return True when this plot contains the point, else false
	 */
    boolean hasPoint(Point point);
	
	/**
	 * Checks whether the given player is a member (user) of this plot.
	 * 
	 * @param player The player to check
	 * @return True when the player is a member, else false
	 */
    boolean hasUser(OfflinePlayer player);
	
	/**
	 * Gets the list of all land (points) for this plot.
	 * 
	 * @return The list of points
	 */
    List<Point> getPoints();
	
	/**
	 * Gets the list of all members (users) for this plot.
	 * 
	 * @return The list of players
	 */
    List<OfflinePlayer> getUserOfflinePlayers();
	
}
