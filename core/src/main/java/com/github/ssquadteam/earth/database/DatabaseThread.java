package com.github.ssquadteam.earth.database;

import com.github.ssquadteam.earth.Earth;
import com.github.ssquadteam.earth.model.KonPlayer;
import com.github.ssquadteam.earth.utility.ChatUtil;
import com.github.ssquadteam.earth.utility.CorePath;
import org.bukkit.Bukkit;

public class DatabaseThread implements Runnable {
    private final Earth earth;

    private EarthDB database;
    private final Thread thread;
    private int sleepSeconds;
    private boolean running = false;

    public DatabaseThread(Earth earth) {
        this.earth = earth;
        this.sleepSeconds = 3600;
        thread = new Thread(this);
    }

    public void run() {
        running = true;
        // Start timeout check for database ready
        long delayTicks = 10*20; // 10 seconds
        Bukkit.getScheduler().scheduleSyncDelayedTask(earth.getPlugin(), () -> {
            // Actions to run after delay
            if (database.isReady()) {
                ChatUtil.printConsoleAlert("Earth Database passed startup check.");
            } else {
                ChatUtil.printConsoleError("Something went wrong when starting the Earth Database, and the plugin will not behave correctly! Please report this to the Earth developer.");
            }
        }, delayTicks);

        createDatabase();
        database.initialize();

        //TODO - look at exchanging this for a Schedule every sleepSeconds, instead of using infinite loop
        Thread databaseFlusher = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(sleepSeconds* 1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                flushDatabase();
            }
        });
        databaseFlusher.start();
    }
    
    public boolean isRunning() {
    	return running;
    }

    public void createDatabase() {
    	String dbType = earth.getCore().getString(CorePath.DATABASE_CONNECTION.getPath(),"sqlite");
    	DatabaseType type = DatabaseType.getType(dbType);
    	database = new EarthDB(type,earth);
    }

    public Thread getThread() {
        return thread;
    }

    public EarthDB getDatabase() {
        return database;
    }
    
    public void setSleepSeconds(int val) {
    	sleepSeconds = val;
    }

    public void flushDatabase() {
    	ChatUtil.printDebug("Flushing entire database for all online players");
        for (KonPlayer player : earth.getPlayerManager().getPlayersOnline()) {
            database.flushPlayerData(player.getBukkitPlayer());
        }
    }
}
