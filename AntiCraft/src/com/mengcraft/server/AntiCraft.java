package com.mengcraft.server;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Created by zmy on 14-10-4.
 * GPLv2 licence
 */
public class AntiCraft extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onInventoryClickEvent(InventoryClickEvent event) {
        ItemStack stack = event.getCurrentItem();
        List<Short> typeList = getConfig().getShortList("item." + stack.getType());
        if (typeList.contains(stack.getDurability())) {
            event.setCurrentItem(new ItemStack(Material.AIR));
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
                    sender.sendMessage(ChatColor.RED + "手动去配置文件改吧");
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
