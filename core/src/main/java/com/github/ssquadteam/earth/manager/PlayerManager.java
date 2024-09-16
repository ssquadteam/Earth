package com.github.ssquadteam.earth.manager;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.api.manager.EarthPlayerManager;
import com.github.ssquadteam.earth.api.model.EarthKingdom;
import com.github.ssquadteam.earth.model.*;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.CorePath;
import com.github.ssquadteam.earth.utility.HelperUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager implements EarthPlayerManager {

	private final Earth earth;
	private final HashMap<Player, KonPlayer> onlinePlayers;
	private final ConcurrentHashMap<OfflinePlayer, KonOfflinePlayer> allPlayers;
	private final ArrayList<String> blockedCommands;

	public PlayerManager(Earth earth) {
		this.earth = earth;
		onlinePlayers = new HashMap<>();
		allPlayers = new ConcurrentHashMap<>();
		blockedCommands = new ArrayList<>();
	}
	
	public void initialize() {
		// Gather all block commands from core config
		blockedCommands.clear();
		blockedCommands.addAll(earth.getCore().getStringList(CorePath.COMBAT_PREVENT_COMMAND_LIST.getPath()));
		
		//blockedCommands
		
		ChatUtil.printDebug("Player Manager is ready");
	}

	@NotNull
	public ArrayList<String> getBlockedCommands() {
		return blockedCommands;
	}

	@Nullable
	public KonPlayer removePlayer(@NotNull Player bukkitPlayer) {
		KonPlayer player = onlinePlayers.remove(bukkitPlayer);
		ChatUtil.printDebug("Removed online player: "+bukkitPlayer.getName());
		return player;
	}

	@Nullable
	public KonPlayer getPlayer(Player bukkitPlayer) {
		if (bukkitPlayer == null) {
			return null;
		} else {
			return onlinePlayers.get(bukkitPlayer);
		}
	}

	@Nullable
	public KonPlayer getPlayer(CommandSender sender) {
		if (sender == null) return null;
		if (sender instanceof Player) {
			return onlinePlayers.get((Player)sender);
		} else {
			return null;
		}
	}
	
	public boolean isOnlinePlayer(Player bukkitPlayer) {
		if (bukkitPlayer == null) {
			return false;
		} else {
			return onlinePlayers.containsKey(bukkitPlayer);
		}
	}
	
	@Nullable
	public KonOfflinePlayer getOfflinePlayer(OfflinePlayer offlineBukkitPlayer) {
		return allPlayers.get(offlineBukkitPlayer);
	}
	
	public boolean isOfflinePlayer(OfflinePlayer offlineBukkitPlayer) {
		return allPlayers.containsKey(offlineBukkitPlayer);
	}
	
	public boolean isPlayerNameExist(String name) {
		boolean result = false;
		for(OfflinePlayer offlineBukkitPlayer : allPlayers.keySet()) {
			if(offlineBukkitPlayer.getName() != null && name.equalsIgnoreCase(offlineBukkitPlayer.getName())) {
				result = true;
				break;
			}
		}
		return result;
	}
	
	/**
	 * This method is used for creating a new KonPlayer that has never joined before.
	 * Once the new player is created, attempt to match it to existing memberships in kingdoms/towns.
	 * @param bukkitPlayer - Bukkit player
	 * @return KonPlayer of the bukkit player
	 */
	@NotNull
	public KonPlayer createKonPlayer(Player bukkitPlayer) {
		KonPlayer newPlayer = new KonPlayer(bukkitPlayer, earth.getKingdomManager().getBarbarians(), true);
		// Attempt to match existing memberships
		UUID id = bukkitPlayer.getUniqueId();
		for(KonKingdom kingdom : earth.getKingdomManager().getKingdoms()) {
			if(kingdom.isMember(id)) {
				// Found a kingdom membership, update player's data
				newPlayer.setKingdom(kingdom);
				newPlayer.setExileKingdom(kingdom);
				newPlayer.setBarbarian(false);
				ChatUtil.printConsoleWarning("New player "+bukkitPlayer.getName()+" already has kingdom membership in "+kingdom.getName()+". Check your SQL database settings in core.yml, the database connection may have been lost or corrupted.");
				break;
			}
		}
		onlinePlayers.put(bukkitPlayer, newPlayer);
		linkOnlinePlayerToCache(newPlayer);
		ChatUtil.printDebug("Created player "+bukkitPlayer.getName());
        return newPlayer;
    }
	
	/**
	 * This method is used for instantiating a KonPlayer using info from the database
	 * @param bukkitPlayer - Bukkit player
	 * @param kingdomName - Name of the kingdom the player is in
	 * @param exileKingdomName - Name of the kingdom the player is exiled from
	 * @param isBarbarian - Is the player a barbarian
	 * @return KonPlayer of the bukkit player
	 */
	@NotNull
	public KonPlayer importKonPlayer(Player bukkitPlayer, String kingdomName, String exileKingdomName, boolean isBarbarian) {
		KonPlayer importedPlayer;
		UUID id = bukkitPlayer.getUniqueId();
    	// Create KonPlayer instance
    	if(isBarbarian) {
    		// Player data has barbarian flag set true, create barbarian player
    		importedPlayer = new KonPlayer(bukkitPlayer, earth.getKingdomManager().getBarbarians(), true);
			// Attempt to match existing memberships
			for(KonKingdom kingdom : earth.getKingdomManager().getKingdoms()) {
				if(kingdom.isMember(id)) {
					// Found a kingdom membership, update player's data
					importedPlayer.setKingdom(kingdom);
					importedPlayer.setExileKingdom(kingdom);
					importedPlayer.setBarbarian(false);
					ChatUtil.printConsoleWarning("Existing barbarian player "+bukkitPlayer.getName()+" has kingdom membership in "+kingdom.getName()+". Check your SQL database settings in core.yml, the database connection may have been lost or corrupted.");
					break;
				}
			}
    	} else {
    		// Player's barbarian flag is false, should belong to a kingdom
    		KonKingdom playerKingdom = earth.getKingdomManager().getKingdom(kingdomName);

			// Check for valid kingdom membership
			if(playerKingdom.equals(earth.getKingdomManager().getBarbarians())) {
				// The player's kingdom does not exist
				// Possibly renamed or removed
				// When a kingdom is renamed, all offline players have their database entry updated with the new kingdom name.
				importedPlayer = new KonPlayer(bukkitPlayer, earth.getKingdomManager().getBarbarians(), true);

				// Attempt to match existing memberships
				boolean foundMembership = false;
				for(KonKingdom kingdom : earth.getKingdomManager().getKingdoms()) {
					if(kingdom.isMember(id)) {
						// Found a kingdom membership, update player's data
						importedPlayer.setKingdom(kingdom);
						importedPlayer.setExileKingdom(kingdom);
						importedPlayer.setBarbarian(false);
						ChatUtil.printConsoleWarning("Existing player "+bukkitPlayer.getName()+" is in an unknown kingdom, but already has kingdom membership in "+kingdom.getName()+". Check your SQL database settings in core.yml, the database connection may have been lost or corrupted.");
						break;
					}
				}
				if(!foundMembership) {
					ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_FORCE_BARBARIAN.getMessage());
					ChatUtil.printDebug("Forced non-barbarian player with missing kingdom to become a barbarian");
				}
			} else {
				// The player's kingdom was found
				if(playerKingdom.isMember(bukkitPlayer.getUniqueId())) {
					// The player is a kingdom member
					importedPlayer = new KonPlayer(bukkitPlayer, playerKingdom, false);
				} else {
					// The player is not a member of their kingdom
					// Possibly kicked or renamed
					// The most likely scenario is that the player was kicked.
					// Just make them a barbarian.
					importedPlayer = new KonPlayer(bukkitPlayer, earth.getKingdomManager().getBarbarians(), true);
					ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_FORCE_BARBARIAN.getMessage());
					ChatUtil.printDebug("Forced non-barbarian player without kingdom membership to become a barbarian");
					// Remove any town residencies to be safe
					playerKingdom.removeTownResidencies(bukkitPlayer.getUniqueId());
				}
			}
    	}
    	
    	// Check if player has exceeded offline timeout
    	long timeoutSeconds = earth.getOfflineTimeoutSeconds();
    	long lastSeenTimeMilliseconds = bukkitPlayer.getLastPlayed();
    	// ignore this check if timeout is 0 or if there is no lastSeen timestamp
    	if(timeoutSeconds != 0 && lastSeenTimeMilliseconds != 0) {
    		Date now = new Date();
    		if(now.after(new Date(lastSeenTimeMilliseconds + (timeoutSeconds*1000)))) {
    			ChatUtil.printDebug("Player has exceeded offline timeout");
				// Inform player of purged residencies
				// The process of purging the player should have already happened in the past, this
				// check is just to inform them that it happened.
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_ABSENT.getMessage());
    		}
    	}
    	
    	// Update player's exile kingdom
    	if(earth.getKingdomManager().isKingdom(exileKingdomName)) {
    		// Apply saved info
    		importedPlayer.setExileKingdom(earth.getKingdomManager().getKingdom(exileKingdomName));
    	} else {
    		// By default, make exile kingdom barbarians
    		importedPlayer.setExileKingdom(earth.getKingdomManager().getBarbarians());
    	}
    	
    	if(onlinePlayers.containsKey(bukkitPlayer)) {
    		ChatUtil.printDebug("Skipped importing existing player "+bukkitPlayer.getName());
    	} else {
    		onlinePlayers.put(bukkitPlayer, importedPlayer);
        	linkOnlinePlayerToCache(importedPlayer);
        	ChatUtil.printDebug("Imported player "+bukkitPlayer.getName());
    	}
    	
    	return importedPlayer;
	}
	
	/**
	 * This method puts an online player into the allPlayers cache and removes any existing duplicates by UUID.
	 * This ensures that online players with changed fields will be visible to other methods utilizing the cache for player data.
	 * @param player - KonPlayer to link to the cache
	 */
	private void linkOnlinePlayerToCache(KonPlayer player) {
		OfflinePlayer bukkitOfflinePlayer = player.getOfflineBukkitPlayer();
		// Search cache for duplicates and remove
		ArrayList<OfflinePlayer> removingList = new ArrayList<>();
		for(OfflinePlayer offline : allPlayers.keySet()) {
			if(offline.getUniqueId().equals(bukkitOfflinePlayer.getUniqueId())) {
				removingList.add(offline);
			}
		}
		for(OfflinePlayer offline : removingList) {
			allPlayers.remove(offline);
		}
		// Put online player into cache
		if(bukkitOfflinePlayer.getName() != null) {
			// Removing the cast to KonOfflinePlayer should preserve the same object in both allPlayers and onlinePlayers maps.
			// When a KonPlayer is modified from onlinePlayers, it should also reflect in allPlayers, and vice-versa.
			allPlayers.put(bukkitOfflinePlayer, player);
		}
	}

    public KonPlayer getPlayerFromName(String displayName) {
    	for(KonPlayer player : onlinePlayers.values()) {
			if(player != null &&
					player.getBukkitPlayer().getName() != null &&
					player.getBukkitPlayer().getName().equalsIgnoreCase(displayName)) {
				return player;
			}
    	}
        return null;
    }
    
    public KonOfflinePlayer getOfflinePlayerFromName(String displayName) {
    	for(KonOfflinePlayer offlinePlayer : allPlayers.values()) {
    		if(offlinePlayer != null && 
    				offlinePlayer.getOfflineBukkitPlayer().getName() != null &&
    				offlinePlayer.getOfflineBukkitPlayer().getName().equalsIgnoreCase(displayName)) {
    			return offlinePlayer;
    		}
    	}
        return null;
    }
    
    public KonPlayer getPlayerFromID(UUID id) {
    	for(KonPlayer player : onlinePlayers.values()) {
    		if(player != null &&
					player.getBukkitPlayer().getName() != null &&
					player.getBukkitPlayer().getUniqueId().equals(id)) {
				return player;
			}
    	}
        return null;
    }
    
    public KonOfflinePlayer getOfflinePlayerFromID(UUID id) {
    	for(KonOfflinePlayer offlinePlayer : allPlayers.values()) {
    		if(offlinePlayer != null &&
    				offlinePlayer.getOfflineBukkitPlayer().getName() != null &&
    				offlinePlayer.getOfflineBukkitPlayer().getUniqueId().equals(id)) {
    			return offlinePlayer;
    		}
    	}
        return null;
    }
    
    public Collection<OfflinePlayer> getAllOfflinePlayers() {
		return new HashSet<>(allPlayers.keySet());
    }
    
    public Collection<KonOfflinePlayer> getAllEarthOfflinePlayers() {
		return new HashSet<>(allPlayers.values());
    }
    
    public ArrayList<KonPlayer> getPlayersInKingdom(String kingdomName) {
    	ArrayList<KonPlayer> playerList = new ArrayList<>();
    	for(KonPlayer player : onlinePlayers.values()) {
    		if(player.getKingdom().getName().equalsIgnoreCase(kingdomName)) {
    			playerList.add(player);
    		}
    	}
    	return playerList;
    }
    
    public ArrayList<KonPlayer> getPlayersInKingdom(EarthKingdom kingdom) {
    	ArrayList<KonPlayer> playerList = new ArrayList<>();
    	for(KonPlayer player : onlinePlayers.values()) {
    		if(player.getKingdom().equals(kingdom)) {
    			playerList.add(player);
    		}
    	}
    	return playerList;
    }
    
    public ArrayList<String> getPlayerNamesInKingdom(String kingdomName) {
    	ArrayList<String> playerNameList = new ArrayList<>();
    	for(KonPlayer player : onlinePlayers.values()) {
    		if(player.getKingdom().getName().equalsIgnoreCase(kingdomName)) {
    			playerNameList.add(player.getBukkitPlayer().getName());
    		}
    	}
    	return playerNameList;
    }
    
    public ArrayList<String> getPlayerNames() {
    	ArrayList<String> playerNameList = new ArrayList<>();
    	for(KonPlayer player : onlinePlayers.values()) {
    		playerNameList.add(player.getBukkitPlayer().getName());
    	}
    	return playerNameList;
    }
    
    public ArrayList<String> getAllPlayerNames() {
    	ArrayList<String> playerNameList = new ArrayList<>();
    	for(KonOfflinePlayer player : allPlayers.values()) {
    		playerNameList.add(player.getOfflineBukkitPlayer().getName());
    	}
    	return playerNameList;
    }
    
    public ArrayList<KonOfflinePlayer> getAllPlayersInKingdom(String kingdomName) {
    	ArrayList<KonOfflinePlayer> playerList = new ArrayList<>();
    	for(KonOfflinePlayer player : allPlayers.values()) {
    		if(player.getKingdom().getName().equalsIgnoreCase(kingdomName)) {
    			playerList.add(player);
    		}
    	}
    	return playerList;
    }
    
    public ArrayList<KonOfflinePlayer> getAllPlayersInKingdom(EarthKingdom kingdom) {
    	ArrayList<KonOfflinePlayer> playerList = new ArrayList<>();
    	for(KonOfflinePlayer player : allPlayers.values()) {
    		if(player.getKingdom().equals(kingdom)) {
    			playerList.add(player);
    		}
    	}
    	return playerList;
    }

	// Returns a list of players inside or near the edge (within 2 chunks) of a territory
	public ArrayList<KonPlayer> getPlayersNearTerritory(KonTerritory territory) {
		ArrayList<KonPlayer> playerList = new ArrayList<>();
		for(KonPlayer onlinePlayer : onlinePlayers.values()) {
			for(Chunk chunk : HelperUtil.getAreaChunks(onlinePlayer.getBukkitPlayer().getLocation(), 2)) {
				if(territory.hasChunk(chunk)) {
					playerList.add(onlinePlayer);
					break;
				}
			}
		}
		return playerList;
	}
    
    public ArrayList<KonPlayer> getPlayersInMonument(KonMonument monument) {
    	ArrayList<KonPlayer> playerList = new ArrayList<>();
    	Chunk monumentChunk = monument.getTravelPoint().getChunk();
    	Chunk playerChunk;
    	int playerY;
    	for(KonPlayer player : onlinePlayers.values()) {
    		playerChunk = player.getBukkitPlayer().getLocation().getChunk();
    		playerY = player.getBukkitPlayer().getLocation().getBlockY();
    		boolean isInChunk = playerChunk.getX() == monumentChunk.getX() && playerChunk.getZ() == monumentChunk.getZ();
    		if(isInChunk && playerY >= monument.getBaseY() && playerY <= monument.getTopY()) {
    			playerList.add(player);
    		}
    	}
    	return playerList;
    }
    
    public Collection<KonPlayer> getPlayersOnline() {
		return new HashSet<>(onlinePlayers.values());
    }

	/**
	 * This method should only be called on plugin enable,
	 * when the database is initialized.
	 * Fetch all saved players from the database, and add them to
	 * the allPlayers cache.
	 * Run checks to ensure that database kingdom matches membership in that kingdom.
	 */
	public void initAllSavedPlayers() {
    	allPlayers.clear();
    	// Fetch all players from database
    	for(KonOfflinePlayer offlinePlayer : earth.getDatabaseThread().getDatabase().getAllSavedPlayers()) {
    		// Add player to cache
    		allPlayers.put(offlinePlayer.getOfflineBukkitPlayer(), offlinePlayer);
			// Check player's kingdom
			KonKingdom playerKingdom = offlinePlayer.getKingdom();
			UUID id = offlinePlayer.getOfflineBukkitPlayer().getUniqueId();
			String playerName = offlinePlayer.getOfflineBukkitPlayer().getName();
			if(playerKingdom.isCreated() && !playerKingdom.isMember(id)) {
				// The player is not in the membership list of the kingdom from their database entry.
				// This should not happen.
				ChatUtil.printDebug("Init allPlayers cache failed to match kingdom membership for player "+playerName+" in kingdom "+playerKingdom.getName());
				playerKingdom.addMember(id,false);
			}
    	}
		ChatUtil.printDebug("Initialized " + allPlayers.size() + " players from saved data.");
    }

	public Collection<Player> getBukkitPlayersInKingdom(String kingdomName) {
		Collection<Player> playerList = new HashSet<>();
    	for(Player bukkitPlayer : onlinePlayers.keySet()) {
    		KonPlayer player = onlinePlayers.get(bukkitPlayer);
    		if(player.getKingdom().getName().equalsIgnoreCase(kingdomName)) {
    			playerList.add(bukkitPlayer);
    		}
    	}
    	return playerList;
	}

	public Collection<Player> getBukkitPlayersInKingdom(EarthKingdom kingdom) {
		Collection<Player> playerList = new HashSet<>();
    	for(Player bukkitPlayer : onlinePlayers.keySet()) {
    		KonPlayer player = onlinePlayers.get(bukkitPlayer);
    		if(player.getKingdom().equals(kingdom)) {
    			playerList.add(bukkitPlayer);
    		}
    	}
    	return playerList;
	}

	public Collection<OfflinePlayer> getAllBukkitPlayersInKingdom(String kingdomName) {
		Collection<OfflinePlayer> playerList = new HashSet<>();
    	for(OfflinePlayer bukkitOfflinePlayer : allPlayers.keySet()) {
    		KonOfflinePlayer player = allPlayers.get(bukkitOfflinePlayer);
    		if(player.getKingdom().getName().equalsIgnoreCase(kingdomName)) {
    			playerList.add(bukkitOfflinePlayer);
    		}
    	}
    	return playerList;
	}

	public Collection<OfflinePlayer> getAllBukkitPlayersInKingdom(EarthKingdom kingdom) {
    	Collection<OfflinePlayer> playerList = new HashSet<>();
    	for(OfflinePlayer bukkitOfflinePlayer : allPlayers.keySet()) {
    		KonOfflinePlayer player = allPlayers.get(bukkitOfflinePlayer);
    		if(player.getKingdom().equals(kingdom)) {
    			playerList.add(bukkitOfflinePlayer);
    		}
    	}
    	return playerList;
	}

	public Collection<Player> getBukkitPlayersOnline() {
		return new HashSet<>(onlinePlayers.keySet());
	}

}
