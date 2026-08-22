package com.b8b7.punchlist;

import com.b8b7.punchlist.mixin.SchematicVerifierAccessor;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * v0.3 adjusted counts: hidden-per-row over the FULL mismatch set
 * (blockMismatches accessor), not just the selection. Counts follow the
 * mode alone; hiding still requires the filter. Snapshot on the client
 * thread, enclosure test on a single daemon worker, published volatile.
 * Any failure resolves to "no hover line" and touches nothing else.
 */
public final class EnclosedCounts {
    // ~0.5s at 20 tps: recomputes are memo-cheap since v0.6; the
    // debounce only guards against mid-scan thrash
    private static final int STABLE_TICKS = 10;

    record CountKey(SchematicVerifier.MismatchType type, BlockState expected, BlockState found) {}

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "punchlist-enclosed-counts");
        t.setDaemon(true);
        return t;
    });

    private static volatile Map<CountKey, Integer> published = null;
    // written by the worker on publish, read by the client thread
    private static volatile long publishedSignature = 0;
    private static volatile boolean computing = false;
    private static volatile boolean showLines = false;

    // client-thread only
    private static long lastSignature = 0;
    private static int stableTicks = 0;
    private static boolean warnedThisSession = false;

    private EnclosedCounts() {}

    /** Called from FilterState.clientTick; mode-gated only, never filter-gated. */
    static void clientTick(EnclosedMode mode) {
        try {
            if (mode == EnclosedMode.SHOW || !CompatCheck.enclosedOk()) {
                clear();
                return;
            }
            WorldSchematic world = SchematicWorldHandler.getSchematicWorld();
            List<SchematicVerifier> verifiers = SchematicVerifierAccessor.punchlist$getActiveVerifiers();
            if (world == null || verifiers == null || verifiers.isEmpty()) {
                clear();
                return;
            }
            showLines = true;

            long size = verifiers.size();
            for (SchematicVerifier verifier : verifiers) {
                Map<BlockPos, SchematicVerifier.BlockMismatch> map =
                        ((SchematicVerifierAccessor) (Object) verifier).punchlist$getBlockMismatches();
                size = size * 31 + (map != null ? map.size() : -1);
            }
            List<String> softCfg = PunchListConfigs.ENCLOSED_SOFT_OCCLUDERS.getStrings();
            long sig = signature(mode, softCfg, world, size);

            if (sig != lastSignature) {
                lastSignature = sig;
                stableTicks = 0;
                return;
            }
            if (sig == publishedSignature || computing || ++stableTicks < STABLE_TICKS) {
                return;
            }

            // client-thread snapshot: verifier scans mutate on this thread too
            List<BlockPos> positions = new ArrayList<>();
            List<SchematicVerifier.BlockMismatch> keys = new ArrayList<>();
            for (SchematicVerifier verifier : verifiers) {
                Map<BlockPos, SchematicVerifier.BlockMismatch> map =
                        ((SchematicVerifierAccessor) (Object) verifier).punchlist$getBlockMismatches();
                if (map == null) {
                    continue;
                }
                for (Map.Entry<BlockPos, SchematicVerifier.BlockMismatch> e : map.entrySet()) {
                    positions.add(e.getKey());
                    keys.add(e.getValue());
                }
            }
            EnclosureTest.Spec spec = EnclosureTest.spec(mode, softCfg);
            computing = true;
            WORKER.submit(() -> compute(world, positions, keys, spec, sig));
        } catch (Throwable t) {
            warnOnce(t);
            clear();
        }
    }

    private static void compute(WorldSchematic world, List<BlockPos> positions,
                                List<SchematicVerifier.BlockMismatch> keys,
                                EnclosureTest.Spec spec, long sig) {
        try {
            Map<CountKey, Integer> counts = new HashMap<>();
            for (int i = 0; i < positions.size(); i++) {
                if (EnclosureCache.isHidden(world, positions.get(i), spec)) {
                    SchematicVerifier.BlockMismatch m = keys.get(i);
                    counts.merge(new CountKey(m.mismatchType(), m.stateExpected(), m.stateFound()), 1, Integer::sum);
                }
            }
            published = Map.copyOf(counts);
            publishedSignature = sig;
        } catch (Throwable t) {
            // mark attempted so a failing input does not retry every tick
            publishedSignature = sig;
            warnOnce(t);
        } finally {
            computing = false;
        }
    }

    /** Row hover (render thread); null = no extra line. */
    public static List<String> hoverLines(SchematicVerifier.BlockMismatch mismatch) {
        try {
            if (!showLines || mismatch == null) {
                return null;
            }
            Map<CountKey, Integer> counts = published;
            if (counts == null) {
                return computing
                        ? List.of(StringUtils.translate("punchlist.gui.hover.enclosed_computing"))
                        : null;
            }
            Integer hidden = counts.get(new CountKey(mismatch.mismatchType(), mismatch.stateExpected(), mismatch.stateFound()));
            if (hidden == null || hidden == 0) {
                return null;
            }
            int visible = Math.max(0, mismatch.count() - hidden);
            return List.of(StringUtils.translate("punchlist.gui.hover.enclosed_counts",
                    String.format("%,d", hidden), String.format("%,d", visible)));
        } catch (Throwable t) {
            return null;
        }
    }

    private static void clear() {
        published = null;
        publishedSignature = 0;
        lastSignature = 0;
        stableTicks = 0;
        showLines = false;
    }

    private static long signature(EnclosedMode mode, List<String> softCfg, WorldSchematic world, long size) {
        long h = mode.ordinal();
        h = h * 31 + softCfg.hashCode();
        h = h * 31 + System.identityHashCode(world);
        h = h * 31 + size;
        h = h * 31 + EnclosureCache.generation();
        return h;
    }

    private static void warnOnce(Throwable t) {
        if (!warnedThisSession) {
            warnedThisSession = true;
            PunchListClient.LOGGER.warn("PunchList: enclosed count computation failed, hover counts disabled", t);
        }
    }
}
