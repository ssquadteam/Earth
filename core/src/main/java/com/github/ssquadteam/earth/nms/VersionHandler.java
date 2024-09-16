package com.github.ssquadteam.earth.nms;

import org.bukkit.entity.Player;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.scoreboard.Team;

import java.util.List;

public interface VersionHandler {

	void applyTradeDiscount(double discountPercent, boolean isStack, MerchantInventory merchantInventory);
	
	boolean sendPlayerTeamPacket(Player player, List<String> teamNames, Team team);
}
