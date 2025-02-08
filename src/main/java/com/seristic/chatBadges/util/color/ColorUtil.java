package com.seristic.chatBadges.util.color;

import net.kyori.adventure.text.format.NamedTextColor;

public interface ColorUtil {
    static NamedTextColor getNamedTextColor(String name) {
        switch (name.toLowerCase()) {
            case "black": return NamedTextColor.BLACK;
            case "dark_blue":
            case "darkblue": return NamedTextColor.DARK_BLUE;
            case "dark_green":
            case "darkgreen": return NamedTextColor.DARK_GREEN;
            case "dark_aqua":
            case "darkaqua": return NamedTextColor.DARK_AQUA;
            case "dark_red":
            case "darkred": return NamedTextColor.DARK_RED;
            case "dark_purple":
            case "darkpurple": return NamedTextColor.DARK_PURPLE;
            case "gold": return NamedTextColor.GOLD;
            case "gray":
            case "grey": return NamedTextColor.GRAY;
            case "dark_gray":
            case "darkgrey": return NamedTextColor.DARK_GRAY;
            case "blue": return NamedTextColor.BLUE;
            case "green": return NamedTextColor.GREEN;
            case "aqua": return NamedTextColor.AQUA;
            case "red": return NamedTextColor.RED;
            case "light_purple":
            case "lightpurple": return NamedTextColor.LIGHT_PURPLE;
            case "yellow": return NamedTextColor.YELLOW;
            case "white": return NamedTextColor.WHITE;
            default: return null;
        }
    }
}
