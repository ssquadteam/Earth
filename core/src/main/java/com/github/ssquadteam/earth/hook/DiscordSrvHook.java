package com.github.ssquadteam.earth.hook;

import com.github.ssquadteam.earth.utility.CorePath;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.util.DiscordUtil;
import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.listener.DiscordSRVListener;
import com.github.ssquadteam.earth.model.KonKingdom;
import com.github.ssquadteam.earth.model.KonPlayer;
import com.github.ssquadteam.earth.utility.ChatUtil;

public class DiscordSrvHook implements PluginHook {

	private final Earth earth;
	private boolean isEnabled;
	private final DiscordSRVListener discordSrvListener;
	
	public DiscordSrvHook(Earth earth) {
		this.earth = earth;
		this.isEnabled = false;
		this.discordSrvListener = new DiscordSRVListener(earth);
	}

	@Override
	public String getPluginName() {
		return "DiscordSRV";
	}

	@Override
	public int reload() {
		isEnabled = false;
		// Attempt to integrate DiscordSRV
		Plugin discordSrv = Bukkit.getPluginManager().getPlugin("DiscordSRV");
		if(discordSrv == null){
			return 1;
		}
		if(!discordSrv.isEnabled()){
			return 2;
		}
		if(!earth.getCore().getBoolean(CorePath.INTEGRATION_DISCORDSRV.getPath(),false)) {
			return 3;
		}
		try {
			DiscordSRV.api.subscribe(discordSrvListener);
			isEnabled = true;
			return 0;
		} catch (Exception e) {
			ChatUtil.printConsoleError("Failed to integrate DiscordSRV, see exception message:");
			e.printStackTrace();
			return -1;
		}
	}
	
	@Override
	public void shutdown() {
		if(isEnabled) {
			try {
				DiscordSRV.api.unsubscribe(discordSrvListener);
			} catch (Exception ignored) {}
		}
	}

	@Override
	public boolean isEnabled() {
		return isEnabled;
	}
	
	public String getLinkMessage(Player player) {
		if (!isEnabled) return "";
		String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
		if (discordId == null) {
			return ChatColor.RED + MessagePath.DISCORD_SRV_NO_LINK.getMessage();
		}

		User user = DiscordUtil.getJda().getUserById(discordId);
		if (user == null) {
			return ChatColor.YELLOW + MessagePath.DISCORD_SRV_NO_USER.getMessage();
		}

		return ChatColor.GREEN + MessagePath.DISCORD_SRV_LINKED_USER.getMessage(user.getAsTag());

	}
	
	public boolean sendGameToDiscordMessage(String channel, String message) {
		if (!isEnabled) return false;
		TextChannel textChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channel);

		// null if the channel isn't specified in the config.yml
		if (textChannel == null) {
			ChatUtil.printDebug("Channel called \""+channel+"\" could not be found in the DiscordSRV configuration");
			return false;
		}

		textChannel.sendMessage(message).queue();
		return true;
	}
	
	public void sendGameChatToDiscord(Player player, String message, String channel, boolean isCancelled) {
		if (!isEnabled) return;
		DiscordSRV.getPlugin().processChatMessage(player, message, channel, isCancelled);
	}
	
	// Send message from discord to kingdom chat
	public void sendDiscordToGameChatKingdomChannel(User guildUser, Message guildMessage, String kingdomChannel) {
		if (!isEnabled) return;

		if(kingdomChannel.equalsIgnoreCase("global") || !earth.getKingdomManager().isKingdom(kingdomChannel)) return;
		KonKingdom kingdom = earth.getKingdomManager().getKingdom(kingdomChannel);
		for(KonPlayer viewerPlayer : earth.getPlayerManager().getPlayersOnline()) {
			String messageFormat = "";
			String chatFormat =  ChatColor.WHITE+"["+ChatColor.AQUA+MessagePath.DISCORD_SRV_DISCORD.getMessage()+ChatColor.WHITE+"] ";
			String chatMessage = guildMessage.getContentDisplay();
			boolean sendMessage = false;
			if(viewerPlayer.getKingdom().equals(kingdom)) {
				chatFormat = chatFormat + Earth.friendColor1+kingdom.getName()+" "+guildUser.getName();
				messageFormat = ""+ChatColor.RESET+Earth.friendColor2+ChatColor.ITALIC;
				sendMessage = true;
			} else if(viewerPlayer.isAdminBypassActive()) {
				chatFormat = chatFormat + ChatColor.GOLD+kingdom.getName()+" "+guildUser.getName();
				messageFormat = ""+ChatColor.RESET+ChatColor.GOLD+ChatColor.ITALIC;
				sendMessage = true;
			}
			if(sendMessage) {
				viewerPlayer.getBukkitPlayer().sendMessage(chatFormat + " » " + messageFormat + chatMessage);
			}
		}
	}
	
	// Sends the linked player a direct message
	public void alertDiscordMember(OfflinePlayer player, String message) {
		boolean doAlert = earth.getCore().getBoolean(CorePath.INTEGRATION_DISCORDSRV_OPTIONS_RAID_ALERT_DIRECT.getPath(),false);
		if(isEnabled && doAlert) {
			String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
			if (discordId != null) {
				User user = DiscordUtil.getJda().getUserById(discordId);
				// will be null if the bot isn't in a Discord server with the user (e.g. they left the main Discord server)
		        if (user != null) {
		            // opens/retrieves the private channel for the user & sends a message to it (if retrieving the private channel was successful)
		            user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage(message).queue());
		        }
			}
		}
	}
	
	// Sends the channel a message to @everyone
	public void alertDiscordChannel(String channel, String message) {
		boolean doAlert = earth.getCore().getBoolean(CorePath.INTEGRATION_DISCORDSRV_OPTIONS_RAID_ALERT_CHANNEL.getPath(),false);
		if(!isEnabled || !doAlert)return;
		TextChannel textChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channel);

		// null if the channel isn't specified in the config.yml
		if(textChannel == null){
			ChatUtil.printDebug("Channel called \""+channel+"\" could not be found in the DiscordSRV configuration");
			return;
		}
		textChannel.sendMessage("@everyone " + message).queue();
	}

}
