package com.github.ssquadteam.earth.command;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.model.KonPlayer;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class QuestCommand extends CommandBase {

	public QuestCommand() {
		// Define name and sender support
		super("quest",true, false);
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
		// Check for global enable
		if(!earth.getDirectiveManager().isEnabled()) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
			return;
		}
		// Display quest book
		earth.getDirectiveManager().displayBook(player);
	}

	@Override
	public List<String> tabComplete(Earth earth, CommandSender sender, List<String> args) {
		// No arguments to complete
		return Collections.emptyList();
	}
}
