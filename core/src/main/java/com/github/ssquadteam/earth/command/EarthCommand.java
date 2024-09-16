package com.github.ssquadteam.earth.command;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.EarthPlugin;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class EarthCommand extends CommandBase {

    public EarthCommand() {
        super("", false, false);
        // No arguments
    }

    @Override
    public void execute(
        Earth earth,
        CommandSender sender,
        List<String> args
    ) {
        // Display help GUI menu to players.
        // Display logo and tip for console.
        if (sender instanceof Player) {
            earth.getDisplayManager().displayHelpMenu((Player) sender);
        } else if (sender instanceof ConsoleCommandSender) {
            EarthPlugin.printLogo();
            ChatUtil.sendNotice(sender, "Suggested console commands:");
            ChatUtil.sendNotice(sender, "  earth version");
            ChatUtil.sendNotice(sender, "  earth reload");
            ChatUtil.sendNotice(sender, "  earth save");
            ChatUtil.sendNotice(sender, "  earth help");
        }
    }

    @Override
    public List<String> tabComplete(
        Earth earth,
        CommandSender sender,
        List<String> args
    ) {
        // No arguments to complete
        return Collections.emptyList();
    }
}
