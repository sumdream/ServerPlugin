package com.mengcraft.server;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
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
            new Metrics(this).start();
        } catch (IOException e) {
            getLogger().warning("Can not link to Metrics server!");
        }
        String[] strings = {
                ChatColor.GREEN + "梦梦家高性能服务器出租",
                ChatColor.GREEN + "淘宝店 http://shop105595113.taobao.com"
        };
        getServer().getConsoleSender().sendMessage(strings);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("bw.admin") && sender instanceof Player) {
            if (args.length < 1) {
                sendInfo(sender);
            } else if (args.length < 2) {
                if (args[0].equals("add")) {
                    Player player = getServer().getPlayerExact(sender.getName());
                    List<String> bannedList = getConfig().getStringList("banned");
                    if (bannedList.contains(player.getWorld().getName())) {
                        sender.sendMessage(ChatColor.RED + "当前世界已经位于黑名单内");
                    } else {
                        bannedList.add(player.getWorld().getName());
                        saveConfig();
                        sender.sendMessage(ChatColor.RED + "把当前世界添加到黑名单成功");
                    }
                } else if (args[0].equals("remove")) {
                    Player player = getServer().getPlayerExact(sender.getName());
                    List<String> bannedList = getConfig().getStringList("banned");
                    if (bannedList.contains(player.getWorld().getName())) {
                        bannedList.remove(player.getWorld().getName());
                        saveConfig();
                        sender.sendMessage(ChatColor.RED + "把当前世界从黑名单删除成功");
                    } else {
                        sender.sendMessage(ChatColor.RED + "当前世界不位于黑名单内");
                    }
                } else {
                    sendInfo(sender);
                }
            } else {
                sendInfo(sender);
            }
        }
        return true;
    }

    private void sendInfo(CommandSender sender) {
        String[] strings = {
                ChatColor.RED + "/bw add               把当前世界添加到黑名单"
                , ChatColor.RED + "/bw remove            把当前世界从黑名单删除"
        };
        sender.sendMessage(strings);
    }

    private class Listener implements org.bukkit.event.Listener {
        @EventHandler
        public void playerTeleport(PlayerTeleportEvent event) {
            String worldName = event.getTo().getWorld().getName();
            if (getConfig().getStringList("banned").contains(worldName)) {
                boolean permission = event.getPlayer().hasPermission("bw.admin") || event.getPlayer().hasPermission("bw." + worldName);
                if (!permission) {
                    String message = ChatColor.RED + "你没有前往 " + worldName + " 世界的权限";
                    event.getPlayer().sendMessage(message);
                    event.setCancelled(true);
                }
            }
        }

        @EventHandler
        public void playerPortal(PlayerPortalEvent event) {
            String worldName = event.getTo().getWorld().getName();
            if (getConfig().getStringList("banned").contains(worldName)) {
                boolean permission = event.getPlayer().hasPermission("bw.admin") || event.getPlayer().hasPermission("bw." + worldName);
                if (!permission) {
                    String message = ChatColor.RED + "你没有前往 " + worldName + " 世界的权限";
                    event.getPlayer().sendMessage(message);
                    event.setCancelled(true);
                }
            }
        }
    }
}
