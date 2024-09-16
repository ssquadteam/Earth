package com.github.ssquadteam.earth.hook;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.utility.CorePath;

import com.github.ssquadteam.earth.utility.HelperUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.awt.*;

public class WorldGuardHook implements PluginHook {

    private final Earth earth;
    private boolean isEnabled = false;

    public WorldGuardHook(Earth earth) {
        this.earth = earth;
    }

    @Override
    public int reload() {
        isEnabled = false;
        // Attempt to integrate WorldGuard
        Plugin worldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if(worldGuard == null){
            return 1;
        }
        if(!worldGuard.isEnabled()){
            return 2;
        }
        if(!earth.getCore().getBoolean(CorePath.INTEGRATION_WORLDGUARD.getPath(),false)) {
            return 3;
        }
        isEnabled = true;
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
        return "WorldGuard";
    }

    public boolean isChunkClaimAllowed(Location loc, Player player) {
        assert loc.getWorld() != null;
        return isChunkClaimAllowed(loc.getWorld(), HelperUtil.toPoint(loc), player);
    }

    public boolean isChunkClaimAllowed(World world, Point point, Player player) {
        if(!isEnabled) {
            // WorldGuard integration is disabled, always allow
            return true;
        }
        return WorldGuardExec.isChunkClaimAllowed(world, point, player);
    }

    public boolean isChunkUnclaimAllowed(Location loc, Player player) {
        assert loc.getWorld() != null;
        return isChunkUnclaimAllowed(loc.getWorld(), HelperUtil.toPoint(loc), player);
    }

    public boolean isChunkUnclaimAllowed(World world, Point point, Player player) {
        if(!isEnabled) {
            // WorldGuard integration is disabled, always allow
            return true;
        }
        return WorldGuardExec.isChunkUnclaimAllowed(world, point, player);
    }

    public boolean isLocationTravelEnterAllowed(Location loc, Player player) {
        if(!isEnabled) {
            // WorldGuard integration is disabled, always allow
            return true;
        }
        return WorldGuardExec.isLocationTravelEnterAllowed(loc, player);
    }

    public boolean isLocationTravelExitAllowed(Location loc, Player player) {
        if(!isEnabled) {
            // WorldGuard integration is disabled, always allow
            return true;
        }
        return WorldGuardExec.isLocationTravelExitAllowed(loc, player);
    }

    public boolean isLocationArenaAllowed(Location loc, Player player) {
        if(!isEnabled) {
            // WorldGuard integration is disabled, always deny
            return false;
        }
        return WorldGuardExec.isLocationArenaAllowed(loc, player);
    }

}
