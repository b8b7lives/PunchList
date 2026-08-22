package com.b8b7.punchlist;

import com.b8b7.punchlist.mixin.SchematicVerifierAccessor;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Re-centers the verifier's closest-N marker window on the player.
 * Upstream re-sorts only on block change / GUI interaction, so the
 * window anchors to the last interaction (#6). The distance threshold
 * doubles as the debounce; stock triggers stay untouched.
 */
public final class FollowPlayer {
    private static Vec3 lastCenter = null;
    private static boolean warnedThisSession = false;

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
}
