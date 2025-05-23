package com.seristic.badges.util.listeners;

import com.seristic.badges.Badges;
import com.seristic.badges.gui.BadgeManager;
import com.seristic.badges.util.helpers.PluginLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;

public class BadgeClickListener implements Listener {

    @EventHandler
    public void onBadgeClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().contains("Badge Selection")) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        ItemMeta itemMeta = clickedItem.getItemMeta();
        var container = itemMeta.getPersistentDataContainer();

        NamespacedKey badgeKey = new NamespacedKey(Badges.getInstance(), "badge_name");
        NamespacedKey slotKey = new NamespacedKey(Badges.getInstance(), "badge_slot");

        String badgeName = container.get(badgeKey, PersistentDataType.STRING);
        Integer slotIndex = container.get(slotKey, PersistentDataType.INTEGER);

        if (badgeName == null || badgeName.isBlank()) {
            PluginLogger.warning(Badges.PREFIX + "DEBUG: No badge_name found in item metadata.");
            return;
        }

        if (slotIndex == null) {
            PluginLogger.warning(Badges.PREFIX + "DEBUG: No badge_slot found in item metadata.");
            return;
        }

        if (event.getClick() == ClickType.LEFT) {
            BadgeManager.setBadge(player, badgeName, slotIndex);
        } else if (event.getClick() == ClickType.RIGHT) {
            BadgeManager.removeBadge(player, badgeName, slotIndex);
        }
    }
}
