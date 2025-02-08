package com.seristic.chatBadges;

import com.seristic.chatBadges.commands.BadgeCommand;
import com.seristic.chatBadges.util.database.DatabaseManager;
import com.seristic.chatBadges.util.gui.BadgeGUI;
import com.seristic.chatBadges.util.listener.BadgeManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChatBadges extends JavaPlugin {
    public static final String Prefix = "§6[§eChat Badages§6]§r ";

    @Override
    public void onEnable() {
        getLogger().info("Chat Badges has been enabled!");
        saveDefaultConfig(); //Init Config

        Bukkit.getScheduler().runTaskLater(this, () -> {
            BadgeManager.setupLuckPerms();
            if (BadgeManager.getLuckPerms() == null) {
                getLogger().warning("LuckPerms was not found! Disabling badge functionality.");
            }
        }, 1L);

        // Load database config options
        String host = getConfig().getString("database.host");
        int port = getConfig().getInt("database.port");
        String database = getConfig().getString("database.name");
        String user = getConfig().getString("database.user");
        String password = getConfig().getString("database.password");

        DatabaseManager.connect(host, port, database, user, password);

        this.getCommand("badge").setExecutor(new BadgeCommand(new BadgeGUI()));

        BadgeGUI badgeGUI = new BadgeGUI();
        getServer().getPluginManager().registerEvents(new BadgeGUI(), this);


        // Create required tables
    }

    @Override
    public void onDisable() {
        getLogger().info("Chat Badges has been disabled!");
    }
}
