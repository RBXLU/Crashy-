package dev.pycodder.crashy.glue;

import dev.pycodder.crashy.CrashyConfig;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Persistent record of which blocks have been glued together, per level.
 *
 * <p>Blocks are grouped: every glued block belongs to exactly one group id, and gluing two adjacent
 * blocks merges their groups. When the physics activator fires, the group is what gets assembled
 * into a rigid body.
 */
public class GlueSavedData extends SavedData {

    private static final String NAME = "crashy_glue";
    private static final int NO_GROUP = -1;

    private final Long2IntMap positionToGroup = new Long2IntOpenHashMap();
    private final Int2ObjectMap<LongSet> groups = new Int2ObjectOpenHashMap<>();
    private int nextGroupId = 0;

    public GlueSavedData() {
        this.positionToGroup.defaultReturnValue(NO_GROUP);
    }

    public static GlueSavedData get(final ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(GlueSavedData::new, GlueSavedData::load),
                NAME);
    }

    private static GlueSavedData load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final GlueSavedData data = new GlueSavedData();
        final ListTag list = tag.getList("groups", Tag.TAG_LONG_ARRAY);
        for (int i = 0; i < list.size(); i++) {
            final long[] positions = ((LongArrayTag) list.get(i)).getAsLongArray();
            if (positions.length == 0) {
                continue;
            }
            final int id = data.nextGroupId++;
            final LongSet set = new LongOpenHashSet(positions);
            data.groups.put(id, set);
            for (final long pos : positions) {
                data.positionToGroup.put(pos, id);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ListTag list = new ListTag();
        for (final LongSet group : this.groups.values()) {
            if (!group.isEmpty()) {
                list.add(new LongArrayTag(group.toLongArray()));
            }
        }
        tag.put("groups", list);
        return tag;
    }

    public boolean isGlued(final BlockPos pos) {
        return this.positionToGroup.get(pos.asLong()) != NO_GROUP;
    }

    /** @return every block glued to {@code pos}, including {@code pos} itself, or {@code null}. */
    public @Nullable Set<BlockPos> getGroup(final BlockPos pos) {
        final int id = this.positionToGroup.get(pos.asLong());
        if (id == NO_GROUP) {
            return null;
        }
        return toBlockPositions(this.groups.get(id));
    }

    /** Size of the group {@code pos} belongs to, or 0. */
    public int getGroupSize(final BlockPos pos) {
        final int id = this.positionToGroup.get(pos.asLong());
        final LongSet group = id == NO_GROUP ? null : this.groups.get(id);
        return group == null ? 0 : group.size();
    }

    /**
     * Glues {@code pos} and merges it with every already-glued block it touches.
     *
     * @return {@code true} if this actually changed anything.
     */
    public boolean glue(final BlockPos pos) {
        final long key = pos.asLong();
        final IntArrayList neighbourGroups = new IntArrayList();

        for (final Direction direction : Direction.values()) {
            final int neighbourGroup = this.positionToGroup.get(pos.relative(direction).asLong());
            if (neighbourGroup != NO_GROUP && !neighbourGroups.contains(neighbourGroup)) {
                neighbourGroups.add(neighbourGroup);
            }
        }

        final int existing = this.positionToGroup.get(key);
        if (existing != NO_GROUP && !neighbourGroups.contains(existing)) {
            neighbourGroups.add(existing);
        }

        final int maxSize = CrashyConfig.MAX_GLUE_GROUP_SIZE.get();
        int combined = 1;
        for (int i = 0; i < neighbourGroups.size(); i++) {
            final LongSet group = this.groups.get(neighbourGroups.getInt(i));
            if (group != null) {
                combined += group.size();
            }
        }
        if (combined > maxSize) {
            return false;
        }

        final int target;
        if (neighbourGroups.isEmpty()) {
            target = this.nextGroupId++;
            this.groups.put(target, new LongOpenHashSet());
        } else {
            target = neighbourGroups.getInt(0);
        }

        final LongSet targetGroup = this.groups.get(target);
        boolean changed = targetGroup.add(key);
        this.positionToGroup.put(key, target);

        for (int i = 1; i < neighbourGroups.size(); i++) {
            final int otherId = neighbourGroups.getInt(i);
            final LongSet other = this.groups.remove(otherId);
            if (other == null) {
                continue;
            }
            changed = true;
            for (final long otherPos : other) {
                targetGroup.add(otherPos);
                this.positionToGroup.put(otherPos, target);
            }
        }

        if (changed) {
            this.setDirty();
        }
        return changed;
    }

    /**
     * Glued blocks within {@code radius} of {@code center}, for the client-side highlight.
     *
     * <p>Walks every glued position rather than using a spatial index: the group cap keeps that in
     * the low thousands, and this runs twice a second at most.
     */
    public List<BlockPos> near(final BlockPos center, final int radius, final int limit) {
        final List<BlockPos> result = new ArrayList<>();
        final long radiusSq = (long) radius * radius;

        for (final long key : this.positionToGroup.keySet()) {
            final BlockPos pos = BlockPos.of(key);
            if (center.distSqr(pos) > radiusSq) {
                continue;
            }
            result.add(pos);
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    /** Removes a single block from its group. Does not split the remainder — glue is forgiving. */
    public void unglue(final BlockPos pos) {
        final long key = pos.asLong();
        final int id = this.positionToGroup.remove(key);
        if (id == NO_GROUP) {
            return;
        }
        final LongSet group = this.groups.get(id);
        if (group != null) {
            group.remove(key);
            if (group.isEmpty()) {
                this.groups.remove(id);
            }
        }
        this.setDirty();
    }

    /** Removes the whole group {@code pos} belongs to. @return how many blocks were unglued. */
    public int unglueGroup(final BlockPos pos) {
        final int id = this.positionToGroup.get(pos.asLong());
        if (id == NO_GROUP) {
            return 0;
        }
        final LongSet group = this.groups.remove(id);
        if (group == null) {
            return 0;
        }
        for (final long member : group) {
            this.positionToGroup.remove(member);
        }
        this.setDirty();
        return group.size();
    }

    /** Forgets every listed position, used once a group has been turned into a physics object. */
    public void forget(final Iterable<BlockPos> positions) {
        for (final BlockPos pos : positions) {
            this.unglue(pos);
        }
    }

    private static Set<BlockPos> toBlockPositions(final @Nullable LongSet longs) {
        if (longs == null) {
            return Set.of();
        }
        final Set<BlockPos> result = new HashSet<>(longs.size());
        for (final long value : longs) {
            result.add(BlockPos.of(value));
        }
        return result;
    }
}
