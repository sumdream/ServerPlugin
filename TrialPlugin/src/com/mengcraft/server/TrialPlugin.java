package com.mengcraft.server;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.mcstats.Metrics;

import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * Created by zmy on 14-7-8.
 * GPL v2 licence
 */
public class TrialPlugin extends JavaPlugin {
    private static TrialPlugin plugin;

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        getCommand("trial").setExecutor(new Commander());
        getServer().getPluginManager().registerEvents(new Listener(), this);
        try {
            new Metrics(this).start();
            getLogger().info("http://mcstats.org connected!");
        } catch (IOException e) {
            e.printStackTrace();
            getLogger().warning("connect to http://mcstats.org failed!");
        }
        String[] strings = {
                ChatColor.GREEN + "梦梦家高性能服务器出租",
                ChatColor.GREEN + "淘宝店 http://shop105595113.taobao.com"
        };
        Bukkit.getConsoleSender().sendMessage(strings);
    }

    public static TrialPlugin getInstance() {
        return plugin;
    }

    private class Listener implements org.bukkit.event.Listener {
        private final int delay;

        public Listener() {
            int delay = getConfig().getInt("config.delay", 60);
            if (delay > 0) this.delay = delay;
            else this.delay = 60;
        }

        @EventHandler
        public void playerSpawn(PlayerRespawnEvent event) {
            Player player = event.getPlayer();
            long trialTime = getTrialTime(player);
            if (trialTime > 0) {
                if (trialTime - System.currentTimeMillis() > 0) {
                    TrialPlayer task = new TrialPlayer(player);
                    task.runTaskLater(getInstance(), delay);
                } else setTrialTime(player);
            } else if (trialTime < 0) {
                TrialPlayer task = new TrialPlayer(player);
                task.runTaskLater(getInstance(), delay);
            }
        }

        @EventHandler
        public void playerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            if (player.getHealth() > 0) {
                long trialTime = getTrialTime(player);
                if (trialTime > 0) {
                    if (trialTime - System.currentTimeMillis() > 0) {
                        TrialPlayer task = new TrialPlayer(player);
                        task.runTaskLater(getInstance(), delay);
                    } else setTrialTime(player);
                } else if (trialTime < 0) {
                    TrialPlayer task = new TrialPlayer(player);
                    task.runTaskLater(getInstance(), delay);
                }
            }
        }
    }

    private class Commander implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length > 1) {
                try {
                    double time = Double.parseDouble(args[1]);
                    Player player = getServer().getPlayer(args[0]);
                    if (player != null) {
                        long trialTime = getTrialTime(player);
                        if (trialTime != 0) {
                            setTrialTime(player);
                            String message = ChatColor.GREEN + "移除玩家 " + player.getName() + " 从天罚列表";
                            sender.sendMessage(message);
                        } else {
                            setTrialTime(player, time);
                            new TrialPlayer(player).runTaskLater(getInstance(), 1);
                            String message = ChatColor.GREEN + "临时添加玩家 " + player.getName() + " 到天罚列表";
                            sender.sendMessage(message);
                        }
                    } else {
                        String message = ChatColor.RED + "玩家不存在或不在线!";
                        sender.sendMessage(message);
                    }
                } catch (NumberFormatException e) {
                    String message = ChatColor.RED + "参数 " + args[1] + " 不是有效的数字";
                    sender.sendMessage(message);
                }
            } else if (args.length > 0) {
                Player player = getServer().getPlayer(args[0]);
                if (player != null) {
                    long trialTime = getTrialTime(player);
                    if (trialTime != 0) {
                        setTrialTime(player);
                        String message = ChatColor.GREEN + "移除玩家 " + player.getName() + " 从天罚列表";
                        sender.sendMessage(message);
                    } else {
                        setTrialTime(player, -1);
                        new TrialPlayer(player).runTaskLater(getInstance(), 1);
                        String message = ChatColor.GREEN + "添加玩家 " + player.getName() + " 到天罚列表";
                        sender.sendMessage(message);
                    }
                } else {
                    String message = ChatColor.RED + "玩家不存在或不在线!";
                    sender.sendMessage(message);
                }
            } else {
                String[] message = {
                        ChatColor.RED + "/trial [player]           惩罚/解除[player]玩家",
                        ChatColor.RED + "/trial [player] [time]    惩罚某玩家[time]分钟",
                };
                sender.sendMessage(message);
            }
            return true;
        }
    }

    private class TrialPlayer extends BukkitRunnable {

        private final Player player;
        private final int delay;

        public TrialPlayer(Player player) {
            this.player = player;
            this.delay = getConfig().getInt("config.delay", 60);
        }

        @Override
        public void run() {
            boolean isOnline = player.isOnline();
            if (isOnline) {
                player.damage(999);
                boolean isDead = player.isDead();
                if (isDead) {
                    Location location = player.getLocation();
                    location.getWorld().strikeLightningEffect(location);
                    boolean broad = getInstance().getConfig().getBoolean("broadcast", true);
                    if (broad) {
                        String message = getBroadMessage().replaceAll("_PLAYER", player.getName());
                        getInstance().getServer().broadcastMessage(ChatColor.RED + message);
                    }
                } else new TrialPlayer(player).runTaskLater(getInstance(), delay);
            }
        }

        private String getBroadMessage() {
            List<String> messageList = getInstance().getConfig().getStringList("broad");
            int size = messageList.size();
            int i = new Random().nextInt(size);
            return messageList.get(i);
        }
    }

    private void setTrialTime(Player player, double time) {
        if (time > 0) {
            String name = player.getName();
            long current = System.currentTimeMillis();
            time = current + time * 60000;
            getConfig().set("player." + name, time);
        } else {
            String name = player.getName();
            getConfig().set("player." + name, -1);
        }
        saveConfig();
    }

    private void setTrialTime(Player player) {
        String name = player.getName();
        getConfig().set("player." + name, null);
        saveConfig();
    }

    private long getTrialTime(Player player) {
        String name = player.getName();
        return getConfig().getLong("player." + name);
    }
}
