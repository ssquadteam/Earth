package com.github.ssquadteam.earth.manager;


import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.hook.*;
import com.github.ssquadteam.earth.utility.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.ArrayList;


public class IntegrationManager {

	private final ArrayList<PluginHook> hooks;
	private final LuckPermsHook luckpermsHook;
	private final ChestShopHook chestshopHook;
	private final QuickShopHook quickshopHook;
	private final DiscordSrvHook discordsrvHook;
	private final DynmapHook dynmapHook;
	private final BlueMapHook bluemapHook;
	private final WorldGuardHook worldguardHook;
	private final ProtocolLibHook protocollibHook;
	private final PlaceholderAPIHook placeholderapiHook;
	private final EssentialsXHook essentialsXHook;
	
	public IntegrationManager(Earth earth) {
		this.hooks = new ArrayList<>();
		// Define new hooks
		luckpermsHook = new LuckPermsHook(earth);
		chestshopHook = new ChestShopHook(earth);
		quickshopHook = new QuickShopHook(earth);
		discordsrvHook = new DiscordSrvHook(earth);
		dynmapHook = new DynmapHook(earth);
		bluemapHook = new BlueMapHook(earth);
		worldguardHook = new WorldGuardHook(earth);
		protocollibHook = new ProtocolLibHook();
		placeholderapiHook = new PlaceholderAPIHook();
		essentialsXHook = new EssentialsXHook(earth);
		// Add hooks to set
		hooks.add(protocollibHook);
		hooks.add(placeholderapiHook);
		hooks.add(essentialsXHook);
		hooks.add(luckpermsHook);
		hooks.add(dynmapHook);
		hooks.add(bluemapHook);
		hooks.add(worldguardHook);
		hooks.add(discordsrvHook);
		hooks.add(quickshopHook);
		hooks.add(chestshopHook);

	}
	
	public void initialize() {
		// Reload any disabled hooks, display status
		ChatUtil.printConsoleAlert("Integrated Plugin Status...");
		ArrayList<Integer> statusList = new ArrayList<>();
		for (PluginHook hook : hooks) {
			int status = 0; // Active
			if (!hook.isEnabled()) {
				status = hook.reload(); // Update status from reload attempt
			}
			statusList.add(status);
		}
		// Display status at end
		for(int i = 0; i < hooks.size(); i++) {
			PluginHook hook = hooks.get(i);
			int code = statusList.get(i);
			String hookStatus = String.format(ChatColor.GOLD+"> "+ChatColor.RESET+"%-30s -> %s",hook.getPluginName(),getStatus(code));
			Bukkit.getServer().getConsoleSender().sendMessage(hookStatus);
		}
	}
	
	public void disable() {
		// Shutdown all hooks
		for (PluginHook hook : hooks) {
			hook.shutdown();
		}
		ChatUtil.printDebug("Integration Manager is disabled");
	}

	private String getStatus(int code) {
		String result = "";
		switch(code) {
			case 0:
				//result = ChatUtil.parseHex("#60C030")+"Active"; // Green
				result = ChatColor.DARK_GREEN+"Active";
				break;
			case 1:
				//result = ChatUtil.parseHex("#5080B0")+"Missing JAR"; // Light Blue
				result = ChatColor.DARK_AQUA+"Not Loaded";
				break;
			case 2:
				//result = ChatUtil.parseHex("#B040C0")+"Disabled"; // Light Purple
				result = ChatColor.LIGHT_PURPLE+"Disabled";
				break;
			case 3:
				//result = ChatUtil.parseHex("#808080")+"Inactive"; // Dark Gray
				result = ChatColor.GRAY+"Inactive";
				break;
			default:
				//result = ChatUtil.parseHex("#FF2020")+"Failed"; // Red
				result = ChatColor.DARK_RED+"Failed";
				break;
		}
		return result;
	}
	
	public LuckPermsHook getLuckPerms() {
		return luckpermsHook;
	}

	public ChestShopHook getChestShop() {
		return chestshopHook;
	}
	
	public QuickShopHook getQuickShop() {
		return quickshopHook;
	}
	
	public DiscordSrvHook getDiscordSrv() {
		return discordsrvHook;
	}

	public DynmapHook getDynmap() {
		return dynmapHook;
	}

	public BlueMapHook getBlueMap() {
		return bluemapHook;
	}

	public WorldGuardHook getWorldGuard() {
		return worldguardHook;
	}

	public ProtocolLibHook getProtocolLib() {
		return protocollibHook;
	}

	public PlaceholderAPIHook getPlaceholderAPI() {
		return placeholderapiHook;
	}

	public EssentialsXHook getEssentialsXAPI() {
		return essentialsXHook;
	}

}
