package com.github.ssquadteam.earth.hook;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.CorePath;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapAPI;
import org.jetbrains.annotations.Nullable;

public class DynmapHook implements PluginHook {

    private final Earth earth;
    private boolean isEnabled;
    private DynmapAPI dAPI;

    public DynmapHook(Earth earth) {
        this.earth = earth;
        this.isEnabled = false;
        this.dAPI = null;
    }

    @Override
    public String getPluginName() {
        return "Dynmap";
    }

    @Override
    public int reload() {
        isEnabled = false;
        // Attempt to integrate Dynmap
        Plugin dynmap = Bukkit.getPluginManager().getPlugin("dynmap");
        if(dynmap == null){
            return 1;
        }
        if(!dynmap.isEnabled()){
            return 2;
        }
        if(!earth.getCore().getBoolean(CorePath.INTEGRATION_DYNMAP.getPath(),false)) {
            return 3;
        }
        dAPI = (DynmapAPI) Bukkit.getServer().getPluginManager().getPlugin("dynmap");
        if(dAPI == null) {
            ChatUtil.printConsoleError("Failed to register Dynmap. Is it disabled?");
            return -1;
        }
        isEnabled = true;
        return 0;
    }

    @Override
    public boolean isEnabled() { return isEnabled; }

    @Nullable
    public DynmapAPI getAPI() {
        return dAPI;
    }
}
