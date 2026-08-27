package com.b8b7.punchlist;

import com.b8b7.punchlist.mixin.SchematicVerifierAccessor;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Re-centers the verifier's closest-N marker window on the player.
 * Upstream re-sorts only on block change / GUI interaction, so the
 * window anchors to the last interaction. The distance threshold
 * doubles as the debounce; stock triggers stay untouched.
 *
 * Also watches verifierErrorHilightMaxPositions: upstream never re-sorts
 * on config change, so a lowered cap leaves a stale oversized window
 * until some other trigger fires. Deadline-debounced, never
 * reset-on-change.
 */
public final class FollowPlayer {
    private static final int MAX_POS_DEADLINE_TICKS = 10;

    private static Vec3 lastCenter = null;
    private static boolean warnedThisSession = false;
    private static int lastMaxPositions = Integer.MIN_VALUE;
    private static int maxPosCountdown = -1;
    private static boolean warnedMaxPosThisSession = false;

    private FollowPlayer() {}

    static void clientTick(Minecraft mc) {
        try {
            int dist = PunchListConfigs.FOLLOW_PLAYER_DISTANCE.getIntegerValue();
            if (dist <= 0 || mc.player == null) {
                lastCenter = null;
                return;
            }
            List<SchematicVerifier> verifiers = SchematicVerifierAccessor.punchlist$getActiveVerifiers();
            if (verifiers == null || verifiers.isEmpty()) {
                lastCenter = null;
                return;
            }
            Vec3 pos = mc.player.position();
            if (lastCenter == null) {
                // adopt the current anchor; stock triggers own it until the
                // player moves the threshold
                lastCenter = pos;
                return;
            }
            if (lastCenter.distanceToSqr(pos) < (double) dist * dist) {
                return;
            }
            for (SchematicVerifier verifier : verifiers) {
                ((SchematicVerifierAccessor) (Object) verifier).punchlist$updateMismatchOverlays();
            }
            lastCenter = pos;
        } catch (Throwable t) {
            if (!warnedThisSession) {
                warnedThisSession = true;
                PunchListClient.LOGGER.warn("PunchList: follow-player re-center failed, stock marker behavior", t);
            }
        }
    }

    /** Schedule a window re-sort on the shared deadline (never reset-on-change). */
    static void requestResort() {
        if (maxPosCountdown < 0) {
            maxPosCountdown = MAX_POS_DEADLINE_TICKS;
        }
    }

    static void watchMaxPositions(Minecraft mc) {
        try {
            int cur = Configs.InfoOverlays.VERIFIER_ERROR_HILIGHT_MAX_POSITIONS.getIntegerValue();
            if (lastMaxPositions == Integer.MIN_VALUE || mc.player == null) {
                lastMaxPositions = cur;
                maxPosCountdown = -1;
                return;
            }
            if (cur != lastMaxPositions && maxPosCountdown < 0) {
                maxPosCountdown = MAX_POS_DEADLINE_TICKS;
            }
            if (maxPosCountdown < 0) {
                return;
            }
            if (maxPosCountdown-- > 0) {
                return;
            }
            lastMaxPositions = Configs.InfoOverlays.VERIFIER_ERROR_HILIGHT_MAX_POSITIONS.getIntegerValue();
            List<SchematicVerifier> verifiers = SchematicVerifierAccessor.punchlist$getActiveVerifiers();
            if (verifiers == null) {
                return;
            }
            for (SchematicVerifier verifier : verifiers) {
                ((SchematicVerifierAccessor) (Object) verifier).punchlist$updateMismatchOverlays();
            }
        } catch (Throwable t) {
            maxPosCountdown = -1;
            if (!warnedMaxPosThisSession) {
                warnedMaxPosThisSession = true;
                PunchListClient.LOGGER.warn("PunchList: max-positions watch failed, stock config behavior", t);
            }
        }
    }
}
