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
    private int task;

    public OnlinePlayer(String name) {
        this.name = name;
        this.task = -1;
    }

    public void loadPlayer() {
        Player player = PlayerSQL.getInstance().getServer().getPlayerExact(name);
//        System.out.println(player.getWorld().getTime());
        String playerName = getPlayerName(player);
        new GetPlayer(playerName).runTaskLaterAsynchronously(PlayerSQL.getInstance(), 1);
        new LoadPlayer(player, playerName).runTaskLater(PlayerSQL.getInstance(), 2);
        if (task < 0) {
            task = new SaveTimer().runTaskTimer(PlayerSQL.getInstance(), 3600, 3600).getTaskId();
        }
    }

    public void savePlayer(boolean isLogout) {
        Player player = PlayerSQL.getInstance().getServer().getPlayerExact(name);
        if (isLogout) {
            PlayerSQL.getInstance().getServer().getScheduler().cancelTask(task);
        }
        new SavePlayer(
                getPlayerName(player)
                , getPlayerData(player)
                , isLogout
        ).runTaskAsynchronously(PlayerSQL.getInstance());
    }

    public String getPlayerName(Player player) {
        return PlayerSQL.getInstance().getConfig().getBoolean("useUUID", false)
                ? player.getUniqueId().toString()
                : player.getName();
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

    private class SaveTimer extends BukkitRunnable {
        @Override
        public void run() {
            savePlayer(false);
        }
    }

    private class SavePlayer extends BukkitRunnable {

        private final String playerName;
        private final String playerData;
        private final boolean isLogout;

        public SavePlayer(String playerName, String playerData, boolean isLogout) {
            this.playerName = playerName;
            this.playerData = playerData;
            this.isLogout = isLogout;
        }

        @Override
        public void run() {
            try {
                if (isLogout) {
                    PreparedStatement save = PlayerSQL.getConnection().prepareStatement("UPDATE PlayerSQL "
                                    + "SET DATA = ?, ONLINE = 0 "
                                    + "WHERE NAME = ?;"
                    );
                    save.setString(1, playerData);
                    save.setString(2, playerName);
                    save.executeUpdate();
                    save.close();
                } else {
                    PreparedStatement save = PlayerSQL.getConnection().prepareStatement("UPDATE PlayerSQL "
                                    + "SET DATA = ?, ONLINE = 1 "
                                    + "WHERE NAME = ?;"
                    );
                    save.setString(1, playerData);
                    save.setString(2, playerName);
                    save.executeUpdate();
                    save.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private class GetPlayer extends BukkitRunnable {
        private final String playerName;

        public GetPlayer(String playerName) {
            this.playerName = playerName;
        }

        @Override
        public void run() {
//            System.out.println("Get...");
            try {
                PreparedStatement onlineSelect = PlayerSQL.getConnection().prepareStatement("SELECT ONLINE " +
                                "FROM PlayerSQL WHERE NAME = ? FOR UPDATE;"
                );
                onlineSelect.setString(1, playerName);
                ResultSet onlineResult = onlineSelect.executeQuery();
                if (onlineResult.next()) {
                    if (onlineResult.getInt(1) < 1) {
                        getPlayer();
                        clearRetryPoint();
                    } else {
                        if (getRetryPoint() < 20) {
                            addRetryPoint();
                            new GetPlayer(playerName).runTaskLaterAsynchronously(PlayerSQL.getInstance(), 1);
                        } else {
                            getPlayer();
                            clearRetryPoint();
                        }
                    }
                } else {
                    PlayerSQL.getInstance().getConfig().set("player." + playerName, "NewPlayer");
                    PreparedStatement insert = PlayerSQL.getConnection().prepareStatement("INSERT INTO PlayerSQL(NAME) VALUES(?);");
                    insert.setString(1, playerName);
                    insert.executeUpdate();
                    insert.close();
                }
                onlineSelect.close();
                onlineResult.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void clearRetryPoint() {
            PlayerSQL.getInstance().getConfig().set("retry." + playerName, null);
        }

        private int getRetryPoint() {
            return PlayerSQL.getInstance().getConfig().getInt("retry." + playerName, 0);
        }

        private void addRetryPoint() {
            int point = PlayerSQL.getInstance().getConfig().getInt("retry." + playerName, 1);
            PlayerSQL.getInstance().getConfig().set("retry." + playerName, point + 1);
        }

        private void getPlayer() throws SQLException {
            PreparedStatement dataSelect = PlayerSQL.getConnection().prepareStatement("SELECT DATA " +
                            "FROM PlayerSQL WHERE NAME = ? FOR UPDATE;"
            );
            dataSelect.setString(1, playerName);
            ResultSet dataResult = dataSelect.executeQuery();
            if (dataResult.next()) {
                PlayerSQL.getInstance().getConfig().set("player." + playerName, dataResult.getString(1));
            }
            PreparedStatement updateOnline = PlayerSQL.getConnection().prepareStatement("UPDATE PlayerSQL "
                    + "SET ONLINE = 1 "
                    + "WHERE NAME = ?;");
            updateOnline.setString(1, playerName);
            updateOnline.executeUpdate();
            updateOnline.close();
            dataSelect.close();
            dataResult.close();
        }
    }

    private class LoadPlayer extends BukkitRunnable {

        private final Player player;
        private final String playerName;

        public LoadPlayer(Player player, String playerName) {
            this.player = player;
            this.playerName = playerName;
        }

        @Override
        public void run() {
            String data = PlayerSQL.getInstance().getConfig().getString("player." + playerName);
            if (data != null) {
                if (!data.equals("NewPlayer")) {
                    JSONArray array = (JSONArray) JSONValue.parse(data);
                    loadPlayerInventory(player, array);
                    loadPlayerHealth(player, array);
                    loadPlayerLevel(player, array);
                    loadPlayerFood(player, array);
                    loadPlayerPotion(player, array);
                    loadPlayerChest(player, array);
                }
                PlayerSQL.getInstance().getConfig().set("player." + playerName, null);
//                System.out.println(player.getWorld().getTime());
            } else {
//                System.out.println("Load...");
                new LoadPlayer(player, playerName).runTaskLater(PlayerSQL.getInstance(), 1);
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