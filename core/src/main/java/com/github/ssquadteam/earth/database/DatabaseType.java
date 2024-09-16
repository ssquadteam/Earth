package com.github.ssquadteam.earth.database;

import com.github.ssquadteam.earth.utility.ChatUtil;

public enum DatabaseType {
	MYSQL ("mysql.jar"),
    SQLITE ("sqlite.jar");

    private final String driver;

    DatabaseType(String driver) {
        this.driver = driver;
    }

    public String getDriver() {
        return driver;
    }

    public static DatabaseType getType(String str) {
        for(DatabaseType type : values()) {
            if(type.toString().equalsIgnoreCase(str)){
                return type;
            }
        }
        ChatUtil.printConsoleError("Failed to determine database connection type, "+str+". Using default SQLite.");
        return SQLITE;
    }
}
