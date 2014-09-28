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
    private final String playerName;

    public OnlinePlayer(Player player) {
        this.name = player.getName();
        this.playerName = getPlayerName(player);
    }

    public OnlinePlayer() {
        name = null;
        playerName = null;
    }

    public void loadPlayer() {
        Player player = PlayerSQL.getInstance().getServer().getPlayerExact(name);
        new GetPlayer(player).start();
    }

    public void savePlayer(boolean isLogout) {
        Player player = PlayerSQL.getInstance().getServer().getPlayerExact(name);
        new SavePlayer(
                getPlayerData(player)
                , isLogout
        ).start();
    }

    public String getPlayerName(Player player) {
        return PlayerSQL.getInstance().getConfig().getBoolean("useUUID", false)
                ? player.getUniqueId().toString() : player.getName();
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
        List<String> stackList = new ArrayList<String>();
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
        List<Map> potions = new ArrayList<Map>();
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

    private class SavePlayer extends Thread {

        private final String playerData;
        private final boolean isLogout;

        public SavePlayer(String playerData, boolean isLogout) {
            this.playerData = playerData;
            this.isLogout = isLogout;
        }

        @Override
        public void run() {
            try {
                PreparedStatement save = PlayerSQL.getConnection().prepareStatement("UPDATE PlayerSQL "
                                + "SET DATA = ?, ONLINE = ? "
                                + "WHERE NAME = ?;"
                );
                save.setString(1, playerData);
                save.setInt(2, isLogout ? 0 : 1);
                save.setString(3, playerName);
                save.executeUpdate();
                save.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private class GetPlayer extends Thread {
        private final Player player;
        private int times;

        public GetPlayer(Player player) {
            this.player = player;
            this.times = 0;
        }

        @Override
        public void run() {
            try {
                if (times < 1) {
                    Thread.sleep(100);
                }
                PreparedStatement onlineSelect = PlayerSQL.getConnection().prepareStatement(
                        "SELECT ONLINE FROM PlayerSQL WHERE NAME = ? FOR UPDATE;"
                );
                onlineSelect.setString(1, playerName);
                ResultSet onlineResult = onlineSelect.executeQuery();
                if (onlineResult.next()) {
                    if (onlineResult.getInt(1) < 1) {
                        new LoadPlayer(getPlayerData()).runTask(PlayerSQL.getInstance());
                        if (player.isOnline()) {
                            lockPlayer();
                        }
                    } else {
                        if (times < 5) {
                            times += 1;
                            Thread.sleep(50);
                            run();
                        } else {
                            new LoadPlayer(getPlayerData()).runTask(PlayerSQL.getInstance());
                            if (player.isOnline()) {
                                lockPlayer();
                            }
                        }
                    }
                } else {
                    PreparedStatement insert = PlayerSQL.getConnection().prepareStatement(
                            "INSERT INTO PlayerSQL(NAME) VALUES(?);"
                    );
                    insert.setString(1, playerName);
                    insert.executeUpdate();
                    insert.close();
                    PlayerSQL.getListener().remove(player);
                }
                onlineSelect.close();
                onlineResult.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private String getPlayerData() throws SQLException {
            PreparedStatement dataSelect = PlayerSQL.getConnection().prepareStatement("SELECT DATA " +
                            "FROM PlayerSQL WHERE NAME = ? FOR UPDATE;"
            );
            dataSelect.setString(1, playerName);
            ResultSet dataResult = dataSelect.executeQuery();
            String data = dataResult.next() ? dataResult.getString(1) : null;
            dataSelect.close();
            dataResult.close();
            return data;
        }

        private void lockPlayer() throws SQLException {
            PreparedStatement updateOnline = PlayerSQL.getConnection().prepareStatement("UPDATE PlayerSQL "
                    + "SET ONLINE = 1 "
                    + "WHERE NAME = ?;");
            updateOnline.setString(1, playerName);
            updateOnline.executeUpdate();
            updateOnline.close();
        }

        private class LoadPlayer extends BukkitRunnable {

            private final String data;

            public LoadPlayer(String data) {
                this.data = data;
            }

            @Override
            public void run() {
                if (data != null) {
                    JSONArray array = (JSONArray) JSONValue.parse(data);
                    loadPlayerInventory(player, array);
                    loadPlayerHealth(player, array);
                    loadPlayerLevel(player, array);
                    loadPlayerFood(player, array);
                    loadPlayerPotion(player, array);
                    loadPlayerChest(player, array);
                    PlayerSQL.getListener().remove(player);
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
                    int food = Integer.parseInt(array.get(1).toString());
                    player.setFoodLevel(food);
                }
            }

            private void loadPlayerHealth(Player player, List array) {
                boolean sync = PlayerSQL.getInstance().getConfig().getBoolean("sync.health", true);
                if (sync) {
                    double health = Double.parseDouble(array.get(0).toString());
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
                    int exp = Integer.parseInt(array.get(2).toString());
                    SetExpFix.setTotalExperience(player, exp);
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
                List<ItemStack> stacks = new ArrayList<ItemStack>();
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
}