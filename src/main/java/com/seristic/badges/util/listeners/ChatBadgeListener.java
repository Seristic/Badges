package com.seristic.badges.util.listeners;

import com.seristic.badges.Badges;
import com.seristic.badges.gui.BadgeManager;
import com.seristic.badges.util.Badge;
import com.seristic.badges.util.helpers.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ChatBadgeListener implements Listener {

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {

        event.setCancelled(true);

        var player = event.getPlayer();
        String message = event.getMessage();

        List<Badge> badges = BadgeManager.getBadgesForPlayer(player);

        Component badgesComponent = Component.empty();

        for (Badge badge : badges) {
            badgesComponent = badgesComponent.append(badge.asChatComponent()).append(Component.space());
        }

        Component nameComponent = Component.text(player.getName())
                .color(badgeColorOrDefault());

        Component messageComponent = Component.text(": " + message)
                .color(net.kyori.adventure.text.format.NamedTextColor.WHITE);

        Component chatComponent = badgesComponent.append(nameComponent).append(messageComponent);

        event.getRecipients().forEach(recipient -> MessageUtil.send(recipient, chatComponent));
    }

    private net.kyori.adventure.text.format.TextColor badgeColorOrDefault() {
        return net.kyori.adventure.text.format.NamedTextColor.YELLOW;
    }
}
