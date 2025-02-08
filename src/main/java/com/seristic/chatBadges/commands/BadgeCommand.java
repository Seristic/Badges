package com.seristic.chatBadges.commands;

import com.seristic.chatBadges.util.color.ColorUtil;
import com.seristic.chatBadges.util.database.DatabaseManager;
import com.seristic.chatBadges.util.gui.BadgeGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
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

    private final Map<String, String> commands = new HashMap<>();
    private final BadgeGUI badgeGUI;

    public BadgeCommand(BadgeGUI badgeGUI) {
        this.badgeGUI = badgeGUI;

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
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;

        // No subcommand provided: open the badge GUI.
        if (args.length < 1) {
            badgeGUI.openBadgeGUI(player, badgeGUI.getBadges(player), 0);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // Check if the subcommand is recognized.
        if (!commands.containsKey(subCommand)) {
            player.sendMessage(Component.text("Unknown Command! Use /badge for help.", NamedTextColor.RED));
            return true;
        }

        // Handle the "create" subcommand (Admin-only).
        if (subCommand.equals("create")) {
            // Check admin permissions.
            if (!player.hasPermission("chatbadges.admin")) {
                player.sendMessage(Component.text("You do not have permission to create badges.", NamedTextColor.RED));
                return true;
            }
            // Expected usage:
            // /badge create <badgeName> <color> <iconMaterial> <chatIcon> <hoverText...>
            if (args.length < 6) {
                player.sendMessage(Component.text("Usage: /badge create <badgeName> <color> <iconMaterial> <chatIcon> <hoverText...>", NamedTextColor.RED));
                return true;
            }
            String badgeName = args[1];
            String colorStr = args[2];
            String iconMaterialName = args[3];
            String chatIcon = args[4];
            String hoverText = String.join(" ", Arrays.copyOfRange(args, 5, args.length));

            // Convert the color string using the ColorUtil interface.
            NamedTextColor color = ColorUtil.getNamedTextColor(colorStr);
            if (color == null) {
                player.sendMessage(Component.text("Invalid color specified. Use a valid color name (e.g., red, blue, green).", NamedTextColor.RED));
                return true;
            }

            // Validate the icon material.
            Material iconMaterial;
            try {
                iconMaterial = Material.valueOf(iconMaterialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(Component.text("Invalid icon material specified.", NamedTextColor.RED));
                return true;
            }

            // Instead of giving the player a physical nametag,
            // we create the badge "item" solely for display in the badge GUI.
            // (Assuming your BadgeGUI#getBadges(Player) will retrieve badges from the database.)
            ItemStack badgeItem = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = badgeItem.getItemMeta();
            if (meta != null) {
                meta.displayName(
                        Component.text(badgeName)
                                .color(color)
                                .decorate(TextDecoration.BOLD)
                );
                meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                badgeItem.setItemMeta(meta);
            }

            // Create the chat icon component for use in chat/tab displays.
            Component chatBadgeComponent = Component.text(chatIcon)
                    .color(color)
                    .decorate(TextDecoration.BOLD)
                    .hoverEvent(HoverEvent.showText(Component.text(hoverText).color(color)));

            // Insert the new badge into the database.
            Connection connection = DatabaseManager.getConnection();
            if (connection == null) {
                player.sendMessage(Component.text("Database connection not available.", NamedTextColor.RED));
                return true;
            }
            String insertSql = "INSERT INTO badges (badge_name, badge_description, badge_icon) VALUES (?, ?, ?);";
            try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                ps.setString(1, badgeName);
                ps.setString(2, hoverText);
                ps.setString(3, iconMaterial.name());
                ps.executeUpdate();
                player.sendMessage(Component.text("Badge '" + badgeName + "' created successfully and added to the database!", NamedTextColor.GREEN));
            } catch (SQLException e) {
                player.sendMessage(Component.text("Failed to create badge in the database.", NamedTextColor.RED));
                e.printStackTrace();
                return true;
            }

            // Instead of adding the badge item to the player's inventory,
            // refresh the badge GUI so the new badge is available.
            badgeGUI.openBadgeGUI(player, badgeGUI.getBadges(player), 0);
            System.out.println("[ChatBadges] Badge '" + badgeName + "' created by " + player.getName());
            return true;
        }

        // Handle other subcommands.
        // (Implement your other subcommands like set, give, take, etc., as needed.)
        player.sendMessage(Component.text("Executing: " + subCommand, NamedTextColor.AQUA));
        return true;
    }
}