package com.seristic.badges.util;

import com.seristic.badges.util.helpers.PluginLogger;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private static JavaPlugin plugin;
    private static FileConfiguration config;

    // MySQL Settings
    private static String host = "localhost";
    private static int port = 3306;
    private static String database = "badges";
    private static String username = "root";
    private static String password = "";

    // Config options with defaults
    private static String defaultBadge = "&7✦";
    private static boolean assignDefaultBadge = false;
    private static boolean enableHoverText = true;

    private static int guiSize = 27;
    private static String guiTitle = "&aBadge Selection";
    private static Material borderMaterial = Material.BLACK_STAINED_GLASS_PANE;
    private static String borderName = "";

    private static int nextPageSlot = 26;
    private static Material nextPageMaterial = Material.ARROW;
    private static String nextPageName = "&aNext Page";

    private static int prevPageSlot = 18;
    private static Material prevPageMaterial = Material.ARROW;
    private static String prevPageName = "&aPrevious Page";

    private static int badgesPerPage = 7;

    private static Sound applyBadgeSound = Sound.BLOCK_NOTE_BLOCK_BELL;
    private static float applyBadgeSoundVolume = 1.0F;
    private static float applyBadgeSoundPitch = 1.5F;

    private static Map<String, Integer> badgeSlots = new HashMap<>();

    // API Settings
    private static int apiPort = 8080;
    private static boolean apiDebug = false;
    private static boolean apiEnabled = true;
    private static String apiBasePath = "/badges";
    private static String apiKey = "your-secure-key-here";

    // Setup method to initialize and load config
    public static void setup(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        reload();
    }

    // Reload config values
    public static void reload() {
        if (plugin == null) {
            throw new IllegalStateException("ConfigManager has not been setup with a plugin instance!");
        }

        config = plugin.getConfig();

        // MySQL
        ConfigurationSection mysqlSection = config.getConfigurationSection("mysql");
        if (mysqlSection != null) {
            host = mysqlSection.getString("host", host);
            port = mysqlSection.getInt("port", port);
            database = mysqlSection.getString("database", database);
            username = mysqlSection.getString("username", username);
            password = mysqlSection.getString("password", password);
        }

        // Settings
        ConfigurationSection settingsSection = config.getConfigurationSection("settings");
        if (settingsSection != null) {
            assignDefaultBadge = settingsSection.getBoolean("assign-default-badge", assignDefaultBadge);
            defaultBadge = settingsSection.getString("default-badge", defaultBadge);
            enableHoverText = settingsSection.getBoolean("enable-hover-text", enableHoverText);
        }

        // Badge slots
        badgeSlots.clear();
        ConfigurationSection badgeSlotsSection = config.getConfigurationSection("badge_slots");
        if (badgeSlotsSection != null) {
            for (String key : badgeSlotsSection.getKeys(false)) {
                int slots = badgeSlotsSection.getInt(key, 1);
                badgeSlots.put(key.toLowerCase(), slots);
            }
        }

        // API
        ConfigurationSection apiSection = config.getConfigurationSection("api");
        if (apiSection != null) {
            apiPort = apiSection.getInt("port", apiPort);
            apiDebug = apiSection.getBoolean("debug", apiDebug);
            apiEnabled = apiSection.getBoolean("enabled", apiEnabled);
            apiBasePath = apiSection.getString("base-path", apiBasePath);

            ConfigurationSection securitySection = apiSection.getConfigurationSection("security");
            if (securitySection != null) {
                apiKey = securitySection.getString("api-key", apiKey);
            }
        }

        // GUI
        ConfigurationSection guiSection = config.getConfigurationSection("gui");
        if (guiSection != null) {
            guiSize = guiSection.getInt("size", guiSize);
            guiTitle = guiSection.getString("title", guiTitle);

            ConfigurationSection borderSection = guiSection.getConfigurationSection("border");
            if (borderSection != null) {
                borderMaterial = Material.valueOf(borderSection.getString("material", borderMaterial.name()));
                borderName = borderSection.getString("name", borderName);
            }

            ConfigurationSection navNext = guiSection.getConfigurationSection("navigation.next-page");
            if (navNext != null) {
                nextPageSlot = navNext.getInt("slot", nextPageSlot);
                nextPageMaterial = Material.valueOf(navNext.getString("material", nextPageMaterial.name()).toUpperCase());
                nextPageName = navNext.getString("name", nextPageName);
            }

            ConfigurationSection navPrev = guiSection.getConfigurationSection("navigation.previous-page");
            if (navPrev != null) {
                prevPageSlot = navPrev.getInt("slot", prevPageSlot);
                prevPageMaterial = Material.valueOf(navPrev.getString("material", prevPageMaterial.name()).toUpperCase());
                prevPageName = navPrev.getString("name", prevPageName);
            }

            badgesPerPage = guiSection.getInt("badges-per-page", badgesPerPage);
        }

        // Sounds
        ConfigurationSection soundsSection = config.getConfigurationSection("sounds.apply-badge");
        if (soundsSection != null) {
            applyBadgeSound = Sound.valueOf(soundsSection.getString("sound", applyBadgeSound.name()).toUpperCase());
            applyBadgeSoundVolume = (float) soundsSection.getDouble("volume", applyBadgeSoundVolume);
            applyBadgeSoundPitch = (float) soundsSection.getDouble("pitch", applyBadgeSoundPitch);
        }
    }

    // Plugin accessor
    public static JavaPlugin getPlugin() {
        return plugin;
    }

    // Getter methods
    public static String getHost() {
        return host;
    }

    public static int getPort() {
        return port;
    }

    public static String getDatabase() {
        return database;
    }

    public static String getUsername() {
        return username;
    }

    public static String getPassword() {
        return password;
    }

    public static boolean shouldAssignDefaultBadge() {
        return assignDefaultBadge;
    }

    public static String getDefaultBadge() {
        return defaultBadge;
    }

    public static boolean shouldEnableHoverText() {
        return enableHoverText;
    }

    public static int getGuiSize() {
        return guiSize;
    }

    public static String getGuiTitle() {
        return guiTitle;
    }

    public static Material getBorderMaterial() {
        return borderMaterial;
    }

    public static String getBorderName() {
        return borderName;
    }

    public static int getNextPageSlot() {
        return nextPageSlot;
    }

    public static Material getNextPageMaterial() {
        return nextPageMaterial;
    }

    public static String getNextPageName() {
        return nextPageName;
    }

    public static int getPrevPageSlot() {
        return prevPageSlot;
    }

    public static Material getPrevPageMaterial() {
        return prevPageMaterial;
    }

    public static String getPrevPageName() {
        return prevPageName;
    }

    public static int getBadgesPerPage() {
        return badgesPerPage;
    }

    public static Sound getApplyBadgeSound() {
        return applyBadgeSound;
    }

    public static float getApplyBadgeSoundVolume() {
        return applyBadgeSoundVolume;
    }

    public static float getApplyBadgeSoundPitch() {
        return applyBadgeSoundPitch;
    }

    public static int getApiPort() {
        return apiPort;
    }

    public static boolean isApiDebug() {
        return apiDebug;
    }

    public static boolean isApiEnabled() {
        return apiEnabled;
    }

    public static String getApiBasePath() {
        return apiBasePath;
    }

    public static String getApiKey() {
        return apiKey;
    }

    public static int getSlotsPerRank(String rankOrPermission) {
        if (rankOrPermission == null) return 1;
        return badgeSlots.getOrDefault(rankOrPermission.toLowerCase(), 1);
    }

    public static int getSlotCountForPlayer(Player player) {
        int highestFromPermission = 1;

        for (PermissionAttachmentInfo permInfo : player.getEffectivePermissions()) {
            String perm = permInfo.getPermission().toLowerCase();
            if (perm.startsWith("badges.slots.")) {
                try {
                    int value = Integer.parseInt(perm.substring("badges.slots.".length()));
                    if (value > highestFromPermission) {
                        highestFromPermission = value;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        int highestFromConfig = 1;
        for (Map.Entry<String, Integer> entry : badgeSlots.entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                highestFromConfig = Math.max(highestFromConfig, entry.getValue());
            }
        }

        return Math.max(highestFromPermission, highestFromConfig);
    }
}
