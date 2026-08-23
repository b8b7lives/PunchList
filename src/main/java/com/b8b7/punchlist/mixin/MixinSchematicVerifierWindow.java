package com.b8b7.punchlist.mixin;

import com.b8b7.punchlist.FilterState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Comparator;
import java.util.List;

/**
 * Filter-aware window fill: enclosure-hidden candidates are dropped
 * before the closest-N sort in addAndSortPositions (single call site
 * covering all five mismatch types), so the marker window budget only
 * counts actionable blocks (#10). require = 0; failure degrades to
 * the render-time marker filter alone.
 */
@Mixin(value = SchematicVerifier.class, remap = false)
public class MixinSchematicVerifierWindow {
    @WrapOperation(method = "addAndSortPositions",
            at = @At(value = "INVOKE",
                    target = "Ljava/util/List;sort(Ljava/util/Comparator;)V"),
            require = 0)
    private void punchlist$filterWindowCandidates(List<BlockPos> list, Comparator<? super BlockPos> comparator,
            Operation<Void> original) {
        FilterState.filterWindowCandidates(list);
        original.call(list, comparator);
    }
}
