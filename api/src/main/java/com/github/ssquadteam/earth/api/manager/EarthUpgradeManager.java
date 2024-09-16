package com.github.ssquadteam.earth.api.manager;

import com.github.ssquadteam.earth.api.model.EarthTown;
import com.github.ssquadteam.earth.api.model.EarthUpgrade;

import java.util.HashMap;

/**
 * A manager for town upgrades in Earth.
 * 
 * @author squadsteam
 *
 */
public interface EarthUpgradeManager {

	/**
	 * Check whether town upgrades are enabled from the Earth configuration.
	 * 
	 * @return True when upgrades are enabled, else false
	 */
    boolean isEnabled();
	
	/**
	 * Provides a list of upgrades available for purchase for the given town.
	 * Checks for valid cost and level requirements.
	 * Upgrade levels will not be listed if cost is less than 0 (e.g. -1).
	 * 
	 * @param town The town to search for available upgrades
	 * @return A map of available upgrades and their levels
	 */
    HashMap<EarthUpgrade,Integer> getAvailableUpgrades(EarthTown town);
	
	/**
	 * Gets the cost in favor of the given upgrade and level.
	 * The given level must be greater than 0 and less than or equal to the upgrade's max level.
	 * Upgrade costs are pulled from Earth configuration.
	 * 
	 * @param upgrade The upgrade
	 * @param level The level of the given upgrade
	 * @return The cost in favor, or -1 when level is not valid
	 */
    int getUpgradeCost(EarthUpgrade upgrade, int level);
	
	/**
	 * Gets the population requirement of the given upgrade and level.
	 * The given level must be greater than 0 and less than or equal to the upgrade's max level.
	 * Upgrade populations are pulled from Earth configuration.
	 * 
	 * @param upgrade The upgrade
	 * @param level The level of the given upgrade
	 * @return The population, or 0 when level is not valid
	 */
    int getUpgradePopulation(EarthUpgrade upgrade, int level);
	
	/**
	 * Gets the current upgrade level of the given town.
	 * Returns 0 if the town does not have the upgrade.
	 * 
	 * @param town The town to check
	 * @param upgrade The upgrade
	 * @return The level of the upgrade, or 0 if the upgrade is not applied
	 */
    int getTownUpgradeLevel(EarthTown town, EarthUpgrade upgrade);
	
	/**
	 * Apply the given upgrade and level to the given town.
	 * The given level must be greater than or equal to 0 and less than or equal to the upgrade's max level.
	 * Applying a level of 0 will remove the upgrade.
	 * The upgrade feature must be enabled in the Earth configuration.
	 * 
	 * @param town The town to upgrade
	 * @param upgrade The upgrade to apply
	 * @param level The level of the upgrade
	 * @return True when the upgrade is applied successfully, else false
	 */
    boolean applyTownUpgrade(EarthTown town, EarthUpgrade upgrade, int level);
	
}
