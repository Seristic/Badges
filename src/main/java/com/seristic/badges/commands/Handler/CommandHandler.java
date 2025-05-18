package com.seristic.badges.commands.Handler;

import com.seristic.badges.commands.BadgeCreateCommand;
import com.seristic.badges.commands.BadgeDeleteCommand;
import com.seristic.badges.commands.BadgeGUICommand;
import com.seristic.badges.gui.BadgeGUI;
import com.seristic.badges.util.helpers.MessageUtil;
import com.seristic.badges.util.helpers.PluginLogger;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CommandHandler implements CommandExecutor, TabCompleter {

    private final Map<String, BadgeSubCommand> subCommands = new HashMap<>();
    private final BukkitAudiences adventure;
    private final BadgeGUI badgeGUI;

    public CommandHandler(BukkitAudiences adventure, BadgeGUI badgeGUI) {
        this.adventure = adventure;
        this.badgeGUI = badgeGUI;

        // Register all subcommands here
        register(new BadgeGUICommand(badgeGUI));         // This becomes the default GUI command
        register(new BadgeCreateCommand(badgeGUI));      // This handles creation
        register(new BadgeDeleteCommand(badgeGUI));      // This handles deletion
    }

    public void register(BadgeSubCommand cmd) {
        subCommands.put(cmd.getName().toLowerCase(), cmd);
        for (String alias : cmd.getAliases()) {
            subCommands.put(alias.toLowerCase(), cmd);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            // Try to find the default subcommand (one with empty string name)
            BadgeSubCommand defaultCmd = subCommands.get("");
            if (defaultCmd != null) {
                if (!defaultCmd.hasPermission(sender)) {
                    MessageUtil.send(sender, Component.text("You do not have permission to use this command.", NamedTextColor.RED));
                    return true;
                }

                try {
                    defaultCmd.execute(sender, new String[0]);
                } catch (Exception e) {
                    MessageUtil.send(sender, Component.text("An error occurred while executing the command.", NamedTextColor.RED));
                    PluginLogger.logException(null, e);
                }

                return true;
            }

            MessageUtil.send(sender, Component.text("No default command is set for /" + label, NamedTextColor.RED));
            return true;
        }

        BadgeSubCommand sub = subCommands.get(args[0].toLowerCase());
        if (sub == null) {
            MessageUtil.send(sender, Component.text("Unknown subcommand. Use '/" + label + " help' for a list of subcommands.", NamedTextColor.RED));
            return true;
        }

        if (!sub.hasPermission(sender)) {
            MessageUtil.send(sender, Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        try {
            sub.execute(sender, Arrays.copyOfRange(args, 1, args.length));
        } catch (Exception e) {
            MessageUtil.send(sender, Component.text("An error occurred while executing the command.", NamedTextColor.RED));
            PluginLogger.logException(null, e);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            Set<String> completions = new HashSet<>();
            for (Map.Entry<String, BadgeSubCommand> entry : subCommands.entrySet()) {
                if (entry.getKey().startsWith(input)) {
                    BadgeSubCommand sub = entry.getValue();
                    if (sub.hasPermission(sender)) {
                        completions.add(sub.getName()); // Only include main name
                    }
                }
            }
            return new ArrayList<>(completions);
        }

        // Pass tab completion responsibility to the subcommand if needed
        BadgeSubCommand sub = subCommands.get(args[0].toLowerCase());
        if (sub != null && sub instanceof TabCompleter) {
            return ((TabCompleter) sub).onTabComplete(sender, command, alias, Arrays.copyOfRange(args, 1, args.length));
        }

        return Collections.emptyList();
    }

    public Collection<BadgeSubCommand> getSubCommands() {
        return new HashSet<>(subCommands.values());
    }
}
