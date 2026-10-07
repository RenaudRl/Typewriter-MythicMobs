# MythicMobs Extension

![Java Version](https://img.shields.io/badge/Java-21-orange)
![Target](https://img.shields.io/badge/Target-Paper-blue)
![Typewriter](https://img.shields.io/badge/Typewriter-0.9.0--beta--177-purple)

**MythicMobs Extension** connects **Typewriter** to the MythicMobs plugin: spawn mobs, cast skills, react to kills, and build kill objectives and region spawners from the web editor.

---

## 🚀 Key Features

- **Actions**: spawn a MythicMobs mob, despawn it, execute a MythicMobs skill.
- **Events**: a player kills a MythicMobs mob, a player interacts with one, a MythicMobs mob kills a player.
- **Facts**: faction, level and stance of a mob, and the count of active mobs of a type.
- **Cinematics**: spawn a MythicMob or trigger a MythicSkill during a cinematic.
- **Objective**: kill MythicMobs mobs (needs the Quest extension).
- **Region spawner** (`mythic_spawner`): spawns a mob in regions defined by two corners, with max mobs, mobs per cycle, cooldown and warmup (fixed values or facts), a player activation range and `criteria`. The optional `group` limits activation to members of that group: the criteria are checked on that group, and each group has its own warmup and cooldown.

---

## 📦 Entries

| Category | Entries |
|---|---|
| Actions | `spawn_mythicmobs_mob`, `despawn_mythicmobs_mob`, `execute_mythicmob_skill` |
| Events | `on_mythic_mob_die`, `mythicmobs_interact_event`, `mythicmobs_kill_player_event` |
| Facts | `mythic_mob_count_fact`, `mythicmob_faction`, `mythicmob_level`, `mythicmob_stance` |
| Cinematics | `mythicmob_cinematic`, `mythicskill_cinematic` |
| Objective | `mythicmob_kill_objective` |
| Static | `mythic_spawner` |

Full field reference on the [wiki](https://docs.borntocraftstudio.net/extensions/free/mythicmobs/).

---

## 🧩 Requirements

- Typewriter engine `0.9.0-beta-177`, on **Paper**.
- **MythicMobs** plugin (API 5.11.2 at build time).
- Typewriter **Quest** extension.

---

## 🛠 Building

Requires **Java 21**.

```bash
git clone https://github.com/RenaudRl/Typewriter-MythicMobs.git
cd Typewriter-MythicMobs
./gradlew clean build
```

Artifact: `build/libs/`.

---

## 🤝 Credits
- **[Typewriter](https://github.com/gabber235/Typewriter)**: the engine this extension is built for.
- **[BTC Studio](https://github.com/RenaudRl)**: maintenance.

## Documentation

[BTC Studio Docs](https://docs.borntocraftstudio.net/extensions/free/mythicmobs/)

---

## 📜 License

**GNU General Public License v3.0 or later** — [LICENSE](LICENSE) — with a
**linking exception** for the Typewriter engine — [LICENSE-EXCEPTION.md](LICENSE-EXCEPTION.md).

| | |
|---|---|
| You may | Run it anywhere, **including on a monetised server**. Study it, modify it, use it as a base, and redistribute it — **even for a fee**. GPLv3 §4 explicitly allows charging for a copy. |
| You must | Publish the complete corresponding source of your version under GPLv3, preserve the copyright notices, and **state that you modified it and when** (§5(a)). |
| You may not | Ship a closed-source or proprietary version, relicense under stricter terms, or strip the attribution and present this work as your own — §8 terminates your rights automatically. |
| Marks | **"Born To Craft"** and **"BTC Studio"** are **not** covered by the GPL. Fork it freely, sell your fork if you like — but **rebrand it**. |

> Reselling this code is legally allowed and practically pointless: whoever buys a
> copy from you receives, under the GPL, the right to redistribute it for free.
> That is the protection — not a clause forbidding sale, which the GPL does not
> permit us to add.

### About Typewriter

This is a **third-party extension**. It uses the public extension API of the
[Typewriter](https://github.com/gabber235/Typewriter) engine by gabber235 and
contains none of its source. Born To Craft Studio is not affiliated with or
endorsed by the Typewriter project.

The engine itself is **not** free software — its licence forbids redistributing
it. **Get it from the Typewriter project, and never redistribute it**, including
inside a fork of this repository.

Full attribution, the statement of modifications required by §5(a), and the
trademark reservation are in **[NOTICE.md](NOTICE.md)**. Read it before
redistributing.

© 2026 Born To Craft Studio.
