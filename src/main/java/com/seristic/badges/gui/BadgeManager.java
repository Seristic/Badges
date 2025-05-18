package com.seristic.badges.gui;

import com.seristic.badges.Badges;
import com.seristic.badges.util.Badge;
import com.seristic.badges.util.ColourUtil;
import com.seristic.badges.util.database.DatabaseManager;
import com.seristic.badges.util.helpers.MessageUtil;
import com.seristic.badges.util.helpers.PluginLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
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
import java.util.concurrent.CompletableFuture;

public class BadgeManager {
    private static final Map<UUID, List<String>> activeBadges = new HashMap<>();
    private static LuckPerms lp = null;

    private static String defaultBadge;
    private static boolean assignDefaultBadge;
    private static boolean enableHoverText;

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
        enableHoverText = config.getBoolean("enable-hover-text", true);
    }

    public static LuckPerms getLuckPerms() {
        return lp;
    }

    public static void setBadge(Player player, String badgeName) {
        try (Connection connection = DatabaseManager.getConnection()) {
            if (connection == null || connection.isClosed()) {
                MessageUtil.send(player, Component.text("Database connection failed.", NamedTextColor.RED));
                return;
            }

            String badgeQuery = "SELECT badge_id, chat_icon FROM badges WHERE badge_name = ?";
            try (PreparedStatement badgeCheck = connection.prepareStatement(badgeQuery)) {
                badgeCheck.setString(1, badgeName);
                try (ResultSet rs = badgeCheck.executeQuery()) {
                    if (!rs.next()) {
                        MessageUtil.send(player, Component.text("Badge not found in the database: " + badgeName, NamedTextColor.RED));
                        return;
                    }

                    int badgeId = rs.getInt("badge_id");
                    String chatIcon = rs.getString("chat_icon");

                    // ✅ Use centralized logic
                    DatabaseManager.setPlayerBadge(player.getUniqueId(), badgeId);

                    MessageUtil.send(player, Component.text("Equipped badge: ").append(Component.text(chatIcon, NamedTextColor.GOLD)));
                }
            }
        } catch (SQLException e) {
            PluginLogger.logException("Error equipping badge for " + player.getName(), e);
            MessageUtil.send(player, Component.text("An error occurred while equipping badge.", NamedTextColor.RED));
        }
    }

    public static List<Badge> getBadges(Player player) {
        List<Badge> badges = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection()) {
            if (connection == null) return badges;

            String sql = """
            SELECT b.badge_id, b.badge_name, b.chat_icon, b.badge_color, b.badge_description
            FROM player_badges pb
            JOIN badges b ON pb.badge_id = b.badge_id
            WHERE pb.uuid = ? AND pb.is_active = TRUE
        """;

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, player.getUniqueId().toString());

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String id = rs.getString("badge_id");
                        String name = rs.getString("badge_name");
                        String chatIcon = rs.getString("chat_icon");
                        String colorStr = rs.getString("badge_color");
                        String description = rs.getString("badge_description");

                        NamedTextColor color = ColourUtil.getNamedTextColor(colorStr);
                        if (color == null) color = NamedTextColor.WHITE;

                        badges.add(new Badge(id, name, chatIcon, color, description));
                    }
                }
            }
        } catch (SQLException e) {
            PluginLogger.severe("Failed to load active badges: " + e.getMessage());
        }

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
        DatabaseManager.setPlayerBadge(uuid, -1); // Use -1 or a special value to indicate default in your DB logic
    }

    public static void removeBadge(Player player, String badgeName) {
        try (Connection connection = DatabaseManager.getConnection()) {
            if (connection == null || connection.isClosed()) {
                MessageUtil.send(player, Component.text("Database connection failed.", NamedTextColor.RED));
                return;
            }

            String lookupQuery = "SELECT badge_id FROM badges WHERE badge_name = ?";
            int badgeId = -1;

            try (PreparedStatement lookup = connection.prepareStatement(lookupQuery)) {
                lookup.setString(1, badgeName);
                try (ResultSet rs = lookup.executeQuery()) {
                    if (rs.next()) {
                        badgeId = rs.getInt("badge_id");
                    } else {
                        MessageUtil.send(player, Component.text("Badge not found in the database: " + badgeName, NamedTextColor.RED));
                        return;
                    }
                }
            }

            String check = "SELECT badge_id FROM player_badges WHERE uuid = ? AND badge_id = ?";
            try (PreparedStatement checkPs = connection.prepareStatement(check)) {
                checkPs.setString(1, player.getUniqueId().toString());
                checkPs.setInt(2, badgeId);

                try (ResultSet rs = checkPs.executeQuery()) {
                    if (!rs.next()) {
                        MessageUtil.send(player, Component.text("You don't have that badge equipped.", NamedTextColor.RED));
                        return;
                    }
                }
            }

            String removeQuery = "DELETE FROM player_badges WHERE uuid = ? AND badge_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(removeQuery)) {
                ps.setString(1, player.getUniqueId().toString());
                ps.setInt(2, badgeId);
                int affected = ps.executeUpdate();

                if (affected > 0) {
                    MessageUtil.send(player, Component.text("Badge unequipped successfully.", NamedTextColor.GREEN));
                } else {
                    MessageUtil.send(player, Component.text("Failed to unequip badge.", NamedTextColor.RED));
                }
            }
        } catch (SQLException e) {
            PluginLogger.logException("Error unequipping badge for " + player.getName(), e);
            MessageUtil.send(player, Component.text("An error occurred while removing badge.", NamedTextColor.RED));
        }
    }

    public void setBadgePrefix(UUID uuid, String prefix, int priority) {
        String plainPrefix = ChatColor.stripColor(prefix);
        if (plainPrefix.length() > 16) {
            PluginLogger.info(Badges.PREFIX + "Prefix too long: " + plainPrefix);
            return;
        }

        CompletableFuture<User> userFuture = lp.getUserManager().loadUser(uuid);

        userFuture.thenAcceptAsync(user -> {
            if (user == null) {
                PluginLogger.info(Badges.PREFIX + "User not found for UUID: " + uuid);
                return;
            }

            user.data().clear(node -> node.getType() == NodeType.PREFIX);

            PrefixNode prefixNode = PrefixNode.builder()
                    .prefix(prefix)
                    .priority(priority)
                    .build();

            user.data().add(prefixNode);

            lp.getUserManager().saveUser(user);

            PluginLogger.info(Badges.PREFIX + "Prefix set to: " + prefix + " for user: " + plainPrefix);
        });
    }

    public static boolean isDefaultBadge(String badgeIcon) {
        return badgeIcon != null && badgeIcon.equalsIgnoreCase(defaultBadge);
    }
}
