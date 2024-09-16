package com.github.ssquadteam.earth.command;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.EarthPlugin;
import com.github.ssquadteam.earth.model.KonPlayer;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.CorePath;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class FavorCommand extends CommandBase {

	public FavorCommand() {
		// Define name and sender support
		super("favor",true, false);
		// No Arguments
    }

	@Override
	public void execute(Earth earth, CommandSender sender, List<String> args) {
		// Sender must be player
		KonPlayer player = earth.getPlayerManager().getPlayer(sender);
		if (player == null) {
			sendInvalidSenderMessage(sender);
			return;
		}

		if (!args.isEmpty()) {
			sendInvalidArgMessage(sender);
			return;
		}
		Player bukkitPlayer = player.getBukkitPlayer();
		double balance = EarthPlugin.getBalance(bukkitPlayer);
		double cost_spy = earth.getCore().getDouble(CorePath.FAVOR_COST_SPY.getPath(),0.0);
		double cost_settle = earth.getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SETTLE.getPath(),0.0);
		double cost_settle_incr = earth.getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SETTLE_INCREMENT.getPath(),0.0);
		boolean isIncrementKingdom = earth.getCore().getBoolean(CorePath.TOWNS_SETTLE_INCREMENT_KINGDOM.getPath(),false);
		int townCount;
		if (isIncrementKingdom) {
			// All kingdom towns
			townCount = player.getKingdom().getNumTowns();
		} else {
			// Towns that have the player as the lord
			townCount = earth.getKingdomManager().getPlayerLordships(player);
		}
		double cost_settle_adj = (((double)townCount)*cost_settle_incr) + cost_settle;
		double cost_town_rename = earth.getCore().getDouble(CorePath.FAVOR_TOWNS_COST_RENAME.getPath(),0.0);
		double cost_claim = earth.getCore().getDouble(CorePath.FAVOR_COST_CLAIM.getPath(),0.0);
		double cost_travel = earth.getCore().getDouble(CorePath.FAVOR_COST_TRAVEL.getPath(),0.0);
		double cost_kingdom_create = earth.getKingdomManager().getCostCreate();
		double cost_kingdom_rename = earth.getKingdomManager().getCostRename();
		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_FAVOR_NOTICE_MESSAGE.getMessage(EarthPlugin.getCurrencyFormat(balance)));
		ChatUtil.sendMessage(bukkitPlayer, MessagePath.COMMAND_FAVOR_NOTICE_COST_SPY.getMessage() + 			" - "+ChatColor.AQUA + EarthPlugin.getCurrencyFormat(cost_spy));
		ChatUtil.sendMessage(bukkitPlayer, MessagePath.COMMAND_FAVOR_NOTICE_COST_TRAVEL.getMessage() +			" - "+ChatColor.AQUA + EarthPlugin.getCurrencyFormat(cost_travel));
		ChatUtil.sendMessage(bukkitPlayer, MessagePath.COMMAND_FAVOR_NOTICE_COST_CLAIM.getMessage() +			" - "+ChatColor.AQUA + EarthPlugin.getCurrencyFormat(cost_claim));
		ChatUtil.sendMessage(bukkitPlayer, MessagePath.COMMAND_FAVOR_NOTICE_COST_TOWN_SETTLE.getMessage() +		" - "+ChatColor.AQUA + EarthPlugin.getCurrencyFormat(cost_settle_adj));
		ChatUtil.sendMessage(bukkitPlayer, MessagePath.COMMAND_FAVOR_NOTICE_COST_TOWN_RENAME.getMessage() +		" - "+ChatColor.AQUA + EarthPlugin.getCurrencyFormat(cost_town_rename));
		ChatUtil.sendMessage(bukkitPlayer, MessagePath.COMMAND_FAVOR_NOTICE_COST_KINGDOM_CREATE.getMessage() +	" - "+ChatColor.AQUA + EarthPlugin.getCurrencyFormat(cost_kingdom_create));
		ChatUtil.sendMessage(bukkitPlayer, MessagePath.COMMAND_FAVOR_NOTICE_COST_KINGDOM_RENAME.getMessage() +	" - "+ChatColor.AQUA + EarthPlugin.getCurrencyFormat(cost_kingdom_rename));
	}
	
	@Override
	public List<String> tabComplete(Earth earth, CommandSender sender, List<String> args) {
		// No arguments to complete
		return Collections.emptyList();
	}
}
