package com.seristic.badges.util.database;

import com.seristic.badges.Badges;
import com.seristic.badges.util.helpers.PluginLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;

// Package Imports only
//import com.seristic.shaded.hikari.HikariConfig;
//import com.seristic.shaded.hikari.HikariDataSource;

import java.sql.*;

public class DatabaseManager {
    private static HikariDataSource dataSource;

    public static void connect(String host, int port, String database, String username, String password) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            String initUrl = "jdbc:mysql://" + host + ":" + port + "/?useSSL=false";
            try (Connection tempConnection = DriverManager.getConnection(initUrl, username, password)) {
                Statement statement = tempConnection.createStatement();
                statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + database + "`;");
                PluginLogger.getLogger().info(Badges.PREFIX + "Database ensured: " + database);
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
            PluginLogger.getLogger().info(Badges.PREFIX + "Connected to the MySQL using HikariCP.");

            createTable();

        } catch (ClassNotFoundException e) {
            PluginLogger.getLogger().severe(Badges.PREFIX + "MySQL driver not found.");
            e.printStackTrace();
        } catch (SQLException e) {
            PluginLogger.getLogger().severe(Badges.PREFIX + "Failed to connect MySQL");
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
            PluginLogger.getLogger().warning(Badges.PREFIX + "Database connection pool closed");
        }
    }

    private static void createTable() throws SQLException {
        if (dataSource == null) {
            PluginLogger.getLogger().warning(Badges.PREFIX + "No database connection available.");
            return;
        }

        try (Statement statement = getConnection().createStatement()) {
            String createBadgesTable = "CREATE TABLE IF NOT EXISTS badges (" +
                    "badge_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "badge_name VARCHAR(255) NOT NULL UNIQUE, " +
                    "badge_description TEXT, " +
                    "chat_icon TEXT, " +
                    "badge_icon VARCHAR(255), " +
                    "badge_color VARCHAR(32), " +
                    "description VARCHAR(255)," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");";

            String createPlayerBadgesTable = "CREATE TABLE IF NOT EXISTS player_badges (" +
                    "uuid CHAR(36) NOT NULL, " +
                    "badge_id INT NOT NULL, " +
                    "active_badge TEXT, " +
                    "is_active BOOLEAN DEFAULT FALSE, " +
                    "unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "PRIMARY KEY (uuid, badge_id), " +
                    "FOREIGN KEY (badge_id) REFERENCES badges(badge_id) ON DELETE CASCADE" +
                    ");";

            statement.executeUpdate(createBadgesTable);
            statement.executeUpdate(createPlayerBadgesTable);

            PluginLogger.getLogger().info(Badges.PREFIX + "Database tables created successfully.");
        } catch (SQLException e) {
            PluginLogger.getLogger().severe(Badges.PREFIX + "Failed to create database table.");
            e.printStackTrace();
        }
    }

    public static void setPlayerBadge(String uuid, String chatIcon) {
        try (Connection connection = getConnection()) {
            int badgeId = -1;

            try (PreparedStatement findBadge = connection.prepareStatement(
                    "SELECT badge_id FROM badges WHERE chat_icon = ?")) {
                findBadge.setString(1, chatIcon);
                try (ResultSet rs = findBadge.executeQuery()) {
                    if (rs.next()) {
                        badgeId = rs.getInt("badge_id");
                    }
                }
            }

            if (badgeId == -1) {
                PluginLogger.getLogger().warning(Badges.PREFIX + "Badge not found in the database for chat_icon: " + chatIcon);
                return;
            }

            try (PreparedStatement deactivate = connection.prepareStatement(
                    "UPDATE player_badges SET is_active = FALSE WHERE uuid = ?")) {
                deactivate.setString(1, uuid);
                deactivate.executeUpdate();
            }

            boolean alreadyUnlocked = false;
            try (PreparedStatement check = connection.prepareStatement(
                    "SELECT * FROM player_badges WHERE uuid = ? AND badge_id = ?")) {
                check.setString(1, uuid);
                check.setInt(2, badgeId);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        alreadyUnlocked = true;
                    }
                }
            }

            if (alreadyUnlocked) {
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE player_badges SET is_active = TRUE, active_badge = ? WHERE uuid = ? AND badge_id = ?")) {
                    update.setString(1, chatIcon);
                    update.setString(2, uuid);
                    update.setInt(3, badgeId);
                    update.executeUpdate();
                }
            } else {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO player_badges (uuid, badge_id, active_badge, is_active) VALUES (?, ?, ?, TRUE")) {
                    insert.setString(1, uuid);
                    insert.setInt(2, badgeId);
                    insert.setString(3, chatIcon);
                    insert.executeUpdate();
                }
            }
        } catch (SQLException e) {
            PluginLogger.getLogger().severe(Badges.PREFIX + "An error occurred while setting the player's badge.");
            e.printStackTrace();
        }
    }

    public static boolean badgeExists(String chatIcon) {
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT 1 FROM badges WHERE chat_icon = ? LIMIT 1")) {
            ps.setString(1, chatIcon);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // true if found
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning("Failed to check badge existence: " + e.getMessage());
            return false;
        }
    }
}