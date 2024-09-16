package com.github.ssquadteam.earth.model;

import com.github.ssquadteam.earth.utility.MessagePath;

public enum KonDirective {
	CREATE_KINGDOM	("earth.directive.kingdom",	1,		MessagePath.DIRECTIVE_CREATE_KINGDOM.getMessage(),	MessagePath.DIRECTIVE_CREATE_KINGDOM_INFO.getMessage()),
	SETTLE_TOWN		("earth.directive.settle",	1,		MessagePath.DIRECTIVE_SETTLE_TOWN.getMessage(),		MessagePath.DIRECTIVE_SETTLE_TOWN_INFO.getMessage()),
	CLAIM_LAND		("earth.directive.claim",	5,		MessagePath.DIRECTIVE_CLAIM_LAND.getMessage(),		MessagePath.DIRECTIVE_CLAIM_LAND_INFO.getMessage()),
	BUILD_TOWN		("earth.directive.build",	500,		MessagePath.DIRECTIVE_BUILD_TOWN.getMessage(),		MessagePath.DIRECTIVE_BUILD_TOWN_INFO.getMessage()),
	CRAFT_ARMOR		("earth.directive.armor",	4,		MessagePath.DIRECTIVE_CRAFT_ARMOR.getMessage(),		MessagePath.DIRECTIVE_CRAFT_ARMOR_INFO.getMessage()),
	CREATE_GOLEM	("earth.directive.golem",	1,		MessagePath.DIRECTIVE_CREATE_GOLEM.getMessage(),	MessagePath.DIRECTIVE_CREATE_GOLEM_INFO.getMessage()),
	ENCHANT_ITEM	("earth.directive.enchant",	3,		MessagePath.DIRECTIVE_ENCHANT_ITEM.getMessage(),	MessagePath.DIRECTIVE_ENCHANT_ITEM_INFO.getMessage()),
	ATTACK_TOWN		("earth.directive.attack",	1,		MessagePath.DIRECTIVE_ATTACK_TOWN.getMessage(),		MessagePath.DIRECTIVE_ATTACK_TOWN_INFO.getMessage()),
	CAPTURE_TOWN	("earth.directive.capture",	1,		MessagePath.DIRECTIVE_CAPTURE_TOWN.getMessage(),	MessagePath.DIRECTIVE_CAPTURE_TOWN_INFO.getMessage()),
	KILL_ENEMY		("earth.directive.kill",	1,		MessagePath.DIRECTIVE_KILL_ENEMY.getMessage(),		MessagePath.DIRECTIVE_KILL_ENEMY_INFO.getMessage());
	
	private final String permission;
	private final int stages;
	private final String title;
	private final String description;
	KonDirective(String permission, int stages, String title, String description) {
		this.permission = permission;
		this.stages = stages;
		this.title = title;
		this.description = description;
	}
	
	public String permission() {
		return permission;
	}
	
	public int stages() {
		return stages;
	}
	
	public String title() {
		return title;
	}
	
	public String description(){
		return description;
	}
	
	/**
	 * Gets a KonDirective enum given a string command.
	 * @param name The string name of the KonDirective
	 * @return KonDirective - Corresponding enum
	 */
	public static KonDirective getDirective(String name) {
		KonDirective result = null;
		for(KonDirective dir : KonDirective.values()) {
			if(dir.toString().equalsIgnoreCase(name)) {
				result = dir;
			}
		}
		return result;
	}
	
	
	
}
