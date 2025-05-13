package com.seristic.badges.commands.Handler;

import com.seristic.badges.commands.BadgeCommand;
import com.seristic.badges.commands.BadgeDeleteCommand;
import com.seristic.badges.gui.BadgeGUI;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandHandler {

    private final BadgeGUI badgeGUI;
    private final BukkitAudiences adventure;
    private final JavaPlugin plugin;


    public CommandHandler(JavaPlugin plugin, BukkitAudiences adventure, BadgeGUI badgeGUI) {
        this.badgeGUI = badgeGUI;
        this.adventure = adventure;
        this.plugin = plugin;
    }

    public void registerCommands(JavaPlugin plugin) {
        registerBadgeCommand(plugin);
    }

    private void registerBadgeCommand(JavaPlugin plugin) {
        BadgeCommand badgeCommand = new BadgeCommand(adventure, badgeGUI);
        plugin.getCommand("badge").setExecutor(badgeCommand);
    }
}
