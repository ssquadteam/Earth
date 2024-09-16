package com.github.ssquadteam.earth.api.model;

import org.bukkit.Material;

/**
 * The types of kingdom diplomatic relationships in Earth.
 *
 * @author squadsteam
 *
 */
public enum EarthDiplomacyType {

    /**
     * Kingdoms at war can attack each other and capture towns.
     */
    WAR				    (Material.TNT),
    /**
     * Peaceful kingdoms cannot fight in towns and may allow pvp.
     */
    PEACE				(Material.QUARTZ_PILLAR),
    /**
     * Trading kingdoms allow villager trades and cannot pvp.
     */
    TRADE			    (Material.GOLD_BLOCK),
    /**
     * Allied kingdoms can travel to each other's towns and may share a defense pact.
     */
    ALLIANCE			(Material.DIAMOND_BLOCK);

    private final Material icon;

    EarthDiplomacyType(Material icon) {
        this.icon = icon;
    }

    /**
     * Gets the display icon material used in GUI menus
     *
     * @return The icon material
     */
    public Material getIcon() {
        return icon;
    }

    /**
     * Gets the default diplomatic state
     *
     * @return The default state
     */
    public static EarthDiplomacyType getDefault() {
        return PEACE;
    }

}
