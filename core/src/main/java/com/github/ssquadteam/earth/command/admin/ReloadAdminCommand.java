package com.github.ssquadteam.earth.command.admin;

import java.util.Collections;
import java.util.List;

import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.command.CommandBase;
import com.github.ssquadteam.earth.utility.ChatUtil;

public class ReloadAdminCommand extends CommandBase {

	public ReloadAdminCommand() {
		// Define name and sender support
		super("reload",false, true);
		// No arguments
    }

	@Override
	public void execute(Earth earth, CommandSender sender, List<String> args) {
		if (!args.isEmpty()) {
			sendInvalidArgMessage(sender);
			return;
		}
		// Reload config files
		earth.reload();
		ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_RELOAD_NOTICE_MESSAGE.getMessage());
    }
    
    @Override
	public List<String> tabComplete(Earth earth, CommandSender sender, List<String> args) {
		// No arguments to complete
		return Collections.emptyList();
	}
	
	
}
