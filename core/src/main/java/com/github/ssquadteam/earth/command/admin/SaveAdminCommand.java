package com.github.ssquadteam.earth.command.admin;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.command.CommandBase;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class SaveAdminCommand extends CommandBase {
	
	public SaveAdminCommand() {
		// Define name and sender support
		super("save",false, true);
		// No arguments
    }

	@Override
    public void execute(Earth earth, CommandSender sender, List<String> args) {
		if (!args.isEmpty()) {
			sendInvalidArgMessage(sender);
			return;
		}
		// Save config files
		earth.save();
		ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_SAVE_NOTICE_MESSAGE.getMessage());
    }
    
    @Override
	public List<String> tabComplete(Earth earth, CommandSender sender, List<String> args) {
		// No arguments to complete
		return Collections.emptyList();
	}
}
