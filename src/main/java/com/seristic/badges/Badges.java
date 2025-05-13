package com.seristic.badges;

import com.seristic.badges.commands.BadgeCommand;
import com.seristic.badges.commands.Handler.CommandHandler;
import com.seristic.badges.gui.BadgeGUI;
import com.seristic.badges.gui.BadgeManager;
import com.seristic.badges.util.database.DatabaseManager;
import com.seristic.badges.util.helpers.MessageUtil;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Badges extends JavaPlugin {

    private BukkitAudiences adventure;
    private BadgeGUI badgeGUI;

    public static final String PREFIX = "§6[§eChat Badges§6]§r ";

    @Override
    public void onEnable() {
        getLogger().info(PREFIX + " has been enabled.");

        MessageUtil.initialize(BukkitAudiences.create(this));

        saveDefaultConfig();



        this.adventure = BukkitAudiences.create(this);
        this.badgeGUI = new BadgeGUI(adventure, badgeGUI, this);

        String host = getConfig().getString("mysql.host");
        int port = getConfig().getInt("mysql.port");
        String database = getConfig().getString("mysql.database");
        String username = getConfig().getString("mysql.username");
        String password = getConfig().getString("mysql.password");

        DatabaseManager.connect(host, port, database, username, password);

        BadgeManager.loadConfig(getConfig());

        // Setup LuckPerms after tick
        Bukkit.getScheduler().runTaskLater(this, () -> {
            BadgeManager.setupLuckPerms();
            if (BadgeManager.getLuckPerms() == null) {
                getLogger().warning("LuckPerms was not found! Disabling badge functionality.");
            }
        }, 1L);

        getServer().getPluginManager().registerEvents(new BadgeGUI(adventure, badgeGUI, this), this);
        CommandHandler commandHandler = new CommandHandler(this, adventure, badgeGUI);
        commandHandler.registerCommands(this);

    }

    @Override
    public void onDisable() {
        if (this.adventure != null) {
            this.adventure.close();
        }
        DatabaseManager.close();
    }
}
