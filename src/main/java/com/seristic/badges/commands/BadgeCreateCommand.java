package com.seristic.badges.commands;

import com.seristic.badges.Badges;
import com.seristic.badges.commands.Handler.BadgeSubCommand;
import com.seristic.badges.gui.BadgeGUI;
import com.seristic.badges.util.ColourUtil;
import com.seristic.badges.util.database.DatabaseManager;
import com.seristic.badges.util.helpers.MessageUtil;
import com.seristic.badges.util.helpers.PluginLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.messenger.message.Message;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class BadgeCreateCommand extends BadgeSubCommand {

    private final BadgeGUI badgeGUI;

    public BadgeCreateCommand(BadgeGUI badgeGUI) {
        this.badgeGUI = badgeGUI;
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public List<String> getAliases() {
        return List.of();
    }

    @Override
    public String getPermission() {
        return "badges.admin";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, Component.text("Only players can run this command.", NamedTextColor.RED));
            return;
        }

        if (args.length < 4) {
            MessageUtil.send(sender, Component.text("Usage: /badge create <name> <color> <icon> [description] [symbol]", NamedTextColor.RED));
            return;
        }

        String badgeName = args[0];
        String colorStr = args[1];
        String iconMaterialName = args[2];
        String chatIcon = args[3]
                ;
        String hoverText = args.length > 4 ? String.join(" ", Arrays.copyOfRange(args, 4, args.length)) : "";

        NamedTextColor color = ColourUtil.getNamedTextColor(colorStr);
        if (color == null) {
            MessageUtil.send(sender, Component.text("Invalid colour specified. Use a valid colour name (e.g, RED, BLUE, GREEN).", NamedTextColor.RED));
            return;
        }

        Material iconMaterial;
        try {
            iconMaterial = Material.valueOf(iconMaterialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            MessageUtil.send(sender, Component.text("Invalid icon material specific", NamedTextColor.RED));
            return;
        }

        try (Connection connection = DatabaseManager.getConnection()) {
            if (connection == null) {
                MessageUtil.send(sender, Component.text("Database connection not available.", NamedTextColor.RED));
                return;
            }

            String checkSql = "SELECT COUNT(*) FROM badges WHERE badge_name = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setString(1, badgeName);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    MessageUtil.send(player, Component.text(Badges.PREFIX + "A badge with the name '" + badgeName + "' already exists", NamedTextColor.RED));
                    return;
                }
            }

            String insertSql = """
                    INSERT INTO badges (badge_name, badge_description, badge_icon, badge_color, chat_icon)
                    VALUES (?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                ps.setString(1, badgeName);
                ps.setString(2, hoverText);
                ps.setString(3, iconMaterial.name());
                ps.setString(4, color.toString().toLowerCase());
                ps.setString(5, chatIcon);
                ps.executeUpdate();

                MessageUtil.send(sender, Component.text("Badge '" + badgeName + "' created successfully and added to the database.", NamedTextColor.GREEN));
                badgeGUI.openBadgeGUI(player, badgeGUI.getBadges(player), 0);;
                PluginLogger.info(Badges.PREFIX + "Badge '" + badgeName + "' created by " + player.getName());
            }
        } catch (SQLException e) {
            MessageUtil.send(sender, Component.text("Failed to create badge in the database.", NamedTextColor.RED));
            PluginLogger.logException(null, e);
        }
    }
}