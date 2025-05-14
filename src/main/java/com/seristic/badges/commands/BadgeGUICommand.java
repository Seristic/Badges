package com.seristic.badges.commands;

import com.seristic.badges.commands.Handler.BadgeSubCommand;
import com.seristic.badges.gui.BadgeGUI;
import com.seristic.badges.util.helpers.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class BadgeGUICommand extends BadgeSubCommand {

    private final BadgeGUI badgeGUI;

    public BadgeGUICommand(BadgeGUI badgeGUI) {
        this.badgeGUI = badgeGUI;
    }

    @Override
    public String getName() {
        return ""; // Default command if no subcommand is given
    }

    @Override
    public List<String> getAliases() {
        return List.of("gui", "open");
    }

    @Override
    public String getPermission() {
        return "badges.use";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, Component.text("Only players can open the badge GUI.", NamedTextColor.RED));
            return;
        }

        badgeGUI.openBadgeGUI(player, badgeGUI.getBadges(player), 0);
    }
}
