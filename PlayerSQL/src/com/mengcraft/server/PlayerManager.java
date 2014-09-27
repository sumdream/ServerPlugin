package com.mengcraft.server;

import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.util.HashMap;

public class PlayerManager {

    private static HashMap<String, OnlinePlayer> playerMap = new HashMap<String, OnlinePlayer>();

    public static void saveAll() {
        Player[] players = PlayerSQL.getInstance().getServer().getOnlinePlayers();
        if (players.length > 0) {
            try {
                OnlinePlayer onlinePlayer = new OnlinePlayer();
                String sql = "UPDATE PlayerSQL " +
                        "SET DATA = ?, ONLINE = 0 " +
                        "WHERE NAME = ?;";
                PreparedStatement statement = PlayerSQL.getConnection().prepareStatement(sql);
                for (Player player : players) {
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

    public static OnlinePlayer getOnlinePlayer(Player player) {
        OnlinePlayer onlinePlayer = playerMap.get(player.getName());
        if (onlinePlayer == null) {
            onlinePlayer = new OnlinePlayer(player);
            playerMap.put(player.getName(), onlinePlayer);
        }
        return onlinePlayer;
    }
}
