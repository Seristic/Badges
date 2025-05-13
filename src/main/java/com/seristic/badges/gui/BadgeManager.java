package com.seristic.badges.gui;

import com.seristic.badges.Badges;
import com.seristic.badges.util.database.DatabaseManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.PrefixNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

public class BadgeManager {
    private static final HashMap<UUID, String> activeBadges = new HashMap<>();
    private static LuckPerms lp = null;

    private static String defaultBadge;
    private static boolean assignDefaultBadge;

    public static void setupLuckPerms() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null &&
        Bukkit.getPluginManager().getPlugin("LuckPerms").isEnabled()) {
            lp = LuckPermsProvider.get();
            Bukkit.getLogger().info(Badges.PREFIX + "Successfully hooked into LuckPerms.");
        } else {
            Bukkit.getLogger().warning(Badges.PREFIX + "LuckPerms not found or not enabled.");
        }
    }

    public static void loadConfig(FileConfiguration config) {
        assignDefaultBadge = config.getBoolean("assign-default-badge", true);
        defaultBadge = config.getString("default-badge", "&7✦");
    }

    public static LuckPerms getLuckPerms() {
        return lp;
    }

    public static void setBadge(Player player, String chatIcon) {
        if (lp == null) {
            player.sendMessage("§cLuckPerms is required for badges!");
            return;
        }

        UUID uuid = player.getUniqueId();
        String formattedIcon = ChatColor.translateAlternateColorCodes('&', chatIcon);
        activeBadges.put(uuid, chatIcon);

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "REPLACE INTO player_badges (uuid, active_badges) VALUES (?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, chatIcon);
        } catch (SQLException e) {
            Bukkit.getLogger().warning(Badges.PREFIX + "Failed to set badge: " + e.getMessage());
        }
        User user = lp.getUserManager().getUser(uuid);
        if (user != null) {
            user.data().clear(node -> node instanceof PrefixNode && node.getKey().startsWith("prefix"));

            PrefixNode newBadge = PrefixNode.builder()
                    .prefix(formattedIcon + "§r")
                    .priority(100)
                    .build();

            user.data().add(newBadge);
            lp.getUserManager().saveUser(user);

            player.sendMessage("§aYour badge has been updated to: " + formattedIcon);
        } else {
            player.sendMessage("§Could not retrieve your LuckPerms data.");
        }
    }

    public static String getBadge(Player player) {
        UUID uuid = player.getUniqueId();

        if (activeBadges.containsKey(uuid)) {
            return activeBadges.get(uuid);
        }

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT active_badge FROM player_badges WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String badge = rs.getString("active_badge");
                    activeBadges.put(uuid, badge);
                    return badge;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get badge for " + uuid, e);
        }
        return defaultBadge;
    }
    public static void clearBadge(Player player) {
        if (!assignDefaultBadge) return;

        UUID uuid = player.getUniqueId();

        try (Connection connection = DatabaseManager.getConnection()) {
            try (PreparedStatement check = connection.prepareStatement(
                    "SELECT uuid FROM player_badges WHERE uuid = ?")) {
                check.setString(1, uuid.toString());
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) return;
                }
            }

            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO player_badges (uuid, active_badge) VALUES (?, ?)")) {
                insert.setString(1, uuid.toString());
                insert.setString(2, defaultBadge);
                insert.executeUpdate();
            }
            setBadge(player, defaultBadge);

        } catch (SQLException e) {
            Bukkit.getLogger().warning("Failed to assign default badge: " + e.getMessage());
        }
    }

    public static boolean isDefaultBadge(String badgeIcon) {
        return badgeIcon != null && badgeIcon.equalsIgnoreCase(defaultBadge);
    }
}
