<div align="center">

# Crashy!

### Glue your build together. Switch on physics. Throw it at something.

<a href="https://modrinth.com/mod/crashy"><img alt="Available on Modrinth" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.3.1/assets/cozy/available/modrinth_vector.svg"></a>
<a href="https://neoforged.net/"><img alt="Works with NeoForge" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.3.1/assets/cozy/supported/neoforge_vector.svg"></a>
<a href="https://modrinth.com/mod/sable"><img alt="Powered by Sable" height="56" src="https://cdn.modrinth.com/data/cached_images/d13ac17ef50ed090e7d21fcae0caf8958eeece3d.png"></a>

**English** · [Русский](README.ru.md)

</div>

---

Crashy! is a survival-friendly toolkit for **[Sable](https://modrinth.com/mod/sable)**, the
sub-level physics library. Sable gives Minecraft real rigid-body physics but deliberately adds no
way to use it in-game — Crashy! is that way. Five items and blocks, one gameplay loop, and a lot of
rubble.

> [!IMPORTANT]
> **Sable is required.** Crashy! does nothing on its own. Install Sable and its own dependencies
> alongside it.

---

## The loop

**1. Glue it.** Hold **Construction Glue** in your off-hand and build normally — every block you
place is bonded to the ones it touches. Glued blocks glow with an outline while the glue is in your
hand, so you always see exactly what counts as "the build".

**2. Switch on physics.** Put a **Physics Activator** on the build and right-click it with an empty
hand, or feed it redstone. The whole glued group becomes one real rigid body: it falls, it tumbles,
it collides with the world.

**3. Launch it.** Right-click the object with the **Launcher** to pick it up — it floats in front of
you. Hold right-click to charge up to **power 10**, let go, and watch it go.

**4. Watch it come apart.** Whatever it hits shatters into loose physics blocks, and so does the
object itself. The debris carries the momentum of the crash, arcs away from the impact, bounces,
and eventually settles back into the world as ordinary blocks.

---

## The tools

### Construction Glue

Bonds blocks into one rigid structure. Glue is stored per-world and survives reloads.

| Action | Result |
| --- | --- |
| Hold in the **off-hand** while building | every block you place is glued and merged with the glued blocks it touches |
| **Right-click** a block | glue that single block |
| **Sneak + right-click** a glued block | dissolve the whole group |

### Physics Activator

Place it on a finished build and switch it on with an **empty-hand right-click** or a **redstone
signal**. The glued group it touches becomes a live physics object.

If nothing nearby is glued, the activator falls back to gathering every physically connected block
— that can be turned off in the config.

To put an object **back into the world**, right-click it with the activator *item*. Physics switches
off, the build snaps to the block grid, and it stays glued so you can activate it again.

### Launcher

| Action | Result |
| --- | --- |
| **Right-click** a physics object | grab it — the object floats in front of you |
| **Hold right-click** | charge up, power climbs from 1 to **10** (shown in the action bar) |
| **Release** | launch along your line of sight |
| **Sneak + right-click** | put the object down |

Power 10 is roughly 80 m/s by default.

### Build Saver and Blueprint

| Action | Result |
| --- | --- |
| **Right-click** the saver with an empty hand | record the build attached to it |
| **Right-click** the saver with a **Blueprint** | copy the build onto the blueprint |
| **Right-click** a block face with a Blueprint | stamp the build out, already glued |
| **Sneak + right-click** | wipe the saver / wipe the blueprint |

Blueprints are reusable, and pasting skips occupied spots rather than overwriting somebody else's
build.

---

<div align="center">

### Enjoying Crashy?

<a href="https://ko-fi.com/pycodder"><img alt="Support me on Ko-fi" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.3.1/assets/cozy/donate/kofi-plural_vector.svg"></a>

</div>

---

## Destruction settings — press F7

**F7** (rebindable in the controls menu) opens the world's destruction panel.

| Mode | What happens |
| --- | --- |
| **Instant** | Everything inside the blast radius shatters, whatever it is made of. |
| **Realistic** | Impact force is measured against each block's blast resistance. A power-8 hit scatters planks across the whole crater but only chips cobblestone near the middle, and obsidian shrugs it off entirely. |
| **Indestructible** | Nothing breaks. Objects still fly and bounce off the world. |

Plus sliders for block toughness, crater size, debris scatter and launch speed, and toggles for
world destruction, TNT blasts and debris settling.

Settings live **in the world** and are shared by everyone on the server. Only operators — or the
host of a single-player world — can change them; everyone else sees the panel greyed out.

---

## Impacts

When a launched object ploughs into ordinary blocks:

- **the object itself breaks apart** — each shard becomes its own physics body and carries the
  momentum of the crash;
- **the blocks it hit break the same way** — crater size scales with speed and launch power;
- a custom impact sound plays, and real block shrapnel, a ground-hugging dust ring and sparks come
  with it;
- once the debris comes to rest it **turns back into ordinary world blocks**, because otherwise
  every shard would stay a live rigid body forever and the server would grind to a halt.

An object too big to disintegrate does not explode into confetti — only the region that actually
took the hit breaks off. That is both cheaper and more convincing.

## TNT

Fly an object into TNT and the charge goes off **instead of** a vanilla explosion. The block is
removed by hand, so nothing gets deleted: the surrounding blocks survive and are thrown much further
instead. Nearby TNT chains, up to 24 charges per crash.

---

## Installation

1. Minecraft **1.21.1** with **[NeoForge](https://neoforged.net/) 21.1.0+**
2. **[Sable](https://modrinth.com/mod/sable) 2.0.0+** and its dependencies (Veil, Forge Config API
   Port)
3. Drop `crashy-neoforge-1.21.1-<version>.jar` into `mods`

Sable is an intrusive, mixin-heavy mod and can conflict with other mods that touch world rendering
or chunk handling. If something breaks, check whether Sable alone reproduces it before opening an
issue here.

## Building from source

```bash
./gradlew build
```

The jar lands in `build/libs/`. Needs JDK 21 — Gradle provisions one automatically if the system
does not have it — and roughly 5 GB of free disk for the Minecraft/NeoForge cache.

## Configuration

Most settings live on **F7**, alongside the destruction mode. The file
`config/crashy-server.toml` holds the hard limits and the starting values for new worlds.

| Section | Controls |
| --- | --- |
| `glue` | maximum size of a glued group |
| `activator` | block limit per object, fallback to connected blocks |
| `blueprint` | block limit per blueprint |
| `launcher` | hold distance, speed per power level, charge rate |
| `destruction` | minimum impact speed, maximum crater radius, debris limits |
| `tnt` | radius, scatter and breaking force of a TNT blast |
| `debris` | whether debris settles back into blocks, and after how long |

If shards look like they "snap back" a second after the crash, raise `debrisScatter`: a shard that
never moves counts as settled and gets written straight back where it broke off.

The limits (`maxWorldBlocksDestroyed`, `maxDebrisPerImpact`) are there for a reason — every shard is
a separate rigid body in Sable's physics pipeline.

---

## Project layout

```
dev.pycodder.crashy
├── Crashy              entry point, registration
├── CrashyConfig        server-side config
├── glue/               glue item, events, and the per-world SavedData behind it
├── block/              physics activator block, plus the item that switches physics off
├── launcher/           launcher item and the grab/charge/launch controller
├── structure/          build saver, blueprints, StructureData, StructureScanner
├── settings/           destruction modes and world-owned settings
├── network/            glue highlight and settings sync payloads
├── client/             glued-block outlines, F7 screen, keybinds
├── physics/
│   ├── SableBridge     the only place that talks to Sable's API
│   ├── PhysicsActivation  assembling a build into an object, and back
│   ├── ImpactTracker   tracking launched objects and detecting the hit
│   ├── Destruction     shattering the object and the world, detonating TNT
│   ├── CrashyEffects   every particle
│   └── DebrisManager   returning debris to the world
└── registry/           items, blocks, block entities, sounds, components, tab
```

## Licence

MIT — see [LICENSE](LICENSE).

---

<div align="center">

Made by **pycodder** · Physics by **[Sable](https://modrinth.com/mod/sable)** from RyanHCode

<a href="https://ko-fi.com/pycodder"><img alt="Support me on Ko-fi" height="46" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.3.1/assets/cozy/donate/kofi-plural_vector.svg"></a>

</div>
