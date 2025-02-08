package com.seristic.chatBadges.util.gui;

import com.seristic.chatBadges.util.database.DatabaseManager;
import com.seristic.chatBadges.util.listener.BadgeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BadgeGUI implements Listener, CommandExecutor {
    private static final int GUI_SIZE = 27;
    private static final int NEXT_PAGE_SLOT = 26;
    private static final int PREV_PAGE_SLOT = 18;
    private static final String GUI_TITLE = "Badge Selection";
    private static final ItemStack BLACK_GLASS = createGlassPane();
    private static final int MAX_PAGE = 4;
    private static final int MIN_PAGE = 0;

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

        if (event.getView().getTitle().startsWith(GUI_TITLE)) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            String title = event.getView().getTitle();
            int currentPage = Math.max(MIN_PAGE, Math.min(MAX_PAGE, Integer.parseInt(title.replaceAll("[^0-9]", "")) - 1));

            // Navigation buttons
            if (event.getSlot() == NEXT_PAGE_SLOT && event.getCurrentItem().getType() == Material.ARROW && currentPage < MAX_PAGE) {
                openBadgeGUI(player, getBadges(player), currentPage + 1);
                return;
            } else if (event.getSlot() == PREV_PAGE_SLOT && event.getCurrentItem().getType() == Material.ARROW && currentPage > MIN_PAGE) {
                openBadgeGUI(player, getBadges(player), currentPage - 1);
                return;
            }

            // Treat the clicked item as a badge selection.
            ItemStack clickedItem = event.getCurrentItem();
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasLore() && !meta.getLore().isEmpty()) {
                // Extract chat icon from lore
                String chatIconLore = meta.getLore().get(0);
                String chatIcon = chatIconLore.replace("Chat Icon: ", ""); // Remove prefix

                applyBadgeWithLuckPerms(player, chatIcon);
            } else {
                player.sendMessage(Component.text("Could not apply this badge. (Missing chat icon)", NamedTextColor.RED));
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

    private ItemStack createNavItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
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
            meta.displayName(Component.text("Default Badge")
                    .color(NamedTextColor.GRAY)
                    .decorate(TextDecoration.BOLD));

            // Default chat icon, always formatted with [ ]
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
        Connection connection = DatabaseManager.getConnection();
        if (connection == null) {
            System.out.println("[ChatBadges] No database connection available!");
            badges.add(createDefaultBadge());
            return badges;
        }

        String query = "SELECT badge_name, badge_icon, chat_icon FROM badges";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String badgeName = rs.getString("badge_name");
                String badgeIcon = rs.getString("badge_icon");
                String chatIcon = rs.getString("chat_icon");

                // Ensure chatIcon is formatted as [ Icon ]
                String formattedChatIcon = chatIcon != null ? "[ " + chatIcon + " ]" : "[ ✦ ]";

                // Convert badgeIcon to a valid Material
                Material iconMaterial;
                try {
                    iconMaterial = Material.valueOf(badgeIcon.toUpperCase());
                } catch (Exception e) {
                    iconMaterial = Material.NAME_TAG; // Fallback if invalid
                }

                ItemStack badge = new ItemStack(iconMaterial);
                ItemMeta meta = badge.getItemMeta();
                if (meta != null) {
                    meta.displayName(Component.text(badgeName)
                            .color(NamedTextColor.RED)
                            .decorate(TextDecoration.BOLD));

                    // Add chat icon to lore
                    List<String> lore = new ArrayList<>();
                    lore.add("Chat Icon: " + formattedChatIcon);
                    meta.setLore(lore);

                    meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    badge.setItemMeta(meta);
                }
                badges.add(badge);
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (badges.isEmpty()) {
            badges.add(createDefaultBadge());
        }
        return badges;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            openBadgeGUI(player, getBadges(player), 0);
            return true;
        }
        return false;
    }
}
