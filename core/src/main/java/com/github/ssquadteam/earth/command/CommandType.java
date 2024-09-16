package com.github.ssquadteam.earth.command;

import com.github.ssquadteam.earth.command.admin.AdminCommand;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * All regular player commands are defined here.
 * ADMIN is a special case. It has no explicit permission, but all admin sub-commands have their own permissions.
 * Executing the ADMIN command runs sub-commands based on arguments.
 */
public enum CommandType {
	HELP    (Material.LANTERN,          "earth.command.help",    "h",  new HelpCommand(),      MessagePath.DESCRIPTION_HELP.getMessage()),
	INFO    (Material.SPRUCE_SIGN,      "earth.command.info",    "i",  new InfoCommand(),      MessagePath.DESCRIPTION_INFO.getMessage()),
	LIST    (Material.PAPER,            "earth.command.list",    "l",  new ListCommand(),      MessagePath.DESCRIPTION_LIST.getMessage()),
	KINGDOM (Material.GOLDEN_SWORD,     "earth.command.kingdom", "k",  new KingdomCommand(),   MessagePath.DESCRIPTION_KINGDOM.getMessage()),
	TOWN    (Material.OBSIDIAN,         "earth.command.town",    "t",  new TownCommand(),      MessagePath.DESCRIPTION_TOWN.getMessage()),
	MAP     (Material.FILLED_MAP,       "earth.command.map",     "m",  new MapCommand(),       MessagePath.DESCRIPTION_MAP.getMessage()),
	SETTLE  (Material.DIAMOND_PICKAXE,  "earth.command.settle",  "",   new SettleCommand(),    MessagePath.DESCRIPTION_SETTLE.getMessage()),
	CLAIM   (Material.DIAMOND_SHOVEL,   "earth.command.claim",   "",   new ClaimCommand(),     MessagePath.DESCRIPTION_CLAIM.getMessage()),
	UNCLAIM (Material.COBWEB,           "earth.command.unclaim", "",   new UnclaimCommand(),   MessagePath.DESCRIPTION_UNCLAIM.getMessage()),
	TRAVEL  (Material.COMPASS,          "earth.command.travel",  "v",  new TravelCommand(),    MessagePath.DESCRIPTION_TRAVEL.getMessage()),
	CHAT    (Material.EMERALD,          "earth.command.chat",    "c",  new ChatCommand(),      MessagePath.DESCRIPTION_CHAT.getMessage()),
	SPY     (Material.IRON_BARS,        "earth.command.spy",     "",   new SpyCommand(),       MessagePath.DESCRIPTION_SPY.getMessage()),
	FAVOR   (Material.GOLD_INGOT,       "earth.command.favor",   "f",  new FavorCommand(),     MessagePath.DESCRIPTION_FAVOR.getMessage()),
	SCORE   (Material.DIAMOND,          "earth.command.score",   "",   new ScoreCommand(),     MessagePath.DESCRIPTION_SCORE.getMessage()),
	QUEST   (Material.WRITABLE_BOOK,    "earth.command.quest",   "q",  new QuestCommand(),     MessagePath.DESCRIPTION_QUEST.getMessage()),
	STATS   (Material.BOOK,             "earth.command.stats",   "s",  new StatsCommand(),     MessagePath.DESCRIPTION_STATS.getMessage()),
	PREFIX  (Material.NAME_TAG,         "earth.command.prefix",  "p",  new PrefixCommand(),    MessagePath.DESCRIPTION_PREFIX.getMessage()),
	BORDER  (Material.GREEN_CARPET,     "earth.command.border",  "b",  new BorderCommand(),    MessagePath.DESCRIPTION_BORDER.getMessage()),
	FLY     (Material.ELYTRA,           "earth.command.fly",     "",   new FlyCommand(),       MessagePath.DESCRIPTION_FLY.getMessage()),
	ADMIN   (Material.NETHER_STAR,      "",                         "",   new AdminCommand(),     MessagePath.DESCRIPTION_ADMIN.getMessage());

	private final Material iconMaterial;	// Item for menu icons
	private final String permission;		// Permission node from plugin.yml, or "" for no permission
	private final String alias;				// Alternative command name
	private final CommandBase command;		// Command class implementation
	private final String description;		// Description of the command for help

	CommandType(Material iconMaterial, String permission, String alias, CommandBase command, String description) {
		this.iconMaterial = iconMaterial;
		this.permission = permission;
		this.command = command;
		this.description = description;
		this.alias = alias;
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
	
	public String alias() {
		return alias;
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
	 * Gets a CommandType enum given a string command.
	 * If an alias is given, this returns the command enum
	 * @param command - The string name of the command
	 * @return CommandType - Corresponding enum
	 */
	public static CommandType getCommand(String command) {
		CommandType result = HELP;
		for(CommandType cmd : CommandType.values()) {
			if(cmd.toString().equalsIgnoreCase(command) || (!cmd.alias().equals("") && cmd.alias().equalsIgnoreCase(command))) {
				result = cmd;
			}
		}
		return result;
	}
	
	/**
	 * Determines whether a string command is a CommandType
	 * @param command - The string name of the command
	 * @return Boolean - True if the string is a command, false otherwise
	 */
	public static boolean contains(String command) {
		boolean result = false;
		for(CommandType cmd : CommandType.values()) {
			if(cmd.toString().equalsIgnoreCase(command)) {
				result = true;
			}
		}
		return result;
	}
	
}
