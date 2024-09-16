package com.github.ssquadteam.earth.model;

import org.bukkit.OfflinePlayer;

import com.github.ssquadteam.earth.api.model.EarthOfflinePlayer;

public class KonOfflinePlayer implements EarthOfflinePlayer {

	private final OfflinePlayer offlineBukkitPlayer;
	private KonKingdom kingdom;
	private KonKingdom exileKingdom;
	private boolean isBarbarian;
	
	
	public KonOfflinePlayer(OfflinePlayer offlineBukkitPlayer, KonKingdom kingdom, boolean isBarbarian) {
		this.offlineBukkitPlayer = offlineBukkitPlayer;
		this.kingdom = kingdom;
		this.exileKingdom = kingdom;
		this.isBarbarian = isBarbarian;
	}
	
	
	// Getters
	public OfflinePlayer getOfflineBukkitPlayer() {
		return offlineBukkitPlayer;
	}
	
	public KonKingdom getKingdom() {
		return kingdom;
	}
	
	public KonKingdom getExileKingdom() {
		return exileKingdom;
	}
	
	public boolean isBarbarian() {
		return isBarbarian;
	}
	
	// Setters
	public void setKingdom(KonKingdom newKingdom) {
		kingdom = newKingdom;
	}
	
	public void setExileKingdom(KonKingdom newKingdom) {
		exileKingdom = newKingdom;
	}
	
	public void setBarbarian(boolean isNewBarbarian) {
		isBarbarian = isNewBarbarian;
	}
	
}
