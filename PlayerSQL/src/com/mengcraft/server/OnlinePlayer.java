package com.mengcraft.server;

import com.comphenix.protocol.utility.StreamSerializer;
import com.earth2me.essentials.craftbukkit.SetExpFix;
import com.google.common.collect.ImmutableMap;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by zmy on 14-7-26.
 * GPLv2 licence
 */

public class OnlinePlayer {

    private final String name;
    private int taskId = -1;

    public OnlinePlayer(String name) {
        this.name = name;
    }

    public void startSchedule() {
        if (taskId < 0) {
            taskId = new PlayerSchedule().runTaskTimerAsynchronously(
                    PlayerSQL.getInstance(),
                    6000,
                    6000
            ).getTaskId();
        }
    }

    public void stopSchedule() {
        if (taskId > 0) {
            PlayerSQL.getInstance().getServer().getScheduler().cancelTask(taskId);
        }
    }

    public void loadPlayer() {
        new LoadPlayer().runTaskLaterAsynchronously(PlayerSQL.getInstance(), 5);
    }

    public void savePlayer() {
        new SavePlayer().runTaskAsynchronously(PlayerSQL.getInstance());
    }

    public String getPlayerName(Player player) {
        boolean useUUID = PlayerSQL.getInstance().getConfig().getBoolean("useUUID", false);
        if (useUUID) {
            return player.getUniqueId().toString();
        }
        return player.getName();
    }

    public String getPlayerData(Player player) {
        ItemStack[] inventory = player.getInventory().getContents();
        ItemStack[] armor = player.getInventory().getArmorContents();
        ItemStack[] chest = player.getEnderChest().getContents();
        Object[] objects = {
                player.getHealth(),
                player.getFoodLevel(),
                SetExpFix.getTotalExperience(player),
                getStacks(inventory),
                getStacks(armor),
                getStacks(chest),
                getPotion(player)
        };
        List<Object> list = Arrays.asList(objects);
        return JSONArray.toJSONString(list);
    }

    private List<String> getStacks(ItemStack[] stacks) {
        List<String> stackList = new ArrayList<>();
        StreamSerializer serializer = StreamSerializer.getDefault();
        try {
            for (ItemStack stack : stacks) {
                if (stack != null) {
                    String base64 = serializer.serializeItemStack(stack);
                    stackList.add(base64);
                } else {
                    stackList.add(null);
                }
            }
        } catch (Exception e) {
            stackList.clear();
            e.printStackTrace();
        }
        return stackList;
    }

    private List<Map> getPotion(Player player) {
        List<Map> potions = new ArrayList<>();
        int size = player.getActivePotionEffects().size();
        if (size > 0) {
            PotionEffect[] active = player.getActivePotionEffects().toArray(new PotionEffect[size]);
            for (PotionEffect effect : active) {
                Map<String, Object> potion = effect.serialize();
                potions.add(potion);
            }
        }
        return potions;
    }

    private class PlayerSchedule extends BukkitRunnable {
        @Override
        public void run() {
            savePlayer();
        }
    }

    private class SavePlayer extends BukkitRunnable {
        @Override
        public void run() {
            Player player = PlayerSQL.getInstance().getServer().getPlayer(name);
            String playerName = getPlayerName(player);
            try {
                String sql = "UPDATE PlayerSQL " +
                        "SET DATA = ? " +
                        "WHERE NAME = ?;";
                PreparedStatement statement = PlayerSQL.getConnection().prepareStatement(sql);
                String playerData = getPlayerData(player);
                statement.setString(1, playerData);
                statement.setString(2, playerName);
                statement.executeUpdate();
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private class LoadPlayer extends BukkitRunnable {
        @Override
        public void run() {
            Player player = PlayerSQL.getInstance().getServer().getPlayerExact(name);
            String playerName = getPlayerName(player);
            try {
                String sql = "SELECT DATA FROM PlayerSQL " +
                        "WHERE NAME = ? FOR UPDATE;";
                PreparedStatement statement = PlayerSQL.getConnection().prepareStatement(sql);
                statement.setString(1, playerName);
                ResultSet result = statement.executeQuery();
                Boolean next = result.next();
                if (next) {
                    String data = result.getString(1);
                    if (data != null) {
                        JSONArray array = (JSONArray) JSONValue.parse(data);
                        loadPlayerInventory(player, array);
                        loadPlayerHealth(player, array);
                        loadPlayerLevel(player, array);
                        loadPlayerFood(player, array);
                        loadPlayerPotion(player, array);
                        loadPlayerChest(player, array);
                    }
                } else {
                    sql = "INSERT INTO PlayerSQL(NAME) " +
                            "VALUES(?);";
                    PreparedStatement statement_ = PlayerSQL.getConnection().prepareStatement(sql);
                    statement_.setString(1, playerName);
                    statement_.executeUpdate();
                    statement_.close();
                }
                statement.close();
                result.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void loadPlayerPotion(Player player, JSONArray array) {
            boolean sync = PlayerSQL.getInstance().getConfig().getBoolean("sync.potion", true);
            if (sync) {
                int size = player.getActivePotionEffects().size();
                if (size > 0) {
                    PotionEffect[] effects = player.getActivePotionEffects().toArray(new PotionEffect[size]);
                    for (PotionEffect effect : effects) {
                        PotionEffectType type = effect.getType();
                        player.removePotionEffect(type);
                    }
                }
                JSONArray potionArray = (JSONArray) array.get(6);
                if (potionArray.size() > 0) {
                    for (Object potion : potionArray) {
                        JSONObject object = (JSONObject) potion;
                        String effect = object.get("effect").toString();
                        String duration = object.get("duration").toString();
                        String amplifier = object.get("amplifier").toString();
                        Map<String, Object> map = ImmutableMap.of(
                                "effect", Integer.parseInt(effect),
                                "duration", Integer.parseInt(duration),
                                "amplifier", Integer.parseInt(amplifier),
                                "ambient", object.get("ambient")
                        );
                        player.addPotionEffect(new PotionEffect(map), true);
                    }
                }
            }
        }

        private void loadPlayerFood(Player player, List array) {
            boolean sync = PlayerSQL.getInstance().getConfig().getBoolean("sync.food", true);
            if (sync) {
                long food = (long) array.get(1);
                player.setFoodLevel((int) food);
            }
        }

        private void loadPlayerHealth(Player player, List array) {
            boolean sync = PlayerSQL.getInstance().getConfig().getBoolean("sync.health", true);
            if (sync) {
                double health = (double) array.get(0);
                try {
                    player.setHealth(health);
                } catch (Exception e) {
                    player.setHealth(player.getMaxHealth());
                }
            }
        }

        private void loadPlayerLevel(Player player, List array) {
            boolean sync = PlayerSQL.getInstance().getConfig().getBoolean("sync.exp", true);
            if (sync) {
                long exp = (long) array.get(2);
                SetExpFix.setTotalExperience(player, (int) exp);
            }
        }

        private void loadPlayerChest(Player player, List array) {
            boolean sync = PlayerSQL.getInstance().getConfig().getBoolean("sync.enderChest", true);
            if (sync) {
                JSONArray chest = (JSONArray) array.get(5);
                ItemStack[] stacks = getStacks(chest);
                player.getEnderChest().setContents(stacks);
            }
        }

        private void loadPlayerInventory(Player player, JSONArray array) {
            boolean sync = PlayerSQL.getInstance().getConfig().getBoolean("sync.inventory", true);
            if (sync) {
                JSONArray inventoryArray = (JSONArray) array.get(3);
                JSONArray armorArray = (JSONArray) array.get(4);
                ItemStack[] stacks = getStacks(inventoryArray);
                ItemStack[] armors = getStacks(armorArray);
                player.getInventory().setContents(stacks);
                player.getInventory().setArmorContents(armors);
            }
        }

        private ItemStack[] getStacks(List array) {
            StreamSerializer serializer = StreamSerializer.getDefault();
            List<ItemStack> stacks = new ArrayList<>();
            try {
                for (Object o : array) {
                    if (o != null) {
                        String base64 = o.toString();
                        ItemStack stack = serializer.deserializeItemStack(base64);
                        stacks.add(stack);
                    } else {
                        ItemStack stack = new ItemStack(Material.AIR);
                        stacks.add(stack);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                stacks.clear();
            }
            return stacks.toArray(new ItemStack[stacks.size()]);
        }
    }
}