# Crashy! 1.0

First release. Minecraft 1.21.1, NeoForge.

Crashy! is a survival toolkit for [Sable](https://modrinth.com/mod/sable), the sub-level physics
library. Sable gives Minecraft real rigid-body physics but deliberately adds no way to reach it
in-game — this is that way. Glue a build together, switch on physics, then pick it up and throw it
at something.

---

## What's in it

**Construction Glue** — bonds blocks into one rigid structure. Hold it in your off-hand and every
block you place is glued to the ones it touches; glued blocks are outlined while the glue is in
your hand. Glue is stored per-world and survives reloads.

**Physics Activator** — turns a glued build into a live rigid body, on an empty-hand right-click or
a redstone signal. Right-click an existing object with the item form to switch physics back off:
the build settles into the world and stays glued, so you can activate it again.

**Launcher** — right-click an object to grab it, hold to charge to power 10, release to fire. Full
charge throws at about 80 m/s.

**Build Saver + Blueprint** — a table that records the build attached to it, and a reusable
blueprint that stamps it out anywhere, already glued. Pasting skips occupied spots instead of
overwriting them.

**Destruction settings on F7** — three modes, changeable at any time:

- **Instant** — everything inside the blast radius shatters, whatever it is made of
- **Realistic** — impact force is measured against each block's blast resistance, so a power-8 hit
  scatters planks across the whole crater but only chips cobblestone near the middle, and obsidian
  is untouched
- **Indestructible** — nothing breaks; objects still fly and bounce

Plus sliders for block toughness, crater size, debris scatter and launch speed, and toggles for
world destruction, TNT and debris settling. Settings live in the world and are shared by everyone;
only operators, or the host of a single-player world, can change them.

**TNT** — a crash sets off any TNT it reaches, *instead of* a vanilla explosion. The block is
removed by hand, so the surroundings are not deleted: they survive and get thrown much further.
Chains up to 24 charges per crash.

**Impacts** — custom crash audio in three variants, shrapnel made of the blocks that actually
broke, a ground-hugging dust ring, and sparks that scale with the hit. Debris settles back into
ordinary world blocks once it comes to rest.

Recipes for everything, and English and Russian translations.

---

## Requirements

- Minecraft **1.21.1**
- **NeoForge 21.1.0+**
- **[Sable](https://modrinth.com/mod/sable) 2.0.0+** and its dependencies (Veil, Forge Config API Port)

**Needed on both the client and the server, and the versions have to match.** Crashy! registers
blocks, items and network channels, so a client without it — or with a different version — will be
turned away at the handshake.

---

## Worth knowing before you install

**Sable is intrusive.** It makes heavy use of mixins and is prone to conflicts with mods that touch
world rendering or chunk handling. If something breaks, check whether Sable on its own reproduces
it before reporting it here.

**Switching physics off snaps the build to the grid.** Positions are rounded and block states are
kept exactly as they were, so a tilted object comes back upright and, say, a staircase keeps the
facing it had before launch. Matching block rotation to an arbitrary quaternion would be guesswork,
and a build that returns subtly rearranged is worse than one that returns straight. Anything with
nowhere to land drops as an item rather than vanishing.

**Large objects only break where they were hit.** Past the debris cap, the impact region breaks off
and the rest stays one body. That is deliberate — cheaper, and it looks better than a tower
exploding into confetti.

**There are limits, and they matter.** Every shard is a separate rigid body in Sable's pipeline.
The defaults cap a single crash at 220 world blocks and 320 debris pieces, an object at 8192
blocks, and a blueprint at 4096. Raising them a lot on a busy server will cost you tick time.

**Debris settling is on by default and should stay on.** Turning it off leaves every shard as a
live physics body forever.

**The glue outline covers 28 blocks around you, up to 2048 blocks at a time.** A very large glued
structure will not be highlighted all at once.

---

## Configuration

Most of it is on **F7**. The file `config/crashy-server.toml` holds the hard limits and the starting
values for new worlds — glue group size, blocks per object, blueprint size, hold distance and charge
rate, impact threshold and crater radius, TNT blast figures, and how long debris waits before
settling.

If shards look like they snap back a second after a crash, raise `debrisScatter`: a shard that never
moves counts as settled and gets written back exactly where it broke off.
