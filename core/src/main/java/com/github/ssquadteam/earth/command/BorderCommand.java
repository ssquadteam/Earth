package com.github.ssquadteam.earth.command;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.model.KonPlayer;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.Labeler;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BorderCommand extends CommandBase {

	public BorderCommand() {
		// Define name and sender support
		super("border",true, false);
		// None
		setOptionalArgs(true);
		// [on|off]
		List<String> argNames = Arrays.asList("on", "off");
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
		// Parse arguments
		if (args.isEmpty()) {
			// Toggle between borders
			if(player.isBorderDisplay()) {
				ChatUtil.sendNotice(player, MessagePath.COMMAND_BORDER_NOTICE_DISABLE.getMessage());
				player.setIsBorderDisplay(false);
			} else {
				ChatUtil.sendNotice(player, MessagePath.COMMAND_BORDER_NOTICE_ENABLE.getMessage());
				player.setIsBorderDisplay(true);
			}
		} else {
			// Set specific border mode
			String borderMode = args.get(0);
			switch (borderMode.toLowerCase()) {
				case "off":
					// Disabled borders
					ChatUtil.sendNotice(player, MessagePath.COMMAND_BORDER_NOTICE_DISABLE.getMessage());
					player.setIsBorderDisplay(false);
					break;
				case "on":
					// Enabled borders
					ChatUtil.sendNotice(player, MessagePath.COMMAND_BORDER_NOTICE_ENABLE.getMessage());
					player.setIsBorderDisplay(true);
					break;
				default:
					sendInvalidArgMessage(sender);
					return;
			}
		}
		// Update borders
		earth.getTerritoryManager().updatePlayerBorderParticles(player);
	}

	@Override
	public List<String> tabComplete(Earth earth, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		// Give suggestions
		if(args.size() == 1) {
			tabList.add("on");
			tabList.add("off");
		}
		return matchLastArgToList(tabList,args);
	}

}
