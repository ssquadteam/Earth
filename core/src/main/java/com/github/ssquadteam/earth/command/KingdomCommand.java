package com.github.ssquadteam.earth.command;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.api.model.EarthDiplomacyType;
import com.github.ssquadteam.earth.api.model.EarthRelationshipType;
import com.github.ssquadteam.earth.model.*;
import com.github.ssquadteam.earth.utility.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.awt.*;
import java.util.*;
import java.util.List;

public class KingdomCommand extends CommandBase {
	
	public KingdomCommand() {
		// Define name and sender support
		super("kingdom",true, false);
		// Define arguments
		// None
		setOptionalArgs(true);
		// menu
		addArgument(
				newArg("menu",true,false)
		);
		// create <template> <name>
		addArgument(
				newArg("create",true,false)
						.sub( newArg("template",false,false)
								.sub( newArg("name",false,false) ) )
		);
		// invites
		addArgument(
				newArg("invites",true,false)
		);
		// join <kingdom>
		addArgument(
				newArg("join",true,false)
						.sub( newArg("kingdom",false,false) )
		);
		// exile
		addArgument(
				newArg("exile",true,false)
		);
		// templates
		addArgument(
				newArg("templates",true,false)
		);
		// manage...
		List<String> accessArgNames = Arrays.asList("open", "closed");
		List<String> requestsArgNames = Arrays.asList("accept", "deny");
		List<String> memberArgNames = Arrays.asList("invite", "kick", "promote", "demote", "master");
		addArgument(
				newArg("manage",true,false)
						// manage disband
						.sub( newArg("disband",true,false) )
						// manage destroy <town>
						.sub( newArg("destroy",true,false)
								.sub( newArg("town",false,false) ) )
						// manage rename <name>
						.sub( newArg("rename",true,false)
								.sub( newArg("name",false,false) ) )
						// manage webcolor [<color>]
						.sub( newArg("webcolor",true,true)
								.sub( newArg("color",false,false) ) )
						// manage template [<name>]
						.sub( newArg("template",true,true)
								.sub( newArg("name",false,false) ) )
						// manage access open|closed
						.sub( newArg("access",true,false)
								.sub( newArg(accessArgNames,true,false) ) )
						// manage diplomacy <kingdom> [<relation>]
						.sub( newArg("diplomacy",true,false)
								.sub( newArg("kingdom",false,true)
										.sub( newArg("relation",false,false) ) ) )
						// manage requests [player] accept|deny
						.sub( newArg("requests",true,true)
								.sub( newArg("player",false,false)
										.sub( newArg(requestsArgNames,true,false) ) ) )
						// manage member invite|kick|promote|demote|master <player>
						.sub( newArg("member",true,false)
								.sub( newArg(memberArgNames,true,false)
										.sub( newArg("player",false,false) ) ) )
		);
    }

	@Override
	public void execute(Earth earth, CommandSender sender, List<String> args) {
		final int MAX_LIST_NAMES = 10;
		// Sender must be player
		KonPlayer player = earth.getPlayerManager().getPlayer(sender);
		if (player == null) {
			ChatUtil.printDebug("Command executed with null player", true);
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return;
		}
		// Check for player's kingdom, should be either barbarians or another non-null kingdom
		KonKingdom kingdom = player.getKingdom();
		Player bukkitPlayer = player.getBukkitPlayer();
		UUID playerID = bukkitPlayer.getUniqueId();
		// Parse arguments
		if(args.isEmpty()) {
			// No arguments, open kingdom menu
			earth.getDisplayManager().displayKingdomMenu(player, kingdom, false);
			return;
		}
		// Has arguments
		String subCmd = args.get(0);

		switch(subCmd.toLowerCase()) {
			case "menu":
				// Open the kingdom menu
				earth.getDisplayManager().displayKingdomMenu(player, kingdom, false);
				break;
			case "create":
				// Create a new kingdom
				// Check if players can create kingdoms from config
				boolean isAdminOnly = earth.getCore().getBoolean(CorePath.KINGDOMS_CREATE_ADMIN_ONLY.getPath());
				if(isAdminOnly) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
					return;
				}
				// Check for permission
				if(!bukkitPlayer.hasPermission("earth.create.kingdom")) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_PERMISSION.getMessage()+" earth.create.kingdom");
					return;
				}
				// Needs template name arguments
				if(args.size() == 1) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_KINGDOM_ERROR_MISSING_TEMPLATE.getMessage());
					return;
				}
				// Pre-Check for any available templates
				if(earth.getSanctuaryManager().getNumTemplates() == 0) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_ADMIN_KINGDOM_ERROR_NO_TEMPLATES.getMessage());
					return;
				}
				String templateName = args.get(1);
				// Needs kingdom name arguments
				if(args.size() == 2) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_KINGDOM_ERROR_MISSING_NAME.getMessage());
					return;
				}
				// Pre-Check for valid kingdom name
				String newKingdomName = args.get(2);
				if(earth.validateName(newKingdomName,bukkitPlayer) != 0) {
					// Player receives error message within validateName method
					return;
				}
				// Check for other plugin flags
				if(earth.getIntegrationManager().getWorldGuard().isEnabled()) {
					// Check new territory claims
					Location settleLoc = bukkitPlayer.getLocation();
					int radius = earth.getCore().getInt(CorePath.TOWNS_INIT_RADIUS.getPath());
					World locWorld = settleLoc.getWorld();
					for(Point point : HelperUtil.getAreaPoints(settleLoc, radius)) {
						if(!earth.getIntegrationManager().getWorldGuard().isChunkClaimAllowed(locWorld,point,bukkitPlayer)) {
							// A region is denying this action
							ChatUtil.sendError(bukkitPlayer, MessagePath.REGION_ERROR_CLAIM_DENY.getMessage());
							return;
						}
					}
				}
				// Create the kingdom
				int createStatus = earth.getKingdomManager().createKingdom(bukkitPlayer.getLocation(), newKingdomName, templateName, player, false);
				// Check result
				if(createStatus == 0) {
					// Successful kingdom creation
					KonKingdom createdKingdom = earth.getKingdomManager().getKingdom(newKingdomName);
					ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_CREATE.getMessage(bukkitPlayer.getName(), newKingdomName));
					// Teleport player to safe place around monument, facing monument
					earth.getKingdomManager().teleportAwayFromCenter(createdKingdom.getCapital());
					// Optionally apply starter shield
					int starterShieldDuration = earth.getCore().getInt(CorePath.TOWNS_SHIELD_NEW_TOWNS.getPath(),0);
					if(starterShieldDuration > 0) {
						earth.getShieldManager().shieldSet(createdKingdom.getCapital(),starterShieldDuration);
					}
					// Play a success sound
					Earth.playTownSettleSound(bukkitPlayer.getLocation());
					// Open kingdom menu for newly created kingdom
					KonKingdom newKingdom = earth.getKingdomManager().getKingdom(newKingdomName);
					earth.getDisplayManager().displayKingdomMenu(player, newKingdom, false);
					// Updates
					earth.getKingdomManager().updatePlayerMembershipStats(player);
					// Update directive progress
					earth.getDirectiveManager().updateDirectiveProgress(player, KonDirective.CREATE_KINGDOM);
					// Update stats
					earth.getAccomplishmentManager().modifyPlayerStat(player, KonStatsType.KINGDOMS, 1);
				} else {
					switch(createStatus) {
						case 1:
							// Note that this error should not be reached due to previous checks
							ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_TAKEN_NAME.getMessage());
							break;
						case 2:
							ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_KINGDOM_ERROR_BARBARIAN_CREATE.getMessage());
							break;
						case 3:
							double templateCost = earth.getSanctuaryManager().getTemplate(templateName).getCost();
							double totalCost = earth.getKingdomManager().getCostCreate() + templateCost;
							String cost = String.format("%.2f",totalCost);
							ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(cost));
							break;
						case 4:
							ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_KINGDOM_ERROR_INVALID_TEMPLATE.getMessage(templateName));
							break;
						case 5:
							ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
							break;
						case 6:
							int distance = earth.getTerritoryManager().getDistanceToClosestTerritory(bukkitPlayer.getLocation());
							int min_distance_sanc = earth.getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_SANCTUARY.getPath());
							int min_distance_town = earth.getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_TOWN.getPath());
							int min_distance = Math.min(min_distance_sanc, min_distance_town);
							ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_FAIL_PROXIMITY.getMessage(distance,min_distance));
							break;
						case 7:
							distance = earth.getTerritoryManager().getDistanceToClosestTerritory(bukkitPlayer.getLocation());
							int max_distance_all = earth.getCore().getInt(CorePath.TOWNS_MAX_DISTANCE_ALL.getPath());
							ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_FAIL_MAX.getMessage(distance,max_distance_all));
							break;
						case 8:
							ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_FAIL_PLACEMENT.getMessage());
							break;
						case 12:
							ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_FAIL_HEIGHT.getMessage());
							break;
						case 13:
							ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_FAIL_INIT.getMessage());
							break;
						case 14:
							ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_FAIL_AIR.getMessage());
							break;
						case 15:
							ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_FAIL_WATER.getMessage());
							break;
						case 16:
							ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_FAIL_CONTAINER.getMessage());
							break;
						case 22:
							ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_FAIL_FLAT.getMessage());
							break;
						case 23:
							ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_FAIL_HEIGHT.getMessage());
							break;
						default:
							ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
							break;
					}
					ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_SETTLE_NOTICE_MAP_HINT.getMessage());
				}
				break;
			case "invites":
				// List kingdoms that have invited the player to join them
				if(args.size() == 1) {
					String nameListStr = formatStringListLimited(earth.getKingdomManager().getInviteKingdomNames(player), MAX_LIST_NAMES);
					ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_KINGDOM_NOTICE_INVITE_LIST.getMessage());
					ChatUtil.sendMessage(bukkitPlayer, nameListStr);
				} else {
					sendInvalidArgMessage(bukkitPlayer);
				}
				break;
			case "join":
				// Join a new kingdom
				if(args.size() == 2) {
					String joinKingdomName = args.get(1);
					if(!earth.getKingdomManager().isKingdom(joinKingdomName)) {
						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(joinKingdomName));
						return;
					}
					KonKingdom joinKingdom = earth.getKingdomManager().getKingdom(joinKingdomName);
					earth.getKingdomManager().menuJoinKingdomRequest(player, joinKingdom);
				} else {
					sendInvalidArgMessage(bukkitPlayer);
				}
				break;
			case "exile":
				// Exile to become a barbarian
				if(args.size() == 1) {
					earth.getKingdomManager().menuExileKingdom(player);
				} else {
					sendInvalidArgMessage(bukkitPlayer);
				}
				break;
			case "templates":
				// Display templates menu
				if(args.size() == 1) {
					earth.getDisplayManager().displayTemplateInfoMenu(player);
				} else {
					sendInvalidArgMessage(bukkitPlayer);
				}
				break;
			case "manage":
				// Management sub-commands
				if(player.isBarbarian()) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
					return;
				}
				if(args.size() >= 2) {
					String manageSubCmd = args.get(1);
					switch (manageSubCmd.toLowerCase()) {
						case "disband":
							// Disband and remove the kingdom (master only)
							if(!kingdom.isMaster(playerID)) {
								ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
								return;
							}
							if(args.size() == 2) {
								earth.getKingdomManager().menuDisbandKingdom(kingdom, player);
							} else {
								sendInvalidArgMessage(bukkitPlayer);
							}
							break;
						case "destroy":
							// Destroy a town in the kingdom (master only)
							if(!kingdom.isMaster(playerID)) {
								ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
								return;
							}
							// Check for enabled feature
							if(!earth.getKingdomManager().getIsTownDestroyMasterEnable()) {
								ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
								return;
							}
							if(args.size() == 3) {
								String townName = args.get(2);
								// Check valid town name
								if (!kingdom.hasTown(townName)) {
									ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_BAD_NAME.getMessage(townName));
									return;
								}
								// Manager method includes messages
								earth.getKingdomManager().menuDestroyTown(kingdom.getTown(townName),player);
							} else {
								sendInvalidArgMessage(bukkitPlayer);
							}
							break;
						case "rename":
							// Rename your kingdom (master only)
							if(!kingdom.isMaster(playerID)) {
								ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
								return;
							}
							if(args.size() == 3) {
								String newName = args.get(2);
								String oldName = kingdom.getName();
								if(earth.validateName(newName,bukkitPlayer) != 0) {
									// Player receives error message within validateName method
									return;
								}
								int status = earth.getKingdomManager().renameKingdom(oldName, newName, player, false);
								switch(status) {
									case 0:
										ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_KINGDOM_NOTICE_RENAME.getMessage(newName));
										ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_RENAME.getMessage(oldName,newName));
										break;
									case 1:
										ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage());
										break;
									case 2:
										ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_TAKEN_NAME.getMessage());
										break;
									case 3:
										String cost = String.format("%.2f",earth.getKingdomManager().getCostRename());
										ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(cost));
										break;
									default:
										ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_FAILED.getMessage());
										break;
								}
							} else {
								sendInvalidArgMessage(bukkitPlayer);
							}
							break;
						case "webcolor":
							if(earth.getCore().getBoolean(CorePath.KINGDOMS_WEB_COLOR_ADMIN_ONLY.getPath())) {
								// Only admins may set kingdom web colors
								ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
								return;
							}
							if(!kingdom.isMaster(playerID)) {
								// The player is not the kingdom master
								ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
								return;
							}
							if(args.size() == 2) {
								// Display current color
								int currentWebColor = kingdom.getWebColor();
								String colorStr = "default";
								if(currentWebColor != -1) {
									colorStr = ChatUtil.reverseLookupColorRGB(currentWebColor);
								}
								ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_KINGDOM_NOTICE_WEB_COLOR_SHOW.getMessage(kingdom.getName(),colorStr));
							} else if(args.size() == 3) {
								// Set new color
								String colorStr = args.get(2);
								if(colorStr.equalsIgnoreCase("default")) {
									kingdom.setWebColor(-1);
								} else {
									int newWebColor = ChatUtil.lookupColorRGB(colorStr);
									if(newWebColor != -1) {
										kingdom.setWebColor(newWebColor);
									} else {
										// The provided color string could not be resolved to a color
										ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_KINGDOM_ERROR_WEB_COLOR_INVALID.getMessage());
										return;
									}
								}
								// Update map
								earth.getMapHandler().drawUpdateTerritory(kingdom);
								ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_KINGDOM_NOTICE_WEB_COLOR_SET.getMessage(kingdom.getName(),colorStr));
							} else {
								// Incorrect arguments
								sendInvalidArgMessage(bukkitPlayer);
							}
							break;
						case "template":
							// View or change the kingdom's monument template (master only)
							if(args.size() == 2) {
								// Display current template
								ChatUtil.sendNotice(sender, MessagePath.COMMAND_KINGDOM_NOTICE_TEMPLATE_CURRENT.getMessage(kingdom.getMonumentTemplateName()));
							} else if(args.size() == 3) {
								// Change template
								if(!kingdom.isMaster(playerID)) {
									ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
									return;
								}
								String newTemplateName = args.get(2);
								if(!earth.getSanctuaryManager().isValidTemplate(newTemplateName)) {
									ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_KINGDOM_ERROR_INVALID_TEMPLATE.getMessage(newTemplateName));
									return;
								}
								earth.getKingdomManager().menuChangeKingdomTemplate(kingdom, earth.getSanctuaryManager().getTemplate(newTemplateName), player, false);
							} else {
								sendInvalidArgMessage(bukkitPlayer);
							}
							break;
						case "access":
							// Make the kingdom open or closed (master only)
							if (!kingdom.isMaster(playerID)) {
								ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
								return;
							}
							if (args.size() == 3) {
								String accessType = args.get(2);
								if (accessType.equalsIgnoreCase("open")) {
									kingdom.setIsOpen(true);
									ChatUtil.sendNotice(sender, MessagePath.COMMAND_KINGDOM_NOTICE_OPEN.getMessage());
								} else if (accessType.equalsIgnoreCase("closed")) {
									kingdom.setIsOpen(false);
									ChatUtil.sendNotice(sender, MessagePath.COMMAND_KINGDOM_NOTICE_CLOSED.getMessage());
								} else {
									sendInvalidArgMessage(bukkitPlayer);
								}
							} else {
								sendInvalidArgMessage(bukkitPlayer);
							}
							break;
						case "diplomacy":
							// View or change kingdom diplomacy relations (officers only)
							if (args.size() >= 3) {
								String otherKingdomName = args.get(2);
								if (!earth.getKingdomManager().isKingdom(otherKingdomName)) {
									ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(otherKingdomName));
									return;
								}
								KonKingdom otherKingdom = earth.getKingdomManager().getKingdom(otherKingdomName);
								if(args.size() == 3) {
									// View relation with kingdom
									ChatUtil.sendNotice(sender, MessagePath.COMMAND_KINGDOM_NOTICE_DIPLOMACY_CURRENT.getMessage(otherKingdom.getName(),kingdom.getActiveRelation(otherKingdom).toString()));
								} else if(args.size() == 4) {
									// Change diplomacy
									if (!kingdom.isOfficer(playerID)) {
										ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
										return;
									}
									if(kingdom.isPeaceful() || otherKingdom.isPeaceful()) {
										// Cannot change diplomacy of peaceful kingdoms
										ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_KINGDOM_ERROR_DIPLOMACY_PEACEFUL.getMessage());
										return;
									}
									String relationName = args.get(3);
									EarthDiplomacyType relation;
									try {
										relation = EarthDiplomacyType.valueOf(relationName.toUpperCase());
									} catch (IllegalArgumentException ex) {
										ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(relationName));
										return;
									}
									earth.getKingdomManager().menuChangeKingdomRelation(kingdom, otherKingdom, relation, player, false);
								} else {
									sendInvalidArgMessage(bukkitPlayer);
								}
							} else {
								sendInvalidArgMessage(bukkitPlayer);
							}
							break;
						case "requests":
							// View, accept or deny player join requests (officers only)
							if(!kingdom.isOfficer(playerID)) {
								ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
								return;
							}
							if (args.size() == 2) {
								// No player name given, list requests
								ArrayList<String> requestPlayerNames = new ArrayList<>();
								for (OfflinePlayer requester : kingdom.getJoinRequests()) {
									requestPlayerNames.add(requester.getName());
								}
								String nameListStr = formatStringListLimited(requestPlayerNames, MAX_LIST_NAMES);
								ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_KINGDOM_NOTICE_REQUEST_LIST.getMessage());
								ChatUtil.sendMessage(bukkitPlayer, nameListStr);
							} else if (args.size() == 4) {
								// Accept or deny player
								String requesterName = args.get(2);
								String requestDecision = args.get(3);
								KonOfflinePlayer requester = earth.getPlayerManager().getOfflinePlayerFromName(requesterName);
								if(requester == null) {
									ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(requesterName));
									return;
								}
								boolean response;
								if (requestDecision.equalsIgnoreCase("accept")) {
									response = true;
								} else if (requestDecision.equalsIgnoreCase("deny")) {
									response = false;
								} else {
									sendInvalidArgMessage(bukkitPlayer);
									return;
								}
								earth.getKingdomManager().menuRespondKingdomRequest(player,requester,kingdom,response);
							} else {
								sendInvalidArgMessage(bukkitPlayer);
							}
							break;
						case "member":
							// Member management sub-command
							if(args.size() == 4) {
								String memberSubCmd = args.get(2);
								String playerName = args.get(3);
								KonOfflinePlayer offlinePlayer = earth.getPlayerManager().getOfflinePlayerFromName(playerName);
								if(offlinePlayer == null) {
									ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
									return;
								}
								playerName = offlinePlayer.getOfflineBukkitPlayer().getName();
								switch (memberSubCmd.toLowerCase()) {
									case "invite":
										// Invite a new kingdom member (officers only)
										if(!kingdom.isOfficer(playerID)) {
											ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
											return;
										}
										if(earth.getKingdomManager().joinKingdomInvite(player, offlinePlayer, kingdom)) {
											ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_KINGDOM_NOTICE_INVITE_SENT.getMessage(playerName));
										}
										break;
									case "kick":
										// Remove a kingdom member (officers only)
										if(!kingdom.isOfficer(playerID)) {
											ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
											return;
										}
										OfflinePlayer bukkitOfflinePlayer = offlinePlayer.getOfflineBukkitPlayer();
										// Cannot kick master
										if(kingdom.isMaster(bukkitOfflinePlayer.getUniqueId())) {
											ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_KINGDOM_ERROR_KICK_MASTER.getMessage());
											return;
										}
										// Cannot kick self
										if (offlinePlayer.getOfflineBukkitPlayer().getUniqueId().equals(playerID)) {
											ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
											return;
										}
										// Use manager method
										if(earth.getKingdomManager().kickKingdomMember(player, bukkitOfflinePlayer)) {
											ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_KINGDOM_NOTICE_KICK.getMessage(playerName));
										}
										break;
									case "promote":
										// Promote a kingdom member to officer (master only)
										if(!kingdom.isMaster(playerID)) {
											ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
											return;
										}
										// Manager method includes broadcast on success
										if(!earth.getKingdomManager().menuPromoteOfficer(offlinePlayer.getOfflineBukkitPlayer(), kingdom)) {
											// Failed
											ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
											return;
										}
										break;
									case "demote":
										// Demote a kingdom officer to member (master only)
										if(!kingdom.isMaster(playerID)) {
											ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
											return;
										}
										// Manager method includes broadcast on success
										if(!earth.getKingdomManager().menuDemoteOfficer(offlinePlayer.getOfflineBukkitPlayer(), kingdom)) {
											// Failed
											ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
											return;
										}
										break;
									case "master":
										// Transfer kingdom master to another member (master only)
										if(!kingdom.isMaster(playerID)) {
											ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
											return;
										}
										// Manager method includes status messages
										earth.getKingdomManager().menuTransferMaster(offlinePlayer.getOfflineBukkitPlayer(), kingdom, player);
										break;
									default:
										sendInvalidArgMessage(bukkitPlayer);
										return;
								}
							} else {
								sendInvalidArgMessage(bukkitPlayer);
							}
							break;
						default:
							sendInvalidArgMessage(bukkitPlayer);
							break;
					} // End manage switch
				} else {
					sendInvalidArgMessage(bukkitPlayer);
				}
				break;
			default:
				sendInvalidArgMessage(bukkitPlayer);
				break;
		} // End kingdom switch
	}

	@Override
	public List<String> tabComplete(Earth earth, CommandSender sender, List<String> args) {
		// Sender must be player to get kingdom
		KonPlayer player = earth.getPlayerManager().getPlayer(sender);
		if (player == null) return Collections.emptyList();
		// Check for player's kingdom
		KonKingdom kingdom = player.getKingdom();
		if (kingdom == null) return Collections.emptyList();
		List<String> tabList = new ArrayList<>();
		int numArgs = args.size();
		if (numArgs == 1) {
			// suggest sub-commands
			tabList.add("menu");
			tabList.add("create");
			tabList.add("invites");
			tabList.add("join");
			tabList.add("exile");
			tabList.add("templates");
			tabList.add("manage");
		} else if (numArgs == 2) {
			switch (args.get(0).toLowerCase()) {
				case "create":
					tabList.addAll(earth.getSanctuaryManager().getAllValidTemplateNames());
					break;
				case "join":
					List<String> kingdomList = new ArrayList<>(earth.getKingdomManager().getKingdomNames());
					kingdomList.remove(player.getKingdom().getName());
					tabList.addAll(kingdomList);
					break;
				case "manage":
					tabList.add("disband");
					tabList.add("rename");
					tabList.add("template");
					tabList.add("access");
					tabList.add("diplomacy");
					tabList.add("requests");
					tabList.add("member");
					if (!earth.getCore().getBoolean(CorePath.KINGDOMS_WEB_COLOR_ADMIN_ONLY.getPath())) {
						tabList.add("webcolor");
					}
					if (earth.getKingdomManager().getIsTownDestroyMasterEnable()) {
						tabList.add("destroy");
					}
					break;
			}
		} else if (numArgs == 3) {
			switch (args.get(0).toLowerCase()) {
				case "create":
					tabList.add("***");
					break;
				case "manage":
					switch (args.get(1).toLowerCase()) {
						case "rename":
							tabList.add("***");
							break;
						case "destroy":
							if (earth.getKingdomManager().getIsTownDestroyMasterEnable()) {
								tabList.addAll(kingdom.getTownNames());
							}
							break;
						case "webcolor":
							if (!earth.getCore().getBoolean(CorePath.KINGDOMS_WEB_COLOR_ADMIN_ONLY.getPath())) {
								for (ColorRGB color : ColorRGB.values()) {
									tabList.add(color.getName());
								}
								tabList.add("#rrggbb");
								tabList.add("default");
							}
							break;
						case "template":
							tabList.addAll(earth.getSanctuaryManager().getAllTemplateNames());
							tabList.remove(kingdom.getMonumentTemplateName());
							break;
						case "access":
							tabList.add("open");
							tabList.add("closed");
							break;
						case "diplomacy":
							List<String> kingdomList = new ArrayList<>(earth.getKingdomManager().getKingdomNames());
							kingdomList.remove(player.getKingdom().getName());
							tabList.addAll(kingdomList);
							break;
						case "requests":
							for (OfflinePlayer requester : kingdom.getJoinRequests()) {
								tabList.add(requester.getName());
							}
							break;
						case "member":
							tabList.add("invite");
							tabList.add("kick");
							tabList.add("promote");
							tabList.add("demote");
							tabList.add("master");
							break;
					}
					break;
			}
		} else if (numArgs == 4) {
			if (args.get(0).equalsIgnoreCase("manage")) {
				switch (args.get(1).toLowerCase()) {
					case "diplomacy":
						String otherKingdomName = args.get(2);
						if (earth.getKingdomManager().isKingdom(otherKingdomName)) {
							for (EarthDiplomacyType relation : EarthDiplomacyType.values()) {
								if (earth.getKingdomManager().isValidRelationChoice(kingdom,earth.getKingdomManager().getKingdom(otherKingdomName),relation)) {
									tabList.add(relation.toString());
								}
							}
						}
						break;
					case "requests":
						tabList.add("accept");
						tabList.add("deny");
						break;
					case "member":
						switch (args.get(2).toLowerCase()) {
							case "invite":
								for(KonOfflinePlayer offlinePlayer : earth.getPlayerManager().getAllEarthOfflinePlayers()) {
									String name = offlinePlayer.getOfflineBukkitPlayer().getName();
									if(name != null && !kingdom.isMember(offlinePlayer.getOfflineBukkitPlayer().getUniqueId())) {
										tabList.add(name);
									}
								}
								break;
							case "kick":
								for(KonOfflinePlayer offlinePlayer : earth.getPlayerManager().getAllEarthOfflinePlayers()) {
									String name = offlinePlayer.getOfflineBukkitPlayer().getName();
									if(name != null && kingdom.isMember(offlinePlayer.getOfflineBukkitPlayer().getUniqueId())) {
										tabList.add(name);
									}
								}
								break;
							case "promote":
								for(OfflinePlayer offlinePlayer : kingdom.getPlayerMembersOnly()) {
									String name = offlinePlayer.getName();
									if(name != null) {
										tabList.add(name);
									}
								}
								break;
							case "demote":
								for(OfflinePlayer offlinePlayer : kingdom.getPlayerOfficersOnly()) {
									String name = offlinePlayer.getName();
									if(name != null) {
										tabList.add(name);
									}
								}
								break;
							case "master":
								for(OfflinePlayer offlinePlayer : kingdom.getPlayerMembers()) {
									String name = offlinePlayer.getName();
									if(name != null && !kingdom.isMaster(offlinePlayer.getUniqueId())) {
										tabList.add(name);
									}
								}
								break;
						}
						break;
				}
			}
		}
		return matchLastArgToList(tabList,args);
	}

}
