package com.github.ssquadteam.earth.map;

import com.flowpowered.math.vector.Vector2d;
import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.api.model.EarthTerritoryType;
import com.github.ssquadteam.earth.model.KonKingdom;
import com.github.ssquadteam.earth.model.KonTerritory;
import com.github.ssquadteam.earth.model.KonTown;
import com.github.ssquadteam.earth.utility.ChatUtil;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.DetailMarker;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;

import java.util.Objects;

public class BlueMapRender implements Renderable {

    private final Earth earth;
    private boolean isEnabled;
    private static BlueMapAPI bapi = null;

    public BlueMapRender(Earth earth) {
        this.earth = earth;
        this.isEnabled = false;
    }

    @Override
    public void initialize() {
        // Get BlueMap API from integration manager
        boolean isReady = earth.getIntegrationManager().getBlueMap().isReady();
        isEnabled = isReady && earth.getIntegrationManager().getBlueMap().isEnabled();
        if(isEnabled) {
            bapi = earth.getIntegrationManager().getBlueMap().getAPI();
            if(bapi == null) {
                isEnabled = false;
                ChatUtil.printDebug("Failed to initialize BlueMapRender with null API reference.");
            }
        }
    }

    /*
     * BlueMap Marker ID formats:
     * Sanctuaries
     * 		MarkerSet Group:  		earth.marker.sanctuary
     * 		Marker Shapes: 	        earth.area.sanctuary.<name>
     * Ruins
     * 		MarkerSet Group:  		earth.marker.ruin
     * 		Marker Shapes: 	        earth.area.ruin.<name>
     * Camps
     * 		MarkerSet Group:  		earth.marker.camp
     * 		Marker Shapes: 	        earth.area.camp.<name>
     * Kingdoms
     * 		MarkerSet Group:  		earth.marker.kingdom
     * 		Capital
     * 		Marker Shapes: 	        earth.area.kingdom.<kingdom>.capital
     * 		Towns
     * 		Marker Shapes: 	        earth.area.kingdom.<kingdom>.<town>
     */

    @Override
    public void drawUpdate(KonKingdom kingdom) {
        drawUpdate(kingdom.getCapital());
        for (KonTown town : kingdom.getTowns()) {
            drawUpdate(town);
        }
    }

    @Override
    public void drawUpdate(KonTerritory territory) {
        if (!isEnabled) return;

        if(MapHandler.isTerritoryDisabled(territory)) return;

        if (MapHandler.isTerritoryInvalid(territory)) {
            ChatUtil.printDebug("Could not draw territory "+territory.getName()+" with invalid type, "+territory.getTerritoryType().toString());
            return;
        }

        String groupId = getGroupId(territory);
        String areaId = getAreaId(territory);
        String groupLabel = MapHandler.getGroupLabel(territory); // The display name of the group
        String areaLabel = MapHandler.getIconLabel(territory); // The display name of the area (icon)
        String areaDetail = MapHandler.getAreaLabel(territory); // The display details of the area
        Color areaColor = getAreaColor(territory);
        Color lineColor = getLineColor(territory);

        // Get territory group, create if doesn't exist
        MarkerSet territoryGroup = null;
        if(bapi.getWorld(territory.getWorld()).isPresent()) {
            BlueMapWorld world = bapi.getWorld(territory.getWorld()).get();
            // Get markerset from first iteration of map collection.
            // Every BlueMapMap in a BlueMapWorld gets the same MarkerSet.
            if(!world.getMaps().isEmpty() && world.getMaps().iterator().hasNext()) {
                territoryGroup = world.getMaps().iterator().next().getMarkerSets().get(groupId);
            }
        }
        if (territoryGroup == null) {
            // Need to create the group
            ChatUtil.printDebug("Creating new group in BlueMap for territory "+territory.getName());
            territoryGroup = MarkerSet.builder()
                    .label(groupLabel)
                    .toggleable(true)
                    .build();
        }

        // Render markers for the territory
        AreaTerritory drawArea = new AreaTerritory(territory);
        float areaY = (float)territory.getCenterLoc().getY();

        // Create an area shape
        ShapeMarker.Builder areaBuilder = ShapeMarker.builder()
                .label(areaLabel)
                .fillColor(areaColor)
                .lineColor(lineColor);
        for(int i = 0; i < drawArea.getNumContours(); i++) {
            // Build a new shape from the current area
            // First contour is always the outline
            // The rest are holes
            double[] xPoints = drawArea.getXContour(i);
            double[] zPoints = drawArea.getZContour(i);
            Shape.Builder shapeBuilder = Shape.builder();
            for(int c = 0; c < xPoints.length; c++) {
                Vector2d point = new Vector2d(xPoints[c], zPoints[c]);
                shapeBuilder.addPoint(point);
            }
            if(i == 0) {
                // First contour is primary shape
                areaBuilder.shape(shapeBuilder.build(),areaY);
            } else {
                // Other contours are holes in the primary shape
                areaBuilder.holes(shapeBuilder.build());
            }
        }
        areaBuilder.centerPosition();
        areaBuilder.depthTestEnabled(false);
        areaBuilder.detail(areaDetail);
        // Add area to group
        territoryGroup.put(areaId,areaBuilder.build());

        // Put group into maps
        if(bapi.getWorld(territory.getWorld()).isPresent()) {
            BlueMapWorld world = bapi.getWorld(territory.getWorld()).get();
            for(BlueMapMap map : world.getMaps()) {
                map.getMarkerSets().put(groupId,territoryGroup);
            }
        }
    }

    @Override
    public void drawRemove(KonTerritory territory) {
        if (!isEnabled) return;

        if(MapHandler.isTerritoryDisabled(territory)) return;

        if (MapHandler.isTerritoryInvalid(territory)) {
            ChatUtil.printDebug("Could not delete territory "+territory.getName()+" with invalid type, "+territory.getTerritoryType().toString());
            return;
        }
        ChatUtil.printDebug("Erasing BlueMap area of territory "+territory.getName());
        String groupId = getGroupId(territory);
        String areaId = getAreaId(territory);

        // Get territory group
        if(bapi.getWorld(territory.getWorld()).isPresent()) {
            BlueMapWorld world = bapi.getWorld(territory.getWorld()).get();
            for(BlueMapMap map : world.getMaps()) {
                if(map.getMarkerSets().containsKey(groupId)) {
                    map.getMarkerSets().get(groupId).remove(areaId);
                    // Delete group if no more areas
                    if(map.getMarkerSets().get(groupId).getMarkers().isEmpty()) {
                        map.getMarkerSets().remove(groupId);
                        ChatUtil.printDebug("Removing BlueMap group of territory "+territory.getName());
                    }
                } else {
                    ChatUtil.printDebug("Failed to erase from missing group, territory "+territory.getName());
                }
            }
        }
    }

    @Override
    public void drawLabel(KonTerritory territory) {
        if (!isEnabled) return;

        if(MapHandler.isTerritoryDisabled(territory)) return;

        if (MapHandler.isTerritoryInvalid(territory)) {
            ChatUtil.printDebug("Could not update label for territory "+territory.getName()+" with invalid type, "+territory.getTerritoryType().toString());
            return;
        }
        String groupId = getGroupId(territory);
        String areaId = getAreaId(territory);
        String areaDetail = MapHandler.getAreaLabel(territory);
        // Get territory group
        if(bapi.getWorld(territory.getWorld()).isPresent()) {
            BlueMapWorld world = bapi.getWorld(territory.getWorld()).get();
            for(BlueMapMap map : world.getMaps()) {
                if(!map.getMarkerSets().containsKey(groupId)) {
                    ChatUtil.printDebug("Failed to set detail of missing group, BlueMap territory "+territory.getName());
                    continue;
                }
                MarkerSet territoryGroup = map.getMarkerSets().get(groupId);
                if(!territoryGroup.getMarkers().containsKey(areaId)) {
                    ChatUtil.printDebug("Failed to set detail of missing area, BlueMap territory "+territory.getName());
                    continue;
                }
                Marker area = territoryGroup.get(areaId);
                if(area instanceof DetailMarker) {
                    ((DetailMarker)area).setDetail(areaDetail);
                } else {
                    ChatUtil.printDebug("Failed to set detail of wrong marker type, BlueMap territory "+territory.getName());
                }
            }
        }
    }

    @Override
    public void postBroadcast(String message) {
        // TODO Does BlueMap support posting messages to the web map?
    }

    /* Helper Methods */

    private String getGroupId(KonTerritory territory) {
        String result = "earth";
        switch (territory.getTerritoryType()) {
            case SANCTUARY:
                result = result+".marker.sanctuary";
                break;
            case RUIN:
                result = result+".marker.ruin";
                break;
            case CAMP:
                result = result+".marker.camp";
                break;
            case CAPITAL:
            case TOWN:
                result = result+".marker.kingdom";
                break;
            default:
                break;
        }
        return result;
    }

    private String getAreaId(KonTerritory territory) {
        String result = "earth";
        switch (territory.getTerritoryType()) {
            case SANCTUARY:
                result = result+".area.sanctuary."+territory.getName().toLowerCase();
                break;
            case RUIN:
                result = result+".area.ruin."+territory.getName().toLowerCase();
                break;
            case CAMP:
                result = result+".area.camp."+territory.getName().toLowerCase();
                break;
            case CAPITAL:
                result = result+".area.kingdom."+territory.getKingdom().getName().toLowerCase()+".capital";
                break;
            case TOWN:
                result = result+".area.kingdom."+territory.getKingdom().getName().toLowerCase()+"."+territory.getName().toLowerCase();
                break;
            default:
                break;
        }
        return result;
    }

    private Color getAreaColor(KonTerritory territory) {
        Color result = new Color(0xFFFFFF, 128);
        switch (territory.getTerritoryType()) {
            case SANCTUARY:
                result = new Color(MapHandler.sanctuaryColor, 128);
                break;
            case RUIN:
                result = new Color(MapHandler.ruinColor, 128);
                break;
            case CAMP:
                result = new Color(MapHandler.campColor, 128);
                break;
            case CAPITAL:
            case TOWN:
                result = new Color(MapHandler.getWebColor(territory), 128);
                break;
            default:
                break;
        }
        return result;
    }

    private Color getLineColor(KonTerritory territory) {
        Color result = new Color(MapHandler.lineDefaultColor, 255);
        if (Objects.requireNonNull(territory.getTerritoryType()) == EarthTerritoryType.CAPITAL) {
            result = new Color(MapHandler.lineCapitalColor, 255);
        }
        return result;
    }

}
