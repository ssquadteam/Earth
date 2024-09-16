package com.github.ssquadteam.earth.api.manager;

import com.github.ssquadteam.earth.api.model.EarthKingdom;
import com.github.ssquadteam.earth.api.model.EarthOfflinePlayer;
import com.github.ssquadteam.earth.api.model.EarthPlayer;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * A manager for players in Earth.
 * 
 * @author squadsteam
 *
 */
public interface EarthPlayerManager {

	/**
	 * Gets a Earth player object from the given Bukkit Player.
	 * 
	 * @param bukkitPlayer The player to look up
	 * @return The Earth player if one exists, else null
	 */
    EarthPlayer getPlayer(Player bukkitPlayer);
	
	/**
	 * Checks whether a player is currently online and represented in Earth.
	 * 
	 * @param bukkitPlayer The player to check
	 * @return True when the player is online and has a EarthPlayer object, else false
	 */
    boolean isOnlinePlayer(Player bukkitPlayer);
	
	/**
	 * Gets a Earth offline player object from the given Bukkit OfflinePlayer.
	 * 
	 * @param offlineBukkitPlayer The offline player to look up
	 * @return The Earth offline player if one exists, else null
	 */
    EarthOfflinePlayer getOfflinePlayer(OfflinePlayer offlineBukkitPlayer);
	
	/**
	 * Checks whether a player exists and is represented in Earth.
	 * This can be used to check if a player has ever joined the server and is present in the Earth player database.
	 * 
	 * @param offlineBukkitPlayer The player to check
	 * @return True when the player exists and has a EarthOfflinePlayer object, else false
	 */
    boolean isOfflinePlayer(OfflinePlayer offlineBukkitPlayer);
	
	/**
	 * Checks whether a player name exists and is represented in Earth.
	 * 
	 * @param name The player's name
	 * @return True if there is a player with a matching name in the Earth player database, else false
	 */
    boolean isPlayerNameExist(String name);
	
	/**
	 * Gets a Earth player object from the given name.
	 * 
	 * @param displayName The player name to look up
	 * @return The Earth player if one exists, else null
	 */
    EarthPlayer getPlayerFromName(String displayName);
	
	/**
	 * Gets a Earth offline player object from the given name.
	 * 
	 * @param displayName The player name to look up
	 * @return The Earth offline player if one exists, else null
	 */
    EarthOfflinePlayer getOfflinePlayerFromName(String displayName);
	
	/**
	 * Gets a Earth player object from the given UUID.
	 * 
	 * @param id The player UUID to look up
	 * @return The Earth player if one exists, else null
	 */
    EarthPlayer getPlayerFromID(UUID id);
	
	/**
	 * Gets a Earth offline player object from the given UUID.
	 * 
	 * @param id The player UUID to look up
	 * @return The Earth offline player if one exists, else null
	 */
    EarthOfflinePlayer getOfflinePlayerFromID(UUID id);
	
	/**
	 * Gets all of Bukkit's OfflinePlayers from the Earth player database.
	 * This is a collection of every player that has joined the server.
	 * 
	 * @return The collection of players
	 */
    Collection<OfflinePlayer> getAllOfflinePlayers();
	
	/**
	 * Gets all of Earth's OfflinePlayers from the Earth player database.
	 * This is a collection of every player that has joined the server.
	 * 
	 * @return The collection of players
	 */
    Collection<? extends EarthOfflinePlayer> getAllEarthOfflinePlayers();
	
	/**
	 * Gets the players currently online that are members of the given kingdom name.
	 * 
	 * @param kingdomName The kingdom name, ignoring case
	 * @return The list of online players in the given kingdom
	 */
    ArrayList<? extends EarthPlayer> getPlayersInKingdom(String kingdomName);
	
	/**
	 * Gets the players currently online that are members of the given kingdom object.
	 * 
	 * @param kingdom The kingdom instance
	 * @return The list of online players in the given kingdom
	 */
    ArrayList<? extends EarthPlayer> getPlayersInKingdom(EarthKingdom kingdom);
	
	/**
	 * Gets the Bukkit players currently online that are members of the given kingdom name.
	 * 
	 * @param kingdomName The kingdom name, ignoring case
	 * @return The list of online players in the given kingdom
	 */
    Collection<Player> getBukkitPlayersInKingdom(String kingdomName);
	
	/**
	 * Gets the Bukkit players currently online that are members of the given kingdom object.
	 * 
	 * @param kingdom The kingdom instance
	 * @return The list of online players in the given kingdom
	 */
    Collection<Player> getBukkitPlayersInKingdom(EarthKingdom kingdom);
	
	/**
	 * Gets the names of players currently online that are members of the given kingdom name.
	 * This is a convenience method.
	 * 
	 * @param kingdomName The kingdom name, ignoring case
	 * @return The list of player names in the given kingdom
	 */
    ArrayList<String> getPlayerNamesInKingdom(String kingdomName);
	
	/**
	 * Gets all the players that are members of the given kingdom name.
	 * 
	 * @param kingdomName The kingdom name, ignoring case
	 * @return The list of players in the given kingdom
	 */
    ArrayList<? extends EarthOfflinePlayer> getAllPlayersInKingdom(String kingdomName);
	
	/**
	 * Gets all the players that are members of the given kingdom object.
	 * 
	 * @param kingdom The kingdom instance
	 * @return The list of players in the given kingdom
	 */
    ArrayList<? extends EarthOfflinePlayer> getAllPlayersInKingdom(EarthKingdom kingdom);
	
	/**
	 * Gets all the Bukkit players that are members of the given kingdom name.
	 * 
	 * @param kingdomName The kingdom name, ignoring case
	 * @return The list of players in the given kingdom
	 */
    Collection<OfflinePlayer> getAllBukkitPlayersInKingdom(String kingdomName);
	
	/**
	 * Gets all the Bukkit players that are members of the given kingdom object.
	 * 
	 * @param kingdom The kingdom instance
	 * @return The list of players in the given kingdom
	 */
    Collection<OfflinePlayer> getAllBukkitPlayersInKingdom(EarthKingdom kingdom);
	
	/**
	 * Gets all of Earth's players that are currently online.
	 * 
	 * @return The collection of players
	 */
    Collection<? extends EarthPlayer> getPlayersOnline();
	
	/**
	 * Gets all Bukkit players that are currently online.
	 * 
	 * @return The collection of players
	 */
    Collection<Player> getBukkitPlayersOnline();
	
}
