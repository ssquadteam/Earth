package com.github.ssquadteam.earth.utility;

import com.github.ssquadteam.earth.api.model.EarthDiplomacyType;

/**
 * This class is a utility wrapper for kingdom diplomacy relationships.
 * It is used while loading all kingdoms on plugin startup to queue up diplomatic assignments
 * after all kingdoms are loaded from file.
 */
public class DiplomacyTicket {

    private final String ownerKingdomName;
    private final String otherKingdomName;
    private final EarthDiplomacyType relation;
    private final boolean isActive;

    public DiplomacyTicket(String ownerKingdomName, String otherKingdomName, EarthDiplomacyType relation, boolean isActive) {
        this.ownerKingdomName = ownerKingdomName;
        this.otherKingdomName = otherKingdomName;
        this.relation = relation;
        this.isActive = isActive;
    }

    public String getOwnerKingdomName() {
        return ownerKingdomName;
    }

    public String getOtherKingdomName() {
        return otherKingdomName;
    }

    public EarthDiplomacyType getRelation() {
        return relation;
    }

    public boolean isActive() {
        return isActive;
    }

}
