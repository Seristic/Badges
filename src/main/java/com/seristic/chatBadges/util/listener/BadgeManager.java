package com.seristic.chatBadges.util.listener;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.PrefixNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class BadgeManager {

    private static LuckPerms luckPerms = null;

    public static void setupLuckPerms() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null &&
                Bukkit.getPluginManager().getPlugin("LuckPerms").isEnabled()) {
            luckPerms = LuckPermsProvider.get();
            Bukkit.getLogger().info("[ChatBadges] Successfully hooked into LuckPerms!");
        } else {
            Bukkit.getLogger().warning("[ChatBadges] LuckPerms not found or not enabled!");
        }
    }

    // Optional getter if needed elsewhere
    public static LuckPerms getLuckPerms() {
        return luckPerms;
    }

    /**
     * Sets the player's prefix to the given chat icon.
     * The chatIcon parameter should already include the desired color codes (using '&' for legacy codes).
     * Only the icon will show in chat.
     *
     * @param player   The player whose badge (prefix) will be set.
     * @param chatIcon The chat icon string (e.g. "&6★") to display.
     */
    public static void setBadge(Player player, String chatIcon) {
        if (luckPerms == null) {
            player.sendMessage("§cLuckPerms is required for badges!");
            return;
        }

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            // (Optional) Remove any previous badge prefix nodes.
            // You can refine this filter if you need to keep other prefixes.
            user.data().clear(node -> node instanceof PrefixNode && node.toString().contains("[BADGE_ICON]"));

            // Translate legacy color codes from the badge creation command.
            String formattedIcon = ChatColor.translateAlternateColorCodes('&', chatIcon);

            // Create a new PrefixNode that uses only the icon.
            // (We add a hidden marker "[BADGE_ICON]" in a reset code at the end if needed for removal later.)
            PrefixNode newBadge = PrefixNode.builder()
                    .prefix(formattedIcon + "§r") // The prefix is exactly the formatted icon followed by a reset.
                    .priority(100)               // Adjust the priority as needed.
                    .build();

            // Add the new prefix node and save the changes.
            user.data().add(newBadge);
            luckPerms.getUserManager().saveUser(user);

            player.sendMessage("§aYour badge has been updated to: " + formattedIcon);
        } else {
            player.sendMessage("§cCould not retrieve your LuckPerms data.");
        }
    }
}