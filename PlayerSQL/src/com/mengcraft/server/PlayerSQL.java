package com.mengcraft.server;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.mcstats.Metrics;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class PlayerSQL extends JavaPlugin {

    private static PlayerSQL plugin = null;
    private static Connection connection = null;

    public static Connection getConnection() {
        return connection;
    }

    public static PlayerSQL getInstance() {
        return plugin;
    }

    @Override
    public void onLoad() {
        saveDefaultConfig();
        plugin = this;
    }

    @Override
    public void onEnable() {
        boolean use = getConfig().getBoolean("plugin.use", false);
        if (use) {
            try {
                setConnection();
                setDataTable();
                new CheckTask().runTaskTimer(this,
                        getConfig().getInt("plugin.check", 3000)
                        , getConfig().getInt("plugin.check", 3000)
                );
                getServer().getPluginManager().registerEvents(new PlayerListener(), this);

                String[] version = getServer().getBukkitVersion().split("-")[0].split("\\.");
                boolean useUUID = Integer.parseInt(version[1]) > 7
                        || (Integer.parseInt(version[1]) > 6
                        && Integer.parseInt(version[2]) > 5);
                getConfig().set("useUUID", useUUID);
                getLogger().info("Author: min梦梦");
                getLogger().info("插件作者: min梦梦");
            } catch (Exception e) {
                getLogger().warning("Failed to connect to database");
                getLogger().warning("Please modify config.yml!!!!!");
                getServer().getPluginManager().disablePlugin(this);
            }
            try {
                Metrics metrics = new Metrics(this);
                metrics.start();
            } catch (Exception e) {
                getLogger().warning("Failed to connect to mcstats.org");
                getLogger().warning("Failed to connect to mcstats.org");
            }
        } else {
            getLogger().warning("Please modify config.yml!!!");
            setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        PlayerManager.saveAll();
        getLogger().info("Author: min梦梦");
        getLogger().info("插件作者: min梦梦");
    }

    private void setConnection() {
        String driver = getConfig().getString("plugin.driver");
        String database = getConfig().getString("plugin.database");
        String username = getConfig().getString("plugin.username");
        String password = getConfig().getString("plugin.password");
        try {
            Class.forName(driver);
            PlayerSQL.connection = DriverManager.getConnection(database, username, password);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setDataTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS PlayerSQL(" +
                "ID int NOT NULL AUTO_INCREMENT, " +
                "NAME text NULL, " +
                "DATA text NULL, " +
                "PRIMARY KEY(ID)" +
                ");";
        Statement statement = connection.createStatement();
        statement.executeUpdate(sql);
        statement.close();
    }

    private class PlayerListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void playerQuit(PlayerQuitEvent event) {
            String name = event.getPlayer().getName();
            OnlinePlayer onlinePlayer = PlayerManager.getOnlinePlayer(name);
            onlinePlayer.savePlayer();
            onlinePlayer.stopSchedule();
            String message = "Player " + name + " offline";
            getLogger().info(message);
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void playerJoin(PlayerJoinEvent event) {
            String name = event.getPlayer().getName();
            OnlinePlayer onlinePlayer = PlayerManager.getOnlinePlayer(name);
            onlinePlayer.loadPlayer();
            onlinePlayer.startSchedule();
            String message = "Player " + name + " online";
            getLogger().info(message);
        }
    }

    private class CheckTask extends BukkitRunnable {
        @Override
        public void run() {
            if (connection != null) {
                boolean isClosed = false;
                try {
                    isClosed = connection.isClosed();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                if (isClosed) {
                    setConnection();
                }
            }
        }
    }
}
