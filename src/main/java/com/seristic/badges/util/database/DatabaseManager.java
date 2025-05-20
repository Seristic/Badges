package com.seristic.badges.util.database;

import com.seristic.badges.Badges;
import com.seristic.badges.util.Badge;
import com.seristic.badges.util.ColourUtil;
import com.seristic.badges.util.ConfigManager;
import com.seristic.badges.util.helpers.PluginLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.kyori.adventure.text.format.NamedTextColor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {
    private static HikariDataSource dataSource;
    private static Badges plugin;

    public static void init(Badges pluginInstance) {
        plugin = pluginInstance;
        connect(
                ConfigManager.getHost(),
                ConfigManager.getPort(),
                ConfigManager.getDatabase(),
                ConfigManager.getUsername(),
                ConfigManager.getPassword()
        );
    }

    public static void connect(String host, int port, String database, String username, String password) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            String initUrl = "jdbc:mysql://" + host + ":" + port + "/?useSSL=false";
            try (Connection tempConnection = DriverManager.getConnection(initUrl, username, password)) {
                Statement statement = tempConnection.createStatement();
                statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + database + "`;");
                PluginLogger.info(Badges.PREFIX + "Database ensured: " + database);
            }

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC");
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(10);
            config.setIdleTimeout(60000);
            config.setConnectionTimeout(30000);
            config.setMaxLifetime(1800000);

            dataSource = new HikariDataSource(config);
            PluginLogger.info(Badges.PREFIX + "Connected to the MySQL using HikariCP.");

            createTable();

        } catch (ClassNotFoundException e) {
            PluginLogger.severe(Badges.PREFIX + "MySQL driver not found.");
            PluginLogger.logException(null, e);
        } catch (SQLException e) {
            PluginLogger.severe(Badges.PREFIX + "Failed to connect MySQL");
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource is not initialized");
        }
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            PluginLogger.warning(Badges.PREFIX + "Database connection pool closed");
        }
    }

    private static void createTable() throws SQLException {
        if (dataSource == null) {
            PluginLogger.warning(Badges.PREFIX + "No database connection available.");
            return;
        }

        try (Statement statement = getConnection().createStatement()) {
            String createBadgesTable = """
                        CREATE TABLE IF NOT EXISTS badges (
                            badge_id INT AUTO_INCREMENT PRIMARY KEY,
                            badge_name VARCHAR(255) NOT NULL UNIQUE,
                            chat_icon TEXT,
                            badge_color VARCHAR(32),
                            badge_hover TEXT,
                            description TEXT,
                            requires_permission BOOLEAN DEFAULT TRUE,
                            is_hidden BOOLEAN DEFAULT TRUE,
                            badge_group VARCHAR(128),
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        );
                    """;
            String createPlayerBadgesTable = """
                        CREATE TABLE IF NOT EXISTS player_badges (
                            uuid CHAR(36) NOT NULL,
                            badge_id INT NOT NULL,
                            slot_index INT DEFAULT 0,
                            active_badge TEXT,
                            is_active BOOLEAN DEFAULT FALSE,
                            unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            equipped_by VARCHAR(255),
                            notes TEXT,
                            PRIMARY KEY (uuid, badge_id),
                            FOREIGN KEY (badge_id) REFERENCES badges(badge_id) ON DELETE CASCADE
                        );
                    """;
            statement.executeUpdate(createBadgesTable);
            statement.executeUpdate(createPlayerBadgesTable);

            PluginLogger.info(Badges.PREFIX + "Database tables created successfully.");
        } catch (SQLException e) {
            PluginLogger.severe(Badges.PREFIX + "Failed to create database table.");
            PluginLogger.logException(null, e);
        }
    }

    /**
     * Sets or equips a badge for a player in a specific slot.
     * If badgeId == -1, unequips any badge in that slot.
     */
    public static void setPlayerBadge(UUID uuid, int badgeId, int slotIndex) {
        try (Connection connection = getConnection()) {
            // Deactivate all current badges
            try (PreparedStatement deactivate = connection.prepareStatement(
                    "UPDATE player_badges SET is_active = FALSE WHERE uuid = ? AND slot_index = ?")) {
                deactivate.setString(1, uuid.toString());
                deactivate.setInt(2, slotIndex);
                deactivate.executeUpdate();
            }

            if (badgeId == -1) {
                // Special case: no badge or default badge, don't insert or update player_badges
                // You could also insert a row with badge_id = NULL or skip entirely
                return;
            }

            // Check if badge already unlocked
            boolean alreadyUnlocked = false;
            try (PreparedStatement check = connection.prepareStatement(
                    "SELECT 1 FROM player_badges WHERE uuid = ? AND badge_id = ?")) {
                check.setString(1, uuid.toString());
                check.setInt(2, badgeId);
                try (ResultSet rs = check.executeQuery()) {
                    alreadyUnlocked = rs.next();
                }
            }

            if (alreadyUnlocked) {
                // Activate badge
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE player_badges SET is_active = TRUE, slot_index = ? WHERE uuid = ? AND badge_id = ?")) {
                    update.setInt(1, slotIndex);
                    update.setString(2, uuid.toString());
                    update.setInt(3, badgeId);
                    update.executeUpdate();
                }
            } else {
                // Insert new badge
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO player_badges (uuid, badge_id, slot_index, active_badge, is_active) VALUES (?, ?, ?, '', TRUE)")) {
                    insert.setString(1, uuid.toString());
                    insert.setInt(2, badgeId);
                    insert.setInt(3, slotIndex);
                    insert.executeUpdate();
                }
            }
        } catch (SQLException e) {
            PluginLogger.severe(Badges.PREFIX + "An error occurred while setting the player's badge.");
            PluginLogger.logException("Failed to equip badge id " + badgeId + " for UUID " + uuid.toString(), e);
        }
    }

    /**
     * Retrieves all badges unlocked by the player.
     * Returns a list of Badge objects.
     */
    public static List<Badge> getAllBadges(UUID uuid) {
        List<Badge> badges = new ArrayList<>();
        String sql = """
                SELECT b.badge_id, b.badge_name, b.chat_icon, b.badge_color
                FROM badges b
                JOIN player_badges pb ON b.badge_id = pb.badge_id
                WHERE pb.uuid = ?
                """;

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = String.valueOf(rs.getInt("badge_id"));
                    String name = rs.getString("badge_name");
                    String chatIcon = rs.getString("chat_icon");
                    String colorStr = rs.getString("badge_color");

                    NamedTextColor color = ColourUtil.getNamedTextColor(colorStr);
                    if (color == null) color = NamedTextColor.WHITE;

                    badges.add(new Badge(id, name, chatIcon, color));
                }
            }

        } catch (SQLException e) {
            PluginLogger.severe(Badges.PREFIX + "Failed to get all badges for UUID " + uuid);
            PluginLogger.logException(null, e);
        }
        return badges;
    }

    /**
     * Retrieves all active badges for the player.
     * Returns a list of Badge objects where is_active = TRUE.
     */
    public static List<Badge> getActiveBadges(UUID uuid) {
        List<Badge> badges = new ArrayList<>();
        String sql = """
                SELECT b.badge_id, b.badge_name, b.chat_icon, b.badge_color
                FROM badges b
                JOIN player_badges pb ON b.badge_id = pb.badge_id
                WHERE pb.uuid = ? AND pb.is_active = TRUE
                """;

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = String.valueOf(rs.getInt("badge_id"));
                    String name = rs.getString("badge_name");
                    String chatIcon = rs.getString("chat_icon");
                    String colorStr = rs.getString("badge_color");

                    NamedTextColor color = ColourUtil.getNamedTextColor(colorStr);
                    if (color == null) color = NamedTextColor.WHITE;

                    badges.add(new Badge(id, name, chatIcon, color));
                }
            }

        } catch (SQLException e) {
            PluginLogger.severe(Badges.PREFIX + "Failed to get active badges for UUID " + uuid);
            PluginLogger.logException(null, e);
        }
        return badges;
    }
    /**
     * Get the active badge equipped in a specific slot.
     * Returns null if none equipped.
     */
    public static Badge getBadgeInSlot(UUID uuid, int slotIndex) {
        String sql = """
            SELECT b.badge_id, b.badge_name, b.chat_icon, b.badge_color
            FROM badges b
            JOIN player_badges pb ON b.badge_id = pb.badge_id
            WHERE pb.uuid = ? AND pb.slot_index = ? AND pb.is_active = TRUE
            LIMIT 1;
            """;
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, slotIndex);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String id = String.valueOf(rs.getInt("badge_id"));
                    String name = rs.getString("badge_name");
                    String chatIcon = rs.getString("chat_icon");
                    String colorStr = rs.getString("badge_color");

                    NamedTextColor color = ColourUtil.getNamedTextColor(colorStr);
                    if (color == null) color = NamedTextColor.WHITE;

                    return new Badge(id, name, chatIcon, color);
                }
            }
        } catch (SQLException e) {
            PluginLogger.severe(Badges.PREFIX + "Failed to get badge in slot " + slotIndex + " for UUID " + uuid);
            PluginLogger.logException(null, e);
        }
        return null;
    }
    /**
     * Checks if a player has unlocked a specific badge.
     */
    public static boolean hasUnlockedBadge(UUID uuid, int badgeId) {
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT 1 FROM player_badges WHERE uuid = ? AND badge_id = ?")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, badgeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            PluginLogger.severe(Badges.PREFIX + "Failed to check badge unlock for UUID " + uuid);
            PluginLogger.logException(null, e);
            return false;
        }
    }
}