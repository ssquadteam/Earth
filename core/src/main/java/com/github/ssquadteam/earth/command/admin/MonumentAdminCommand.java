package com.github.ssquadteam.earth.command.admin;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.command.CommandBase;
import com.github.ssquadteam.earth.model.KonMonumentTemplate;
import com.github.ssquadteam.earth.model.KonPlayer;
import com.github.ssquadteam.earth.model.KonPlayer.RegionType;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MonumentAdminCommand extends CommandBase {
	
	public MonumentAdminCommand() {
		// Define name and sender support
		super("monument",true, true);
		// Define arguments
		// create <name> [<cost>]
		addArgument(
				newArg("create",true,false)
						.sub( newArg("name",false,true)
								.sub (newArg("cost",false,false) ) )
		);
		// reset <monument> [<cost>]
		addArgument(
				newArg("reset",true,false)
						.sub( newArg("monument",false,true)
								.sub (newArg("cost",false,false) ) )
		);
		// rename <monument> <name>
		addArgument(
				newArg("rename",true,false)
						.sub( newArg("monument",false,false)
								.sub (newArg("name",false,false) ) )
		);
		// remove|show|status <monument>
		List<String> argNames = Arrays.asList("remove", "show", "status");
		addArgument(
				newArg(argNames,true,false)
						.sub( newArg("monument",false,false) )
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
		if (args.size() != 2 && args.size() != 3) {
			sendInvalidArgMessage(sender);
			return;
		}
		Location playerLoc = player.getBukkitPlayer().getLocation();
		World bukkitWorld = playerLoc.getWorld();
		// Check for valid world
		if(earth.isWorldIgnored(bukkitWorld)) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
			return;
		}
		// Check for prior region setting mode
		if(player.isSettingRegion()) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_REGION.getMessage());
			return;
		}
		// Parse sub-commands
		String cmdMode = args.get(0);
		String templateName = args.get(1);
		if(cmdMode.equalsIgnoreCase("create")) {
			// Creating a new template
			double costNum = 0;
			if (args.size() == 3) {
				try {
					costNum = Double.parseDouble(args.get(2));
				} catch (NumberFormatException e) {
					ChatUtil.sendError(sender, MessagePath.COMMAND_ADMIN_MONUMENT_ERROR_VALUE.getMessage());
					ChatUtil.sendError(sender, e.getMessage());
					return;
				}
				costNum = Math.max(costNum, 0);
			}
			// Validate name first
			if (earth.validateName(templateName, sender) != 0) {
				return;
			}
			// Begin region setting flow
			player.settingRegion(RegionType.MONUMENT);
			player.setRegionTemplateName(templateName);
			player.setRegionTemplateCost(costNum);
			// Send flow messages
			ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_CREATE_1.getMessage(), ChatColor.LIGHT_PURPLE);
			ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_CLICK_AIR.getMessage());

		} else if(cmdMode.equalsIgnoreCase("reset")) {
			// Resetting an existing template fields
			double costNum = 0;
			if (args.size() == 3) {
				try {
					costNum = Double.parseDouble(args.get(2));
				} catch (NumberFormatException e) {
					ChatUtil.sendError(sender, MessagePath.COMMAND_ADMIN_MONUMENT_ERROR_VALUE.getMessage());
					ChatUtil.sendError(sender, e.getMessage());
					return;
				}
				costNum = Math.max(costNum, 0);
			}
			// Check for existing valid template name
			if(!earth.getSanctuaryManager().isTemplate(templateName)) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(templateName));
				return;
			}
			// Stop any template blanking
			String sanctuaryName = earth.getSanctuaryManager().getSanctuaryNameOfTemplate(templateName);
			if(earth.getSanctuaryManager().isSanctuary(sanctuaryName)) {
				earth.getSanctuaryManager().getSanctuary(sanctuaryName).stopTemplateBlanking(templateName);
			}
			// Begin region setting flow
			player.settingRegion(RegionType.MONUMENT);
			player.setRegionTemplateName(templateName);
			player.setRegionTemplateCost(costNum);
			// Send flow messages
			ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_CREATE_1.getMessage(), ChatColor.LIGHT_PURPLE);
			ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_CLICK_AIR.getMessage());

		} else if(cmdMode.equalsIgnoreCase("remove")) {
			// Confirm name is a template
			if(!earth.getSanctuaryManager().isTemplate(templateName)) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(templateName));
				return;
			}
			// Remove template
			player.settingRegion(RegionType.NONE);
			earth.getSanctuaryManager().removeMonumentTemplate(templateName);
			ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_REMOVE.getMessage(templateName));

		} else if(cmdMode.equalsIgnoreCase("rename")) {
			// Confirm name is a template
			if(!earth.getSanctuaryManager().isTemplate(templateName)) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(templateName));
				return;
			}
			// Get the new name
			if(args.size() != 3) {
				sendInvalidArgMessage(sender);
			}
			String newTemplateName = args.get(2);
			// Validate new name
			if (earth.validateName(newTemplateName, sender) != 0) {
				return;
			}
			// Rename the template
			if (earth.getSanctuaryManager().renameMonumentTemplate(templateName,newTemplateName)) {
				ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_RENAME.getMessage(templateName,newTemplateName));
			} else {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_FAILED.getMessage());
			}

		} else if(cmdMode.equalsIgnoreCase("show")) {
			// Confirm name is a template
			if(!earth.getSanctuaryManager().isTemplate(templateName)) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(templateName));
				return;
			}
			// Show template even when invalid
			player.settingRegion(RegionType.NONE);
			Location loc0 = earth.getSanctuaryManager().getTemplate(templateName).getCornerOne();
			Location loc1 = earth.getSanctuaryManager().getTemplate(templateName).getCornerTwo();
			player.startMonumentShow(loc0, loc1);
			String sanctuaryName = earth.getSanctuaryManager().getSanctuaryNameOfTemplate(templateName);
			ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_SHOW.getMessage(templateName,sanctuaryName));

		} else if(cmdMode.equalsIgnoreCase("status")) {
			// Confirm name is a template
			if(!earth.getSanctuaryManager().isTemplate(templateName)) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(templateName));
				return;
			}
			ChatColor loreColor = ChatColor.YELLOW;
			ChatColor valueColor = ChatColor.AQUA;
			KonMonumentTemplate template = earth.getSanctuaryManager().getTemplate(templateName);
			String tempName = template.getName();
			String sanctuaryName = earth.getSanctuaryManager().getSanctuaryNameOfTemplate(templateName);
			String cost = String.format("%.2f", template.getCost());
			String isValid = String.format("%s", template.isValid());
			String isBlanking = String.format("%s", template.isBlanking());
			String allBlocks = String.format("%d", template.getNumBlocks());
			String critBlocks = String.format("%d", template.getNumCriticals());
			String lootChests = String.format("%d", template.getNumLootChests());
			ChatUtil.sendNotice(sender, MessagePath.LABEL_MONUMENT_TEMPLATE.getMessage()+" "+tempName);
			ChatUtil.sendMessage(sender, loreColor+MessagePath.LABEL_SANCTUARY.getMessage()+": "+valueColor+sanctuaryName);
			ChatUtil.sendMessage(sender, loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+cost);
			ChatUtil.sendMessage(sender, loreColor+MessagePath.LABEL_VALID.getMessage()+": "+valueColor+isValid);
			ChatUtil.sendMessage(sender, loreColor+MessagePath.LABEL_MODIFIED.getMessage()+": "+valueColor+isBlanking);
			ChatUtil.sendMessage(sender, loreColor+MessagePath.LABEL_BLOCKS.getMessage()+": "+valueColor+allBlocks);
			ChatUtil.sendMessage(sender, loreColor+MessagePath.LABEL_CRITICAL_HITS.getMessage()+": "+valueColor+critBlocks);
			ChatUtil.sendMessage(sender, loreColor+MessagePath.LABEL_LOOT_CHESTS.getMessage()+": "+valueColor+lootChests);
		} else {
			sendInvalidArgMessage(sender);
		}

    }
    
    @Override
	public List<String> tabComplete(Earth earth, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		if (args.size() == 1) {
			tabList.add("create");
			tabList.add("remove");
			tabList.add("rename");
			tabList.add("reset");
			tabList.add("show");
			tabList.add("status");
		} else if (args.size() == 2) {
			if(args.get(0).equalsIgnoreCase("create")) {
				// Name
				tabList.add("***");
			} else {
				// Existing templates
				tabList.addAll(earth.getSanctuaryManager().getAllTemplateNames());
			}
		} else if (args.size() == 3) {
			if(args.get(0).equalsIgnoreCase("create") || args.get(0).equalsIgnoreCase("reset")) {
				// Cost
				tabList.add("#");
			} else if (args.get(0).equalsIgnoreCase("rename")) {
				// New name
				tabList.add("***");
			}
		}
		return matchLastArgToList(tabList,args);
	}
}
