package com.seristic.badges.gui;

import com.seristic.badges.Badges;
import com.seristic.badges.util.ConfigManager;
import com.seristic.badges.util.database.DatabaseManager;
import com.seristic.badges.util.helpers.MessageUtil;
import com.seristic.badges.util.helpers.PluginLogger;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class BadgeGUI implements Listener {

    private final JavaPlugin plugin;
    private final BukkitAudiences adventure;

    private final int guiSize = ConfigManager.getGuiSize();
    private final String guiTitle = ChatColor.translateAlternateColorCodes('&', ConfigManager.getGuiTitle());
    private final int nextPageSlot = ConfigManager.getNextPageSlot();
    private final int prevPageSlot = ConfigManager.getPrevPageSlot();
    private final int badgesPerPage = ConfigManager.getBadgesPerPage();
    private final Sound applyBadgeSound = ConfigManager.getApplyBadgeSound();
    private final float applyBadgeVolume = ConfigManager.getApplyBadgeSoundVolume();
    private final float applyBadgePitch = ConfigManager.getApplyBadgeSoundPitch();

    private final ItemStack blackGlass = createGlassPane();

    public BadgeGUI(BukkitAudiences adventure, JavaPlugin plugin) {
        this.adventure = adventure;
        this.plugin = plugin;
    }

    /**
     * Opens the paginated Badge GUI for the player.
     *
     * @param player the player opening the GUI
     * @param badges the list of badges to display
     * @param page the page number (0-based)
     */
    public void openBadgeGUI(Player player, List<ItemStack> badges, int page) {
        int maxPage = (int) Math.ceil((double) badges.size() / badgesPerPage) - 1;
        if (page < 0 || page > maxPage) return;

        Inventory gui = Bukkit.createInventory(null, guiSize, guiTitle + " (Page " + (page + 1) + ")");

        // Fill GUI borders with black glass panes
        for (int i = 0; i < guiSize; i++) {
            if (i < 9 || i >= guiSize - 9 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, blackGlass);
            }
        }

        // Populate the badges for this page starting at slot 10
        int startIndex = page * badgesPerPage;
        int endIndex = Math.min(startIndex + badgesPerPage, badges.size());
        int slot = 10;
        for (int i = startIndex; i < endIndex; i++, slot++) {
            ItemStack badgeItem = badges.get(i).clone(); // clone to prevent side-effects

            // Store slot in persistent data for reference
            ItemMeta meta = badgeItem.getItemMeta();
            if (meta != null) {
                NamespacedKey slotKey = new NamespacedKey(plugin, "badge_slot");
                meta.getPersistentDataContainer().set(slotKey, PersistentDataType.INTEGER, slot);
                badgeItem.setItemMeta(meta);
            }

            gui.setItem(slot, badgeItem);
        }

        // Navigation buttons
        if (page > 0) {
            gui.setItem(prevPageSlot, createNavItem(ConfigManager.getPrevPageMaterial(), ConfigManager.getPrevPageName()));
        }
        if (page < maxPage) {
            gui.setItem(nextPageSlot, createNavItem(ConfigManager.getNextPageMaterial(), ConfigManager.getNextPageName()));
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().startsWith(guiTitle)) return;
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) {
            MessageUtil.send(player, Component.text("Invalid badge data.", NamedTextColor.RED));
            return;
        }

        NamespacedKey nameKey = new NamespacedKey(plugin, "badge_name");
        String badgeName = meta.getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
        if (badgeName == null || badgeName.isEmpty()) {
            MessageUtil.send(player, Component.text("You cannot equip this badge.", NamedTextColor.RED));
            return;
        }

        // Hidden badge check — must have unhide permission to equip
        NamespacedKey hiddenKey = new NamespacedKey(plugin, "is_hidden");
        Byte isHidden = meta.getPersistentDataContainer().get(hiddenKey, PersistentDataType.BYTE);
        String permUnhideNode = "badges.unhide." + badgeName.toLowerCase(Locale.ROOT);
        if (isHidden != null && isHidden == 1 && !player.hasPermission(permUnhideNode)) {
            MessageUtil.send(player, Component.text("This badge is hidden and cannot be equipped.", NamedTextColor.RED));
            return;
        }

        // Locked badge check (requires permission but player lacks badges.use.*)
        NamespacedKey lockedKey = new NamespacedKey(plugin, "is_locked");
        Byte isLocked = meta.getPersistentDataContainer().get(lockedKey, PersistentDataType.BYTE);
        if (isLocked != null && isLocked == 1) {
            MessageUtil.send(player, Component.text("You do not have permission to equip the badge '" + badgeName + "'.", NamedTextColor.RED));
            return;
        }

        // Passed all checks, equip the badge
        int clickedSlot = event.getSlot();
        applyBadgeWithLuckPerms(player, badgeName, clickedSlot);
    }

    /**
     * Applies the badge to the player and plays sound.
     */
    private void applyBadgeWithLuckPerms(Player player, String badgeName, int clickedSlot) {
        PluginLogger.info(Badges.PREFIX + badgeName + " from slot: " + clickedSlot);
        player.playSound(player.getLocation(), applyBadgeSound, SoundCategory.MASTER, applyBadgeVolume, applyBadgePitch);
        BadgeManager.setBadge(player, badgeName, clickedSlot);
    }

    /**
     * Creates the black glass pane for GUI borders.
     */
    private ItemStack createGlassPane() {
        ItemStack item = new ItemStack(ConfigManager.getBorderMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', ConfigManager.getBorderName()));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates navigation items (Next/Prev buttons).
     */
    private ItemStack createNavItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates a default badge item for fallback.
     */
    private ItemStack createDefaultBadge() {
        ItemStack badge = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = badge.getItemMeta();
        if (meta != null) {
            Component displayName = Component.text("Default Badge", NamedTextColor.GRAY, net.kyori.adventure.text.format.TextDecoration.BOLD);
            meta.setDisplayName(net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(displayName));

            List<String> lore = new ArrayList<>();
            lore.add("Chat Icon: [ ✦ ]");
            meta.setLore(lore);

            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            badge.setItemMeta(meta);
        }
        return badge;
    }

    /**
     * Retrieves badges from database, filtering based on player permissions and badge flags.
     */
    public List<ItemStack> getBadges(Player player) {
        List<ItemStack> badges = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection()) {
            if (connection == null || connection.isClosed()) {
                badges.add(createDefaultBadge());
                return badges;
            }

            String query = "SELECT badge_id, badge_name, chat_icon, requires_permission, is_hidden FROM badges";
            try (PreparedStatement ps = connection.prepareStatement(query); ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    String badgeId = rs.getString("badge_id");
                    String badgeName = rs.getString("badge_name");
                    String chatIcon = rs.getString("chat_icon");
                    boolean requiresPermission = rs.getBoolean("requires_permission");
                    boolean isHidden = rs.getBoolean("is_hidden");

                    String permUseNode = "badges.use." + badgeName.toLowerCase(Locale.ROOT);
                    String permUnhideNode = "badges.unhide." + badgeName.toLowerCase(Locale.ROOT);

                    // 1. Hide the badge completely if it is hidden and player lacks unhide permission
                    if (isHidden && !player.hasPermission(permUnhideNode)) {
                        // Skip badge - hidden and player cannot see it
                        continue;
                    }

                    // 2. Build badge item
                    ItemStack badgeItem = new ItemStack(Material.NAME_TAG);
                    ItemMeta meta = badgeItem.getItemMeta();

                    if (meta != null) {
                        meta.setDisplayName(ChatColor.GOLD + badgeName);

                        List<String> lore = new ArrayList<>();
                        lore.add("Chat Icon: [ " + chatIcon.trim() + " ]");
                        lore.add("Badge ID: " + badgeId);
                        lore.add("Click to Apply!");

                        // 3. Mark badge locked if requires permission but player lacks badges.use.*
                        if (requiresPermission && !player.hasPermission(permUseNode)) {
                            lore.add(ChatColor.RED + "You do not have permission to equip this badge.");
                            meta.setDisplayName(ChatColor.DARK_GRAY + badgeName + ChatColor.RED + " [LOCKED]");
                            meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);

                            NamespacedKey lockedKey = new NamespacedKey(plugin, "is_locked");
                            meta.getPersistentDataContainer().set(lockedKey, PersistentDataType.BYTE, (byte) 1);
                        }

                        // 4. Store hidden flag for click handler (optional)
                        NamespacedKey hiddenKey = new NamespacedKey(plugin, "is_hidden");
                        meta.getPersistentDataContainer().set(hiddenKey, PersistentDataType.BYTE, (byte) (isHidden ? 1 : 0));

                        // Store badge data keys
                        meta.setLore(lore);

                        NamespacedKey idKey = new NamespacedKey(plugin, "badge_id");
                        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, badgeId);

                        NamespacedKey nameKey = new NamespacedKey(plugin, "badge_name");
                        meta.getPersistentDataContainer().set(nameKey, PersistentDataType.STRING, badgeName);

                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                        badgeItem.setItemMeta(meta);
                    }

                    badges.add(badgeItem);
                }
            }
        } catch (SQLException e) {
            PluginLogger.logException(null, e);
            badges.add(createDefaultBadge());
        }

        return badges;
    }

    /**
     * Deletes a badge from player's equipped badges in database.
     */
    public void deleteBadge(Player player, String badgeId) {
        try (Connection connection = DatabaseManager.getConnection()) {
            if (connection == null || connection.isClosed()) return;

            String deleteQuery = "DELETE FROM player_badges WHERE uuid = ? AND badge_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(deleteQuery)) {
                ps.setString(1, player.getUniqueId().toString());
                ps.setString(2, badgeId);

                int rowsAffected = ps.executeUpdate();
                if (rowsAffected > 0) {
                    MessageUtil.send(player, Component.text("Badge deleted successfully.", NamedTextColor.GREEN));
                    Bukkit.getScheduler().runTask(plugin, () -> openBadgeGUI(player, getBadges(player), 0));
                } else {
                    MessageUtil.send(player, Component.text("No badge found to delete.", NamedTextColor.RED));
                }
            }
        } catch (SQLException e) {
            PluginLogger.logException(null, e);
            MessageUtil.send(player, Component.text("Error deleting badge.", NamedTextColor.RED));
        }
    }

    /**
     * Prevent players from moving locked badges within the GUI.
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!event.getView().getTitle().startsWith(guiTitle)) return;

        // If any dragged item is locked, cancel event
        for (ItemStack item : event.getNewItems().values()) {
            if (item == null) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            NamespacedKey lockedKey = new NamespacedKey(plugin, "is_locked");
            Byte isLocked = meta.getPersistentDataContainer().get(lockedKey, PersistentDataType.BYTE);
            if (isLocked != null && isLocked == 1) {
                MessageUtil.send(player, Component.text("You cannot move locked badges.", NamedTextColor.RED));
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Prevent players from swapping locked badges with off-hand.
     */
    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        Inventory inv = player.getOpenInventory().getTopInventory();

        if (inv == null || !player.getOpenInventory().getTitle().startsWith(guiTitle)) return;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        if (isLockedBadge(mainHand) || isLockedBadge(offHand)) {
            MessageUtil.send(player, Component.text("You cannot swap locked badges.", NamedTextColor.RED));
            event.setCancelled(true);
        }
    }

    private boolean isLockedBadge(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        NamespacedKey lockedKey = new NamespacedKey(plugin, "is_locked");
        Byte isLocked = meta.getPersistentDataContainer().get(lockedKey, PersistentDataType.BYTE);
        return isLocked != null && isLocked == 1;
    }
}



