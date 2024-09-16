package com.github.ssquadteam.earth.hook;

import com.github.ssquadteam.earth.utility.CorePath;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.utility.ChatUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LuckPermsHook implements PluginHook {

	private final Earth earth;
	private boolean isEnabled;
	private LuckPerms lpAPI;
	
	public LuckPermsHook(Earth earth) {
		this.earth = earth;
		this.isEnabled = false;
		this.lpAPI = null;
	}

	@Override
	public String getPluginName() {
		return "LuckPerms";
	}

	@Override
	public int reload() {
		isEnabled = false;
		// Attempt to integrate Luckperms
		Plugin luckPerms = Bukkit.getPluginManager().getPlugin("LuckPerms");
		if(luckPerms == null){
			return 1;
		}
		if(!luckPerms.isEnabled()){
			return 2;
		}
		if(!earth.getCore().getBoolean(CorePath.INTEGRATION_LUCKPERMS.getPath(),false)) {
			return 3;
		}
		RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
		if(provider == null){
			ChatUtil.printConsoleError("Failed to integrate LuckPerms, plugin not found or disabled");
			return -1;
		}
		lpAPI = provider.getProvider();
		isEnabled = true;
		return 0;
	}

	@Override
	public boolean isEnabled() {
		return isEnabled;
	}

	@Nullable
	public LuckPerms getLuckPermsAPI() {
		return lpAPI;
	}

	@NotNull
	public String getPrefix(Player player) {
		String prefix = "";
		if(!isEnabled)return prefix;

		User user = lpAPI.getUserManager().getUser(player.getUniqueId());
		QueryOptions queryOptions = lpAPI.getContextManager().getQueryOptions(player);
		CachedMetaData metaData = user.getCachedData().getMetaData(queryOptions);
		if (metaData.getPrefix() != null) {
			prefix = metaData.getPrefix().equalsIgnoreCase("null") ? "" : metaData.getPrefix();
		}

		return prefix;
	}

	@NotNull
	public String getSuffix(Player player) {
		String suffix = "";
		if(!isEnabled)return suffix;
		User user = lpAPI.getUserManager().getUser(player.getUniqueId());
		QueryOptions queryOptions = lpAPI.getContextManager().getQueryOptions(player);
		CachedMetaData metaData = user.getCachedData().getMetaData(queryOptions);
		if (metaData.getSuffix() != null) {
			suffix = metaData.getSuffix().equalsIgnoreCase("null") ? "" : metaData.getSuffix();
		}
		return suffix;
	}

}
