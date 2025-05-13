package com.seristic.badges.commands;

import com.seristic.badges.Badges;
import com.seristic.badges.gui.BadgeGUI;
import com.seristic.badges.util.ColourUtil;
import com.seristic.badges.util.database.DatabaseManager;
import com.seristic.badges.util.helpers.MessageUtil;
import com.seristic.badges.util.helpers.PluginLogger;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BadgeCommand implements CommandExecutor {

    private final BukkitAudiences adventure;
    private final BadgeDeleteCommand deleteCommand;

    private final Map<String, String> commands = new HashMap<>();
    private final BadgeGUI badgeGUI;

    public BadgeCommand(BukkitAudiences adventure, BadgeGUI badgeGUI) {
        this.adventure = adventure;
        this.badgeGUI = badgeGUI;
        this.deleteCommand = new BadgeDeleteCommand(badgeGUI);

        // Player Commands
        commands.put("set", "Change your badge to one you own. Usage: /badge set [badge]");
        commands.put("give", "Grant a player's access to a group badge. Usage: /badge give [badge] [player]");
        commands.put("take", "Remove a player's access to a group badge. Usage: /badge take [badge] [player]");
        commands.put("leave", "Leave a group badge");
        commands.put("remove", "Remove your current badge");
        commands.put("owned", "List all your owned badges");
        commands.put("group", "List all your group badges");
        commands.put("list", "List all possible badges");
        commands.put("members", "List all members of the named badge group");

        // Admin-Only Commands
        commands.put("share", "Grant a player access to give a group badge. Usage: /badge share [badge] [player]");
        commands.put("create", "Create a new badge. [Admin Only] Usage: /badge create <badgeName> <color> <iconMaterial> <chatIcon> <hoverText...>");
        commands.put("rename", "Rename a badge group. Usage: /badge rename [badge] [newname] [badgeText]");
        commands.put("delete", "Deletes a badge [Admin Only]");
        commands.put("reload", "Reload Badges from the Config [Admin Only]");
        commands.put("rerun", "Debug Command [Admin Only]");
        commands.put("setowner", "Transfer Ownership of the badge. Usage: /badge setowner [badge] [newOwnerName]");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            adventure.sender(sender).sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        // Open badge GUI by default
        if (args.length < 1) {
            badgeGUI.openBadgeGUI(player, badgeGUI.getBadges(player), 0);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // Unknown command
        if (!commands.containsKey(subCommand)) {
            MessageUtil.send(sender, Component.text("Unknown Command! Use /badge for help.", NamedTextColor.RED));
            return true;
        }

        // === CREATE SUBCOMMAND ===
        if (subCommand.equals("create")) {
            if (!player.hasPermission("chatbadges.admin")) {
                MessageUtil.send(sender, Component.text("You do not have permission to create badges.", NamedTextColor.RED));
                return true;
            }

            if (args.length < 6) {
                MessageUtil.send(sender, Component.text("Usage: /badge create <badgeName> <color> <iconMaterial> <chatIcon> <hoverText...>", NamedTextColor.RED));
                return true;
            }

            String badgeName = args[1];
            String colorStr = args[2];
            String iconMaterialName = args[3];
            String chatIcon = args[4];
            String hoverText = String.join(" ", Arrays.copyOfRange(args, 5, args.length));

            // Validate color
            NamedTextColor color = ColourUtil.getNamedTextColor(colorStr);
            if (color == null) {
                MessageUtil.send(sender, Component.text("Invalid color specified. Use a valid color name (e.g., red, blue, green).", NamedTextColor.RED));
                return true;
            }

            // Validate icon material
            Material iconMaterial;
            try {
                iconMaterial = Material.valueOf(iconMaterialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                MessageUtil.send(sender, Component.text("Invalid icon material specified.", NamedTextColor.RED));
                return true;
            }

            // Create fake badge item for display
            ItemStack badgeItem = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = badgeItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(Component.text(badgeName).color(NamedTextColor.RED).decorate(TextDecoration.BOLD).toString()); // Convert the Component to String

                // Add enchantments and hide them
                meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

                // Set the modified meta back to the item
                badgeItem.setItemMeta(meta);
            }

            // Database insert
            try (Connection connection = DatabaseManager.getConnection()) {
                if (connection == null) {
                    MessageUtil.send(sender, Component.text("Database connection not available.", NamedTextColor.RED));
                    return true;
                }

                String insertSql = """
                    INSERT INTO badges (badge_name, badge_description, badge_icon, badge_color, chat_icon)
                    VALUES (?, ?, ?, ?, ?);
                """;

                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    ps.setString(1, badgeName);
                    ps.setString(2, hoverText);
                    ps.setString(3, iconMaterial.name());
                    ps.setString(4, color.toString().toLowerCase());
                    ps.setString(5, chatIcon);
                    ps.executeUpdate();

                    MessageUtil.send(sender, Component.text("Badge '" + badgeName + "' created successfully and added to the database!", NamedTextColor.GREEN));
                    badgeGUI.openBadgeGUI(player, badgeGUI.getBadges(player), 0);
                    PluginLogger.getLogger().info("[ChatBadges] Badge '" + badgeName + "' created by " + player.getName());
                }
            } catch (SQLException e) {
                MessageUtil.send(sender, Component.text("Failed to create badge in the database.", NamedTextColor.RED));
                e.printStackTrace();
            }

            return true;
        }

        // === HANDLE OTHER SUBCOMMANDS ===
        if (subCommand.equalsIgnoreCase("delete")) {
            deleteCommand.handle(sender, args);
            return true;
        }
        return false;
    }
}
