package com.github.ssquadteam.earth.command.admin;

import com.github.ssquadteam.earth.command.CommandBase;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import java.util.List;

public enum AdminCommandType {
	HELP        (Material.REDSTONE_TORCH,		"earth.admin.help",         new HelpAdminCommand(),         MessagePath.DESCRIPTION_ADMIN_HELP.getMessage()),
	BYPASS      (Material.SPECTRAL_ARROW,		"earth.admin.bypass",       new BypassAdminCommand(),       MessagePath.DESCRIPTION_ADMIN_BYPASS.getMessage()),
	KINGDOM     (Material.GOLDEN_SWORD,			"earth.admin.kingdom",      new KingdomAdminCommand(),      MessagePath.DESCRIPTION_ADMIN_KINGDOM.getMessage()),
	TOWN        (Material.OBSIDIAN,				"earth.admin.town",         new TownAdminCommand(),         MessagePath.DESCRIPTION_ADMIN_TOWN.getMessage()),
	CAMP        (Material.ORANGE_BED,			"earth.admin.camp",         new CampAdminCommand(),         MessagePath.DESCRIPTION_ADMIN_CAMP.getMessage()),
	CLAIM       (Material.DIAMOND_SHOVEL,		"earth.admin.claim",        new ClaimAdminCommand(),        MessagePath.DESCRIPTION_ADMIN_CLAIM.getMessage()),
	UNCLAIM     (Material.COBWEB,				"earth.admin.unclaim",      new UnclaimAdminCommand(),      MessagePath.DESCRIPTION_ADMIN_UNCLAIM.getMessage()),
	CAPTURE     (Material.FISHING_ROD,			"earth.admin.capture",      new CaptureAdminCommand(),      MessagePath.DESCRIPTION_ADMIN_CAPTURE.getMessage()),
	MONUMENT    (Material.CRAFTING_TABLE,		"earth.admin.monument",     new MonumentAdminCommand(),     MessagePath.DESCRIPTION_ADMIN_MONUMENT.getMessage()),
	RUIN        (Material.CRACKED_STONE_BRICKS,	"earth.admin.ruin",         new RuinAdminCommand(),         MessagePath.DESCRIPTION_ADMIN_RUIN.getMessage()),
	SANCTUARY   (Material.BEDROCK,				"earth.admin.sanctuary",    new SanctuaryAdminCommand(),    MessagePath.DESCRIPTION_ADMIN_SANCTUARY.getMessage()),
	TRAVEL      (Material.COMPASS,				"earth.admin.travel",       new TravelAdminCommand(),       MessagePath.DESCRIPTION_ADMIN_TRAVEL.getMessage()),
	SETTRAVEL   (Material.OAK_SIGN,				"earth.admin.settravel",    new SetTravelAdminCommand(),    MessagePath.DESCRIPTION_ADMIN_SETTRAVEL.getMessage()),
	FLAG        (Material.ORANGE_BANNER,		"earth.admin.flag",         new FlagAdminCommand(),         MessagePath.DESCRIPTION_ADMIN_FLAG.getMessage()),
	STAT        (Material.BOOKSHELF,			"earth.admin.stat",         new StatAdminCommand(),         MessagePath.DESCRIPTION_ADMIN_STAT.getMessage()),
	SAVE        (Material.TOTEM_OF_UNDYING,		"earth.admin.save",         new SaveAdminCommand(),         MessagePath.DESCRIPTION_ADMIN_SAVE.getMessage()),
	RELOAD      (Material.GLOWSTONE,			"earth.admin.reload",       new ReloadAdminCommand(),       MessagePath.DESCRIPTION_ADMIN_RELOAD.getMessage());

	private final Material iconMaterial;	// Item for menu icons
	private final String permission;		// Permission node from plugin.yml, or "" for no permission
	private final CommandBase command;		// Command class implementation
	private final String description;		// Description of the command for help

	AdminCommandType(Material iconMaterial, String permission, CommandBase command, String description) {
		this.iconMaterial = iconMaterial;
		this.permission = permission;
		this.command = command;
		this.description = description;
	}

	public Material iconMaterial() {
		return iconMaterial;
	}

	public String permission() {
		return permission;
	}

	public CommandBase command() {
		return command;
	}
	
	public String description(){
		return description;
	}

	public boolean isSenderHasPermission(CommandSender sender) {
		if (permission.isEmpty()) {
			return true;
		} else {
			return sender.hasPermission(permission);
		}
	}

	public boolean isSenderAllowed(CommandSender sender) {
		return command.isSenderAllowed(sender);
	}

	public String baseUsage() {
		return command.getBaseUsage();
	}

	public List<String> argumentUsage() {
		return command.getArgumentUsage();
	}

	/**
	 * Gets a AdminCommandType enum given a string command
	 * @param command - The string name of the command
	 * @return AdminCommandType - Corresponding enum
	 */
	public static AdminCommandType getCommand(String command) {
		AdminCommandType result = HELP;
		for(AdminCommandType cmd : AdminCommandType.values()) {
			if(cmd.toString().equalsIgnoreCase(command)) {
				result = cmd;
			}
		}
		return result;
	}
	
	/**
	 * Determines whether a string command is a AdminCommandType
	 * @param command - The string name of the command
	 * @return Boolean - True if the string is a command, false otherwise
	 */
	public static boolean contains(String command) {
		boolean result = false;
		for(AdminCommandType cmd : AdminCommandType.values()) {
			if(cmd.toString().equalsIgnoreCase(command)) {
				result = true;
			}
		}
		return result;
	}
}
