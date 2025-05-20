package com.seristic.badges.gui;

import com.seristic.badges.Badges;
import com.seristic.badges.util.Badge;
import com.seristic.badges.util.ColourUtil;
import com.seristic.badges.util.database.DatabaseManager;
import com.seristic.badges.util.helpers.BadgeHelper;
import com.seristic.badges.util.helpers.MessageUtil;
import com.seristic.badges.util.helpers.PluginLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class BadgeManager {
    private static final Map<UUID, List<String>> activeBadges = new HashMap<>();
    private static final Map<String, Badge> badges = new HashMap<>();

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

//    public static void registerBadge(Badge badge) {
//        badges.put(badge.getId(), badge);
//    }

    public static void setBadge(Player player, String badgeName) {
        Bukkit.getLogger().info("[Badges] setBadge called with badgeName='" + badgeName + "'");
        try (Connection connection = DatabaseManager.getConnection()) {
            if (connection == null || connection.isClosed()) {
                MessageUtil.send(player, Component.text("Database connection failed.", NamedTextColor.RED));
                return;
            }

            String badgeQuery = "SELECT badge_id, badge_name, chat_icon, badge_color FROM badges WHERE LOWER(TRIM(badge_name)) = LOWER(TRIM(?))";
            int badgeId;
            String chatIcon;
            String realName;
            NamedTextColor color;


            try (PreparedStatement badgeCheck = connection.prepareStatement(badgeQuery)) {
                badgeCheck.setString(1, badgeName.trim().toLowerCase(Locale.ROOT));
                try (ResultSet rs = badgeCheck.executeQuery()) {
                    if (!rs.next()) {
                        PluginLogger.severe("Badge not found for input: [" + badgeName + "]");
                        MessageUtil.send(player, Component.text("No such badge exists: [ " + badgeName + " ]", NamedTextColor.RED));
                        return;
                    }
                    badgeId = rs.getInt("badge_id");
                    chatIcon = rs.getString("chat_icon");
                    realName = rs.getString("badge_name");
                    String colorStr = rs.getString("badge_color");
                    color = ColourUtil.getNamedTextColor(colorStr);
                    if (color == null) color = NamedTextColor.WHITE;
                    Bukkit.getLogger().info("Found badge: ID=" + badgeId + ", Name=" + realName + ", Icon=" + chatIcon + ", Color=" + colorStr);
                }
            }

            Badge badge = new Badge(
                    String.valueOf(badgeId),
                    realName,
                    chatIcon,
                    color
            );

            badgeId = Integer.parseInt(badge.getId());


            String checkQuery = "SELECT 1 FROM player_badges WHERE uuid = ? AND badge_id = ?";
            try (PreparedStatement checkPs = connection.prepareStatement(checkQuery)) {
                checkPs.setString(1, player.getUniqueId().toString());
                checkPs.setInt(2, badgeId);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        // Player already has this badge equipped
                        MessageUtil.send(player, BadgeHelper.formatBadgeMessage("Badge already equipped: ", badge));
                        return;
                    }
                }
            }
            DatabaseManager.setPlayerBadge(player.getUniqueId(), badgeId, 0);


            MessageUtil.send(player, BadgeHelper.formatBadgeMessage("Equipped badge: ", badge));

        } catch (SQLException e) {
            PluginLogger.logException("Error equipping badge for " + player.getName(), e);
            MessageUtil.send(player, Component.text(Badges.PREFIX + "An error occurred while equipping the badge.", NamedTextColor.RED));
        }
    }


    public static List<Badge> getBadges(Player player) {
        List<Badge> badges = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection()) {
            if (connection == null) return badges;

            String sql = """
            SELECT b.badge_id, b.badge_name, b.chat_icon, b.badge_color
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

                        NamedTextColor color = ColourUtil.getNamedTextColor(colorStr);
                        if (color == null) color = NamedTextColor.WHITE;

                        badges.add(new Badge(id, name, chatIcon, color));
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
        DatabaseManager.setPlayerBadge(uuid, -1, 0); // Use -1 or a special value to indicate default in your DB logic
    }

    public static void removeBadge(Player player, String badgeName) {
        try (Connection connection = DatabaseManager.getConnection()) {
            if (connection == null || connection.isClosed()) {
                MessageUtil.send(player, Component.text("Database connection failed.", NamedTextColor.RED));
                return;
            }

            String lookupQuery = "SELECT badge_id FROM badges WHERE badge_name = ?";
            int badgeId;

            try (PreparedStatement lookup = connection.prepareStatement(lookupQuery)) {
                lookup.setString(1, badgeName.trim());
                try (ResultSet rs = lookup.executeQuery()) {
                    if (!rs.next()) {
                        // If badge not found at all
                        MessageUtil.send(player, Component.text("No such badge exists: " + badgeName, NamedTextColor.RED));
                        return;
                    }
                    badgeId = rs.getInt("badge_id");
                }
            }

            // Check if the player has this badge equipped
            String checkQuery = "SELECT badge_id FROM player_badges WHERE uuid = ? AND badge_id = ?";
            try (PreparedStatement checkPs = connection.prepareStatement(checkQuery)) {
                checkPs.setString(1, player.getUniqueId().toString());
                checkPs.setInt(2, badgeId);

                try (ResultSet rs = checkPs.executeQuery()) {
                    if (!rs.next()) {
                        MessageUtil.send(player, Component.text("You don’t have that badge equipped.", NamedTextColor.RED));
                        return;
                    }
                }
            }

            // Now delete the badge
            String removeQuery = "DELETE FROM player_badges WHERE uuid = ? AND badge_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(removeQuery)) {
                ps.setString(1, player.getUniqueId().toString());
                ps.setInt(2, badgeId);

                int affectedRows = ps.executeUpdate();
                if (affectedRows > 0) {
                    Badge badge = BadgeHelper.getBadgeByName(badgeName);
                    if (badge != null) {
                        MessageUtil.send(player, BadgeHelper.formatBadgeMessage("Unequipped badge: ", badge));
                    } else {
                        MessageUtil.send(player, Component.text("Badge unequipped, but failed to load display info."));
                    }
                }
            }

        } catch (SQLException e) {
            PluginLogger.logException("Error unequipping badge for " + player.getName(), e);
            MessageUtil.send(player, Component.text("An error occurred while removing the badge.", NamedTextColor.RED));
        }
    }

    public static boolean isDefaultBadge(String badgeIcon) {
        return badgeIcon != null && badgeIcon.equalsIgnoreCase(defaultBadge);
    }
}
