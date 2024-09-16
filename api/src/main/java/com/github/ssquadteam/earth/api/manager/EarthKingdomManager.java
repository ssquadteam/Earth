package com.github.ssquadteam.earth.api.manager;

import com.github.ssquadteam.earth.api.model.*;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A manager for kingdoms and towns in Earth.
 * 
 * @author squadsteam
 *
 */
public interface EarthKingdomManager {

	
	/**
	 * Remove a kingdom with the given name.
	 * All kingdom members will be exiled to barbarians.
	 * All towns in the kingdom will be removed, with monuments replaced with air.
	 * The kingdom capital will be removed.
	 * 
	 * @param name The name of the kingdom to remove, case-sensitive
	 * @return True when the kingdom is successfully removed, else false
	 */
    boolean removeKingdom(String name);
	
	/**
	 * Primary method for adding a player member to a kingdom.
	 * Assign a player to a kingdom and teleport them to the capital spawn point.
	 * Optionally checks for join permissions based on Earth configuration.
	 * Optionally enforces maximum kingdom membership difference based on Earth configuration.
	 * 
	 * @param id The player to assign, by UUID
	 * @param kingdomName The kingdom name, case-sensitive
	 * @param force Ignore permission and max membership limits when true
	 * @return status
	 * 				<br>0	- success
	 *  			<br>1 	- kingdom name does not exist
	 *  			<br>2 	- the kingdom is full (config option max_player_diff)
	 *  			<br>3 	- missing permission
	 *  			<br>4 	- cancelled
	 *  			<br>5 	- joining is denied, player is already member
	 *  		    <br>6 	- joining is denied, player is a kingdom master
	 *  			<br>7	- joining is denied, failed switch criteria
	 *  		    <br>8	- joining is denied, cool-down
	 *  			<br>9 	- Unknown player ID
	 *             <br>-1 	- internal error
	 */
    int assignPlayerKingdom(UUID id, String kingdomName, boolean force);
	
	/**
	 * Exiles a player to be a Barbarian.
	 * Sets their exileKingdom value to their current Kingdom.
	 * (Optionally) Teleports to a random Wild location.
	 * (Optionally) Removes all stats and disables prefix.
	 * (Optionally) Resets their exileKingdom to Barbarians, making them look like a new player to Earth.
	 * Applies exile cool-down timer.
	 * 
	 * @param id The id of the player to exile
	 * @param teleport Teleport the player based on Earth configuration when true
	 * @param clearStats Remove all player stats and prefix when true
	 * @param isFull Perform a full exile such that the player has no exile kingdom, like they just joined the server
	 * @param force Ignore most checks when true
	 * @return status
	 * 				<br>0	- success
	 * 				<br>1	- Player is already a barbarian
	 * 				<br>4	- cancelled by event
	 * 				<br>6	- Exile denied, player is a kingdom master
	 * 				<br>8   - Cool-down remaining
	 * 				<br>9   - Unknown player ID
	 *             <br>-1 	- internal error
	 */
    int exilePlayerBarbarian(UUID id, boolean teleport, boolean clearStats, boolean isFull, boolean force);
	
	/**
	 * Create a new town centered at the given location, with the given name, for the given kingdom name.
	 * The town will copy a new monument from the kingdom monument template into the chunk of the given location.
	 * The town will create an initial territory based on the Earth configuration.
	 * 
	 * @param loc The center location of the town
	 * @param name The name of the town
	 * @param kingdomName The name of the kingdom that the town belongs to, case-sensitive
	 * @return  Status code
	 * 			<br>0 - success
	 * 			<br>1 - error, initial territory chunks conflict with another territory
	 * 			<br>3 - error, invalid name
	 * 			<br>4 - error, invalid monument template
	 * 			<br>5 - error, bad town placement, invalid world
	 * 			<br>6 - error, bad town placement, too close to another territory
	 * 			<br>7 - error, bad town placement, too far from other territories
	 *  	   <br>12 - error, town init fail, bad town height
	 *  	   <br>13 - error, town init fail, could not add land chunks
	 *  	   <br>14 - error, town init fail, too much air below town
	 *  	   <br>15 - error, town init fail, too much water below town
	 *  	   <br>16 - error, town init fail, containers below monument
	 *  	   <br>17 - error, town init fail, invalid monument template
     * 		   <br>21 - error, town init fail, invalid monument
	 * 		   <br>22 - error, town init fail, land height gradient is too high
	 * 		   <br>23 - error, town init fail, location is on bedrock or outside of gradient range
	 */
    int createTown(Location loc, String name, String kingdomName);
	
	/**
	 * Remove a town.
	 * The territory will be deleted, and the town monument will be replaced with air.
	 * 
	 * @param name The name of the town to remove
	 * @param kingdomName The name of the kingdom that the town belongs to
	 * @return True when the town is successfully removed, else false
	 */
    boolean removeTown(String name, String kingdomName);
	
	/**
	 * Rename a town.
	 * 
	 * @param oldName The current name of the town to be renamed
	 * @param newName The new name for the town
	 * @param kingdomName The name of the kingdom that the town belongs to
	 * @return True when the town is successfully renamed, else false
	 */
    boolean renameTown(String oldName, String newName, String kingdomName);
	
	/**
	 * Transfers ownership of a town to the given player's kingdom.
	 * The player that captures the town will become the new town lord.
	 * The player's kingdom will assume ownership of the town.
	 * 
	 * @param name The name of the town to capture
	 * @param oldKingdomName The name of the town's current kingdom
	 * @param conquerPlayer The player to capture the town for
	 * @return The captured town when successful, else null
	 */
    EarthTown captureTownForPlayer(String name, String oldKingdomName, EarthPlayer conquerPlayer);
	
	/**
	 * Gets a list of all kingdom names.
	 * 
	 * @return The list of kingdom names
	 */
    ArrayList<String> getKingdomNames();
	
	/**
	 * Gets a list of all kingdoms.
	 * 
	 * @return The list of kingdoms
	 */
    ArrayList<? extends EarthKingdom> getKingdoms();
	
	/**
	 * Checks whether the given name is a kingdom. Excludes the default barbarians kingdom.
	 * 
	 * @param name The name of the kingdom
	 * @return True when the name matches a kingdom, else false
	 */
    boolean isKingdom(String name);
	
	/**
	 * Gets a kingdom by name. Returns the barbarian kingdom by default.
	 * 
	 * @param name The name of the kingdom
	 * @return The kingdom, or barbarians when not found
	 */
    EarthKingdom getKingdom(String name);
	
	/**
	 * Checks whether the given name is a town.
	 * 
	 * @param name The name of the town
	 * @return True when the name matches a town, else false
	 */
    boolean isTown(String name);
	
	/**
	 * Gets the barbarians' kingdom.
	 * This is a default kingdom created internally by Earth for barbarian players.
	 * 
	 * @return The barbarians kingdom
	 */
    EarthKingdom getBarbarians();
	
	/**
	 * Gets the neutrals' kingdom.
	 * This is a default kingdom created internally by Earth for ruin territories and other territories that do not belong to a kingdom.
	 * 
	 * @return The neutrals kingdom
	 */
    EarthKingdom getNeutrals();
	
	/**
	 * Gets the towns which the given player is the lord of.
	 * 
	 * @param player The player
	 * @return List of towns
	 */
    List<? extends EarthTown> getPlayerLordshipTowns(EarthOfflinePlayer player);
	
	/**
	 * Gets the towns which the given player is a knight of.
	 * 
	 * @param player The player
	 * @return List of towns
	 */
    List<? extends EarthTown> getPlayerKnightTowns(EarthOfflinePlayer player);
	
	/**
	 * Gets the towns which the given player is a resident of.
	 * 
	 * @param player The player
	 * @return List of towns
	 */
    List<? extends EarthTown> getPlayerResidenceTowns(EarthOfflinePlayer player);
	
	/**
	 * Gets the monument critical block material.
	 * This is the block type that must be destroyed within town monuments in order to capture the town.
	 * 
	 * @return The critical material
	 */
    Material getTownCriticalBlock();
	
	/**
	 * Gets the maximum number of critical hits for each town monument.
	 * When this number of critical blocks are destroyed, the town will be captured.
	 * 
	 * @return The number of maximum critical hits
	 */
    int getMaxCriticalHits();

	/**
	 * Get the shared diplomatic type of two kingdoms.
	 * Performs error checking to ensure both kingdoms have the same active relation.
	 * If there is a mismatch, they are reset to default (peace).
	 *
	 * @param kingdom1 The reference kingdom
	 * @param kingdom2 The target kingdom
	 * @return The diplomatic type
	 */
	EarthDiplomacyType getDiplomacy(EarthKingdom kingdom1, EarthKingdom kingdom2);

	/**
	 * Get the relationship type of two kingdoms.
	 *
	 * @param kingdom1 The reference kingdom
	 * @param kingdom2 The target kingdom
	 * @return The relationship type
	 */
	EarthRelationshipType getRelationRole(EarthKingdom kingdom1, EarthKingdom kingdom2);
	
}
