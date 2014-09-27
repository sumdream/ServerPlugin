package com.mengcraft.server;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.Metrics;

import java.io.IOException;
import java.util.List;

/**
 * Created by zmy on 14-5-30.
 * GPL v2 license
 */

public class BanWorld extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new Listener(), this);
        try {
            Metrics metrics = new Metrics(this);
            metrics.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] strings = {
                ChatColor.GREEN + "梦梦家高性能服务器出租",
                ChatColor.GREEN + "淘宝店 http://shop105595113.taobao.com"
        };
        Bukkit.getConsoleSender().sendMessage(strings);
    }

    public class Listener implements org.bukkit.event.Listener {
        @EventHandler
        public void playerTeleport(PlayerTeleportEvent event) {
            List<String> whiteList = getConfig().getStringList("white-list");
            String worldName = event.getTo().getWorld().getName();
            boolean status = whiteList.contains(worldName);
            if (!status) {
                Player player = event.getPlayer();
                status = player.hasPermission("world." + worldName);
                if (!status) {
                    String message = ChatColor.RED + "你没有前往 " + worldName + " 世界的权限";
                    player.sendMessage(message);
                    event.setCancelled(true);
                }
            }
        }

        @EventHandler
        public void playerPortal(PlayerPortalEvent event) {
            List<String> whiteList = getConfig().getStringList("white-list");
            String worldName = event.getTo().getWorld().getName();
            boolean status = whiteList.contains(worldName);
            if (!status) {
                Player player = event.getPlayer();
                status = player.hasPermission("world." + worldName);
                if (!status) {
                    String message = ChatColor.RED + "你没有前往 " + worldName + " 世界的权限";
                    player.sendMessage(message);
                    event.setCancelled(true);
                }
            }
        }
    }
}
