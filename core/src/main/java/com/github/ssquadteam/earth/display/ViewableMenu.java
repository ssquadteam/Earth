package com.github.ssquadteam.earth.display;

public interface ViewableMenu {

	DisplayMenu getCurrentView();
	
	DisplayMenu updateState(int slot, boolean clickType);
}
