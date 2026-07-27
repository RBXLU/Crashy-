# Crashy! 1.1.1

Hotfix for a crash introduced in 1.1.

## Fixed

**Runaway destruction when an object is buried in solid ground.** 1.1 let an object keep breaking
things after its first impact, which is the point — but nothing bounded it. An object ploughing
through solid rock finds blocks on every check and never escapes, so it kept carving craters every
six ticks for its whole thirty-second tracking window: up to a hundred impacts, tens of thousands
of queued shatter jobs, and a debris pool pinned at its ceiling the entire time. On a machine
already short on memory that ends with Rapier's native side failing and aborting the process.

Three bounds were missing and are now in place:

- a launch may break something **five times**, then it is left alone
- an object that has lost most of its mass stops being treated as a projectile
- the shatter queue is capped, and work past the cap is dropped rather than accumulating

Blocks that never get shattered stay standing; nothing is deleted.

## Changed

Default `shattersPerTick` lowered from 12 to 8 and `maxLiveDebris` from 300 to 150. Every live shard
is a Sable plot, a chunk and a rigid body, and the previous defaults were set without measuring on
modest hardware. Raise them if your machine has room.

## If you still crash with `PoisonError`

That message means Rapier's lock was poisoned by an *earlier* panic, so it is the second failure,
not the first — the useful information is whatever crashed before it. Worth checking:

- how much memory the game actually has, and whether the system is swapping
- whether `maxLiveDebris` is set high
- whether plain Sable reproduces it without Crashy! installed
