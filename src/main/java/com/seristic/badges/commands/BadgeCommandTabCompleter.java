package com.seristic.badges.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BadgeCommandTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "create", "delete"
    );
    private static final List<String> COLOURS = Arrays.asList(
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold",
            "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white"
    );
    private static final List<String> BOOLEAN_OPTIONS = Arrays.asList("true", "false");

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // Step 1: Handle subcommands
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            completions = SUBCOMMANDS.stream()
                    .filter(cmd -> cmd.startsWith(input))
                    .collect(Collectors.toList());
        }

        // Step 2: Badge name
        if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            completions.add("<badgeName>");
        }

        // Step 3: Color
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            String input = args[2].toLowerCase();
            completions = COLOURS.stream()
                    .filter(color -> color.startsWith(input))
                    .collect(Collectors.toList());
        }

        // Step 4: Material icon
        if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            String input = args[3].toLowerCase();
            completions = Arrays.stream(Material.values())
                    .map(Material::name)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }

        // Step 5: Chat icon (e.g. ❀, ❤️)
        if (args.length == 5 && args[0].equalsIgnoreCase("create")) {
            completions.add("<chatIcon>");
        }

        // Step 6: Hover text
        if (args.length == 6 && args[0].equalsIgnoreCase("create")) {
            completions.add("<hoverText>");
        }

        // Step 7: requiresPermission (permission:true/false)
        if (args.length == 7 && args[0].equalsIgnoreCase("create")) {
            String input = args[6].toLowerCase().trim();
            completions = BOOLEAN_OPTIONS.stream()
                    .map(val -> "permission:" + val)
                    .filter(opt -> opt.startsWith(input))
                    .collect(Collectors.toList());
        }

        // Step 8: isHidden (hidden:true/false)
        if (args.length == 8 && args[0].equalsIgnoreCase("create")) {
            String input = args[7].toLowerCase().trim();
            completions = BOOLEAN_OPTIONS.stream()
                    .map(val -> "hidden:" + val)
                    .filter(opt -> opt.startsWith(input))
                    .collect(Collectors.toList());
        }

        // Step 9: Badge group
        if (args.length == 9 && args[0].equalsIgnoreCase("create")) {
            completions.add("<badge_group>");
        }

        return completions;
    }
}
