package com.github.ssquadteam.earth.listener;

import com.github.ssquadteam.earth.Earth;

import com.github.ssquadteam.earth.utility.ChatUtil;
import github.scarsz.discordsrv.DiscordSRV;

import github.scarsz.discordsrv.api.ListenerPriority;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.*;


import github.scarsz.discordsrv.util.DiscordUtil;


public class DiscordSRVListener {

	private final Earth earth;

    public DiscordSRVListener(Earth earth) {
        this.earth = earth;
    }
    
    @Subscribe
    public void discordReadyEvent(DiscordReadyEvent event) {
        // Example of using JDA's events
        // We need to wait until DiscordSRV has initialized JDA, thus we're doing this inside DiscordReadyEvent
        DiscordUtil.getJda().addEventListener(new JDAListener());

        // ... we can also do anything other than listen for events with JDA now,
        ChatUtil.printConsole("Chatting on Discord with " + DiscordUtil.getJda().getUsers().size() + " users!");
        // see https://ci.dv8tion.net/job/JDA/javadoc/ for JDA's javadoc
        // see https://github.com/DV8FromTheWorld/JDA/wiki for JDA's wiki
    }

    @Subscribe(priority = ListenerPriority.MONITOR)
    public void onDiscordMessagePostProcess(DiscordGuildMessagePostProcessEvent event) {
    	String name = event.getAuthor().getName();
    	String channel = event.getChannel().getName();
    	String linkChannel = DiscordSRV.getPlugin().getDestinationGameChannelNameForTextChannel(event.getChannel());
    	String message = event.getMessage().getContentDisplay();
    	ChatUtil.printDebug("Received Discord message: Channel "+channel+"; Link "+linkChannel+"; Author "+name+"; Message "+message);
    	
    	// Send to kingdom chat if valid
    	earth.getIntegrationManager().getDiscordSrv().sendDiscordToGameChatKingdomChannel(event.getAuthor(), event.getMessage(), linkChannel);
    }

}
