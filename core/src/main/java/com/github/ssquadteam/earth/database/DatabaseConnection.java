package com.github.ssquadteam.earth.database;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.CorePath;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.*;

public class DatabaseConnection {

    private Connection connection;
    private final ExecutorService queryExecutor;
    private final DatabaseType type;

    public DatabaseConnection(DatabaseType type) {
        queryExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.type = type;
    }

    public void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
        	ChatUtil.printConsoleAlert("Could not connect to SQL database of type "+type.toString()+", connection is already open.");
            return;
        }
        Properties properties = new Properties();
        switch(type) {
        	case SQLITE:
        		try {
        			ChatUtil.printConsoleAlert("Connecting to SQLite database");
        			File databaseFile = migrateDatabaseFile();
                    connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath(), properties);
                    return;
                } catch (SQLException e) {
                	ChatUtil.printConsoleAlert("Failed to connect to SQLite database!");
                    e.printStackTrace();
                }
        		break;
        	case MYSQL:
        		try {
        			ChatUtil.printConsoleAlert("Connecting to MySQL database");
        			FileConfiguration coreConfig = Earth.getInstance().getCore();
                	String hostname = coreConfig.getString(CorePath.DATABASE_MYSQL_HOSTNAME.getPath());
                	String port = coreConfig.getString(CorePath.DATABASE_MYSQL_PORT.getPath());
                	String database = coreConfig.getString(CorePath.DATABASE_MYSQL_DATABASE.getPath());
                	String username = coreConfig.getString(CorePath.DATABASE_MYSQL_USERNAME.getPath(),"");
                	properties.put("user", username);
                	String password = coreConfig.getString(CorePath.DATABASE_MYSQL_PASSWORD.getPath(),"");
                    properties.put("password", password);
                    for(String nameValuePair : coreConfig.getStringList(CorePath.DATABASE_MYSQL_PROPERTIES.getPath())) {
                    	String[] propNameValue = nameValuePair.split("=",2);
                    	if(propNameValue.length == 2) {
                    		properties.put(propNameValue[0], propNameValue[1]);
                    	}
                    }
                    // DEBUG
                    ChatUtil.printDebug("Applying connection properties...");
                    for(String key : properties.stringPropertyNames()) {
                    	String value = properties.getProperty(key);
                    	ChatUtil.printDebug("  "+key+" = "+value);
                    }
                    // END DEBUG
                    connection = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + database, properties);
                    return;
                } catch (SQLException e) {
                	ChatUtil.printConsoleAlert("Failed to connect to MySQL database!");
                    e.printStackTrace();
                }
        		break;
        	default:
        		ChatUtil.printConsoleError("Could not connect to unknown database type "+ type);
        }
        
    }

    public void disconnect() {
        try {
            if (connection != null) {
                queryExecutor.shutdown();
                queryExecutor.awaitTermination(5, TimeUnit.SECONDS);
                connection.close();
            }
        } catch (SQLException | InterruptedException e) {
            e.printStackTrace();
        }

        connection = null;
    }

    public PreparedStatement prepare(String sql) {
        if (connection == null) {
            return null;
        }

        try {
            return connection.prepareStatement(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            ChatUtil.printConsoleError("Failed to prepare SQL statement, is the connection closed?");
        }

        return null;
    }

    public ResultSet scheduleQuery(String query) {
    	// Verify good connection, try to reconnect
    	if(testConnection(true)) {
    		ChatUtil.printConsoleAlert("Successfully reconnected to database");
    	}
    	
        Future<ResultSet> futureResult = queryExecutor.submit(new AsyncQuerySQL(this, query));
        try {
            return futureResult.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            ChatUtil.printConsoleError("Failed to schedule SQL query, InterruptedException");
        } catch (ExecutionException e) {
            e.printStackTrace();
            ChatUtil.printConsoleError("Failed to schedule SQL query, ExecutionException");
        }

        return null;
    }

    public void scheduleUpdate(String query) {
    	// Verify good connection, try to reconnect
    	if(testConnection(true)) {
    		ChatUtil.printConsoleAlert("Successfully reconnected to database");
    	}
    	
        queryExecutor.execute(new AsyncUpdateSQL(this, query));
    }

    public boolean pingDatabase() {
    	boolean result = false;
        Statement statement;
        try {
            statement = connection.createStatement();
            statement.executeQuery("SELECT 1;");
            statement.close();
            result = true;
        } catch (SQLException e) {
        	ChatUtil.printDebug("Failed to ping SQL database, caught exception:");
        	ChatUtil.printDebug(e.getMessage());
        }
        return result;
    }

    public Connection getConnection() {
        return connection;
    }
    
    private boolean testConnection(boolean reconnect) {
    	boolean result = false;
    	Statement statement;
        try {
            statement = connection.createStatement();
            statement.executeQuery("SELECT 1;");
            statement.close();
        } catch (SQLException e) {
        	if(reconnect) {
        		ChatUtil.printConsoleError("Failed to connect to database, trying to reconnect");
        		try {
        			connect();
        			result = true;
        		} catch(SQLException r) {
        			e.printStackTrace();
        			r.printStackTrace();
        		}
        	} else {
        		ChatUtil.printConsoleError("Failed to connect to database :(");
        		e.printStackTrace();
        	}
        }
    	return result;
    }
    
    private @NotNull File migrateDatabaseFile() {
        final String oldFileName = "Earthdatabase.db";
        final String newFileName = "data/EarthDatabase.db";
		File oldFile = new File(Earth.getInstance().getPlugin().getDataFolder(), oldFileName);
		File newFile = new File(Earth.getInstance().getPlugin().getDataFolder(), newFileName);
        if(!oldFile.exists()) return newFile;
        Path source = oldFile.toPath();
        Path destination = newFile.toPath();
        try {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
            oldFile.delete();
            ChatUtil.printConsoleAlert("Migrated database file "+oldFileName+" to "+newFileName);
        } catch (IOException e) {
            e.printStackTrace();
            ChatUtil.printDebug("Failed to move database file "+oldFileName+" to "+newFileName);
        }
        return newFile;
	}
}
