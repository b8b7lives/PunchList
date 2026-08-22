package com.b8b7.punchlist.mixin;

import com.b8b7.punchlist.FilterState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Marker overlay respects the enclosed layer: without this, enclosed
 * positions keep their verifier marker boxes with no ghost under them.
 * require = 0; failure degrades to unfiltered markers only.
 */
@Mixin(value = OverlayRenderer.class, remap = false)
public class MixinOverlayRenderer {
    @WrapOperation(method = "renderSchematicVerifierMismatches",
            at = @At(value = "INVOKE",
                    target = "Lfi/dy/masa/litematica/schematic/verifier/SchematicVerifier;getSelectedMismatchPositionsForRender()Ljava/util/List;"),
            require = 0)
    private List<SchematicVerifier.MismatchRenderPos> punchlist$filterEnclosedMarkers(
            SchematicVerifier verifier, Operation<List<SchematicVerifier.MismatchRenderPos>> original) {
        return FilterState.filterMarkers(original.call(verifier));
    }
}
