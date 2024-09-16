package com.github.ssquadteam.earth.utility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.Date;

public class LoadingPrinter {

	private final int total;
	private int progress;
	private final String title;
	private final int normalTotal = 10;
	private int normalProgress;
	private Date start;
	
	public LoadingPrinter(int total, String title) {
		this.total = total;
		this.title = title;
		this.progress = 0;
		this.normalProgress = 0;
	}
	
	public void addProgress(int val) {
		if(progress == 0) {
			Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.GOLD+"[Earth] "+title);
			start = new Date();
		}
		
		progress = progress + val;
		if(progress > total) {
			progress = total;
		}
		int normalProgressNew = (int)(((double)progress / (double)total) * normalTotal);
		if(normalProgressNew != normalProgress) {
			int percentage = (int)((double)(100*normalProgress) / (double)(normalTotal));
			Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.GOLD+"> "+ChatColor.RESET+percentage+"%");
			normalProgress = normalProgressNew;
		}
		
		if(progress == total) {
			Date stop = new Date();
			int timeMs = (int)(stop.getTime()-start.getTime());
			Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.GOLD+"> "+ChatColor.RESET+"Done! Took "+timeMs+" ms");
		}

	}
	
}
