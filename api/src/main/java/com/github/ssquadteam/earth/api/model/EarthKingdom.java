package com.github.ssquadteam.earth.api.model;

import java.util.ArrayList;


/**
 * My kingdom for a horse!
 * A kingdom is a capital, a collection of towns, and a monument template.
 * 
 * @author squadsteam
 *
 */
public interface EarthKingdom {

	/**
	 * Checks whether this kingdom is currently the smallest, in terms of players.
	 * 
	 * @return True when this kingdom is the smallest, else false
	 */
    boolean isSmallest();
	
	/**
	 * Checks whether this kingdom is currently protected from attacks.
	 * Kingdoms become protected when there are not enough players online, based on settings in the Earth configuration.
	 * When a kingdom is protected, enemies cannot attack its towns.
	 * 
	 * @return True when this kingdom is protected, else false
	 */
    boolean isOfflineProtected();
	
	/**
	 * Get whether the kingdom is peaceful. Peaceful kingdoms cannot be attacked, or attack others.
	 * 
	 * @return True if peaceful, else false
	 */
    boolean isPeaceful();

	/**
	 * Get whether the kingdom is created by players.
	 * Non-created kingdoms are the defaults included by Earth: Barbarians and Neutrals.
	 *
	 * @return True if created, else false
	 */
	boolean isCreated();

	/**
	 * Get whether the kingdom is operated by admins.
	 * Admin kingdoms cannot have player masters or officers, where player-operated kingdoms can.
	 * Admin kingdoms are created using admin commands.
	 *
	 * @return True if admin operated, else false
	 */
	boolean isAdminOperated();

	/**
	 * Get the active diplomatic relationship of this kingdom to the given kingdom.
	 *
	 * @param kingdom The other kingdom to compare
	 * @return The diplomacy type of this kingdom relative to the given kingdom
	 */
	EarthDiplomacyType getActiveRelation(EarthKingdom kingdom);
	
	/**
	 * Get the name of this kingdom.
	 * 
	 * @return The name
	 */
    String getName();
	
	/**
	 * Gets the capital territory instance for this kingdom.
	 * 
	 * @return The capital territory
	 */
    EarthCapital getCapital();
	
	/**
	 * Gets the list of all town names in this kingdom.
	 * 
	 * @return The list of towns
	 */
    ArrayList<String> getTownNames();
	
	/**
	 * Checks whether this kingdom has a town with the given name.
	 * 
	 * @param name The town name to check
	 * @return True when a town exists with the given name in this kingdom, else false
	 */
    boolean hasTown(String name);
	
	/**
	 * Gets the town instance by the given name.
	 * 
	 * @param name The town name
	 * @return The town instance
	 */
    EarthTown getTown(String name);
	
	/**
	 * Get a list of all towns in this kingdom.
	 * 
	 * @return The list of towns
	 */
    ArrayList<? extends EarthTown> getTowns();
	
}
