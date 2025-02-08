package com.seristic.chatBadges.util.database;

import com.seristic.chatBadges.ChatBadges;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static Connection connection;

    public static void connect(String host, int port, String database, String username, String password) {
        try {
            // Ensure MySQL driver is loaded
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Create the database if it doesn't exist
            String url = "jdbc:mysql://" + host + ":" + port + "/?useSSL=false";
            try (Connection tempConnection = DriverManager.getConnection(url, username, password);
                 Statement statement = tempConnection.createStatement()) {
                statement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + database);
                System.out.println(ChatBadges.Prefix + "Database ensured: " + database);
            }

            // Now connect to the actual database
            url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false";
            connection = DriverManager.getConnection(url, username, password);
            System.out.println(ChatBadges.Prefix + "Connected to MySQL database!");

            // Create tables
            createTables();
        } catch (ClassNotFoundException e) {
            System.out.println(ChatBadges.Prefix + "MySQL driver not found!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println(ChatBadges.Prefix + "Could not connect to MySQL database!");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() {
        return connection;
    }

    private static void createTables() {
        if (connection == null) {
            System.out.println(ChatBadges.Prefix + "No database connection available!");
            return;
        }
        try (Statement statement = connection.createStatement()) {
            String createBadgesTable = "CREATE TABLE IF NOT EXISTS badges ("
                    + "badge_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "badge_name VARCHAR(255) NOT NULL UNIQUE, "
                    + "badge_description TEXT, "
                    + "chat_icon TEXT, "
                    + "badge_icon VARCHAR(255), "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ");";

            String createPlayerBadgesTable = "CREATE TABLE IF NOT EXISTS player_badges ("
                    + "player_uuid CHAR(36) NOT NULL, "
                    + "player_name VARCHAR(255) NOT NULL, "
                    + "badge_id INT NOT NULL, "
                    + "granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "PRIMARY KEY (player_uuid, badge_id), "
                    + "FOREIGN KEY (badge_id) REFERENCES badges(badge_id) ON DELETE CASCADE"
                    + ");";

            statement.executeUpdate(createBadgesTable);
            statement.executeUpdate(createPlayerBadgesTable);

            System.out.println(ChatBadges.Prefix + "Database tables created successfully.");
        } catch (SQLException e) {
            System.out.println(ChatBadges.Prefix + "Failed to create database tables.");
            e.printStackTrace();
        }
    }
}