# Crashy! 1.1.2

Fixes the `Rapier native panic: No center of mass for body!` crash.

## What was happening

Sable removes a physics body once its blocks are gone, but only when its container next ticks.
Between the blocks leaving and that removal there is a window where the body still exists, still
sits in the physics pipeline, and has no mass and no centre of mass.

Sable's own code never touches a body in that window. This mod does: it applies velocity to shards
the instant they are split off, drives a grabbed object's velocity every tick, and reads the
velocity of everything it is tracking. Hand Rapier a body with no centre of mass and it aborts the
whole process — which is also why the follow-up message is always a `PoisonError`, the poisoned
lock being the *second* failure rather than the first.

Every physics call now refuses bodies without usable mass data.

Second contributor: emptying a plot used `UPDATE_CLIENTS` rather than `UPDATE_ALL`. That was
introduced in 1.0 to stop debris being dropped twice, and it skipped neighbour updates — which
Sable uses to keep its voxel neighbourhood state current for collider baking. Stale neighbourhood
data means colliders for blocks that are no longer there, on a body that no longer has mass. It now
uses the full update.

## If it still happens

The assertion is Sable's, and this mod is unusual in how many single-block bodies it creates and
destroys. If the crash survives this build it is worth reporting upstream with that context — and
worth checking whether plain Sable reproduces it, using `/sable assemble shatter` on a big area.
