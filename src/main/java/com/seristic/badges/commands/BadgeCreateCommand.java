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
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
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

        if (args.length < 8) {
            MessageUtil.send(sender, Component.text("Usage: /badge create <name> <color> <material> <icon> <hoverText> permission:<true|false> hidden:<true|false> [group]", NamedTextColor.RED));
            return;
        }

        String badgeName = args[0];
        String colorStr = args[1];
        String materialName = args[2];
        String chatIcon = args[3];

        // Join hoverText (which may span multiple args) — args[4] through args.length - 3
        StringBuilder hoverTextBuilder = new StringBuilder();
        for (int i = 4; i < args.length - 3; i++) {
            hoverTextBuilder.append(args[i]).append(" ");
        }
        String hoverText = hoverTextBuilder.toString().trim();

        String permissionArg = args[args.length - 3];
        String hiddenArg = args[args.length - 2];
        String badgeGroup = args.length >= 9 ? args[args.length - 1] : "default";

        // === Color ===
        NamedTextColor color = ColourUtil.getNamedTextColor(colorStr);
        if (color == null) {
            MessageUtil.send(sender, Component.text("Invalid color specified.", NamedTextColor.RED));
            return;
        }

        // === Material ===
        Material iconMaterial;
        try {
            iconMaterial = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            MessageUtil.send(sender, Component.text("Invalid icon material.", NamedTextColor.RED));
            return;
        }

        // === Boolean Flags ===
        boolean requiresPermission = false;
        boolean isHidden = false;

        if (permissionArg.toLowerCase().startsWith("permission:")) {
            String[] split = permissionArg.split(":", 2);
            if (split.length == 2) {
                requiresPermission = Boolean.parseBoolean(split[1]);
            }
        } else {
            MessageUtil.send(sender, Component.text("Invalid permission flag format. Use permission:true or permission:false.", NamedTextColor.RED));
            return;
        }

        if (hiddenArg.toLowerCase().startsWith("hidden:")) {
            String[] split = hiddenArg.split(":", 2);
            if (split.length == 2) {
                isHidden = Boolean.parseBoolean(split[1]);
            }
        } else {
            MessageUtil.send(sender, Component.text("Invalid hidden flag format. Use hidden:true or hidden:false.", NamedTextColor.RED));
            return;
        }

        try (Connection connection = DatabaseManager.getConnection()) {
            if (connection == null) {
                MessageUtil.send(sender, Component.text("Database connection unavailable.", NamedTextColor.RED));
                return;
            }

            String checkSql = "SELECT COUNT(*) FROM badges WHERE badge_name = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setString(1, badgeName);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    MessageUtil.send(player, Component.text(Badges.PREFIX + "A badge with the name '" + badgeName + "' already exists.", NamedTextColor.RED));
                    return;
                }
            }

            String insertSql = "INSERT INTO badges " +
                    "(badge_name, chat_icon, badge_color, description, requires_permission, is_hidden, badge_group) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                ps.setString(1, badgeName);
                ps.setString(2, chatIcon);
                ps.setString(3, color.toString());
                ps.setString(4, hoverText);
                ps.setBoolean(5, requiresPermission);
                ps.setBoolean(6, isHidden);
                ps.setString(7, badgeGroup);

                ps.executeUpdate();

                MessageUtil.send(sender, Component.text("Badge '" + badgeName + "' created successfully.", NamedTextColor.GREEN));
                badgeGUI.openBadgeGUI(player, badgeGUI.getBadges(player), 0);
                PluginLogger.info("[Badges] Badge '" + badgeName + "' created by " + player.getName());
            }
        } catch (SQLException e) {
            MessageUtil.send(sender, Component.text("Failed to insert badge into the database.", NamedTextColor.RED));
            PluginLogger.logException(null, e);
        }
    }
}
