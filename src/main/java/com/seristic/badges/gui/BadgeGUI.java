package com.seristic.badges.gui;

import com.seristic.badges.util.Badge;
import com.seristic.badges.util.database.DatabaseManager;
import com.seristic.badges.util.helpers.MessageUtil;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BadgeGUI implements Listener, CommandExecutor {

    private final JavaPlugin plugin;

    private static final int GUI_SIZE = 27;
    private static final int NEXT_PAGE_SLOT = 26;
    private static final int PREV_PAGE_SLOT = 18;
    private static final String GUI_TITLE = "Badge Selection";
    private static final ItemStack BLACK_GLASS = createGlassPane();
    private static final int MAX_PAGE = 4;
    private static final int MIN_PAGE = 0;

    private final BukkitAudiences adventure;
    private BadgeGUI badgeGUI;

    public BadgeGUI(BukkitAudiences adventure, BadgeGUI badgeGUI, JavaPlugin plugin) {
        this.adventure = adventure;
        this.badgeGUI = badgeGUI;
        this.plugin = plugin;

    }

    public void openBadgeGUI(Player player, List<ItemStack> badges, int page) {
        if (page < MIN_PAGE || page > MAX_PAGE) return;

        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE + " (Page " + (page + 1) + ")");

        for (int i = 0; i < GUI_SIZE; i++) {
            if (i < 9 || i >= GUI_SIZE - 9 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, BLACK_GLASS);
            }
        }

        int startIndex = page * 7;
        int endIndex = Math.min(startIndex + 7, badges.size());

        for (int i = startIndex, slot = 10; i < endIndex; i++, slot++) {
            gui.setItem(slot, badges.get(i));
        }

        if (page > MIN_PAGE) {
            gui.setItem(PREV_PAGE_SLOT, createNavItem(Material.ARROW, "Previous Page"));
        }
        if (page < MAX_PAGE) {
            gui.setItem(NEXT_PAGE_SLOT, createNavItem(Material.ARROW, "Next Page"));
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        // Ensure that the event is happening in the BadgeGUI
        if (event.getView().getTitle().startsWith(GUI_TITLE)) {
            event.setCancelled(true);  // Prevent any interaction with the GUI (removal, movement, etc.)

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            // Handle Navigation Buttons (Next and Previous Page)
            String title = event.getView().getTitle();
            int currentPage = Math.max(MIN_PAGE, Math.min(MAX_PAGE, Integer.parseInt(title.replaceAll("[^0-9]", "")) - 1));

            if (event.getSlot() == NEXT_PAGE_SLOT && event.getCurrentItem().getType() == Material.ARROW && currentPage < MAX_PAGE) {
                openBadgeGUI(player, getBadges(player), currentPage + 1);
                return;
            } else if (event.getSlot() == PREV_PAGE_SLOT && event.getCurrentItem().getType() == Material.ARROW && currentPage > MIN_PAGE) {
                openBadgeGUI(player, getBadges(player), currentPage - 1);
                return;
            }

            // Handle Badge Selection
            ItemStack clickedItem = event.getCurrentItem();
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasLore() && !meta.getLore().isEmpty()) {
                // Extract chat icon from lore
                String chatIconLore = meta.getLore().get(0);
                String chatIcon = chatIconLore.replace("Chat Icon: ", ""); // Remove prefix

                applyBadgeWithLuckPerms(player, chatIcon);
            } else {
                MessageUtil.send(player, Component.text("Could not apply this badge. (Missing chat icon)", NamedTextColor.RED));
            }
        }
    }

    private void applyBadgeWithLuckPerms(Player player, String chatIcon) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0F, 1.5F);
        BadgeManager.setBadge(player, chatIcon);
    }

    private static ItemStack createGlassPane() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNavItem(Material arrow, String name) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createDefaultBadge() {
        ItemStack badge = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = badge.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§7§lDefault Badge");

            List<String> lore = new ArrayList<>();
            lore.add("Chat Icon: [ ✦ ]");
            meta.setLore(lore);

            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            badge.setItemMeta(meta);
        }
        return badge;
    }

    public List<ItemStack> getBadges(Player player) {
        List<ItemStack> badges = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection()) {
            if (connection == null || connection.isClosed()) {
                badges.add(createDefaultBadge());
                return badges;
            }

            String query = "SELECT badge_name, badge_icon, chat_icon, description FROM badges";  // Make sure the description is fetched too

            try (PreparedStatement ps = connection.prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    String badgeName = rs.getString("badge_name");
                    String badgeIcon = rs.getString("badge_icon");
                    String chatIcon = rs.getString("chat_icon");
                    String description = rs.getString("description");

                    if (chatIcon == null || chatIcon.trim().isEmpty()) continue; // Skip badges without a chat icon

                    Material iconMaterial;
                    try {
                        iconMaterial = Material.valueOf(badgeIcon.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        iconMaterial = Material.NAME_TAG;  // Fallback to NAME_TAG if invalid
                    }

                    // Create the Badge object
                    Badge badge = new Badge(badgeName, badgeName, chatIcon.trim(), NamedTextColor.GOLD, description);

                    // Create ItemStack for the badge
                    ItemStack badgeItem = new ItemStack(iconMaterial);
                    ItemMeta meta = badgeItem.getItemMeta();
                    if (meta != null) {
                        // Set the display name (this can use a Component if you're using adventure, or a string)
                        meta.setDisplayName(badgeName);  // Use just the badge name for display name

                        // Set lore (tooltip) with chat icon and description
                        List<String> lore = new ArrayList<>();
                        lore.add("Chat Icon: [ " + chatIcon.trim() + " ]");  // Add the chat icon lore
                        lore.add("Description: " + description);  // Add the description lore
                        lore.add("Click to Apply!");  // Optional additional lore
                        meta.setLore(lore);

                        // Optionally add enchantments, flags, etc.
                        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);  // Hide enchantments from view

                        badgeItem.setItemMeta(meta);  // Apply the ItemMeta to the ItemStack
                    }

                    badges.add(badgeItem);  // Add the badge item to the list
                }
            }
        } catch (SQLException e) {
            badges.add(createDefaultBadge());  // Return default badge in case of an error
            e.printStackTrace();
        }

        return badges;
    }

    public void deleteBadge(Player player, String badgeId) {
        try (Connection connection = DatabaseManager.getConnection()) {
            if (connection == null || connection.isClosed()) {
                return;
            }
            String deleteQuery = "DELETE FROM player_badges WHERE uuid = ? AND badge_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(deleteQuery)) {
                ps.setString(1, player.getUniqueId().toString());
                ps.setString(2, badgeId);

                int rowsAffected = ps.executeUpdate();
                if (rowsAffected > 0) {
                    MessageUtil.send(player, Component.text("Badge deleted successfully.", NamedTextColor.GREEN));
                    Bukkit.getScheduler().runTask(plugin, () -> openBadgeGUI(player, getBadges(player), 0));
                } else {
                    MessageUtil.send(player, Component.text("Failed to delete the badge.", NamedTextColor.RED));
                }
            }
        } catch (SQLException e) {
            MessageUtil.send(player, Component.text("Error occurred while deleting the badge", NamedTextColor.RED));
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player player) {
            if (args.length == 1) {
                String badgeId = args[0];
                deleteBadge(player, badgeId);  // Call the delete method to delete the badge
                return true;
            } else {
                MessageUtil.send(player, Component.text("Usage: /badge delete <badgeId>", NamedTextColor.RED));
                return false;
            }
        }
        return false;
    }
}
