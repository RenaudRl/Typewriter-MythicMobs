# MythicMobs Extension

![Java Version](https://img.shields.io/badge/Java-21-orange)
![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Target](https://img.shields.io/badge/Target-Paper%20/%20Folia%20/%20BTC--CORE-blue)

**MythicMobs Extension** is a powerful integration module for **TypeWriter**, designed for **BTC Studio** infrastructure. It bridges the gap between TypeWriter's action system and the MythicMobs engine, allowing for deep interaction with custom mobs and skills.

---

## 🚀 Key Features

### ⚔️ Action System
- **Spawn Mob**: Programmatically spawn MythicMobs at specific locations.
- **Despawn Mob**: Efficiently remove MythicMobs from the world.
- **Execute Skill**: Trigger complex MythicMobs skills through TypeWriter actions.

### 🎭 Events & Interactions
- **Death Events**: React to MythicMob deaths.
- **Kill Events**: Detect and process when a player is killed by a MythicMob.
- **Interactions**: Handle player-to-mob interaction events seamlessly.
- **Region Spawner**: Set MythicMobs to spawn in defined regions(corners).

### 📊 Facts & Placeholders
- **Faction Tracking**: Check mob factions for conditioned logic.
- **Leveling**: Integrate with MythicMob levels.
- **Stance System**: Read and react to mob stances.
- **Mob Counters**: Monitor the count of specific mobs in given areas.

### 🎬 Cinematics
- **MythicMob Cinematics**: Use MythicMobs as actors in TypeWriter cinematic sequences.
- **Skill Cinematics**: Synchronize complex skill executions with cinematic timing.

---

## ⚙️ Configuration

The MythicMobs Extension configuration is managed through TypeWriter's manifest system. It requires the MythicMobs plugin to be present on the server.

## 🛠 Building & Deployment

Requires **Java 21**.

```bash
# Clone the repository
git clone https://github.com/RenaudRl/Typewriter-MythicMobs.git
cd Typewriter-MythicMobs

# Build the project
./gradlew clean build
```

### Artifact Locations:
- `build/libs/Typewriter-MythicMobs-[Version].jar`

---

## 🤝 Credits & Inspiration
- **[TypeWriter](https://github.com/gabber235/Typewriter)** - The engine this extension is built for.
- **[BTC Studio](https://github.com/RenaudRl)** - Maintenance and specialized optimizations.

---

## 📜 License
Licensed under the **MIT License**.
