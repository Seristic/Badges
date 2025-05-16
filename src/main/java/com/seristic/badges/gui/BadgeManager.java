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
import java.util.*;

public class BadgeManager {
    private static final Map<UUID, List<String>> activeBadges = new HashMap<>();
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

        // Extract text inside brackets if present
        String dbCheckIcon = formattedIcon.matches("\\[.*]?")
                ? formattedIcon.replaceAll("^\\[\\s*(.*?)\\s*]$", "$1")
                : formattedIcon;

        // Check if badge exists in main badges table
        if (!DatabaseManager.badgeExists(dbCheckIcon)) {
            player.sendMessage("§cBadge not found in the database: " + dbCheckIcon);
            Bukkit.getLogger().warning(Badges.PREFIX + "Badge not found in the database for chat_icon: " + dbCheckIcon);
            return;
        }

        activeBadges.computeIfAbsent(uuid, k -> new ArrayList<>());
        if (!activeBadges.get(uuid).contains(formattedIcon)) {
            activeBadges.get(uuid).add(formattedIcon);

            // Save to player_badges table, ensure badge_id is populated from the badge lookup
            try (Connection connection = DatabaseManager.getConnection();
                 PreparedStatement ps = connection.prepareStatement(
                         "INSERT INTO player_badges (uuid, badge_id, active_badge, is_active) SELECT ?, badge_id, ?, TRUE FROM badges WHERE chat_icon = ?")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, formattedIcon);
                ps.setString(3, dbCheckIcon);
                ps.executeUpdate();
            } catch (SQLException e) {
                Bukkit.getLogger().warning(Badges.PREFIX + "Failed to insert badge in player_badges table: " + e.getMessage());
            }
        }

        User user = lp.getUserManager().getUser(uuid);
        if (user != null) {
            user.data().clear(node -> node instanceof PrefixNode);

            String combinedPrefix = String.join(" ", activeBadges.get(uuid));
            PrefixNode newBadge = PrefixNode.builder()
                    .prefix(combinedPrefix + "§r ")
                    .priority(100)
                    .build();

            user.data().add(newBadge);
            lp.getUserManager().saveUser(user);

            player.sendMessage("§aYour badge has been updated to: " + combinedPrefix);
        } else {
            player.sendMessage("§cCould not retrieve your LuckPerms data.");
        }
    }

    public static List<String> getBadges(Player player) {
        UUID uuid = player.getUniqueId();

        if (activeBadges.containsKey(uuid)) {
            return activeBadges.get(uuid);
        }

        List<String> badges = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT active_badge FROM player_badges WHERE uuid = ? AND is_active = TRUE")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String badge = rs.getString("active_badge");
                    if (badge != null) {
                        badges.add(badge);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get badges for " + uuid, e);
        }

        activeBadges.put(uuid, badges);
        return badges;
    }

    public static void clearBadge(Player player) {
        if (!assignDefaultBadge) return;

        UUID uuid = player.getUniqueId();

        try (Connection connection = DatabaseManager.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM player_badges WHERE uuid = ? AND is_active = TRUE")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO player_badges (uuid, badge_id, active_badge, is_active) SELECT ?, badge_id, ?, TRUE FROM badges WHERE chat_icon = ?")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, defaultBadge);
                ps.setString(3, defaultBadge);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning("Failed to assign default badge: " + e.getMessage());
        }

        activeBadges.put(uuid, new ArrayList<>(Collections.singletonList(defaultBadge)));
        DatabaseManager.setPlayerBadge(uuid.toString(), ChatColor.translateAlternateColorCodes('&', defaultBadge));
    }

    public static boolean isDefaultBadge(String badgeIcon) {
        return badgeIcon != null && badgeIcon.equalsIgnoreCase(defaultBadge);
    }
}
