package com.seristic.badges.commands.Handler;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public abstract class BadgeSubCommand {

    public abstract String getName();

    public List<String> getAliases() {
        return Collections.emptyList(); // default empty list
    }

    public abstract String getPermission();

    public abstract void execute(CommandSender sender, String[] args);

    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission());
    }

    /**
     * Override this method to provide tab completion for the command.
     * @param sender the command sender
     * @param args current arguments
     * @return list of possible completions, or empty list for none
     */
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}