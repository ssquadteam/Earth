package com.github.ssquadteam.earth.display;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.api.model.EarthTerritoryType;
import com.github.ssquadteam.earth.api.model.EarthUpgrade;
import com.github.ssquadteam.earth.display.icon.*;
import com.github.ssquadteam.earth.manager.DisplayManager;
import com.github.ssquadteam.earth.manager.KingdomManager;
import com.github.ssquadteam.earth.model.*;
import com.github.ssquadteam.earth.utility.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Villager;

import java.util.*;

public class TownManagementMenu extends StateMenu implements ViewableMenu {

    enum MenuState implements StateMenu.State {
        ROOT,
        A_REQUESTS,
        //A_PLOTS, <- there is no explicit plots state in this menu; it opens a new menu
        A_SHIELD,
        A_ARMOR,
        B_PROMOTE,
        B_DEMOTE,
        B_TRANSFER,
        B_DESTROY,
        B_UPGRADES,
        B_OPTIONS,
        B_SPECIALIZATION
    }

    enum AccessType implements StateMenu.Access {
        DEFAULT,
        KNIGHT,
        LORD
    }

    /* Icon slot indexes */
    private final int ROOT_SLOT_REQUESTS 		= 0;
    private final int ROOT_SLOT_PLOTS 			= 2;
    private final int ROOT_SLOT_INFO 			= 4;
    private final int ROOT_SLOT_SHIELD 			= 6;
    private final int ROOT_SLOT_ARMOR 			= 8;
    private final int ROOT_SLOT_PROMOTE 		= 10;
    private final int ROOT_SLOT_DEMOTE 	        = 11;
    private final int ROOT_SLOT_TRANSFER 		= 12;
    private final int ROOT_SLOT_DESTROY 		= 13;
    private final int ROOT_SLOT_UPGRADES 		= 14;
    private final int ROOT_SLOT_OPTIONS 		= 15;
    private final int ROOT_SLOT_SPECIALIZATION	= 16;

    private final int SLOT_YES 					= 3;
    private final int SLOT_NO 					= 5;

    private final String propertyColor = DisplayManager.propertyFormat;
    private final String alertColor = DisplayManager.alertFormat;
    private final String loreColor = DisplayManager.loreFormat;
    private final String valueColor = DisplayManager.valueFormat;
    private final String hintColor = DisplayManager.hintFormat;

    private final KingdomManager manager;
    private final KonPlayer player;
    private final KonTown town;
    private final boolean isAdmin;

    public TownManagementMenu(Earth earth, KonPlayer player, KonTown town, boolean isAdmin) {
        super(earth,MenuState.ROOT,AccessType.DEFAULT);
        this.manager = earth.getKingdomManager();
        this.player = player;
        this.town = town;
        this.isAdmin = isAdmin;

        initializeMenu();
        renderDefaultViews();
    }

    private void initializeMenu() {
        if(town != null) {
            UUID id = player.getBukkitPlayer().getUniqueId();
            if(isAdmin) {
                menuAccess = AccessType.LORD;
            } else {
                if(town.isLord(id)) {
                    menuAccess = AccessType.LORD;
                } else if(town.isPlayerKnight(player.getBukkitPlayer())) {
                    menuAccess = AccessType.KNIGHT;
                } else {
                    menuAccess = AccessType.DEFAULT;
                }
            }
        }
    }

    private void renderDefaultViews() {
        DisplayMenu renderView;

        /* Root View */
        renderView = createRootView();
        views.put(MenuState.ROOT, renderView);
        refreshNavigationButtons(MenuState.ROOT);

    }

    private DisplayMenu createRootView() {
        DisplayMenu result;
        MenuIcon icon;
        List<String> loreList = new ArrayList<>();
        String kingdomColor = Earth.friendColor2;

        int rows = 1;
        if(menuAccess.equals(AccessType.LORD)) {
            rows = 2;
        }

        result = new DisplayMenu(rows+1, getTitle(MenuState.ROOT));

        if(menuAccess.equals(AccessType.DEFAULT)) {
            // Invalid access, error icon
            icon = new InfoIcon("Error", loreList, Material.VOID_AIR, 4, false);
            result.addIcon(icon);

        } else {
            // Render icons for Knights and Lords
            if(menuAccess.equals(AccessType.KNIGHT) || menuAccess.equals(AccessType.LORD)) {
                /* Requests Icon */
                loreList.clear();
                loreList.add(propertyColor+MessagePath.LABEL_KNIGHT.getMessage());
                loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_REQUESTS.getMessage(),loreColor));
                int numRequests = town.getJoinRequests().size();
                Material requestMat = Material.GLASS_BOTTLE;
                if(numRequests > 0) {
                    loreList.add(valueColor+""+numRequests);
                    requestMat = Material.HONEY_BOTTLE;
                }
                loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
                icon = new InfoIcon(kingdomColor+MessagePath.MENU_TOWN_REQUESTS.getMessage(), loreList, requestMat, ROOT_SLOT_REQUESTS, true);
                result.addIcon(icon);

                /* Plots Icon */
                if (earth.getPlotManager().isEnabled()) {
                    loreList.clear();
                    loreList.add(propertyColor + MessagePath.LABEL_KNIGHT.getMessage());
                    loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_PLOTS.getMessage(), loreColor));
                    boolean isPlotsClickable = true;
                    boolean isTownPlotPropertyDisabled = town.hasPropertyValue(KonPropertyFlag.PLOTS) && !town.getPropertyValue(KonPropertyFlag.PLOTS);
                    if (isTownPlotPropertyDisabled) {
                        isPlotsClickable = false;
                        loreList.add(alertColor + MessagePath.LABEL_DISABLED.getMessage());
                    } else {
                        loreList.add(hintColor + MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
                    }
                    icon = new InfoIcon(kingdomColor + MessagePath.MENU_TOWN_PLOTS.getMessage(), loreList, Material.GRASS_BLOCK, ROOT_SLOT_PLOTS, isPlotsClickable);
                    result.addIcon(icon);
                }

                /* Info Icon */
                loreList.clear();
                loreList.add(hintColor + MessagePath.MENU_SCORE_HINT.getMessage());
                icon = new TownIcon(town, kingdomColor, loreList, ROOT_SLOT_INFO, true);
                result.addIcon(icon);

                /* Shields Icon */
                if (earth.getShieldManager().isShieldsEnabled()) {
                    loreList.clear();
                    loreList.add(propertyColor + MessagePath.LABEL_KNIGHT.getMessage());
                    loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_SHIELDS.getMessage(), loreColor));
                    loreList.add(hintColor + MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
                    icon = new InfoIcon(kingdomColor + MessagePath.MENU_TOWN_SHIELDS.getMessage(), loreList, Material.SHIELD, ROOT_SLOT_SHIELD, true);
                    result.addIcon(icon);
                }

                /* Armor Icon */
                if (earth.getShieldManager().isArmorsEnabled()) {
                    loreList.clear();
                    loreList.add(propertyColor + MessagePath.LABEL_KNIGHT.getMessage());
                    loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_ARMOR.getMessage(), loreColor));
                    loreList.add(hintColor + MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
                    icon = new InfoIcon(kingdomColor + MessagePath.MENU_TOWN_ARMOR.getMessage(), loreList, Material.DIAMOND_CHESTPLATE, ROOT_SLOT_ARMOR, true);
                    result.addIcon(icon);
                }
            }
            // Render icons for lords only
            if(menuAccess.equals(AccessType.LORD)) {
                /* Promote Icon */
                loreList = new ArrayList<>();
                loreList.add(propertyColor+MessagePath.LABEL_LORD.getMessage());
                loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_PROMOTE.getMessage(),loreColor));
                boolean isPromoteClickable = true;
                if(town.isPromoteable() || isAdmin) {
                    loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
                } else {
                    // This option is unavailable due to property flags
                    isPromoteClickable = false;
                    loreList.add(alertColor+MessagePath.LABEL_UNAVAILABLE.getMessage());
                }
                icon = new InfoIcon(kingdomColor+MessagePath.MENU_TOWN_PROMOTE.getMessage(), loreList, Material.DIAMOND_HORSE_ARMOR, ROOT_SLOT_PROMOTE, isPromoteClickable);
                result.addIcon(icon);

                /* Demote Icon */
                loreList.clear();
                loreList.add(propertyColor+MessagePath.LABEL_LORD.getMessage());
                loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_DEMOTE.getMessage(),loreColor));
                boolean isDemoteClickable = true;
                if(town.isDemoteable() || isAdmin) {
                    loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
                } else {
                    // This option is unavailable due to property flags
                    isDemoteClickable = false;
                    loreList.add(alertColor+MessagePath.LABEL_UNAVAILABLE.getMessage());
                }
                icon = new InfoIcon(kingdomColor+MessagePath.MENU_TOWN_DEMOTE.getMessage(), loreList, Material.LEATHER_HORSE_ARMOR, ROOT_SLOT_DEMOTE, isDemoteClickable);
                result.addIcon(icon);

                /* Transfer Icon */
                loreList.clear();
                loreList.add(propertyColor+MessagePath.LABEL_LORD.getMessage());
                loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_TRANSFER.getMessage(),loreColor));
                boolean isTransferClickable = true;
                if(town.isTransferable() || isAdmin) {
                    loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
                } else {
                    // This option is unavailable due to property flags
                    isTransferClickable = false;
                    loreList.add(alertColor+MessagePath.LABEL_UNAVAILABLE.getMessage());
                }
                icon = new InfoIcon(kingdomColor+MessagePath.MENU_TOWN_TRANSFER.getMessage(), loreList, Material.ELYTRA, ROOT_SLOT_TRANSFER, isTransferClickable);
                result.addIcon(icon);

                /* Destroy Icon */
                if (town.getTerritoryType().equals(EarthTerritoryType.TOWN) && earth.getKingdomManager().getIsTownDestroyLordEnable()) {
                    loreList.clear();
                    loreList.add(propertyColor + MessagePath.LABEL_LORD.getMessage());
                    loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_DESTROY.getMessage(), loreColor));
                    loreList.add(hintColor + MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
                    icon = new InfoIcon(kingdomColor + MessagePath.MENU_TOWN_DESTROY.getMessage(), loreList, Material.TNT, ROOT_SLOT_DESTROY, true);
                    result.addIcon(icon);
                }

                /* Upgrades Icon */
                if (earth.getUpgradeManager().isEnabled()) {
                    loreList.clear();
                    loreList.add(propertyColor + MessagePath.LABEL_LORD.getMessage());
                    loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_UPGRADES.getMessage(), loreColor));
                    boolean isUpgradesClickable = true;
                    boolean isTownUpgradePropertyDisabled = town.hasPropertyValue(KonPropertyFlag.UPGRADE) && !town.getPropertyValue(KonPropertyFlag.UPGRADE);
                    if (isTownUpgradePropertyDisabled) {
                        isUpgradesClickable = false;
                        loreList.add(alertColor + MessagePath.LABEL_DISABLED.getMessage());
                    } else {
                        loreList.add(hintColor + MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
                    }
                    icon = new InfoIcon(kingdomColor + MessagePath.MENU_TOWN_UPGRADES.getMessage(), loreList, Material.GOLDEN_APPLE, ROOT_SLOT_UPGRADES, isUpgradesClickable);
                    result.addIcon(icon);
                }

                /* Options Icon */
                loreList.clear();
                loreList.add(propertyColor+MessagePath.LABEL_LORD.getMessage());
                loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_OPTIONS.getMessage(),loreColor));
                loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
                icon = new InfoIcon(kingdomColor+MessagePath.MENU_TOWN_OPTIONS.getMessage(), loreList, Material.OAK_SIGN, ROOT_SLOT_OPTIONS, true);
                result.addIcon(icon);

                /* Specialization Icon */
                if (earth.getKingdomManager().getIsDiscountEnable()) {
                    loreList.clear();
                    loreList.add(propertyColor + MessagePath.LABEL_LORD.getMessage());
                    loreList.add(loreColor + MessagePath.MENU_OPTIONS_CURRENT.getMessage(valueColor + town.getSpecializationName()));
                    loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_SPECIAL.getMessage(), loreColor));
                    loreList.add(hintColor + MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
                    icon = new InfoIcon(kingdomColor + MessagePath.MENU_TOWN_SPECIAL.getMessage(), loreList, Material.EMERALD, ROOT_SLOT_SPECIALIZATION, true);
                    result.addIcon(icon);
                }
            }
        }
        return result;
    }

    private DisplayMenu createPlayerView(MenuState context) {
        // A paged view of players, with lore based on context
        DisplayMenu result;
        pages.clear();
        currentPage = 0;
        String loreHintStr1;
        String loreHintStr2 = "";
        PlayerIcon.PlayerIconAction iconAction = PlayerIcon.PlayerIconAction.DISPLAY_INFO;
        boolean isClickable;
        List<OfflinePlayer> players = new ArrayList<>();

        // Determine list of players given context
        if(context.equals(MenuState.A_REQUESTS)) {
            players.addAll(town.getJoinRequests());
            loreHintStr1 = MessagePath.MENU_TOWN_HINT_ACCEPT.getMessage();
            loreHintStr2 = MessagePath.MENU_TOWN_HINT_DECLINE.getMessage();
            isClickable = true;
        } else if(context.equals(MenuState.B_PROMOTE)) {
            players.addAll(town.getPlayerResidentsOnly());
            loreHintStr1 = MessagePath.MENU_TOWN_HINT_PROMOTE.getMessage();
            isClickable = true;
        } else if(context.equals(MenuState.B_DEMOTE)) {
            players.addAll(town.getPlayerKnightsOnly());
            loreHintStr1 = MessagePath.MENU_TOWN_HINT_DEMOTE.getMessage();
            isClickable = true;
        } else if(context.equals(MenuState.B_TRANSFER)) {
            players.addAll(town.getPlayerKnightsOnly());
            players.addAll(town.getPlayerResidentsOnly());
            loreHintStr1 = MessagePath.MENU_TOWN_HINT_TRANSFER.getMessage();
            isClickable = true;
        } else {
            return null;
        }

        // Create page(s)
        String pageLabel;
        List<String> loreList;
        int pageTotal = getTotalPages(players.size());
        int pageNum = 0;
        ListIterator<OfflinePlayer> listIter = players.listIterator();
        for(int i = 0; i < pageTotal; i++) {
            int pageRows = getNumPageRows(players.size(), i);
            pageLabel = getTitle(context)+" "+(i+1)+"/"+pageTotal;
            pages.add(pageNum, new DisplayMenu(pageRows+1, pageLabel));
            int slotIndex = 0;
            while(slotIndex < MAX_ICONS_PER_PAGE && listIter.hasNext()) {
                /* Player Icon (n) */
                OfflinePlayer currentPlayer = listIter.next();
                loreList = new ArrayList<>();
                String townRole = town.getPlayerRoleName(currentPlayer);
                if(!townRole.equals("")) {
                    loreList.add(propertyColor+townRole);
                }
                if(!loreHintStr1.equals("")) {
                    loreList.add(hintColor+loreHintStr1);
                }
                if(!loreHintStr2.equals("")) {
                    loreList.add(hintColor+loreHintStr2);
                }
                PlayerIcon playerIcon = new PlayerIcon(ChatColor.GREEN+currentPlayer.getName(),loreList,currentPlayer,slotIndex,isClickable,iconAction);
                pages.get(pageNum).addIcon(playerIcon);
                slotIndex++;
            }
            pageNum++;
        }
        result = pages.get(currentPage);
        return result;
    }

    private DisplayMenu createShieldView() {
        DisplayMenu result;
        pages.clear();
        currentPage = 0;
        String pageLabel;

        boolean isShieldsEnabled = earth.getShieldManager().isShieldsEnabled();
        if(!isShieldsEnabled) {
            return null;
        }

        // Page 0+
        List<KonShield> allShields = earth.getShieldManager().getShields();
        int pageTotal = getTotalPages(allShields.size());
        int pageNum = 0;
        ListIterator<KonShield> shieldIter = allShields.listIterator();
        for(int i = 0; i < pageTotal; i++) {
            int pageRows = getNumPageRows(allShields.size(), i);
            pageLabel = getTitle(MenuState.A_SHIELD)+" "+(i+1)+"/"+pageTotal;
            pages.add(pageNum, new DisplayMenu(pageRows+1, pageLabel));
            int slotIndex = 0;
            while(slotIndex < MAX_ICONS_PER_PAGE && shieldIter.hasNext()) {
                /* Shield Icon (n) */
                KonShield currentShield = shieldIter.next();
                ShieldIcon shieldIcon = new ShieldIcon(currentShield, true, town.getNumResidents(), town.getNumLand(), slotIndex);
                pages.get(pageNum).addIcon(shieldIcon);
                slotIndex++;
            }
            pageNum++;
        }
        result = pages.get(currentPage);
        return result;
    }

    private DisplayMenu createArmorView() {
        DisplayMenu result;
        pages.clear();
        currentPage = 0;
        String pageLabel;

        boolean isArmorsEnabled = earth.getShieldManager().isArmorsEnabled();
        if(!isArmorsEnabled) {
            return null;
        }

        // Page 0+
        List<KonArmor> allArmors = earth.getShieldManager().getArmors();
        int pageTotal = getTotalPages(allArmors.size());
        int pageNum = 0;
        ListIterator<KonArmor> armorIter = allArmors.listIterator();
        for(int i = 0; i < pageTotal; i++) {
            int pageRows = getNumPageRows(allArmors.size(), i);
            pageLabel = getTitle(MenuState.A_ARMOR)+" "+(i+1)+"/"+pageTotal;
            pages.add(pageNum, new DisplayMenu(pageRows+1, pageLabel));
            int slotIndex = 0;
            while(slotIndex < MAX_ICONS_PER_PAGE && armorIter.hasNext()) {
                /* Armor Icon (n) */
                KonArmor currentArmor = armorIter.next();
                ArmorIcon armorIcon = new ArmorIcon(currentArmor, true, town.getNumResidents(), town.getNumLand(), slotIndex);
                pages.get(pageNum).addIcon(armorIcon);
                slotIndex++;
            }
            pageNum++;
        }
        result = pages.get(currentPage);
        return result;
    }

    private DisplayMenu createUpgradeView() {
        DisplayMenu result;
        pages.clear();
        currentPage = 0;
        String pageLabel;

        boolean isUpgradesEnabled = earth.getUpgradeManager().isEnabled();
        if(!isUpgradesEnabled) {
            return null;
        }

        // Page 0+
        HashMap<EarthUpgrade, Integer> availableUpgrades = earth.getUpgradeManager().getAvailableUpgrades(town);
        // Place upgrades in ordinal order into list
        List<KonUpgrade> allUpgrades = new ArrayList<>();
        for(KonUpgrade upgrade : KonUpgrade.values()) {
            if(availableUpgrades.containsKey(upgrade)) {
                allUpgrades.add(upgrade);
            }
        }
        int pageTotal = getTotalPages(allUpgrades.size());
        int pageNum = 0;
        ListIterator<KonUpgrade> upgradeIter = allUpgrades.listIterator();
        for(int i = 0; i < pageTotal; i++) {
            int pageRows = getNumPageRows(allUpgrades.size(), i);
            pageLabel = getTitle(MenuState.B_UPGRADES)+" "+(i+1)+"/"+pageTotal;
            pages.add(pageNum, new DisplayMenu(pageRows+1, pageLabel));
            int slotIndex = 0;
            while(slotIndex < MAX_ICONS_PER_PAGE && upgradeIter.hasNext()) {
                /* Upgrade Icon (n) */
                KonUpgrade currentUpgrade = upgradeIter.next();
                int currentLevel = availableUpgrades.get(currentUpgrade);
                int cost = earth.getUpgradeManager().getUpgradeCost(currentUpgrade, currentLevel);
                int pop = earth.getUpgradeManager().getUpgradePopulation(currentUpgrade, currentLevel);
                UpgradeIcon upgradeIcon = new UpgradeIcon(currentUpgrade, currentLevel, slotIndex, cost, pop);
                pages.get(pageNum).addIcon(upgradeIcon);
                slotIndex++;
            }
            pageNum++;
        }
        result = pages.get(currentPage);
        return result;
    }

    private DisplayMenu createOptionsView() {
        DisplayMenu result;
        pages.clear();
        currentPage = 0;
        OptionIcon option;
        ArrayList<String> loreList;
        String currentValue;
        String pageLabel;

        // Page 0
        pageLabel = getTitle(MenuState.B_OPTIONS);
        int pageNum = 0;
        int pageRows = 1;
        pages.add(pageNum, new DisplayMenu(pageRows+1, pageLabel));

        // All town options
        int iconIndex = 1;
        for (KonTownOption townOption : KonTownOption.values()) {
            boolean isOptionEnabled = true;
            // Special option checks
            if (townOption.equals(KonTownOption.ALLIED_BUILDING)) {
                isOptionEnabled = earth.getCore().getBoolean(CorePath.KINGDOMS_ALLY_BUILD.getPath(),false);
            }
            // Try to add icon
            if (isOptionEnabled) {
                boolean val = town.getTownOption(townOption);
                currentValue = DisplayManager.boolean2Lang(val) + " " + DisplayManager.boolean2Symbol(val);
                loreList = new ArrayList<>(HelperUtil.stringPaginate(townOption.getDescription()));
                loreList.add(loreColor + MessagePath.MENU_OPTIONS_CURRENT.getMessage(valueColor + currentValue));
                loreList.add(hintColor + MessagePath.MENU_OPTIONS_HINT.getMessage());
                option = new OptionIcon(townOption, loreColor + townOption.getName(), loreList, townOption.getDisplayMaterial(), iconIndex);
                pages.get(pageNum).addIcon(option);
                iconIndex++;
            }
        }

        result = pages.get(currentPage);
        return result;
    }

    private DisplayMenu createSpecializationView() {
        DisplayMenu result;
        pages.clear();
        currentPage = 0;
        String pageLabel;

        boolean isSpecializationEnabled = earth.getKingdomManager().getIsDiscountEnable();
        if(!isSpecializationEnabled) {
            return null;
        }

        // Page 0
        pageLabel = getTitle(MenuState.B_SPECIALIZATION);
        int numEntries = CompatibilityUtil.getProfessions().size() - 1; // Subtract one to omit current specialization choice
        int pageRows = (int)Math.ceil((double)numEntries / 9);
        int pageNum = 0;
        pages.add(pageNum, new DisplayMenu(pageRows+1, pageLabel));
        int index = 0;
        List<String> loreList = new ArrayList<>();
        loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_LORE_SPECIAL.getMessage(),loreColor));
        if(!isAdmin) {
            double costSpecial = earth.getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SPECIALIZE.getPath());
            String cost = String.format("%.2f",costSpecial);
            loreList.add(loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+cost);
        }
        loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_SPECIAL.getMessage());
        for(Villager.Profession profession : CompatibilityUtil.getProfessions()) {
            if(town != null && !CompatibilityUtil.isProfessionEqual(profession,town.getSpecialization())) {
                ProfessionIcon professionIcon = new ProfessionIcon(loreList,profession,index,true);
                pages.get(pageNum).addIcon(professionIcon);
                index++;
            }
        }
        result = pages.get(currentPage);
        return result;
    }

    private DisplayMenu createDestroyView() {
        DisplayMenu result;
        pages.clear();
        currentPage = 0;
        String pageLabel;
        InfoIcon icon;

        // Page 0
        pageLabel = getTitle(MenuState.B_DESTROY);
        int pageNum = 0;
        pages.add(pageNum, new DisplayMenu(2, pageLabel));
        List<String> loreList = new ArrayList<>();

        loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_DESTROY.getMessage());
        icon = new InfoIcon(DisplayManager.boolean2Symbol(true), loreList, Material.GLOWSTONE_DUST, SLOT_YES, true);
        pages.get(pageNum).addIcon(icon);

        loreList.clear();
        loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_EXIT.getMessage());
        icon = new InfoIcon(DisplayManager.boolean2Symbol(false), loreList, Material.REDSTONE, SLOT_NO, true);
        pages.get(pageNum).addIcon(icon);

        result = pages.get(currentPage);
        return result;
    }

    @Override
    public DisplayMenu getCurrentView() {
        return views.get(currentState);
    }

    @Override
    public DisplayMenu updateState(int slot, boolean clickType) {
        // Assume a clickable icon was clicked
        // Do something based on current state and clicked slot
        DisplayMenu result = null;
        int navMaxIndex = getCurrentView().getInventory().getSize()-1;
        int navMinIndex = getCurrentView().getInventory().getSize()-9;
        if(slot <= navMaxIndex && slot >= navMinIndex) {
            // Clicked in navigation bar
            int index = slot-navMinIndex;
            // (back [0]) close [4], return [5], (next [8])
            if(index == 0) {
                result = goPageBack();
            } else if(index == 5) {
                // Return to previous root
                //result = views.get(MenuState.ROOT);
                result = goToRootView();
                currentState = MenuState.ROOT;
            } else if(index == 8) {
                result = goPageNext();
            }
        } else if(slot < navMinIndex) {
            // Click in non-navigation slot
            MenuIcon clickedIcon = views.get(currentState).getIcon(slot);
            MenuState currentMenuState = (MenuState)currentState;
            switch(currentMenuState) {
                case ROOT:
                    if(slot == ROOT_SLOT_REQUESTS) {
                        // Go to the player requests view
                        currentState = MenuState.A_REQUESTS;
                        result = goToPlayerView(MenuState.A_REQUESTS);

                    } else if(slot == ROOT_SLOT_PLOTS) {
                        // Open the plots menu
                        earth.getDisplayManager().displayTownPlotMenu(player.getBukkitPlayer(),town);
                        // Return null result to close this menu

                    } else if(slot == ROOT_SLOT_INFO) {
                        // Open the town info menu
                        earth.getDisplayManager().displayTownInfoMenu(player,town);
                        // Return null result to close this menu

                    } else if(slot == ROOT_SLOT_SHIELD) {
                        // Go to the shields view
                        currentState = MenuState.A_SHIELD;
                        result = goToShieldView();

                    } else if(slot == ROOT_SLOT_ARMOR) {
                        // Go to the armor view
                        currentState = MenuState.A_ARMOR;
                        result = goToArmorView();

                    } else if(slot == ROOT_SLOT_PROMOTE) {
                        // Clicked to view members to promote
                        currentState = MenuState.B_PROMOTE;
                        result = goToPlayerView(MenuState.B_PROMOTE);

                    } else if(slot == ROOT_SLOT_DEMOTE) {
                        // Clicked to view officers to demote
                        currentState = MenuState.B_DEMOTE;
                        result = goToPlayerView(MenuState.B_DEMOTE);

                    } else if(slot == ROOT_SLOT_TRANSFER) {
                        // Clicked to view members to transfer master
                        currentState = MenuState.B_TRANSFER;
                        result = goToPlayerView(MenuState.B_TRANSFER);

                    } else if(slot == ROOT_SLOT_DESTROY) {
                        // Clicked to destroy town
                        currentState = MenuState.B_DESTROY;
                        result = goToDestroyView();

                    } else if(slot == ROOT_SLOT_UPGRADES) {
                        // Clicked to view town upgrades
                        currentState = MenuState.B_UPGRADES;
                        result = goToUpgradeView();

                    }  else if(slot == ROOT_SLOT_OPTIONS) {
                        // Clicked to view town options
                        currentState = MenuState.B_OPTIONS;
                        result = goToOptionsView();

                    } else if(slot == ROOT_SLOT_SPECIALIZATION) {
                        // Clicked to view trade specializations
                        currentState = MenuState.B_SPECIALIZATION;
                        result = goToSpecializationView();

                    }
                    break;
                case A_REQUESTS:
                    if(clickedIcon instanceof PlayerIcon) {
                        PlayerIcon icon = (PlayerIcon)clickedIcon;
                        OfflinePlayer clickPlayer = icon.getOfflinePlayer();
                        boolean status = manager.menuRespondTownRequest(player, clickPlayer, town, clickType);
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = goToPlayerView(MenuState.A_REQUESTS);
                    }
                    break;
                case A_SHIELD:
                    if(clickedIcon instanceof ShieldIcon) {
                        ShieldIcon icon = (ShieldIcon)clickedIcon;
                        boolean status = earth.getShieldManager().activateTownShield(icon.getShield(), town, player.getBukkitPlayer(), isAdmin);
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = goToShieldView();
                    }
                    break;
                case A_ARMOR:
                    if(clickedIcon instanceof ArmorIcon) {
                        ArmorIcon icon = (ArmorIcon)clickedIcon;
                        boolean status = earth.getShieldManager().activateTownArmor(icon.getArmor(), town, player.getBukkitPlayer(), isAdmin);
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = goToArmorView();
                    }
                    break;
                case B_PROMOTE:
                    if(clickedIcon instanceof PlayerIcon) {
                        PlayerIcon icon = (PlayerIcon)clickedIcon;
                        OfflinePlayer clickPlayer = icon.getOfflinePlayer();
                        boolean status = manager.menuPromoteDemoteTownKnight(player,clickPlayer,town,true);
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = goToPlayerView(MenuState.B_PROMOTE);
                    }
                    break;
                case B_DEMOTE:
                    if(clickedIcon instanceof PlayerIcon) {
                        PlayerIcon icon = (PlayerIcon)clickedIcon;
                        OfflinePlayer clickPlayer = icon.getOfflinePlayer();
                        boolean status = manager.menuPromoteDemoteTownKnight(player,clickPlayer,town,false);
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = goToPlayerView(MenuState.B_DEMOTE);
                    }
                    break;
                case B_TRANSFER:
                    if(clickedIcon instanceof PlayerIcon) {
                        PlayerIcon icon = (PlayerIcon)clickedIcon;
                        OfflinePlayer clickPlayer = icon.getOfflinePlayer();
                        boolean status = manager.menuTransferTownLord(player,clickPlayer,town,isAdmin);
                        playStatusSound(player.getBukkitPlayer(),status);
                        // No result, close menu
                    }
                    break;
                case B_DESTROY:
                    if(slot == SLOT_YES) {
                        boolean status = manager.menuDestroyTown(town,player);
                        playStatusSound(player.getBukkitPlayer(),status);
                    }
                    break;
                case B_UPGRADES:
                    if(clickedIcon instanceof UpgradeIcon) {
                        UpgradeIcon icon = (UpgradeIcon)clickedIcon;
                        boolean status = earth.getUpgradeManager().addTownUpgrade(town, icon.getUpgrade(), icon.getLevel(), player.getBukkitPlayer());
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = goToUpgradeView();
                    }
                    break;
                case B_OPTIONS:
                    if(clickedIcon instanceof OptionIcon) {
                        OptionIcon icon = (OptionIcon)clickedIcon;
                        boolean status = manager.changeTownOption(icon.getOption(), town, player.getBukkitPlayer());
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = goToOptionsView();
                    }
                    break;
                case B_SPECIALIZATION:
                    if(clickedIcon instanceof ProfessionIcon) {
                        ProfessionIcon icon = (ProfessionIcon)clickedIcon;
                        Villager.Profession clickProfession = icon.getProfession();
                        boolean status = manager.menuChangeTownSpecialization(town, clickProfession, player, isAdmin);
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = goToSpecializationView();
                    }
                    break;
                default:
                    break;
            }
        }
        refreshNavigationButtons(currentState);
        return result;
    }

    private String getTitle(MenuState context) {
        String result = "error";
        String color = DisplayManager.titleFormat;
        if(isAdmin) {
            color = ""+ChatColor.DARK_PURPLE;
        }
        switch(context) {
            case ROOT:
                result = color+MessagePath.MENU_TOWN_TITLE_MANAGE.getMessage();
                break;
            case A_REQUESTS:
                result = color+MessagePath.MENU_TOWN_TITLE_REQUESTS.getMessage();
                break;
            case A_SHIELD:
                result = color+MessagePath.MENU_TOWN_SHIELDS.getMessage();
                break;
            case A_ARMOR:
                result = color+MessagePath.MENU_TOWN_ARMOR.getMessage();
                break;
            case B_PROMOTE:
                result = color+MessagePath.MENU_TOWN_PROMOTE.getMessage();
                break;
            case B_DEMOTE:
                result = color+MessagePath.MENU_TOWN_DEMOTE.getMessage();
                break;
            case B_TRANSFER:
                result = color+MessagePath.MENU_TOWN_TRANSFER.getMessage();
                break;
            case B_DESTROY:
                result = color+MessagePath.MENU_TOWN_DESTROY.getMessage();
                break;
            case B_UPGRADES:
                result = color+MessagePath.MENU_TOWN_UPGRADES.getMessage();
                break;
            case B_OPTIONS:
                result = color+MessagePath.MENU_TOWN_OPTIONS.getMessage();
                break;
            case B_SPECIALIZATION:
                result = color+MessagePath.MENU_TOWN_SPECIAL.getMessage();
                break;
            default:
                break;
        }
        return result;
    }

    private DisplayMenu goToRootView() {
        DisplayMenu result = createRootView();
        views.put(MenuState.ROOT, result);
        return result;
    }

    private DisplayMenu goToPlayerView(MenuState context) {
        DisplayMenu result = createPlayerView(context);
        views.put(context, result);
        return result;
    }

    private DisplayMenu goToShieldView() {
        DisplayMenu result = createShieldView();
        views.put(MenuState.A_SHIELD, result);
        return result;
    }

    private DisplayMenu goToArmorView() {
        DisplayMenu result = createArmorView();
        views.put(MenuState.A_ARMOR, result);
        return result;
    }

    private DisplayMenu goToUpgradeView() {
        DisplayMenu result = createUpgradeView();
        views.put(MenuState.B_UPGRADES, result);
        return result;
    }

    private DisplayMenu goToOptionsView() {
        DisplayMenu result = createOptionsView();
        views.put(MenuState.B_OPTIONS, result);
        return result;
    }

    private DisplayMenu goToSpecializationView() {
        DisplayMenu result = createSpecializationView();
        views.put(MenuState.B_SPECIALIZATION, result);
        return result;
    }

    private DisplayMenu goToDestroyView() {
        DisplayMenu result = createDestroyView();
        views.put(MenuState.B_DESTROY, result);
        return result;
    }

    /**
     * Place all navigation button icons on view given context and update icons
     */
    void refreshNavigationButtons(State context) {
        DisplayMenu view = views.get(context);
        if (view == null) return;
        int navStart = view.getInventory().getSize()-9;
        if(navStart < 0) {
            ChatUtil.printDebug("Guild menu nav buttons failed to refresh in context "+context.toString());
            return;
        }
        if(context.equals(MenuState.ROOT)) {
            // Close [4]
            view.addIcon(navIconEmpty(navStart));
            view.addIcon(navIconEmpty(navStart+1));
            view.addIcon(navIconEmpty(navStart+2));
            view.addIcon(navIconEmpty(navStart+3));
            view.addIcon(navIconClose(navStart+4));
            view.addIcon(navIconEmpty(navStart+5));
            view.addIcon(navIconEmpty(navStart+6));
            view.addIcon(navIconEmpty(navStart+7));
            view.addIcon(navIconEmpty(navStart+8));
        } else if(context.equals(MenuState.B_UPGRADES) || context.equals(MenuState.B_OPTIONS) || context.equals(MenuState.B_SPECIALIZATION) || context.equals(MenuState.B_DESTROY)) {
            // Close [4], Return [5]
            view.addIcon(navIconEmpty(navStart));
            view.addIcon(navIconEmpty(navStart+1));
            view.addIcon(navIconEmpty(navStart+2));
            view.addIcon(navIconEmpty(navStart+3));
            view.addIcon(navIconClose(navStart+4));
            view.addIcon(navIconReturn(navStart+5));
            view.addIcon(navIconEmpty(navStart+6));
            view.addIcon(navIconEmpty(navStart+7));
            view.addIcon(navIconEmpty(navStart+8));
        } else if(context.equals(MenuState.A_REQUESTS) || context.equals(MenuState.A_SHIELD) || context.equals(MenuState.A_ARMOR) ||
                context.equals(MenuState.B_PROMOTE) || context.equals(MenuState.B_DEMOTE) || context.equals(MenuState.B_TRANSFER)) {
            // (back [0]) close [4], return [5] (next [8])
            if(currentPage > 0) {
                // Place a back button
                view.addIcon(navIconBack(navStart));
            } else {
                view.addIcon(navIconEmpty(navStart));
            }
            if(currentPage < pages.size()-1) {
                // Place a next button
                view.addIcon(navIconNext(navStart+8));
            } else {
                view.addIcon(navIconEmpty(navStart+8));
            }
            view.addIcon(navIconEmpty(navStart+1));
            view.addIcon(navIconEmpty(navStart+2));
            view.addIcon(navIconEmpty(navStart+3));
            view.addIcon(navIconClose(navStart+4));
            view.addIcon(navIconReturn(navStart+5));
            view.addIcon(navIconEmpty(navStart+6));
            view.addIcon(navIconEmpty(navStart+7));
        }
        view.updateIcons();
    }

}
