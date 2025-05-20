package com.seristic.badges.api;

import com.google.gson.Gson;
import com.seristic.badges.Badges;
import com.seristic.badges.util.ConfigManager;
import com.seristic.badges.util.helpers.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;
import spark.Spark;

import java.util.UUID;

public class BadgeHttpAPI {
    private final BadgeAPI badgeAPI;
    private final Gson gson;
    private final JavaPlugin plugin;

    public BadgeHttpAPI(BadgeAPI badgeAPI, Gson gson, JavaPlugin plugin) {
        this.plugin = plugin;
        this.badgeAPI = badgeAPI;
        this.gson = gson;

        if (ConfigManager.isApiEnabled()) {
            setupEndpoints();
        }
    }

    private void setupEndpoints() {
        // Get configuration values using ConfigManager
        int port = ConfigManager.getApiPort();
        boolean debug = ConfigManager.isApiDebug();
        String basePath = ConfigManager.getApiBasePath();
        String apiKey = ConfigManager.getApiKey();

        // Configure Spark
        Spark.port(port);

        // Set up response type and authentication
        Spark.before((request, response) -> {
            response.type("application/json");

            // API Key Validation
            String providedKey = request.headers("X-API-Key");
            if (providedKey == null || providedKey.isEmpty()) {
                providedKey = request.queryParams("apiKey");
            }

            if (apiKey == null || !apiKey.equals(providedKey)) {
                response.status(403);
                response.body(gson.toJson(new ErrorResponse("Invalid API key")));
                Spark.halt(403);
            }

            if (debug) {
                PluginLogger.warning(Badges.PREFIX + "Received request: " + request.pathInfo());
            }
        });

        // Get active badges
        Spark.get(basePath + "/active/:uuid", (request, response) -> {
            try {
                String uuidStr = request.params(":uuid");
                UUID playerUUID = UUID.fromString(uuidStr);

                if (debug) {
                    plugin.getLogger().info("Fetching active badges for player: " + uuidStr);
                }

                return gson.toJson(badgeAPI.getActiveBadges(playerUUID));
            } catch (IllegalArgumentException e) {
                if (debug) {
                    plugin.getLogger().warning("Invalid UUID format: " + e.getMessage());
                }
                response.status(400);
                return gson.toJson(new ErrorResponse("Invalid UUID format"));
            } catch (Exception e) {
                if (debug) {
                    plugin.getLogger().severe("Error processing request: " + e.getMessage());
                }
                response.status(500);
                return gson.toJson(new ErrorResponse("Internal server error: " + e.getMessage()));
            }
        });

        // Get all badges
        Spark.get(basePath + "/all/:uuid", (request, response) -> {
            try {
                String uuidStr = request.params(":uuid");
                UUID playerUUID = UUID.fromString(uuidStr);

                if (debug) {
                    plugin.getLogger().info("Fetching all badges for player: " + uuidStr);
                }

                return gson.toJson(badgeAPI.getAllBadges(playerUUID));
            } catch (IllegalArgumentException e) {
                if (debug) {
                    plugin.getLogger().warning("Invalid UUID format: " + e.getMessage());
                }
                response.status(400);
                return gson.toJson(new ErrorResponse("Invalid UUID format"));
            } catch (Exception e) {
                if (debug) {
                    plugin.getLogger().severe("Error processing request: " + e.getMessage());
                }
                response.status(500);
                return gson.toJson(new ErrorResponse("Internal server error: " + e.getMessage()));
            }
        });

        // 404 handler
        Spark.notFound((request, response) -> {
            if (debug) {
                plugin.getLogger().warning("404 - Not found: " + request.pathInfo());
            }
            response.status(404);
            return gson.toJson(new ErrorResponse("Endpoint not found"));
        });

        if (debug) {
            plugin.getLogger().info("Badge API started on port " + port);
        }
    }

    private static class ErrorResponse {
        private final String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}
