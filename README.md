# 🏷️ ChatBadges

**ChatBadges** is a fully customizable Minecraft plugin that allows players to collect, manage, and display badges in chat using GUI interfaces, commands, and database integration.

> Built with ❤️ for community servers, content creators, roleplay worlds, and achievement hunters.

---

## 📦 Features

- 🎨 Custom badges with colors, icons, and hoverable chat tags
- 🧍 Player badge management GUI (`/badge`)
- 🧑‍🤝‍🧑 Group badges with sharing and ownership features
- 🛠️ Admin commands to create, edit, and delete badges
- 🧾 MySQL support for persistent badge storage
- 🔐 Permission-based access control
- 🔄 Reloadable configuration + badge sync

---

## 🚀 Getting Started

### ✅ Requirements

- Minecraft 1.21.4
- Java 21+
- Spigot/Paper server
- A MySQL-compatible database

### 📥 Installation

1. Download the latest release from the [Releases Page](https://github.com/yourname/ChatBadges/releases).
2. Drop the `ChatBadges.jar` into your server’s `plugins/` directory.
3. Configure your database settings in `plugins/ChatBadges/config.yml`.
4. Start (or restart) your server.
5. Use `/badge` in-game!

---

## 💻 Commands & Permissions

### 👤 Player Commands

| Command                    | Description                             | Permission              |
|---------------------------|-----------------------------------------|--------------------------|
| `/badge`                  | Open your badge GUI                     | `chatbadges.use`         |
| `/badge set [name]`       | Set your badge                          | `chatbadges.set`         |
| `/badge remove`           | Remove current badge                    | `chatbadges.set`         |
| `/badge owned`            | View your owned badges                  | `chatbadges.use`         |

### 🛠️ Admin Commands

| Command                              | Description                                       | Permission               |
|--------------------------------------|---------------------------------------------------|---------------------------|
| `/badge create <name> <color> <icon> <chatIcon> <hoverText...>` | Create a new badge     | `chatbadges.admin`        |
| `/badge delete <name>`              | Delete a badge                                    | `chatbadges.admin`        |
| `/badge give <badge> <player>`     | Grant badge to player                             | `chatbadges.give`         |
| `/badge take <badge> <player>`     | Remove badge from player                          | `chatbadges.take`         |
| `/badge reload`                     | Reloads config and badge data                     | `chatbadges.admin`        |

---

## 🧠 Planned Features

See [TODO.md](TODO.md) for the full roadmap.

- PlaceholderAPI support
- Badge categories (seasonal, event, custom)
- GUI theme and layout customization
- Badge achievement triggers (kills, playtime, quests)
- Discord integration
- Web dashboard for badge management

---

## 🧬 Configuration

```yaml
settings:
  assign-default-badge: true
  default-badge: "[ ✦ ]"

mysql:
  host: "localhost"
  port: 3306
  database: "badges"
  username: "root"
  password: ""