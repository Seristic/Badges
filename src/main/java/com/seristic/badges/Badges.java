package com.seristic.badges;

import com.seristic.badges.commands.BadgeCommandTabCompleter;
import com.seristic.badges.commands.Handler.CommandHandler;
import com.seristic.badges.gui.BadgeGUI;
import com.seristic.badges.gui.BadgeManager;
import com.seristic.badges.util.database.DatabaseManager;
import com.seristic.badges.util.helpers.MessageUtil;
import com.seristic.badges.util.helpers.PluginLogger;
import com.seristic.badges.util.listeners.BadgeClickListener;
import com.seristic.badges.util.listeners.ChatBadgeListener;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class Badges extends JavaPlugin {

    private BukkitAudiences adventure;
    private BadgeGUI badgeGUI;

    public static final String PREFIX = "§6[§eBadges§6]§r ";

    @Override
    public void onEnable() {
        getLogger().info(PREFIX + " has been enabled.");

        this.adventure = BukkitAudiences.create(this);
        MessageUtil.initialize(this.adventure);

        BadgeCommandTabCompleter tabCompleter = new BadgeCommandTabCompleter();

        saveDefaultConfig();

        this.adventure = BukkitAudiences.create(this);
        this.badgeGUI = new BadgeGUI(adventure, this.badgeGUI, this);

        CommandHandler commandHandler = new CommandHandler(adventure, badgeGUI);

        String host = getConfig().getString("mysql.host");
        int port = getConfig().getInt("mysql.port");
        String database = getConfig().getString("mysql.database");
        String username = getConfig().getString("mysql.username");
        String password = getConfig().getString("mysql.password");

        DatabaseManager.connect(host, port, database, username, password);

        BadgeManager.loadConfig(getConfig());

        Bukkit.getScheduler().runTaskLater(this, () -> {
            BadgeManager.setupLuckPerms();
            if (BadgeManager.getLuckPerms() == null) {
                PluginLogger.severe("LuckPerms was not found! Disabling badge functionality.");
            }
        }, 1L);

        PluginCommand badgeCommand = getCommand("badge");
        if (badgeCommand != null) {
            badgeCommand.setExecutor(commandHandler);
            badgeCommand.setTabCompleter(tabCompleter);
        } else {
            PluginLogger.warning(PREFIX + "Command 'badge' not found in plugin.yml");
        }

        getServer().getPluginManager().registerEvents(new BadgeGUI(adventure, badgeGUI, this), this);
        getServer().getPluginManager().registerEvents(new ChatBadgeListener(), this);
        getServer().getPluginManager().registerEvents(new BadgeClickListener(), this);

    }

    @Override
    public void onDisable() {
        if (this.adventure != null) {
            this.adventure.close();
        }
        DatabaseManager.close();
    }
    public static Badges getInstance() {
        return JavaPlugin.getPlugin(Badges.class);
    }
}