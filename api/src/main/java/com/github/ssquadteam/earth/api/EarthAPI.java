package com.github.ssquadteam.earth.api;

import com.github.ssquadteam.earth.api.manager.*;
import com.github.ssquadteam.earth.api.model.EarthKingdom;
import com.github.ssquadteam.earth.api.model.EarthOfflinePlayer;
import com.github.ssquadteam.earth.api.model.EarthTerritory;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scoreboard.Scoreboard;

import java.awt.*;

/**
 * The Earth API. This is the primary means of accessing Earth objects and methods. Most of the methods are helpers.
 * Use the manager classes to interact with specific portions of Earth.
 * 
 * @author squadsteam
 *
 */
public interface EarthAPI {

	/**
	 * Gets the friendly primary color, from core.yml.
	 * 
	 * @return The friendly primary color
	 */
	String getFriendlyPrimaryColor();

	/**
	 * Gets the friendly secondary color, from core.yml.
	 *
	 * @return The friendly secondary color
	 */
	String getFriendlySecondaryColor();

	/**
	 * Gets the enemy primary color, from core.yml.
	 * 
	 * @return The enemy primary color
	 */
	String getEnemyPrimaryColor();
	
	/**
	 * Gets the enemy secondary color, from core.yml.
	 * 
	 * @return The enemy secondary color
	 */
	String getEnemySecondaryColor();
	
	/**
	 * Gets the trade primary color, from core.yml.
	 * 
	 * @return The trade primary color
	 */
	String getTradePrimaryColor();

	/**
	 * Gets the trade secondary color, from core.yml.
	 *
	 * @return The trade secondary color
	 */
	String getTradeSecondaryColor();

	/**
	 * Gets the peaceful primary color, from core.yml.
	 * 
	 * @return The peaceful primary color
	 */
	String getPeacefulPrimaryColor();

	/**
	 * Gets the peaceful secondary color, from core.yml.
	 *
	 * @return The peaceful secondary color
	 */
	String getPeacefulSecondaryColor();

	/**
	 * Gets the allied primary color, from core.yml.
	 * 
	 * @return The allied primary color
	 */
	String getAlliedPrimaryColor();

	/**
	 * Gets the allied secondary color, from core.yml.
	 *
	 * @return The allied secondary color
	 */
	String getAlliedSecondaryColor();

	/**
	 * Gets the barbarian primary color, from core.yml.
	 * 
	 * @return The barbarian primary color
	 */
	String getBarbarianPrimaryColor();

	/**
	 * Gets the barbarian secondary color, from core.yml.
	 *
	 * @return The barbarian secondary color
	 */
	String getBarbarianSecondaryColor();

	/**
	 * Gets the neutral primary color, from core.yml.
	 * 
	 * @return The neutral primary color
	 */
	String getNeutralPrimaryColor();

	/**
	 * Gets the neutral secondary color, from core.yml.
	 *
	 * @return The neutral secondary color
	 */
	String getNeutralSecondaryColor();

	/**
	 * Gets the primary Earth scoreboard with teams.
	 * 
	 * @return The primary scoreboard
	 */
	Scoreboard getScoreboard();
	
	/**
	 * Checks for name conflicts and constraints against all Earth names.
	 * 
	 * @param name The name of an object (town, ruin, etc)
	 * @return Status code
	 * 			<br>0 - Success, no issue found
	 * 			<br>1 - Error, name is not strictly alphanumeric
	 * 			<br>2 - Error, name has more than 20 characters
	 * 			<br>3 - Error, name is an existing player
	 * 			<br>4 - Error, name is a kingdom
	 * 			<br>5 - Error, name is a town
	 * 			<br>6 - Error, name is a ruin
	 * 			<br>7 - Error, name is a guild [deprecated]
	 * 	  		<br>8 - Error, name is a sanctuary
	 * 	  		<br>9 - Error, name is a template
	 * 	  		<br>10 - Error, name is reserved word
	 */
	int validateNameConstraints(String name);
	
	/**
	 * Gets the player manager, for all things player related.
	 * 
	 * @return The player manager
	 */
	EarthPlayerManager getPlayerManager();
	
	/**
	 * Gets the kingdom manager, for all things kingdom/town related.
	 * 
	 * @return The kingdom manager
	 */
	EarthKingdomManager getKingdomManager();

	/**
	 * Gets the territory manager, for all things land/territory related.
	 *
	 * @return The territory manager
	 */
	EarthTerritoryManager getTerritoryManager();
	
	/**
	 * Gets the camp manager, for all things camp related.
	 * 
	 * @return The camp manager
	 */
	EarthCampManager getCampManager();
	
	/**
	 * Gets the upgrade manager, for all things town upgrade related.
	 * 
	 * @return The upgrade manager
	 */
	EarthUpgradeManager getUpgradeManager();
	
	/**
	 * Gets the shield manager, for all things town shield and armor related.
	 * 
	 * @return The shield manager
	 */
	EarthShieldManager getShieldManager();
	
	/**
	 * Gets the ruin manager, for all things ruin related.
	 * 
	 * @return The ruin manager
	 */
	EarthRuinManager getRuinManager();
	
	/**
	 * Gets the plot manager, for all things town plot related.
	 * 
	 * @return The ruin manager
	 */
	EarthPlotManager getPlotManager();
	
	/**
	 * Checks if a location is in a valid world.
	 * Invalid worlds prevent some Earth commands.
	 * 
	 * @param loc A location
	 * @return True if the location is in a valid world, else false
	 */
	boolean isWorldValid(Location loc);
	
	/**
	 * Checks if a world is valid.
	 * Invalid worlds prevent some Earth commands.
	 * 
	 * @param world A world
	 * @return True if the world is valid, else false
	 */
	boolean isWorldValid(World world);
	
	/**
	 * Checks if a location is in an ignored world.
	 * Ignored worlds prevent most Earth commands and features.
	 * 
	 * @param loc A location
	 * @return True if the location is in an ignored world, else false
	 */
	boolean isWorldIgnored(Location loc);
	
	/**
	 * Checks if a world is ignored.
	 * Ignored worlds prevent most Earth commands and features.
	 * 
	 * @param world A world
	 * @return True if the world is ignored, else false
	 */
	boolean isWorldIgnored(World world);
	
	/**
	 * Gets a random location in the wild, constrained by radius and offset in Earth's configuration.
	 * 
	 * @param world The world to generate the random location
	 * @return A random location in the wild
	 */
	Location getRandomWildLocation(World world);
	
	/**
	 * Gets a random safe location in a surrounding chunk to the given center location.
	 * A safe location is one that should not kill the player.
	 * The resulting location will not be in the same chunk as the center location.
	 * 
	 * @param center The location to center the new location around
	 * @param radius The distance in blocks from the center to search for a random location
	 * @return The safe random location
	 */
	Location getSafeRandomCenteredLocation(Location center, int radius);
	
	/**
	 * Gets the primary display color based on relationships. This color is set in the Earth configuration.
	 * There is a color for each {@link com.github.ssquadteam.earth.api.model relationship}.
	 * 
	 * @param displayKingdom The observing kingdom that should see the color
	 * @param contextKingdom The target kingdom whose relationship to the observer determines the color
	 * @return The primary display color
	 */
	String getDisplayPrimaryColor(EarthKingdom displayKingdom, EarthKingdom contextKingdom);
	
	/**
	 * Gets the primary display color based on relationships. This color is set in the Earth configuration.
	 * There is a color for each {@link com.github.ssquadteam.earth.api.model relationship}.
	 * 
	 * @param displayPlayer The observing player who should see the color
	 * @param contextPlayer The target player whose relationship to the observer determines the color
	 * @return The primary display color
	 */
	String getDisplayPrimaryColor(EarthOfflinePlayer displayPlayer, EarthOfflinePlayer contextPlayer);
	
	/**
	 * Gets the primary display color based on relationships. This color is set in the Earth configuration.
	 * There is a color for each {@link com.github.ssquadteam.earth.api.model relationship}.
	 * 
	 * @param displayPlayer The observing player who should see the color
	 * @param contextTerritory The target town whose relationship to the observer determines the color
	 * @return The primary display color
	 */
	String getDisplayPrimaryColor(EarthOfflinePlayer displayPlayer, EarthTerritory contextTerritory);
	
	/**
	 * Gets the secondary display color based on relationships. This color is set in the Earth configuration.
	 * There is a color for each {@link com.github.ssquadteam.earth.api.model relationship}.
	 * 
	 * @param displayKingdom The observing kingdom that should see the color
	 * @param contextKingdom The target kingdom whose relationship to the observer determines the color
	 * @return The secondary display color
	 */
	String getDisplaySecondaryColor(EarthKingdom displayKingdom, EarthKingdom contextKingdom);
	
	/**
	 * Gets the secondary display color based on relationships. This color is set in the Earth configuration.
	 * There is a color for each {@link com.github.ssquadteam.earth.api.model relationship}.
	 * 
	 * @param displayPlayer The observing player who should see the color
	 * @param contextPlayer The target player whose relationship to the observer determines the color
	 * @return The secondary display color
	 */
	String getDisplaySecondaryColor(EarthOfflinePlayer displayPlayer, EarthOfflinePlayer contextPlayer);
	
	/**
	 * Gets the secondary display color based on relationships. This color is set in the Earth configuration.
	 * There is a color for each {@link com.github.ssquadteam.earth.api.model relationship}.
	 * 
	 * @param displayPlayer The observing player who should see the color
	 * @param contextTerritory The target town whose relationship to the observer determines the color
	 * @return The secondary display color
	 */
	String getDisplaySecondaryColor(EarthOfflinePlayer displayPlayer, EarthTerritory contextTerritory);
	
	/**
	 * Utility method to convert a location to a point representation of the chunk that contains the location.
	 * The point's "x" field is the chunk's X coordinate, and the point's "y" field is the chunk's Z coordinate.
	 * Note that the point does not preserve the World of the location, so keep track of that separately.
	 * 
	 * @param loc The location to convert into a point
	 * @return A point representing the chunk containing the location
	 */
	static Point toPoint(Location loc) {
		return new Point((int)Math.floor((double)loc.getBlockX()/16),(int)Math.floor((double)loc.getBlockZ()/16));
	}
}
