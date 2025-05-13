package com.seristic.badges.commands;

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

public class BadgeDeleteCommand {

    private final BadgeGUI badgeGUI;

    public BadgeDeleteCommand(BadgeGUI badgeGUI) {
        this.badgeGUI = badgeGUI;
    }

    public void handle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, Component.text("Only players can use this command.", NamedTextColor.RED));
            return;
        }

        if (!player.isOp() && !player.hasPermission("chatbadges.admin")) {
            MessageUtil.send(sender, Component.text("You do not have permission to delete badges.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            MessageUtil.send(sender, Component.text("Usage: /badge delete <badgeName>", NamedTextColor.RED));
            return;
        }

        String badgeName = args[1];

        try (Connection connection = DatabaseManager.getConnection()) {
            if (connection == null) {
                MessageUtil.send(sender, Component.text("Database connection not available.", NamedTextColor.RED));
                return;
            }

            String sql = "DELETE FROM badges WHERE badge_name = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, badgeName);
                int rowsAffected = statement.executeUpdate();

                if (rowsAffected > 0) {
                    MessageUtil.send(sender, Component.text("Badge '" + badgeName + "' deleted successfully.", NamedTextColor.GREEN));
                    PluginLogger.getLogger().info("[ChatBadges] Badge '" + badgeName + "' deleted by " + player.getName());
                } else {
                    MessageUtil.send(sender, Component.text("Badge '" + badgeName + "' not found.", NamedTextColor.RED));
                }
            }
        } catch (SQLException e) {
            MessageUtil.send(sender, Component.text("Failed to delete badge from the database.", NamedTextColor.RED));
            e.printStackTrace();
        }
    }
}