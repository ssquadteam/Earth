package com.github.ssquadteam.earth.command.admin;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.command.CommandBase;
import com.github.ssquadteam.earth.command.CommandType;
import com.github.ssquadteam.earth.model.KonPlayer;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class BypassAdminCommand extends CommandBase {
	
	public BypassAdminCommand() {
		// Define name and sender support
		super("bypass",true, true);
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
		if(player.isAdminBypassActive()) {
			player.setIsAdminBypassActive(false);
			ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_DISABLE_AUTO.getMessage());
			ChatUtil.resetTitle(player.getBukkitPlayer());
		} else {
			player.setIsAdminBypassActive(true);
			ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_ENABLE_AUTO.getMessage());
			ChatUtil.sendConstantTitle(player.getBukkitPlayer(), "", ChatColor.GOLD+MessagePath.LABEL_BYPASS.getMessage());
		}
    }
    
    @Override
	public List<String> tabComplete(Earth earth, CommandSender sender, List<String> args) {
		// No arguments to complete
		return Collections.emptyList();
	}
}
