package com.github.ssquadteam.earth.hook;

import com.Acrobot.ChestShop.ChestShop;
import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.listener.ChestShopListener;
import com.github.ssquadteam.earth.utility.CorePath;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class ChestShopHook implements PluginHook {

    private final Earth earth;
    private ChestShop chestShopAPI;
    private boolean isEnabled;

    public ChestShopHook(Earth earth) {
        this.earth = earth;
        this.isEnabled = false;
        this.chestShopAPI = null;
    }

    @Override
    public int reload() {
        isEnabled = false;
        // Attempt to integrate ChestShop
        Plugin chestShop = Bukkit.getPluginManager().getPlugin("ChestShop");
        if(chestShop == null) {
            return 1;
        }
        if(!chestShop.isEnabled()) {
            return 2;
        }
        if(!earth.getCore().getBoolean(CorePath.INTEGRATION_CHESTSHOP.getPath(),false)) {
            return 3;
        }

        chestShopAPI = (ChestShop) chestShop;
        isEnabled = true;
        earth.getPlugin().getServer().getPluginManager().registerEvents(new ChestShopListener(earth.getPlugin()), earth.getPlugin());
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

    @Override
    public String getPluginName() {
        return "ChestShop";
    }

    @Nullable
    public ChestShop getAPI() {
        return chestShopAPI;
    }
}
