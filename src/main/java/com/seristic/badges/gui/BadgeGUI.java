package com.seristic.badges.gui;

import com.seristic.badges.util.Badge;
import com.seristic.badges.util.ConfigManager;
import com.seristic.badges.util.database.DatabaseManager;
import com.seristic.badges.util.helpers.MessageUtil;
import com.seristic.badges.util.helpers.PluginLogger;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.*;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BadgeGUI implements Listener, CommandExecutor {

    private final JavaPlugin plugin;

    private final int guiSize = ConfigManager.getGuiSize();
    private final String guiTitle = ChatColor.translateAlternateColorCodes('&', ConfigManager.getGuiTitle());
    private final int nextPageSlot = ConfigManager.getNextPageSlot();
    private final int prevPageSlot = ConfigManager.getPrevPageSlot();
    private final int maxPage = 4;
    private final int minPage = 0;
    private final int badgesPerPage = ConfigManager.getBadgesPerPage();
    private final Sound applyBadgeSound = ConfigManager.getApplyBadgeSound();
    private final float applyBadgeVolume = ConfigManager.getApplyBadgeSoundVolume();
    private final float applyBadgePitch = ConfigManager.getApplyBadgeSoundPitch();
    private final ItemStack blackGlass = createGlassPane();

    private final BukkitAudiences adventure;
    private BadgeGUI badgeGUI;

    public BadgeGUI(BukkitAudiences adventure, BadgeGUI badgeGUI, JavaPlugin plugin) {
        this.adventure = adventure;
        this.plugin = plugin;
     }

    public void openBadgeGUI(Player player, List<ItemStack> badges, int page) {
        if (page < minPage || page > maxPage) return;

        Inventory gui = Bukkit.createInventory(null, guiSize, guiTitle + " (Page " + (page + 1) + ")");

        for (int i = 0; i < guiSize; i++) {
            if (i < 9 || i >= guiSize - 9 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, blackGlass);
            }
        }

        int startIndex = page * badgesPerPage;
        int endIndex = Math.min(startIndex + badgesPerPage, badges.size());

        for (int i = startIndex, slot = 10; i < endIndex; i++, slot++) {
            gui.setItem(slot, badges.get(i));
        }

        if (page > minPage) {
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

        if (event.getView().getTitle().startsWith(guiTitle)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            int currentPage = Math.max(minPage, Math.min(maxPage, Integer.parseInt(event.getView().getTitle().replaceAll("[^0-9]", "")) - 1));

            if (event.getSlot() == nextPageSlot && currentPage < maxPage) {
                openBadgeGUI(player, getBadges(player), currentPage + 1);
                return;
            } else if (event.getSlot() == prevPageSlot && currentPage > minPage) {
                openBadgeGUI(player, getBadges(player), currentPage - 1);
                return;
            }

            ItemStack clickedItem = event.getCurrentItem();
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasLore() && !meta.getLore().isEmpty()) {
                String chatIconLore = meta.getLore().getFirst();
                String chatIcon = chatIconLore.replace("Chat Icon: ", "");
                applyBadgeWithLuckPerms(player, chatIcon);
            } else {
                MessageUtil.send(player, Component.text("Could not apply this badge. (Missing chat icon)", NamedTextColor.RED));
            }
        }
    }

    private void applyBadgeWithLuckPerms(Player player, String chatIcon) {
        player.playSound(player.getLocation(), applyBadgeSound, SoundCategory.MASTER, applyBadgeVolume, applyBadgePitch);
        BadgeManager.setBadge(player, chatIcon);
    }

    private ItemStack createGlassPane() {
        ItemStack item = new ItemStack(ConfigManager.getBorderMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', ConfigManager.getBorderName()));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNavItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createDefaultBadge() {
        ItemStack badge = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = badge.getItemMeta();
        if (meta != null) {
            Component displayName = Component.text("Default Badge")
                    .color(NamedTextColor.GRAY)
                    .decorate(TextDecoration.BOLD);

            meta.setDisplayName(GsonComponentSerializer.gson().serialize(displayName));

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

            String query = "SELECT badge_name, chat_icon FROM badges";

            try (PreparedStatement ps = connection.prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    String badgeName = rs.getString("badge_name");
                    String chatIcon = rs.getString("chat_icon");

                    if (chatIcon == null || chatIcon.trim().isEmpty()) continue;

                    Material iconMaterial;
                    try {
                        iconMaterial = Material.valueOf(chatIcon.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        iconMaterial = Material.NAME_TAG;
                    }

                    Badge badge = new Badge(badgeName, badgeName, chatIcon.trim(), NamedTextColor.GOLD);
                    ItemStack badgeItem = new ItemStack(iconMaterial);
                    ItemMeta meta = badgeItem.getItemMeta();

                    if (meta != null) {
                        meta.setDisplayName(badgeName);
                        List<String> lore = new ArrayList<>();
                        lore.add("Chat Icon: [ " + chatIcon.trim() + " ]");
                        lore.add("Click to Apply!");
                        meta.setLore(lore);

                        NamespacedKey key = new NamespacedKey(plugin, "badge_name");
                        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, badge.getName());

                        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                        badgeItem.setItemMeta(meta);
                    }
                    badges.add(badgeItem);
                }
            }
        } catch (SQLException e) {
            badges.add(createDefaultBadge());
            PluginLogger.logException(null, e);
        }
        return badges;
    }

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
                deleteBadge(player, args[0]);
                return true;
            } else {
                MessageUtil.send(player, Component.text("Usage: /badge delete <badgeId>", NamedTextColor.RED));
                return false;
            }
        }
        return false;
    }
}
