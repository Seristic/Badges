package com.seristic.badges.api;

import com.seristic.badges.util.Badge;
import com.seristic.badges.util.ColourUtil;
import com.seristic.badges.util.database.DatabaseManager;
import com.seristic.badges.util.helpers.PluginLogger;
import net.kyori.adventure.text.format.NamedTextColor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class BadgeAPIImpl implements BadgeAPI {

    @Override
    public Collection<Badge> getActiveBadges(UUID playerUUID) {
        List<Badge> badges = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection()) {
            if (connection == null || connection.isClosed()) return badges;

            String sql = """
                SELECT b.badge_id, b.badge_name, b.chat_icon, b.badge_color
                FROM badges b
                JOIN player_active_badges pab ON b.badge_id = pab.badge_id
                WHERE pab.player_uuid = ?
            """;

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, playerUUID.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        badges.add(extractBadgeFromResultSet(rs));
                    }
                }
            }
        } catch (SQLException e) {
            PluginLogger.logException(null, e);
        }
        return badges;
    }

    @Override
    public Collection<Badge> getAllBadges(UUID playerUUID) {
        List<Badge> badges = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection()) {
            if (connection == null || connection.isClosed()) return badges;

            String sql = """
                SELECT b.badge_id, b.badge_name, b.chat_icon, b.badge_color
                FROM badges b
                JOIN player_badges pob ON b.badge_id = pob.badge_id
                WHERE pob.uuid = ?
            """;


            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, playerUUID.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        badges.add(extractBadgeFromResultSet(rs));
                    }
                }
            }
        } catch (SQLException e) {
            PluginLogger.logException(null, e);
        }
        return badges;
    }

    private Badge extractBadgeFromResultSet(ResultSet rs) throws SQLException {
        String id = rs.getString("badge_id");
        String name = rs.getString("badge_name");
        String chatIcon = rs.getString("chat_icon");
        String colorStr = rs.getString("badge_color");

        NamedTextColor color = ColourUtil.getNamedTextColor(colorStr);
        if (color == null) color = NamedTextColor.WHITE;

        return new Badge(id, name, chatIcon, color);
    }
}