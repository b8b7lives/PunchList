package com.b8b7.punchlist.mixin;

import com.b8b7.punchlist.FilterState;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Render-list change signals (writer inventory, 0.28.4): lets build()
 * cache its set copy instead of re-copying every tick. Best-effort
 * (require = 0): a per-tick size check in FilterState backstops any
 * missing hook.
 */
@Mixin(value = SchematicVerifier.class, remap = false)
public class MixinSchematicVerifierDirty {
    @Inject(method = {"updateMismatchOverlays", "clearActiveMismatchRenderPositions",
            "clearData", "combineClosestPositions"}, at = @At("TAIL"), require = 0)
    private void punchlist$listChanged(CallbackInfo ci) {
        FilterState.bumpListGeneration();
    }

    @Inject(method = "clearActiveVerifiers", at = @At("TAIL"), require = 0)
    private static void punchlist$listChangedStatic(CallbackInfo ci) {
        FilterState.bumpListGeneration();
    }
}
