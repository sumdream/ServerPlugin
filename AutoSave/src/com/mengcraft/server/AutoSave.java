package com.mengcraft.server;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.defaults.SaveOffCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.mcstats.Metrics;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class AutoSave extends JavaPlugin implements Listener {

    private HashMap<String, Integer> playerMap = new HashMap<>();

    @Override
    public void onLoad() {
        saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        int delay = getConfig().getInt("delay.world", 1200);
        new SaveOffCommand().execute(getServer().getConsoleSender(), null, null);
        new WorldThread().runTaskTimer(this, delay, delay);
        new SendAdvert().runTaskLater(this, 120);
        try {
            new Metrics(this).start();
            getLogger().info("http://mcstats.org/ connected!");
        } catch (IOException e) {
            e.printStackTrace();
            getLogger().warning("Connect to http://mcstats.org/ failed!");
        }
    }

    @Override
    public void onDisable() {
        boolean save = getConfig().getBoolean("config.saveOnDisable", true);
        if (save) {
            List<World> worlds = getServer().getWorlds();
            for (World world : worlds) world.save();
        }
        String[] strings = {
                ChatColor.GREEN + "梦梦家高性能服务器出租",
                ChatColor.GREEN + "淘宝店 http://shop105595113.taobao.com"
        };
        Bukkit.getConsoleSender().sendMessage(strings);
    }

    @EventHandler
    public void playerLogin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final String key = player.getUniqueId().toString();
        Integer object = new PlayerThread(player).runTaskTimer(this, 6000, getConfig().getInt("delay.player", 6000)).getTaskId();
        playerMap.put(key, object);
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String key = player.getUniqueId().toString();
        boolean containsKey = playerMap.containsKey(key);
        if (containsKey) {
            Integer id = playerMap.get(key);
            playerMap.remove(key);
            getServer().getScheduler().cancelTask(id);
        }
    }

    private class PlayerThread extends BukkitRunnable {

        private final Player player;

        public PlayerThread(Player player) {
            this.player = player;
        }

        @Override
        public void run() {
            player.saveData();
        }
    }

    private class SendAdvert extends BukkitRunnable {
        @Override
        public void run() {
            String[] strings = {
                    ChatColor.GREEN + "梦梦家高性能服务器出租",
                    ChatColor.GREEN + "淘宝店 http://shop105595113.taobao.com"
            };
            Bukkit.getConsoleSender().sendMessage(strings);
        }
    }

    private class WorldThread extends BukkitRunnable {

        private int id = 0;

        @Override
        public void run() {
            List<World> worlds = getServer().getWorlds();
            if (id < worlds.size()) {
                worlds.get(id).save();
                for (Chunk chunk : worlds.get(id).getLoadedChunks()) chunk.unload(false, true);
                id = id + 1;
            } else {
                id = 0;
                run();
            }
        }
    }
}
