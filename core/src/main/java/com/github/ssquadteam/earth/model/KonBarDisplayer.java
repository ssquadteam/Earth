package com.github.ssquadteam.earth.model;

public interface KonBarDisplayer {
	
	void addBarPlayer(KonPlayer player);
	
	void removeBarPlayer(KonPlayer player);
	
	void removeAllBarPlayers();
	
	void updateBarPlayers();

	void updateBarTitle();
}
