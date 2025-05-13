package com.seristic.badges.util.database;

import com.seristic.badges.Badges;
import com.seristic.badges.util.helpers.PluginLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

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
            throw new SQLException("DataSource is not inititalised");
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
        try(Statement statement = getConnection().createStatement()) {
            String createBadgesTable = "CREATE TABLE IF NOT EXISTS badges (" +
                    "badge_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "badge_name VARCHAR(255) NOT NULL UNIQUE, " +
                    "badge_description TEXT, " +
                    "chat_icon TEXT, " +
                    "badge_icon VARCHAR(255), " +
                    "badge_color VARCHAR(32), " +  // <- Add this line
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");";

            String createPlayerBadgesTable = "CREATE TABLE IF NOT EXISTS player_badges (" +
                    "uuid CHAR(36) NOT NULL, " +
                    "badge_id INT NOT NULL, " +
                    "unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "PRIMARY KEY (uuid, badge_id), " +
                    "FOREIGN KEY (badge_id) REFERENCES badges(badge_id) ON DELETE CASCADE" +
                    ");";

            String createActiveBadgesTable = "CREATE TABLE IF NOT EXISTS active_badges (" +
                    "uuid CHAR(36) PRIMARY KEY, " +
                    "active_badge_id INT, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (active_badge_id) REFERENCES badges(badge_id) ON DELETE SET NULL" +
                    ");";

            statement.executeUpdate(createBadgesTable);
            statement.executeUpdate(createActiveBadgesTable);
            statement.executeUpdate(createPlayerBadgesTable);

            PluginLogger.getLogger().info(Badges.PREFIX + "Database tables created successfully.");
        } catch (SQLException e) {
            PluginLogger.getLogger().severe(Badges.PREFIX + "Failed to create database table.");
            e.printStackTrace();
        }
    }
}