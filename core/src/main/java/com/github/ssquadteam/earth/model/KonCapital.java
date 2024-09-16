package com.github.ssquadteam.earth.model;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.api.model.EarthCapital;
import com.github.ssquadteam.earth.api.model.EarthTerritoryType;
import com.github.ssquadteam.earth.utility.CorePath;

import org.bukkit.Location;


public class KonCapital extends KonTown implements EarthCapital {

	public KonCapital(Location loc, KonKingdom kingdom, Earth earth) {
		super(loc, kingdom.getName(), kingdom, earth);
		updateName();
	}
	
	public KonCapital(Location loc, String name, KonKingdom kingdom, Earth earth) {
		super(loc, name, kingdom, earth);
	}
	
	public void updateName() {
		setName(getCapitalName());
		updateBarTitle();
	}

	private String getCapitalName() {
		boolean isPrefix = getEarth().getCore().getBoolean(CorePath.KINGDOMS_CAPITAL_PREFIX_SWAP.getPath(),false);
		String suffix = getEarth().getCore().getString(CorePath.KINGDOMS_CAPITAL_SUFFIX.getPath(),"");
		String name = getKingdom().getName();
		if(isPrefix) {
			// Apply descriptor before name
			return suffix+" "+name;
		} else {
			// Apply descriptor after name
			return name+" "+suffix;
		}
	}
	
	@Override
	public EarthTerritoryType getTerritoryType() {
		return EarthTerritoryType.CAPITAL;
	}

}
