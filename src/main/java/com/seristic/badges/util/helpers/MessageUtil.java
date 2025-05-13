package com.seristic.badges.util.helpers;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
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
}