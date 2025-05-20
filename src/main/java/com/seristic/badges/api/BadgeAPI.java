package com.seristic.badges.api;

import com.seristic.badges.util.Badge;

import java.util.Collection;
import java.util.UUID;

public interface BadgeAPI {
    /**
     * Get all active badges for a player
     * @param playerUUID The UUID of the player
     * @return Collection of active badges
     */
    Collection<Badge> getActiveBadges(UUID playerUUID);

    /**
     * Get all badges owned by a player
     * @param playerUUID The UUID of the player
     * @return Collection of all badges
     */
    Collection<Badge> getAllBadges(UUID playerUUID);
}
