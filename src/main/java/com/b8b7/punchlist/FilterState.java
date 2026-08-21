package com.b8b7.punchlist;

import com.b8b7.punchlist.mixin.SchematicVerifierAccessor;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.util.InfoUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Filter predicate. Fail open: no verifier, empty selection, or guard trip
 * resolves to "show everything" (selected == null). The snapshot is an
 * immutable set swapped on the client thread per tick; every swap schedules
 * a re-mesh.
 */
public final class FilterState {
    // ~500ms at 20 tps
    private static final int DEBOUNCE_TICKS = 10;

    private static volatile boolean enabled = false;
    private static volatile Set<BlockPos> selected = null;

    // client-thread only
    private static boolean lastEnabled = false;
    private static Set<BlockPos> lastBuilt = null;
    private static int pendingRefresh = -1;
    private static boolean warnedThisSession = false;

    private FilterState() {}

    public static boolean isEnabled() {
        return enabled;
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

    public static void clientTick(Minecraft mc) {
        boolean on = enabled;
        Set<BlockPos> built = on ? build() : null;
        selected = built;

        if (on != lastEnabled) {
            lastEnabled = on;
            pendingRefresh = 0;
        } else if (on && !Objects.equals(built, lastBuilt)) {
            pendingRefresh = DEBOUNCE_TICKS;
        }
        lastBuilt = built;

        if (pendingRefresh >= 0) {
            if (pendingRefresh == 0) {
                pendingRefresh = -1;
                refresh();
            } else {
                pendingRefresh--;
            }
        }
    }

    /** Union of selected mismatch positions across all active verifiers; null = no filtering. */
    private static Set<BlockPos> build() {
        try {
            List<SchematicVerifier> verifiers = SchematicVerifierAccessor.punchlist$getActiveVerifiers();
            if (verifiers == null || verifiers.isEmpty()) {
                return null;
            }
            Set<BlockPos> set = new HashSet<>();
            for (SchematicVerifier verifier : verifiers) {
                List<BlockPos> positions = verifier.getSelectedMismatchBlockPositionsForRender();
                if (positions != null) {
                    set.addAll(positions);
                }
            }
            return set.isEmpty() ? null : Set.copyOf(set);
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

    private static void refresh() {
        try {
            SchematicWorldRefresher.INSTANCE.updateAll();
        } catch (Throwable t) {
            PunchListClient.LOGGER.warn("PunchList: schematic re-mesh failed", t);
        }
    }
}
