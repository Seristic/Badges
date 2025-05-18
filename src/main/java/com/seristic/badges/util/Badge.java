package com.seristic.badges.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class Badge {
    private final String id;
    private final String name;
    private final String chatIcon;
    private final NamedTextColor color;
    private final String description;

    // Constructor
    public Badge(String id, String name, String chatIcon, NamedTextColor color, String description) {
        this.id = id;
        this.name = name;
        this.chatIcon = chatIcon;
        this.color = color;
        this.description = description;
    }

    // Getters for the properties
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

    public String getDescription() {
        return description;
    }

    // Create the hoverable component for chat
    public Component asChatComponent() {
        return Component.text(chatIcon)
                .color(color)
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                        Component.text()
                                .append(Component.text(name))
                                .build()
                ));
    }
}