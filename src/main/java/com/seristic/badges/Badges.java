package com.seristic.badges;

import com.google.gson.Gson;
import com.seristic.badges.api.BadgeAPI;
import com.seristic.badges.api.BadgeAPIImpl;
import com.seristic.badges.api.BadgeHttpAPI;
import com.seristic.badges.commands.BadgeCommandTabCompleter;
import com.seristic.badges.commands.Handler.CommandHandler;
import com.seristic.badges.gui.BadgeGUI;
import com.seristic.badges.gui.BadgeManager;
import com.seristic.badges.util.ConfigManager;
import com.seristic.badges.util.database.DatabaseManager;
import com.seristic.badges.util.helpers.MessageUtil;
import com.seristic.badges.util.helpers.PluginLogger;
import com.seristic.badges.util.listeners.BadgeClickListener;
import com.seristic.badges.util.listeners.ChatBadgeListener;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class Badges extends JavaPlugin {

    private BukkitAudiences adventure;
    private BadgeGUI badgeGUI;
    private BadgeAPI badgeAPI;

    public static final Component PREFIX = Component.text()
            .append(Component.text("[", NamedTextColor.GOLD))
            .append(Component.text("Badges", NamedTextColor.YELLOW))
            .append(Component.text("]", NamedTextColor.GOLD))
            .append(Component.space())
            .build();


    @Override
    public void onEnable() {
        PluginLogger.info(PREFIX + " has been enabled.");

        this.adventure = BukkitAudiences.create(this);
        MessageUtil.initialize(this.adventure);

        BadgeCommandTabCompleter tabCompleter = new BadgeCommandTabCompleter();
        badgeAPI = new BadgeAPIImpl();

        ConfigManager.setup(this);

        this.badgeGUI = new BadgeGUI(adventure, null, this);
        CommandHandler commandHandler = new CommandHandler(adventure, badgeGUI);

        // Initialize DatabaseManager using new init method
        DatabaseManager.init(this);

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

        // Start HTTP API
        if (getConfig().getBoolean("api.enabled", true)) {
            new BadgeHttpAPI(badgeAPI, new Gson(), this);
            PluginLogger.info(Badges.PREFIX + "HTTP Badge API initialised.");
        }

        // Event registrations
        getServer().getPluginManager().registerEvents(new ChatBadgeListener(), this);
        getServer().getPluginManager().registerEvents(new BadgeClickListener(), this);
    }

    @Override
    public void onDisable() {
        if (this.adventure != null) {
            this.adventure.close();
        }
        DatabaseManager.close();
        PluginLogger.info(Badges.PREFIX +  "has been disabled.");
    }

    public static Badges getInstance() {
        return JavaPlugin.getPlugin(Badges.class);
    }

    public BadgeAPI getBadgeAPI() {
        return badgeAPI;
    }
}


/**
 * UUID playerUUID = player.getUniqueId();
 *
 * // Get the plugin instance and then the API
 * BadgeAPI badgeAPI = Badges.getInstance().getBadgeAPI();
 *
 * Collection<Badge> activeBadges = badgeAPI.getActiveBadges(playerUUID);
 * Collection<Badge> allBadges = badgeAPI.getAllBadges(playerUUID);
 *
 * This is how I can use the api elsewhere in the code
 */