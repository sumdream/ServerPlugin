package com.mengcraft.server;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.Metrics;

import java.io.IOException;

/**
 * Created by zmy on 14-9-27.
 * GPLv2 licence
 */
public class AntiAnvil extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        try {
            new Metrics(this).start();
        } catch (IOException e) {
            getLogger().warning("Can not link to Metrics server!");
        }
        String[] strings = {
                ChatColor.GREEN + "梦梦家高性能服务器出租",
                ChatColor.GREEN + "淘宝店 http://shop105595113.taobao.com"
        };
        Bukkit.getConsoleSender().sendMessage(strings);
    }

    @EventHandler
    public void onClick(InventoryOpenEvent event) {
        if (event.getView().getTopInventory().getType().equals(InventoryType.ANVIL) && !event.getPlayer().hasPermission("anvil.use")) {
            getServer().getScheduler().runTaskLater(this, new CloseWindow(event.getPlayer()), 1);
        } else if (event.getView().getTopInventory().getType().equals(InventoryType.ENCHANTING) && !event.getPlayer().hasPermission("enchanting.use")) {
            getServer().getScheduler().runTaskLater(this, new CloseWindow(event.getPlayer()), 1);
        }
    }

    private class CloseWindow implements Runnable {
        private final HumanEntity player;

        public CloseWindow(HumanEntity player) {
            this.player = player;
        }

        @Override
        public void run() {
            player.closeInventory();
            getServer().getPlayerExact(player.getName()).sendMessage(ChatColor.RED + "你没有权限使用附魔台");
        }
    }
}
