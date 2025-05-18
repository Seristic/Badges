package com.seristic.badges.util.database;

import com.seristic.badges.Badges;
import com.seristic.badges.util.helpers.PluginLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.UUID;

public class DatabaseManager {
    private static HikariDataSource dataSource;

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

            PluginLogger.info(Badges.PREFIX + "Database tables created successfully.");
        } catch (SQLException e) {
            PluginLogger.severe(Badges.PREFIX + "Failed to create database table.");
            PluginLogger.logException(null, e);
        }
    }

    public static void setPlayerBadge(UUID uuid, int badgeId) {
        try (Connection connection = getConnection()) {
            // Deactivate all current badges
            try (PreparedStatement deactivate = connection.prepareStatement(
                    "UPDATE player_badges SET is_active = FALSE WHERE uuid = ?")) {
                deactivate.setString(1, uuid.toString());
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
                    if (rs.next()) {
                        alreadyUnlocked = true;
                    }
                }
            }

            if (alreadyUnlocked) {
                // Activate badge
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE player_badges SET is_active = TRUE WHERE uuid = ? AND badge_id = ?")) {
                    update.setString(1, uuid.toString());
                    update.setInt(2, badgeId);
                    update.executeUpdate();
                }
            } else {
                // Insert new badge
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO player_badges (uuid, badge_id, active_badge, is_active) VALUES (?, ?, '', TRUE)")) {
                    insert.setString(1, uuid.toString());
                    insert.setInt(2, badgeId);
                    insert.executeUpdate();
                }
            }
        } catch (SQLException e) {
            PluginLogger.severe(Badges.PREFIX + "An error occurred while setting the player's badge.");
            PluginLogger.logException("Failed to equip badge id " + badgeId + " for UUID " + uuid.toString(), e);
        }
    }
}