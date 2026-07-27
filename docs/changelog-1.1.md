# Crashy! 1.1

Damage now depends on what actually hit you, big crashes no longer take the server down, and
objects that punch through something keep going.

---

## Fixed

**Launching a large object no longer crashes the game.** A 400-block build hitting the ground used
to ask for over five hundred separate physics bodies inside a single tick — each one allocating a
Sable plot, a chunk and a rigid body — and the server went down under the allocation. Shattering is
now queued and processed at a fixed budget per tick, so the crater forms over about a second
instead of all at once.

There is also a ceiling on how much debris can be alive at the same time. Once it is reached the
rest of the pending crash is abandoned: the crater comes out smaller under load rather than the
game stalling. Nothing is deleted — a block that never gets shattered simply stays standing. Both
numbers are in `config/crashy-server.toml` under `[performance]`.

**Craters no longer heal themselves seconds after the hit.** Debris now lies where it landed for a
configurable time, ten seconds by default, and gets thrown clear of the crater harder so it does
not simply drop back into the hole it came from.

---

## Changed

**Damage scales with mass, not just speed.** A single block and a 400-block tower travelling at the
same speed used to dig identical craters. Both crater size and breaking force are now driven by the
projectile's kinetic energy, so at full launch speed:

| Object | Crater | Breaks |
| --- | --- | --- |
| 1 block | ~1 block | glass, planks, barely cobblestone |
| 20 blocks | ~3 blocks | up to cobblestone comfortably |
| 150+ blocks | the configured maximum | almost anything except obsidian |

Crater volume scaling with energy is roughly how it works in reality, and it happens to give the
numbers the game wants.

To give that scaling room, the default maximum crater radius went from 4 to 6 blocks and the
per-impact world block cap from 220 to 400.

**Objects keep going after breaking through.** Something with enough behind it to punch through a
floor now stays tracked and keeps breaking what it meets, instead of being written off after the
first hit. Each successive impact is weighed on what is left of the object, so it digs less as it
sheds blocks. Impacts are spaced six ticks apart, so ploughing through a floor reads as one crash
rather than one per tick of contact.

---

## Added

**"Debris lies for" on the F7 screen** — how long shards stay as physics blocks after coming to
rest, from 0 to 120 seconds. Ten by default. Set it high if you want the wreckage to stay put.

---

## Notes

Existing worlds keep their current settings; the new one starts from `settleDelayTicks` in the
config file.

If a big crash still stutters on your hardware, lower `shattersPerTick` — the crater takes longer
to form but each tick does less work. If you have headroom and want bigger craters, raise
`maxLiveDebris` and `maxWorldBlocksDestroyed` together; raising the block cap alone will just hit
the debris ceiling.
