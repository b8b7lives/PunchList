package com.b8b7.punchlist;

import com.b8b7.punchlist.mixin.SchematicVerifierAccessor;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.util.InfoUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Filter predicate. Fail open: no verifier, empty selection, or guard trip
 * resolves to "show everything" (selected == null). The snapshot is an
 * immutable set swapped on the client thread per tick; every swap schedules
 * a re-mesh.
 *
 * v0.2 enclosed layer: positions invisible in the finished build (every face
 * occluded in the schematic world) are subtracted from the selection and
 * from the verifier marker overlay. Layer failure of any kind falls back to
 * the unsubtracted v0.1 behavior.
 */
public final class FilterState {
    // ~500ms at 20 tps
    private static final int DEBOUNCE_TICKS = 10;
    // targeted re-mesh: small deltas flush immediately (placement streaks
    // must refill ghosts in real time); big deltas (GUI mass changes)
    // debounce with a hard deadline so a steady stream can never
    // postpone the refresh forever
    private static final int IMMEDIATE_CHUNK_LIMIT = 32;
    private static final int MAX_PENDING_TICKS = 40;

    private static volatile boolean enabled = false;
    private static volatile Set<BlockPos> selected = null;
    // subtracted enclosed positions; read by the marker overlay mixin
    private static volatile Set<BlockPos> enclosedHidden = null;

    // client-thread only
    private static boolean lastEnabled = false;
    private static Set<BlockPos> lastBuilt = null;
    private static int pendingRefresh = -1;
    private static final Long2ObjectOpenHashMap<BlockPos> pendingChunks = new Long2ObjectOpenHashMap<>();
    private static int chunkIdleTicks = 0;
    private static int chunkPendingAge = 0;
    private static boolean warnedThisSession = false;
    private static boolean warnedEnclosedThisSession = false;
    private static boolean warnedEnclosedUnavailable = false;

    // enclosure cache: intended states are static per placement EXCEPT
    // rebuild-mode schematic edits, which bump schematicGeneration
    // (MixinWorldSchematic); the cache keys on it
    private static Set<BlockPos> encRaw = null;
    private static EnclosedMode encMode = null;
    private static List<String> encSoftCfg = null;
    private static WorldSchematic encWorld = null;
    private static long encGeneration = -1;
    private static Set<BlockPos> encResult = null;

    private static volatile long schematicGeneration = 0;
    private static volatile long listGeneration = 0;

    // build() cache: re-copy the render lists only when they changed
    // (list-generation hooks, content-hash fallback), not every tick.
    // The hash folds every position so a same-size content swap can
    // never serve a stale set even if the dirty hooks fail to apply
    private static long rawGeneration = Long.MIN_VALUE;
    private static long rawContentHash = Long.MIN_VALUE;
    private static Set<BlockPos> rawCached = null;

    private FilterState() {}

    public static boolean isEnabled() {
        return enabled;
    }

    /** Called by MixinWorldSchematic on any schematic content mutation. */
    public static void bumpSchematicGeneration() {
        schematicGeneration++;
    }

    static long schematicGeneration() {
        return schematicGeneration;
    }

    /** Called by MixinSchematicVerifierDirty on any render-list change. */
    public static void bumpListGeneration() {
        listGeneration++;
    }

    public static boolean toggle() {
        if (!enabled && !CompatCheck.ensureRun()) {
            InfoUtils.showGuiOrInGameMessage(Message.MessageType.ERROR, "punchlist.message.incompatible");
            return false;
        }
        enabled = !enabled;
        if (enabled) {
            Set<BlockPos> built = build();
            selected = built;
            InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO,
                    built == null ? "punchlist.message.filter_on_empty" : "punchlist.message.filter_on");
        } else {
            InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, "punchlist.message.filter_off");
        }
        return enabled;
    }

    /** Render workers (off-thread): hide pos unless it is a selected mismatch. */
    public static boolean hiddenInRender(BlockPos pos) {
        Set<BlockPos> s = selected;
        return enabled && s != null && !s.contains(pos);
    }

    /** Schematic-world ray traces: treat pos as air unless selected. */
    public static boolean hiddenInTrace(BlockPos pos) {
        Set<BlockPos> s = selected;
        return enabled && s != null && !s.contains(pos);
    }

    /** Marker overlay (render thread): drop enclosed positions; untouched when the layer is inactive. */
    public static List<SchematicVerifier.MismatchRenderPos> filterMarkers(List<SchematicVerifier.MismatchRenderPos> list) {
        Set<BlockPos> hidden = enclosedHidden;
        if (hidden == null || !enabled || list == null || list.isEmpty()) {
            return list;
        }
        List<SchematicVerifier.MismatchRenderPos> out = new ArrayList<>(list.size());
        for (SchematicVerifier.MismatchRenderPos entry : list) {
            if (!hidden.contains(entry.pos())) {
                out.add(entry);
            }
        }
        return out;
    }

    public static void clientTick(Minecraft mc) {
        // re-center first so this tick's build() reads the new window
        FollowPlayer.clientTick(mc);

        EnclosedMode mode = mode();
        EnclosureCache.clientTick(mode);
        EnclosedCounts.clientTick(mode);

        boolean on = enabled;
        Set<BlockPos> raw = on ? build() : null;
        Set<BlockPos> enclosed = raw != null ? enclosedSubset(raw, mode) : null;
        Set<BlockPos> built = subtract(raw, enclosed);
        selected = built;
        enclosedHidden = (on && enclosed != null && !enclosed.isEmpty()) ? enclosed : null;

        if (on != lastEnabled) {
            lastEnabled = on;
            pendingRefresh = 0;
            pendingChunks.clear();
        } else if (on && !Objects.equals(built, lastBuilt)) {
            if (built == null || lastBuilt == null) {
                // everything shows/hides at once: full re-mesh
                pendingRefresh = DEBOUNCE_TICKS;
                pendingChunks.clear();
            } else {
                addDeltaChunks(lastBuilt, built);
                chunkIdleTicks = 0;
            }
        }
        lastBuilt = built;

        if (pendingRefresh >= 0) {
            if (pendingRefresh == 0) {
                pendingRefresh = -1;
                pendingChunks.clear();
                refresh();
            } else {
                pendingRefresh--;
            }
        } else if (!pendingChunks.isEmpty()) {
            chunkPendingAge++;
            chunkIdleTicks++;
            if (pendingChunks.size() <= IMMEDIATE_CHUNK_LIMIT
                    || chunkIdleTicks > DEBOUNCE_TICKS
                    || chunkPendingAge > MAX_PENDING_TICKS) {
                flushChunks();
            }
        } else {
            chunkIdleTicks = 0;
            chunkPendingAge = 0;
        }
    }

    private static void addDeltaChunks(Set<BlockPos> oldSet, Set<BlockPos> newSet) {
        for (BlockPos pos : newSet) {
            if (!oldSet.contains(pos)) {
                pendingChunks.putIfAbsent(chunkKey(pos), pos);
            }
        }
        for (BlockPos pos : oldSet) {
            if (!newSet.contains(pos)) {
                pendingChunks.putIfAbsent(chunkKey(pos), pos);
            }
        }
    }

    private static long chunkKey(BlockPos pos) {
        return ((long) (pos.getX() >> 4) << 32) | ((pos.getZ() >> 4) & 0xFFFFFFFFL);
    }

    private static void flushChunks() {
        try {
            for (BlockPos pos : pendingChunks.values()) {
                SchematicWorldRefresher.INSTANCE.markSchematicChunkForRenderUpdate(pos);
            }
        } catch (Throwable t) {
            // targeted path unavailable: full re-mesh keeps correctness
            refresh();
        }
        pendingChunks.clear();
        chunkIdleTicks = 0;
        chunkPendingAge = 0;
    }

    /** Union of selected mismatch positions across all active verifiers; null = no filtering. */
    private static Set<BlockPos> build() {
        try {
            List<SchematicVerifier> verifiers = SchematicVerifierAccessor.punchlist$getActiveVerifiers();
            if (verifiers == null || verifiers.isEmpty()) {
                rawCached = null;
                rawContentHash = Long.MIN_VALUE;
                return null;
            }
            long contentHash = verifiers.size();
            for (SchematicVerifier verifier : verifiers) {
                List<BlockPos> positions = verifier.getSelectedMismatchBlockPositionsForRender();
                contentHash = contentHash * 31 + (positions != null ? positions.size() : -1);
                if (positions != null) {
                    long fold = 0;
                    for (int i = 0; i < positions.size(); i++) {
                        fold ^= positions.get(i).asLong() * 0x9E3779B97F4A7C15L;
                    }
                    contentHash = contentHash * 31 + fold;
                }
            }
            long generation = listGeneration;
            if (rawCached != null && generation == rawGeneration && contentHash == rawContentHash) {
                return rawCached;
            }
            Set<BlockPos> set = new HashSet<>();
            for (SchematicVerifier verifier : verifiers) {
                List<BlockPos> positions = verifier.getSelectedMismatchBlockPositionsForRender();
                if (positions != null) {
                    set.addAll(positions);
                }
            }
            rawGeneration = generation;
            rawContentHash = contentHash;
            rawCached = set.isEmpty() ? null : Set.copyOf(set);
            return rawCached;
        } catch (Throwable t) {
            // third-party mixins inside SchematicVerifier can throw
            // (maruohon/litematica#1137); fail open
            if (!warnedThisSession) {
                warnedThisSession = true;
                PunchListClient.LOGGER.warn("PunchList: failed to read verifier data, filter showing everything", t);
                InfoUtils.showGuiOrInGameMessage(Message.MessageType.WARNING, "punchlist.message.verifier_error");
            }
            return null;
        }
    }

    // identity cache: raw and enclosed are instance-stable when unchanged,
    // so the per-tick path stays O(1)
    private static Set<BlockPos> subRaw = null;
    private static Set<BlockPos> subEncl = null;
    private static Set<BlockPos> subResult = null;

    private static Set<BlockPos> subtract(Set<BlockPos> raw, Set<BlockPos> enclosed) {
        if (raw == null || enclosed == null || enclosed.isEmpty()) {
            return raw;
        }
        if (raw == subRaw && enclosed == subEncl) {
            return subResult;
        }
        Set<BlockPos> out = new HashSet<>(raw);
        out.removeAll(enclosed);
        subRaw = raw;
        subEncl = enclosed;
        // empty is legitimate here: every selected mismatch is interior
        subResult = Set.copyOf(out);
        return subResult;
    }

    /** Enclosed subset of raw; null = layer off/unavailable (no subtraction). */
    private static Set<BlockPos> enclosedSubset(Set<BlockPos> raw, EnclosedMode mode) {
        if (mode == EnclosedMode.SHOW) {
            return null;
        }
        try {
            if (!CompatCheck.enclosedOk()) {
                if (!warnedEnclosedUnavailable) {
                    warnedEnclosedUnavailable = true;
                    InfoUtils.showGuiOrInGameMessage(Message.MessageType.WARNING, "punchlist.message.enclosed_unavailable");
                }
                return null;
            }
            WorldSchematic world = SchematicWorldHandler.getSchematicWorld();
            if (world == null) {
                return null;
            }
            List<String> softCfg = PunchListConfigs.ENCLOSED_SOFT_OCCLUDERS.getStrings();
            long generation = EnclosureCache.generation();
            if (raw.equals(encRaw) && mode == encMode && world == encWorld
                    && generation == encGeneration && softCfg.equals(encSoftCfg)) {
                return encResult;
            }
            Set<BlockPos> result = computeEnclosed(raw, mode, world, softCfg);
            encRaw = raw;
            encMode = mode;
            encWorld = world;
            encGeneration = generation;
            encSoftCfg = List.copyOf(softCfg);
            encResult = result;
            return result;
        } catch (Throwable t) {
            if (!warnedEnclosedThisSession) {
                warnedEnclosedThisSession = true;
                PunchListClient.LOGGER.warn("PunchList: enclosed-block check failed, showing all selected positions", t);
            }
            return null;
        }
    }

    static EnclosedMode mode() {
        try {
            return (EnclosedMode) PunchListConfigs.ENCLOSED_MODE.getOptionListValue();
        } catch (Throwable t) {
            return EnclosedMode.SHOW;
        }
    }

    private static Set<BlockPos> computeEnclosed(Set<BlockPos> raw, EnclosedMode mode,
                                                 WorldSchematic world, List<String> softCfg) {
        EnclosureTest.Spec spec = EnclosureTest.spec(mode, softCfg);
        Set<BlockPos> result = new HashSet<>();
        for (BlockPos pos : raw) {
            if (EnclosureCache.isHidden(world, pos, spec)) {
                result.add(pos);
            }
        }
        return Set.copyOf(result);
    }

    private static void refresh() {
        try {
            SchematicWorldRefresher.INSTANCE.updateAll();
        } catch (Throwable t) {
            PunchListClient.LOGGER.warn("PunchList: schematic re-mesh failed", t);
        }
    }
}
