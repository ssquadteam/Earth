package com.github.ssquadteam.earth.utility;

import com.github.ssquadteam.earth.api.model.EarthDiplomacyType;
import com.github.ssquadteam.earth.api.model.EarthTerritoryType;
import com.github.ssquadteam.earth.command.CommandType;
import com.github.ssquadteam.earth.command.admin.AdminCommandType;
import org.bukkit.ChatColor;

/**
 * Provides static methods for looking up MessagePath labels from API enums
 */
public class Labeler {

    public static String lookup(EarthDiplomacyType type) {
        switch(type) {
            case WAR:
                return MessagePath.DIPLOMACY_WAR.getMessage();
            case PEACE:
                return MessagePath.DIPLOMACY_PEACE.getMessage();
            case TRADE:
                return MessagePath.DIPLOMACY_TRADE.getMessage();
            case ALLIANCE:
                return MessagePath.DIPLOMACY_ALLIANCE.getMessage();
            default:
                break;
        }
        return "";
    }

    public static String lookup(EarthTerritoryType type) {
        switch (type) {
            case WILD:
                return MessagePath.TERRITORY_WILD.getMessage();
            case CAPITAL:
                return MessagePath.TERRITORY_CAPITAL.getMessage();
            case TOWN:
                return MessagePath.TERRITORY_TOWN.getMessage();
            case CAMP:
                return MessagePath.TERRITORY_CAMP.getMessage();
            case RUIN:
                return MessagePath.TERRITORY_RUIN.getMessage();
            case SANCTUARY:
                return MessagePath.TERRITORY_SANCTUARY.getMessage();
            default:
                break;
        }
        return "";
    }

}
