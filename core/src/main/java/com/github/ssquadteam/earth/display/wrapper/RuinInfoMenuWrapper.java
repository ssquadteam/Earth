package com.github.ssquadteam.earth.display.wrapper;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.display.icon.InfoIcon;
import com.github.ssquadteam.earth.display.icon.MenuIcon;
import com.github.ssquadteam.earth.manager.DisplayManager;
import com.github.ssquadteam.earth.model.KonPlayer;
import com.github.ssquadteam.earth.model.KonPropertyFlag;
import com.github.ssquadteam.earth.model.KonRuin;
import com.github.ssquadteam.earth.utility.HelperUtil;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class RuinInfoMenuWrapper extends MenuWrapper {

    private final KonRuin infoRuin;

    public RuinInfoMenuWrapper(Earth earth, KonRuin infoRuin) {
        super(earth);
        this.infoRuin = infoRuin;
    }

    @Override
    public void constructMenu() {

        String pageLabel;
        List<String> loreList;

        String neutralColor = Earth.neutralColor2;
        String titleColor = DisplayManager.titleFormat;
        String loreColor = DisplayManager.loreFormat;
        String valueColor = DisplayManager.valueFormat;

        /*
         * Display info about this Ruin:
         *  All properties
         *  Capture status
         *  Total land size, world name, criticals, spawns
         */

        // Page 0
        pageLabel = titleColor+ MessagePath.LABEL_RUIN.getMessage()+" "+infoRuin.getName();
        getMenu().addPage(0, 1, pageLabel);

        /* Flags Info Icon (3) */
        loreList = new ArrayList<>();
        for(KonPropertyFlag flag : KonPropertyFlag.values()) {
            if(infoRuin.hasPropertyValue(flag)) {
                String flagDisplaySymbol = DisplayManager.boolean2Symbol(infoRuin.getPropertyValue(flag));
                loreList.add(loreColor+flag.getName()+": "+flagDisplaySymbol);
            }
        }
        InfoIcon propertyInfo = new InfoIcon(neutralColor+MessagePath.LABEL_FLAGS.getMessage(), loreList, Material.REDSTONE_TORCH, 3, false);
        getMenu().getPage(0).addIcon(propertyInfo);

        /* Capture Status Icon (4) */
        loreList = new ArrayList<>();
        Material iconMat;
        if(infoRuin.isCaptureDisabled()) {
            // Currently on capture cooldown
            for(String line : HelperUtil.stringPaginate(MessagePath.PROTECTION_ERROR_CAPTURE.getMessage(infoRuin.getCaptureCooldownString()))) {
                loreList.add(loreColor+line);
            }
            iconMat = Material.POPPY;
        } else {
            // Can be captured
            for(String line : HelperUtil.stringPaginate(MessagePath.COMMAND_INFO_NOTICE_RUIN_CAPTURE.getMessage())) {
                loreList.add(loreColor+line);
            }
            iconMat = Material.IRON_BLOCK;
        }
        InfoIcon captureInfo = new InfoIcon(neutralColor+MessagePath.COMMAND_INFO_NOTICE_RUIN_STATUS.getMessage(), loreList, iconMat, 4, false);
        getMenu().getPage(0).addIcon(captureInfo);

        /* General Info Icon (5) */
        loreList = new ArrayList<>();
        loreList.add(loreColor + MessagePath.MAP_CRITICAL_HITS.getMessage() + ": " + valueColor + infoRuin.getMaxCriticalHits());
        loreList.add(loreColor + MessagePath.MAP_GOLEM_SPAWNS.getMessage() + ": " + valueColor + infoRuin.getSpawnLocations().size());
        loreList.add(loreColor + MessagePath.LABEL_LAND.getMessage() + ": " + valueColor + infoRuin.getChunkList().size());
        loreList.add(loreColor + MessagePath.LABEL_WORLD.getMessage() + ": " + valueColor + infoRuin.getWorld().getName());
        InfoIcon generalInfo = new InfoIcon(neutralColor+MessagePath.LABEL_INFORMATION.getMessage(), loreList, Material.ENDER_EYE, 5, false);
        getMenu().getPage(0).addIcon(generalInfo);

        getMenu().refreshNavigationButtons();
        getMenu().setPageIndex(0);
    }

    @Override
    public void onIconClick(KonPlayer clickPlayer, MenuIcon clickedIcon) {
        // No clickable icons
    }
}
