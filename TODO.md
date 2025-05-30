# 📝 Badges TODO List

This file contains the current development goals and improvements for the Badges plugin.

---

## ✅ Completed
- [x] Basic badge GUI with pagination
- [x] /badge create command
- [x] Badge database integration
- [x] Adventure API integration
- [x] Implement /badge delete logic
- [x] Hook delete into main command handler
- [x] Hook permissions into badge access (e.g., only admins can give/share)
- [x] Support multi-page GUI navigation for users with many badges
- [x] Replace `e.printStackTrace();` with proper error logging custom made.
- [x] Error handling + logging in database operations
---

## 🛠️ In Progress
- [x] Dynamic tab completion for subcommands
- [x] Improve command dispatcher to fully decouple subcommands (e.g., delete, give, create)
- [x] Proper permission checks for each command (e.g., `chatbadges.give`, `chatbadges.create`)
- [x] Add config options for GUI customization (title, colors, layout)
- [x] Hover tooltips in GUI showing who created the badge
## 🧠 Future Features
- [ ] Badge categories (e.g., earned, seasonal, group)
- [ ] JSON-based badge import/export system
- [ ] Player profile pages showing all earned badges
- [ ] PlaceholderAPI and Vault integration
- [ ] Config option to show badges above player name (scoreboard/team?)
- [ ] Badge update cache to prevent excessive SQL calls
- [ ] Add a reload command that refreshes config + GUI templates without restart
- [ ] Split GUI into views: `My Badges`, `Group Badges`, `All Badges`
- [ ] Profile command (`/badge profile [player]`) showing all badges owned
- [ ] Player achievements or milestones that grant badges
- [ ] Export/import badges via JSON/YAML for migration
- [ ] PlaceholderAPI support for badge display in chat/tab/scoreboard
- [ ] Discord integration: sync badge announcements to channels (Very unlikely)
- [ ] Badge sorting options (alphabetical, rarity, date earned)
- [ ] Group badges with sharing and ownership features

---

## 🐞 Known Bugs
- [ ] Console confirmation message is being overwritten with the incorrect string from formatBadge `String actionText = "Unequipped badge: ";`
- [ ] Breaks TownyChat
