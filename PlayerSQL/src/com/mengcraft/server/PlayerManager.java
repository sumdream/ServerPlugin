package com.mengcraft.server;

import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.util.HashMap;

public class PlayerManager {

    private static HashMap<String, OnlinePlayer> playerMap = new HashMap<>();

    public static void saveAll() {
        Player[] players = PlayerSQL.getInstance().getServer().getOnlinePlayers();
        if (players.length > 0) {
            try {
                String sql = "UPDATE PlayerSQL " +
                        "SET DATA = ? " +
                        "WHERE NAME = ?;";
                PreparedStatement statement = PlayerSQL.getConnection().prepareStatement(sql);
                for (Player player : players) {
                    OnlinePlayer onlinePlayer = PlayerManager.getOnlinePlayer(player.getName());
                    String name = onlinePlayer.getPlayerName(player);
                    String data = onlinePlayer.getPlayerData(player);
                    statement.setString(1, data);
                    statement.setString(2, name);
                    statement.addBatch();
                }
                statement.executeBatch();
                statement.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static OnlinePlayer getOnlinePlayer(String name) {
        OnlinePlayer onlinePlayer = playerMap.get(name);
        if (onlinePlayer == null) {
            onlinePlayer = new OnlinePlayer(name);
            playerMap.put(name, onlinePlayer);
        }
        return onlinePlayer;
    }
}
