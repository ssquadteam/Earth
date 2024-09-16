package com.github.ssquadteam.earth.display.icon;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.command.admin.AdminCommandType;
import com.github.ssquadteam.earth.manager.DisplayManager;
import com.github.ssquadteam.earth.utility.CompatibilityUtil;
import com.github.ssquadteam.earth.utility.HelperUtil;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AdminCommandIcon  implements MenuIcon{

    private final AdminCommandType command;
    private final int index;
    private final ItemStack item;

    private final String loreColor = DisplayManager.loreFormat;

    public AdminCommandIcon(AdminCommandType command, int index) {
        this.command = command;
        this.index = index;
        this.item = initItem();
    }

    private ItemStack initItem() {
        List<String> loreList = new ArrayList<>(HelperUtil.stringPaginate(command.description(), loreColor));
        String name = ""+ChatColor.GOLD+ChatColor.ITALIC+getName();
        return CompatibilityUtil.buildItem(command.iconMaterial(), name, loreList);
    }

    public AdminCommandType getCommand() {
        return command;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public String getName() {
        return command.toString();
    }

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public boolean isClickable() {
        return true;
    }

}
