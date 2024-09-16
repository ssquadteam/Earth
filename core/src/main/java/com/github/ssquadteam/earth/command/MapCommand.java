package com.github.ssquadteam.earth.command;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.manager.TerritoryManager;
import com.github.ssquadteam.earth.model.KonPlayer;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MapCommand extends CommandBase {

	public MapCommand() {
		// Define name and sender support
		super("map",true, false);
		// None
		setOptionalArgs(true);
		// [far|auto]
		List<String> argNames = Arrays.asList("far", "auto");
		addArgument(
				newArg(argNames,true,false)
		);
    }

	@Override
	public void execute(Earth earth, CommandSender sender, List<String> args) {
		// Sender must be player
		KonPlayer player = earth.getPlayerManager().getPlayer(sender);
		if (player == null) {
			sendInvalidSenderMessage(sender);
			return;
		}
		// Display formatted text in chat as chunk map
		if (args.isEmpty()) {
			// Display default map
			Bukkit.getScheduler().runTaskAsynchronously(earth.getPlugin(),
					() -> earth.getTerritoryManager().printPlayerMap(player, TerritoryManager.DEFAULT_MAP_SIZE));
		} else if(args.size() == 1) {
			String subCmd = args.get(0);
			if(subCmd.equalsIgnoreCase("far") || subCmd.equalsIgnoreCase("f")) {
				// Display far map
				Bukkit.getScheduler().runTaskAsynchronously(earth.getPlugin(),
						() -> earth.getTerritoryManager().printPlayerMap(player, TerritoryManager.FAR_MAP_SIZE));
			} else if(subCmd.equalsIgnoreCase("auto") || subCmd.equalsIgnoreCase("a")) {
				// Update auto map field
				if(player.isMapAuto()) {
					player.setIsMapAuto(false);
					ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_DISABLE_AUTO.getMessage());
				} else {
					player.setIsMapAuto(true);
					// Display default map
					Bukkit.getScheduler().runTaskAsynchronously(earth.getPlugin(), () -> {
						earth.getTerritoryManager().printPlayerMap(player, TerritoryManager.DEFAULT_MAP_SIZE);
						ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_ENABLE_AUTO.getMessage());
					});
				}
			} else {
				sendInvalidArgMessage(sender);
			}
		} else {
			sendInvalidArgMessage(sender);
		}
	}
	
	@Override
	public List<String> tabComplete(Earth earth, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		// Give suggestions
		if(args.size() == 1) {
			tabList.add("far");
			tabList.add("auto");
		}
		return matchLastArgToList(tabList,args);
	}
}
