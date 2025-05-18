package com.seristic.badges.commands;

import com.seristic.badges.commands.Handler.BadgeSubCommand;
import com.seristic.badges.gui.BadgeGUI;
import com.seristic.badges.util.database.DatabaseManager;
import com.seristic.badges.util.helpers.MessageUtil;
import com.seristic.badges.util.helpers.PluginLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class BadgeDeleteCommand extends BadgeSubCommand {

    private final BadgeGUI badgeGUI;

    public BadgeDeleteCommand(BadgeGUI badgeGUI) {
        this.badgeGUI = badgeGUI;
    }

    @Override
    public String getName() {
        return "delete";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public String getPermission() {
        return "badges.admin";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, Component.text("Only players can use this command.", NamedTextColor.RED));
            return;
        }

        if (args.length < 1) {
            MessageUtil.send(sender, Component.text("Usage: /badge delete <badgeName>", NamedTextColor.RED));
            return;
        }

        String badgeName = args[0];

        try (Connection connection = DatabaseManager.getConnection()) {
            if (connection == null) {
                MessageUtil.send(sender, Component.text("Database unavailable.", NamedTextColor.RED));
                return;
            }

            String sql = "DELETE FROM badges WHERE badge_name = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, badgeName);
                int rowsAffected = statement.executeUpdate();

                if (rowsAffected > 0) {
                    MessageUtil.send(sender, Component.text("Badge '" + badgeName + "' deleted.", NamedTextColor.GREEN));
                    PluginLogger.info("[ChatBadges] Badge '" + badgeName + "' deleted by " + player.getName());
                } else {
                    MessageUtil.send(sender, Component.text("Badge '" + badgeName + "' not found.", NamedTextColor.RED));
                }
            }

        } catch (SQLException e) {
            MessageUtil.send(sender, Component.text("Failed to delete badge.", NamedTextColor.RED));
            PluginLogger.logException(null, e);
        }
    }
}
