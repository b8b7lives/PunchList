package com.b8b7.punchlist;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Per-position hidden-verdict memo. Verdicts are constants within a
 * generation; generation inputs: schematic world instance, mode,
 * soft-occluder config, placement transform tuple, schematic-content
 * generation (WorldSchematic.setBlock hook), flood epoch. Any change
 * clears the memo and consumers fail open until recomputed. Puts are
 * generation-guarded so a worker racing an invalidation cannot seed
 * the fresh memo with stale verdicts.
 */
public final class EnclosureCache {
    private static final Long2ByteOpenHashMap MEMO = new Long2ByteOpenHashMap();

    static {
        MEMO.defaultReturnValue((byte) -1);
    }

    private static volatile long generation = 0;

    // client-thread only
    private static long lastFullHash = Long.MIN_VALUE;

    private EnclosureCache() {}

    static long generation() {
        return generation;
    }

    static void clientTick(EnclosedMode mode) {
        try {
            WorldSchematic world = SchematicWorldHandler.getSchematicWorld();
            List<String> softCfg = PunchListConfigs.ENCLOSED_SOFT_OCCLUDERS.getStrings();
            long base = 17;
            base = base * 31 + System.identityHashCode(world);
            base = base * 31 + (mode == null ? -1 : mode.ordinal());
            base = base * 31 + softCfg.hashCode();
            base = base * 31 + transformHash();
            base = base * 31 + FilterState.schematicGeneration();
            PocketFill.clientTick(EnclosureTest.spec(mode, softCfg), world, base);
            long full = base * 31 + PocketFill.epoch();
            if (full != lastFullHash) {
                lastFullHash = full;
                synchronized (MEMO) {
                    generation++;
                    MEMO.clear();
                }
            }
        } catch (Throwable t) {
            // next successful tick clears; consumers fail open meanwhile
        }
    }

    /** Any thread; memoized composed predicate. */
    public static boolean isHidden(WorldSchematic world, BlockPos pos, EnclosureTest.Spec spec) {
        if (!CompatCheck.pocketOk()) {
            return EnclosureTest.isHidden(world, pos, spec, null);
        }
        long gen = generation;
        long key = pos.asLong();
        synchronized (MEMO) {
            byte cached = MEMO.get(key);
            if (cached >= 0) {
                return cached == 1;
            }
        }
        boolean hidden = EnclosureTest.isHidden(world, pos, spec, PocketFill.current());
        synchronized (MEMO) {
            if (gen == generation) {
                MEMO.put(key, (byte) (hidden ? 1 : 0));
            }
        }
        return hidden;
    }

    /**
     * Client thread: drop positions with a cached hidden verdict.
     * Unknown verdicts stay (fail open); the counts worker warms the
     * memo full-set, so unknowns are transient. Single lock hold for
     * the whole pass.
     */
    public static void removeCachedHidden(List<BlockPos> list) {
        synchronized (MEMO) {
            list.removeIf(pos -> MEMO.get(pos.asLong()) == 1);
        }
    }

    private static long transformHash() {
        try {
            long h = 1;
            for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements()) {
                if (!placement.hasVerifier()) {
                    continue;
                }
                h = h * 31 + placement.getOrigin().asLong();
                h = h * 31 + placement.getRotation().ordinal();
                h = h * 31 + placement.getMirror().ordinal();
                h = h * 31 + (placement.isEnabled() ? 1 : 0);
                h = h * 31 + placement.getSubRegionCount();
            }
            return h;
        } catch (Throwable t) {
            return 0;
        }
    }
}
