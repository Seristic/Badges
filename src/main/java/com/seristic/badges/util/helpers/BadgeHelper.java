package com.seristic.badges.util.helpers;

import com.seristic.badges.Badges;
import com.seristic.badges.util.Badge;
import com.seristic.badges.util.ColourUtil;
import com.seristic.badges.util.database.DatabaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

public final class BadgeHelper {

    private BadgeHelper() {}

    public static Badge getBadgeByName(String badgeName) {
        try (Connection connection = DatabaseManager.getConnection()) {
            if (connection == null || connection.isClosed()) {
                PluginLogger.warning(Badges.PREFIX + "Database connection failed.");
                return null;
            }
            String sql = "SELECT badge_id, badge_name, chat_icon, badge_color FROM badges WHERE LOWER(TRIM(badge_name)) = LOWER(TRIM(?))";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, badgeName.trim().toLowerCase(Locale.ROOT));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String id = rs.getString("badge_id");
                        String name = rs.getString("badge_name");
                        String chatIcon = rs.getString("chat_icon");
                        String colorStr = rs.getString("badge_color");

                        NamedTextColor color = ColourUtil.getNamedTextColor(colorStr);
                        if (color == null) color = NamedTextColor.WHITE;

                        return new Badge(id, name, chatIcon, color, false, false);
                    }
                }
            }
        } catch (SQLException e) {
            PluginLogger.logException(null, e);
        }
        return null;
    }

    public static Component formatBadgeMessage(String actionPrefix, Badge badge) {
        // Log only the plain string to the console (single prefix)
        String actionText = actionPrefix + badge.getName() + " (" + badge.getChatIcon() + ")";
        PluginLogger.info(actionText);

        // Return the Adventure component for player messages (with prefix)
        return Component.empty()
                .append(Badges.PREFIX)
                .append(Component.text(actionPrefix).color(NamedTextColor.WHITE))
                .append(Component.text(" "))
                .append(Component.text(badge.getChatIcon()).color(badge.getColor()));
    }
}
