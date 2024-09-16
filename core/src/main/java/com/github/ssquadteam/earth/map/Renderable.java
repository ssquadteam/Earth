package com.github.ssquadteam.earth.map;

import com.github.ssquadteam.earth.model.KonKingdom;
import com.github.ssquadteam.earth.model.KonTerritory;

public interface Renderable {

    /**
     * Initialize the API associated with the renderer.
     */
    void initialize();

    /**
     * Draw the capital and all towns in a kingdom on the map.
     * @param kingdom The kingdom to draw
     */
    void drawUpdate(KonKingdom kingdom);

    /**
     * Draw the region of a territory on the map.
     * Adds the region if it doesn't already exist.
     * Updates the region if it exists.
     * @param territory The territory to draw
     */
    void drawUpdate(KonTerritory territory);

    /**
     * Remove the region of a territory from the map.
     * @param territory The territory to remove
     */
    void drawRemove(KonTerritory territory);

    /**
     * Update the label of a territory.
     * @param territory The territory to update
     */
    void drawLabel(KonTerritory territory);

    /**
     * Post a message to the web map.
     * @param message The message to send
     */
    void postBroadcast(String message);

}
