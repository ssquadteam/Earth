package com.github.ssquadteam.earth.hook;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.listener.EssentialsXListener;
import com.github.ssquadteam.earth.utility.CorePath;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class EssentialsXHook implements PluginHook {

    private final Earth earth;
    private boolean isEnabled;

    public EssentialsXHook(Earth earth) {
        this.earth = earth;
        this.isEnabled = false;
    }

    @Override
    public String getPluginName() {
        return "EssentialsX";
    }

    @Override
    public int reload() {
        isEnabled = false;
        // Attempt to integrate EssentialsX
        Plugin essentialsX = Bukkit.getPluginManager().getPlugin("Essentials");
        if(essentialsX == null) {
            return 1;
        }
        if(!essentialsX.isEnabled()) {
            return 2;
        }
        if(!earth.getCore().getBoolean(CorePath.INTEGRATION_ESSENTIALSX.getPath(),false)) {
            return 3;
        }

        isEnabled = true;
        earth.getPlugin().getServer().getPluginManager().registerEvents(new EssentialsXListener(earth), earth.getPlugin());
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
}
