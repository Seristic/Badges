package com.seristic.badges.commands.Handler;

import com.seristic.badges.gui.BadgeGUI;
import com.seristic.badges.commands.BadgeCreateCommand;
import com.seristic.badges.commands.BadgeDeleteCommand;
import com.seristic.badges.util.helpers.MessageUtil;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandHandler implements CommandExecutor {

    private final List<BadgeSubCommand> subCommands = new ArrayList<>();
    private final BadgeGUI badgeGUI;

    public CommandHandler(BukkitAudiences adventure, BadgeGUI badgeGUI) {
        this.badgeGUI = badgeGUI;
        // Register your subcommands here:
        subCommands.add(new BadgeCreateCommand(badgeGUI));
        subCommands.add(new BadgeDeleteCommand(badgeGUI));
        // add others as needed...
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, Component.text("Only players can use this command.", NamedTextColor. RED));
            return true;
        }

        if (args.length == 0) {
            badgeGUI.openBadgeGUI(player, badgeGUI.getBadges(player), 0);
            return true;
        }

        String subCommandName = args[0].toLowerCase();

        for (BadgeSubCommand subCommand : subCommands) {
            if (subCommand.getName().equalsIgnoreCase(subCommandName) ||
                    subCommand.getAliases().contains(subCommandName)) {

                // Pass subcommand args WITHOUT the subcommand itself
                // i.e., args[1..] to subcommand.execute
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, args.length - 1);

                subCommand.execute(sender, subArgs);
                return true;
            }
        }

        sender.sendMessage("Unknown subcommand. Use /badge help");
        return true;
    }
}
