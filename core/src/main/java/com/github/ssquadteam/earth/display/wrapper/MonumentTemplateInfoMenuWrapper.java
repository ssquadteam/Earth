package com.github.ssquadteam.earth.display.wrapper;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.command.CommandType;
import com.github.ssquadteam.earth.display.icon.CommandIcon;
import com.github.ssquadteam.earth.display.icon.InfoIcon;
import com.github.ssquadteam.earth.display.icon.MenuIcon;
import com.github.ssquadteam.earth.display.icon.TemplateIcon;
import com.github.ssquadteam.earth.manager.DisplayManager;
import com.github.ssquadteam.earth.model.KonKingdom;
import com.github.ssquadteam.earth.model.KonMonumentTemplate;
import com.github.ssquadteam.earth.model.KonPlayer;
import com.github.ssquadteam.earth.model.KonSanctuary;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.HelperUtil;
import com.github.ssquadteam.earth.utility.Labeler;
import com.github.ssquadteam.earth.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

public class MonumentTemplateInfoMenuWrapper extends MenuWrapper {


    public MonumentTemplateInfoMenuWrapper(Earth earth) {
        super(earth);
    }

    @Override
    public void constructMenu() {

        String pageLabel;
        List<String> loreList;

        String titleColor = DisplayManager.titleFormat;
        String loreColor = DisplayManager.loreFormat;
        String valueColor = DisplayManager.valueFormat;

        /*
         * Display an icon for each valid monument template. Show:
         * - Name
         * - Associated sanctuary
         * - Number of kingdoms using it
         * - Cost
         * - Critical blocks
         * - Loot chests
         */
        // Pre-fetch template info maps
        HashMap<KonMonumentTemplate,String> templateSanctuaryMap = new HashMap<KonMonumentTemplate,String>();
        HashMap<KonMonumentTemplate,Integer> templateUsedKingdomsMap = new HashMap<KonMonumentTemplate,Integer>();
        ArrayList<KonMonumentTemplate> allTemplates = new ArrayList<>();
        for(KonSanctuary sanctuary : getEarth().getSanctuaryManager().getSanctuaries()) {
            for(KonMonumentTemplate template : sanctuary.getTemplates()) {
                allTemplates.add(template);
                templateSanctuaryMap.put(template,sanctuary.getName());
                // Count kingdoms using this template
                int count = 0;
                for(KonKingdom kingdom : getEarth().getKingdomManager().getKingdoms()) {
                    KonMonumentTemplate kingdomTemplate = kingdom.getMonumentTemplate();
                    if(kingdomTemplate != null && kingdomTemplate.equals(template)) {
                        count++;
                    }
                }
                templateUsedKingdomsMap.put(template,count);
            }
        }

        // Page 0+
        int pageNum = 0;
        if (allTemplates.isEmpty()) {
            // When there are no templates
            pageLabel = titleColor + MessagePath.LABEL_MONUMENT.getMessage();
            getMenu().addPage(pageNum, 1, pageLabel);
            /* Info Icon */
            loreList = new ArrayList<>(HelperUtil.stringPaginate(MessagePath.COMMAND_ADMIN_KINGDOM_ERROR_NO_TEMPLATES.getMessage(), ChatColor.RED));
            InfoIcon info = new InfoIcon(ChatColor.DARK_RED+MessagePath.LABEL_MONUMENT_TEMPLATE.getMessage(),loreList,Material.BARRIER,0,false);
            getMenu().getPage(pageNum).addIcon(info);
        } else {
            // When there are templates
            int pageTotal = getTotalPages(allTemplates.size());
            ListIterator<KonMonumentTemplate> templateIter = allTemplates.listIterator();
            for (int i = 0; i < pageTotal; i++) {
                int numPageRows = getNumPageRows(allTemplates.size(), i);
                pageLabel = titleColor + MessagePath.LABEL_MONUMENT.getMessage() + " " + (i + 1) + "/" + pageTotal;
                getMenu().addPage(pageNum, numPageRows, pageLabel);
                int slotIndex = 0;
                while (slotIndex < MAX_ICONS_PER_PAGE && templateIter.hasNext()) {
                    /* Template Icon (n) */
                    KonMonumentTemplate currentTemplate = templateIter.next();
                    double totalCost = getEarth().getKingdomManager().getCostTemplate() + currentTemplate.getCost();
                    loreList = new ArrayList<>();
                    loreList.add(loreColor + MessagePath.LABEL_SANCTUARY.getMessage() + ": " + valueColor + templateSanctuaryMap.get(currentTemplate));
                    loreList.add(loreColor + MessagePath.LABEL_KINGDOMS.getMessage() + ": " + valueColor + templateUsedKingdomsMap.get(currentTemplate));
                    loreList.add(loreColor + MessagePath.LABEL_COST.getMessage() + ": " + valueColor + totalCost);
                    TemplateIcon templateIcon = new TemplateIcon(currentTemplate, "" + ChatColor.GOLD, loreList, slotIndex, true);
                    getMenu().getPage(pageNum).addIcon(templateIcon);
                    slotIndex++;
                }
                pageNum++;
            }
        }

        getMenu().refreshNavigationButtons();
        getMenu().setPageIndex(0);
    }

    @Override
    public void onIconClick(KonPlayer clickPlayer, MenuIcon clickedIcon) {
        Player bukkitPlayer = clickPlayer.getBukkitPlayer();
        if(clickedIcon instanceof TemplateIcon) {
            // Template Icons close the GUI and print a command in chat
            TemplateIcon icon = (TemplateIcon)clickedIcon;
            KonMonumentTemplate template = icon.getTemplate();
            //TODO derive this from CommandType?
            String createCmdNotice = ChatColor.GOLD+"/k kingdom create "+template.getName()+" (name)";
            ChatUtil.sendNotice(bukkitPlayer, createCmdNotice);
        }
    }
}
