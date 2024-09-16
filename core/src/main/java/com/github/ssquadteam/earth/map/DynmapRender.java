package com.github.ssquadteam.earth.map;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.api.model.EarthTerritoryType;
import com.github.ssquadteam.earth.model.*;
import com.github.ssquadteam.earth.utility.ChatUtil;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;

import java.util.HashMap;
import java.util.Objects;

public class DynmapRender implements Renderable {

    private final Earth earth;
    private boolean isEnabled;
    private static DynmapAPI dapi = null;
    private final HashMap<KonTerritory,AreaTerritory> areaCache;

    public DynmapRender(Earth earth) {
        this.earth = earth;
        this.isEnabled = false;
        this.areaCache = new HashMap<>();
    }

    @Override
    public void initialize() {
        // Get Dynmap API from integration manager
        isEnabled = earth.getIntegrationManager().getDynmap().isEnabled();
        if(isEnabled) {
            dapi = earth.getIntegrationManager().getDynmap().getAPI();
        }
    }

    /*
     * Dynmap Area ID formats:
     * Sanctuaries
     * 		MarkerSet Group:  		earth.marker.sanctuary
     * 		AreaMarker Points: 		earth.area.sanctuary.<name>.point.<n>
     * 		AreaMarker Contours: 	earth.area.sanctuary.<name>.contour.<n>
     * Ruins
     * 		MarkerSet Group:  		earth.marker.ruin
     * 		AreaMarker Points: 		earth.area.ruin.<name>.point.<n>
     * 		AreaMarker Contours: 	earth.area.ruin.<name>.contour.<n>
     * Camps
     * 		MarkerSet Group:  		earth.marker.camp
     * 		AreaMarker Points: 		earth.area.camp.<name>.point.<n>
     * 		AreaMarker Contours: 	earth.area.camp.<name>.contour.<n>
     * Kingdoms
     * 		MarkerSet Group:  		earth.marker.kingdom
     * 		Capital
     * 		AreaMarker Points: 		earth.area.kingdom.<kingdom>.capital.point.<n>
     * 		AreaMarker Contours: 	earth.area.kingdom.<kingdom>.capital.contour.<n>
     * 		Towns
     * 		AreaMarker Points: 		earth.area.kingdom.<kingdom>.<town>.point.<n>
     * 		AreaMarker Contours: 	earth.area.kingdom.<kingdom>.<town>.contour.<n>
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
        String groupLabel = MapHandler.getGroupLabel(territory);
        String areaId = getAreaId(territory);
        String areaLabel = MapHandler.getAreaLabel(territory);
        int areaColor = getAreaColor(territory);
        int lineColor = getLineColor(territory);
        String iconId = getIconId(territory);
        String iconLabel = MapHandler.getIconLabel(territory);
        MarkerIcon icon = getIconMarker(territory);
        // Get territory group
        MarkerSet territoryGroup = dapi.getMarkerAPI().getMarkerSet(groupId);
        if (territoryGroup == null) {
            territoryGroup = dapi.getMarkerAPI().createMarkerSet(groupId, groupLabel, dapi.getMarkerAPI().getMarkerIcons(), false);
        }
        String pointId;
        String contourId;
        AreaMarker areaPoint;
        AreaMarker areaContour;
        // Prune any points and contours
        AreaTerritory drawArea = new AreaTerritory(territory);
        if(areaCache.containsKey(territory)) {
            // Territory is already rendered
            AreaTerritory oldArea = areaCache.get(territory);
            for(int i = (oldArea.getNumContours()-1); i >= drawArea.getNumContours(); i--) {
                contourId = areaId + ".contour." + i;
                areaContour = territoryGroup.findAreaMarker(contourId);
                if (areaContour != null) {
                    // Delete area from group
                    areaContour.deleteMarker();
                }
            }
            for(int i = (oldArea.getNumPoints()-1); i >= drawArea.getNumPoints(); i--) {
                pointId = areaId + ".point." + i;
                areaPoint = territoryGroup.findAreaMarker(pointId);
                if (areaPoint != null) {
                    // Delete area from group
                    areaPoint.deleteMarker();
                }
            }
        }
        areaCache.put(territory, drawArea);
        // Update or create all points and contours
        for(int i = 0; i < drawArea.getNumContours(); i++) {
            contourId = areaId + ".contour." + i;
            areaContour = territoryGroup.findAreaMarker(contourId);
            if (areaContour == null) {
                // Area does not exist, create new
                areaContour = territoryGroup.createAreaMarker(contourId, "", false, drawArea.getWorldName(), drawArea.getXContour(i), drawArea.getZContour(i), false);
                if (areaContour != null) {
                    areaContour.setFillStyle(0, areaColor);
                    areaContour.setLineStyle(1, 1, lineColor);
                }
            } else {
                // Area already exists, update corners and color
                areaContour.setCornerLocations(drawArea.getXContour(i), drawArea.getZContour(i));
                areaContour.setFillStyle(0, areaColor);
            }
        }
        for(int i = 0; i < drawArea.getNumPoints(); i++) {
            pointId = areaId + ".point." + i;
            areaPoint = territoryGroup.findAreaMarker(pointId);
            if (areaPoint == null) {
                // Area does not exist, create new
                areaPoint = territoryGroup.createAreaMarker(pointId, areaLabel, true, drawArea.getWorldName(), drawArea.getXPoint(i), drawArea.getZPoint(i), false);
                if (areaPoint != null) {
                    areaPoint.setFillStyle(0.5, areaColor);
                    areaPoint.setLineStyle(0, 0, lineColor);
                    areaPoint.setLabel(areaLabel,true);
                }
            } else {
                // Area already exists, update corners and label and color
                areaPoint.setCornerLocations(drawArea.getXPoint(i), drawArea.getZPoint(i));
                areaPoint.setLabel(areaLabel,true);
                areaPoint.setFillStyle(0.5, areaColor);
            }
        }
        Marker territoryIcon = territoryGroup.findMarker(iconId);
        if (territoryIcon == null) {
            // Icon does not exist, create new
            territoryIcon = territoryGroup.createMarker(iconId, iconLabel, true, drawArea.getWorldName(), drawArea.getCenterX(), drawArea.getCenterY(), drawArea.getCenterZ(), icon, false);
        } else {
            // Icon already exists, update label
            territoryIcon.setLabel(iconLabel);
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
        ChatUtil.printDebug("Erasing Dynmap area of territory "+territory.getName());
        String groupId = getGroupId(territory);
        String areaId = getAreaId(territory);
        String iconId = getIconId(territory);

        MarkerSet territoryGroup = dapi.getMarkerAPI().getMarkerSet(groupId);
        if (territoryGroup != null) {
            if(areaCache.containsKey(territory)) {
                // Territory is already rendered, remove all points and contours
                String pointId;
                String contourId;
                AreaMarker areaPoint;
                AreaMarker areaContour;
                AreaTerritory oldArea = areaCache.get(territory);
                for(int i = 0; i < oldArea.getNumContours(); i++) {
                    contourId = areaId + ".contour." + i;
                    areaContour = territoryGroup.findAreaMarker(contourId);
                    if (areaContour != null) {
                        // Delete area from group
                        areaContour.deleteMarker();
                    }
                }
                for(int i = 0; i < oldArea.getNumPoints(); i++) {
                    pointId = areaId + ".point." +i ;
                    areaPoint = territoryGroup.findAreaMarker(pointId);
                    if (areaPoint != null) {
                        // Delete area from group
                        areaPoint.deleteMarker();
                    }
                }
            } else {
                ChatUtil.printDebug("Failed to erase un-rendered territory "+territory.getName());
            }
            Marker territoryIcon = territoryGroup.findMarker(iconId);
            if (territoryIcon != null) {
                // Delete icon
                territoryIcon.deleteMarker();
                ChatUtil.printDebug("Removing Dynmap icon of territory "+territory.getName());
            }
            if (territoryGroup.getAreaMarkers().isEmpty()) {
                // Delete group if no more areas
                territoryGroup.deleteMarkerSet();
                ChatUtil.printDebug("Removing Dynmap group of territory "+territory.getName());
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
        String areaLabel = MapHandler.getAreaLabel(territory);
        MarkerSet territoryGroup = dapi.getMarkerAPI().getMarkerSet(groupId);
        if (territoryGroup != null) {
            // Update all area point labels
            if(areaCache.containsKey(territory)) {
                // Territory is already rendered, remove all points and contours
                String pointId;
                AreaMarker areaPoint;
                AreaTerritory oldArea = areaCache.get(territory);
                for(int i = 0; i < oldArea.getNumPoints(); i++) {
                    pointId = areaId + ".point." +i ;
                    areaPoint = territoryGroup.findAreaMarker(pointId);
                    if (areaPoint != null) {
                        // Area already exists, update label
                        areaPoint.setLabel(areaLabel,true);
                    }
                }
            } else {
                ChatUtil.printDebug("Failed to label un-rendered territory "+territory.getName());
            }
        }
    }

    @Override
    public void postBroadcast(String message) {
        if (!isEnabled) return;
        dapi.sendBroadcastToWeb("Earth", message);
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

    private int getAreaColor(KonTerritory territory) {
        int result = 0xFFFFFF;
        switch (territory.getTerritoryType()) {
            case SANCTUARY:
                result = MapHandler.sanctuaryColor;
                break;
            case RUIN:
                result = MapHandler.ruinColor;
                break;
            case CAMP:
                result = MapHandler.campColor;
                break;
            case CAPITAL:
            case TOWN:
                result = MapHandler.getWebColor(territory);
                break;
            default:
                break;
        }
        return result;
    }

    private int getLineColor(KonTerritory territory) {
        int result = MapHandler.lineDefaultColor;
        if (Objects.requireNonNull(territory.getTerritoryType()) == EarthTerritoryType.CAPITAL) {
            result = MapHandler.lineCapitalColor;
        }
        return result;
    }

    private String getIconId(KonTerritory territory) {
        String result = "earth";
        switch (territory.getTerritoryType()) {
            case SANCTUARY:
                result = result+".icon.sanctuary."+territory.getName().toLowerCase();
                break;
            case RUIN:
                result = result+".icon.ruin."+territory.getName().toLowerCase();
                break;
            case CAMP:
                result = result+".icon.camp."+territory.getName().toLowerCase();
                break;
            case CAPITAL:
                result = result+".icon.kingdom."+territory.getKingdom().getName().toLowerCase()+".capital";
                break;
            case TOWN:
                result = result+".icon.kingdom."+territory.getKingdom().getName().toLowerCase()+"."+territory.getName().toLowerCase();
                break;
            default:
                break;
        }
        return result;
    }

    private MarkerIcon getIconMarker(KonTerritory territory) {
        MarkerIcon result = null;
        switch (territory.getTerritoryType()) {
            case SANCTUARY:
                result = dapi.getMarkerAPI().getMarkerIcon("temple");
                break;
            case RUIN:
                result = dapi.getMarkerAPI().getMarkerIcon("tower");
                break;
            case CAMP:
                result = dapi.getMarkerAPI().getMarkerIcon("pirateflag");
                break;
            case CAPITAL:
                result = dapi.getMarkerAPI().getMarkerIcon("star");
                break;
            case TOWN:
                result = dapi.getMarkerAPI().getMarkerIcon("orangeflag");
                break;
            default:
                break;
        }
        return result;
    }

}
