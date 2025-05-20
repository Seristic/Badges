package com.seristic.badges.util;

import com.seristic.badges.util.database.DatabaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a visual badge with chat display information.
 */
public class Badge {
    private final String id;
    private final String name;
    private final String chatIcon;
    private final NamedTextColor color;

    /**
     * Constructs a Badge instance.
     *
     * @param id        The badge ID (usually database ID as a String).
     * @param name      Display name.
     * @param chatIcon  Symbol shown in chat.
     * @param color     Display color for the badge.
     */
    public Badge(String id, String name, String chatIcon, NamedTextColor color) {
        this.id = id;
        this.name = name;
        this.chatIcon = chatIcon;
        this.color = color != null ? color : NamedTextColor.WHITE;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getChatIcon() {
        return chatIcon;
    }

    public NamedTextColor getColor() {
        return color;
    }

    /**
     * Returns a component for displaying the badge icon in chat with hover.
     */
    public Component asChatComponent() {
        return Component.text(chatIcon)
                .color(color)
                .hoverEvent(HoverEvent.showText(Component.text(name)));
    }

    /**
     * Builds a full prefix of stacked badge icons to show before a player's name.
     * Respects permission `badges.stackable` and slot limits.
     *
     * @param player Player to query.
     * @return Combined component of badge icons.
     */
    public static Component getStackedBadgePrefix(Player player) {
        int maxSlots = getMaxBadgeSlots(player);
        if (maxSlots <= 0) return Component.empty();

        boolean canStack = player.hasPermission("badges.stackable");

        List<Badge> activeBadges = new ArrayList<>();
        for (int i = 0; i < maxSlots; i++) {
            Badge badge = DatabaseManager.getBadgeInSlot(player.getUniqueId(), i);
            if (badge != null) {
                activeBadges.add(badge);
            }
        }

        if (activeBadges.isEmpty()) return Component.empty();

        if (!canStack) {
            // Only first active badge shown
            Badge badge = activeBadges.get(0);
            return Component.text(badge.getChatIcon())
                    .color(badge.getColor())
                    .append(Component.text(" "));
        }

        // Build stacked badges
        Component combined = Component.empty();
        for (Badge badge : activeBadges) {
            combined = combined.append(
                    Component.text(badge.getChatIcon())
                            .color(badge.getColor())
                            .append(Component.text(" "))
            );
        }
        return combined;
    }

    /**
     * Determines the number of badge slots a player has via permissions.
     * e.g. badges.slots.3 gives 3 slots.
     */
    public static int getMaxBadgeSlots(Player player) {
        int max = 0;
        for (var perm : player.getEffectivePermissions()) {
            String permission = perm.getPermission();
            if (permission.startsWith("badges.slots.")) {
                try {
                    int value = Integer.parseInt(permission.substring("badges.slots.".length()));
                    max = Math.max(max, value);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return max;
    }
}
