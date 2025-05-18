package com.seristic.badges.util.helpers;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

public final class MessageUtil {

    private static BukkitAudiences adventure;

    // Prevent instantiation
    private MessageUtil() {}

    public static void initialize(BukkitAudiences audience) {
        adventure = audience;
    }

    public static void send(CommandSender sender, Component message) {
        if (adventure == null) {
            sender.sendMessage("[Adventure Missing] " + message.toString());
        } else {
            adventure.sender(sender).sendMessage(message);
        }
    }
    public static Component createBadgeHoverText(String badgePrefix) {
        return Component.text("Badges: " + badgePrefix)
                .color(TextColor.color(0xFFD700))
                .decorate(TextDecoration.BOLD);
    }
}