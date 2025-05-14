package com.seristic.badges.commands.Handler;

import org.bukkit.command.CommandSender;

import java.util.List;

public abstract class BadgeSubCommand {
    public abstract String getName();
    public abstract List<String> getAliases();
    public abstract String getPermission();
    public abstract void execute(CommandSender sender, String[] args);

    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission());
    }
}