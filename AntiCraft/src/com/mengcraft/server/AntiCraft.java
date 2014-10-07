package com.mengcraft.server;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.Metrics;

import java.io.IOException;
import java.util.List;

/**
 * Created by zmy on 14-10-4.
 * GPLv2 licence
 */
public class AntiCraft extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        String[] strings = {
                ChatColor.GREEN + "梦梦家高性能服务器出租",
                ChatColor.GREEN + "淘宝店 http://shop105595113.taobao.com"
        };
        getServer().getConsoleSender().sendMessage(strings);
        try {
            new Metrics(this).start();
        } catch (IOException e) {
            getLogger().warning("Can not link to Metrics server!");
        }
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        ItemStack stack = event.getItem().getItemStack();
        List<Short> typeList = getConfig().getShortList("item." + stack.getType());
        if (typeList.contains(stack.getDurability())) {
            event.getItem().setItemStack(new ItemStack(Material.AIR));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack stack = event.getCurrentItem();
        if (stack != null) {
            List<Short> typeList = getConfig().getShortList("item." + stack.getType());
            if (typeList.contains(stack.getDurability())) {
                event.setCurrentItem(new ItemStack(Material.AIR));
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("craft.admin") && sender instanceof Player) {
            Player player = getServer().getPlayerExact(sender.getName());
            if (args.length < 1) {
                sendInfo(sender);
            } else if (args.length < 2) {
                if (args[0].equals("add")) {
                    String name = player.getItemInHand().getType().toString();
                    List<Short> typeList = getConfig().getShortList("item." + name);
                    short type = player.getItemInHand().getDurability();
                    if (typeList.contains(type)) {
                        sender.sendMessage(ChatColor.RED + "列表内已有此物品");
                    } else {
                        typeList.add(type);
                        sender.sendMessage(ChatColor.RED + "添加物品 " + player.getItemInHand().getType() + " 到列表成功");
                        getConfig().set("item." + name, typeList);
                        saveConfig();
                    }
                } else if (args[0].equals("remove")) {
                    String name = player.getItemInHand().getType().toString();
                    List<Short> typeList = getConfig().getShortList("item." + name);
                    short type = player.getItemInHand().getDurability();
                    if (typeList.contains(type)) {
                        typeList.remove(type);
                        sender.sendMessage(ChatColor.RED + "从列表移除物品 " + player.getItemInHand().getType() + " 成功");
                        if (typeList.isEmpty()) {
                            getConfig().set("item." + name, null);
                        } else {
                            getConfig().set("item." + name, typeList);
                        }
                        saveConfig();
                    } else {
                        sender.sendMessage(ChatColor.RED + "列表内没有此物品");
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
        String[] message = {
                ChatColor.RED + "/craft add"
                , ChatColor.RED + "/craft remove"
        };
        sender.sendMessage(message);
    }
}
