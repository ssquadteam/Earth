package com.github.ssquadteam.earth.api.manager;

import com.github.ssquadteam.earth.api.model.EarthRuin;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.Collection;
import java.util.Set;

/**
 * A manager for ruins in Earth.
 * 
 * @author squadsteam
 *
 */
public interface EarthRuinManager {

	/**
	 * Checks whether the given name is a ruin.
	 * 
	 * @param name The name of the ruin, case-insensitive
	 * @return True when the name matches a ruin, else false
	 */
    boolean isRuin(String name);
	
	/**
	 * Checks whether the given location is inside of the given ruin.
	 * 
	 * @param ruin The ruin to check
	 * @param loc The location
	 * @return True when the location is inside of the ruin, else false
	 */
    boolean isLocInsideRuin(EarthRuin ruin, Location loc);
	
	/**
	 * Adds a new ruin at the given location with the given name.
	 * The chunk of the location will be the initial claim of the new ruin.
	 * 
	 * @param loc The center location of the new ruin
	 * @param name The name of the new ruin
	 * @return True when the ruin is successfully created, else false
	 */
    boolean addRuin(Location loc, String name);
	
	/**
	 * Removes a ruin by the given name.
	 * 
	 * @param name The ruin name
	 * @return True when the ruin is successfully removed, else false
	 */
    boolean removeRuin(String name);
	
	/**
	 * Gets a ruin by the given name.
	 * 
	 * @param name The ruin name
	 * @return The corresponding ruin instance
	 */
    EarthRuin getRuin(String name);
	
	/**
	 * Gets all ruins.
	 * 
	 * @return A collection of all ruin instances
	 */
    Collection<? extends EarthRuin> getRuins();
	
	/**
	 * A convenience method for getting all ruin names.
	 * 
	 * @return A set of all ruin names
	 */
    Set<String> getRuinNames();
	
	/**
	 * Gets the ruin critical block material.
	 * This is the block type that must be destroyed within ruins in order to earn rewards.
	 * 
	 * @return The critical material
	 */
    Material getRuinCriticalBlock();
}
