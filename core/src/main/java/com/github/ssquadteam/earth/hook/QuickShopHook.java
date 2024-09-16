package com.github.ssquadteam.earth.hook;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.listener.QuickShopListener;
import com.github.ssquadteam.earth.utility.CorePath;
import org.bukkit.*;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.api.QuickShopAPI;

public class QuickShopHook implements PluginHook {

	private final Earth earth;
	private QuickShopAPI quickShopAPI;
	private boolean isEnabled;
	
	public QuickShopHook(Earth earth) {
		this.earth = earth;
		this.isEnabled = false;
		this.quickShopAPI = null;
	}

	@Override
	public String getPluginName() {
		return "QuickShop";
	}

	@Override
	public int reload() {
		isEnabled = false;
		// Attempt to integrate QuickShop
		Plugin quickShop = Bukkit.getPluginManager().getPlugin("QuickShop");
		if(quickShop == null) {
			return 1;
		}
		if(!quickShop.isEnabled()) {
			return 2;
		}
		if(!earth.getCore().getBoolean(CorePath.INTEGRATION_QUICKSHOP.getPath(),false)) {
			return 3;
		}

		quickShopAPI = (QuickShopAPI) quickShop;
		isEnabled = true;
		earth.getPlugin().getServer().getPluginManager().registerEvents(new QuickShopListener(earth.getPlugin()), earth.getPlugin());
		return 0;
	}
	
	@Override
	public void shutdown() {
		// Do Nothing
	}

	@Override
	public boolean isEnabled() {
		return isEnabled;
	}

	@Nullable
	public QuickShopAPI getAPI() {
		return quickShopAPI;
	}

}
