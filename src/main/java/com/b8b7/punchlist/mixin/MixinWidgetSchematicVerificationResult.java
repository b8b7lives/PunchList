package com.b8b7.punchlist.mixin;

import com.b8b7.punchlist.EnclosedCounts;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicVerificationResult;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Adjusted-count hover: draws the hidden/visible split below litematica's
 * own mismatch popup. require = 0; failure = no count line only.
 */
@Mixin(value = WidgetSchematicVerificationResult.class, remap = false)
public abstract class MixinWidgetSchematicVerificationResult {
    @Shadow
    @Final
    private GuiSchematicVerifier.BlockMismatchEntry mismatchEntry;

    @WrapOperation(method = "postRenderHovered",
            at = @At(value = "INVOKE",
                    target = "Lfi/dy/masa/litematica/gui/widgets/WidgetSchematicVerificationResult$BlockMismatchInfo;render(Lfi/dy/masa/malilib/render/GuiContext;II)V"),
            require = 0)
    private void punchlist$appendEnclosedCounts(WidgetSchematicVerificationResult.BlockMismatchInfo info,
                                                GuiContext ctx, int x, int y, Operation<Void> original) {
        original.call(info, ctx, x, y);
        try {
            GuiSchematicVerifier.BlockMismatchEntry entry = this.mismatchEntry;
            if (entry == null || entry.blockMismatch == null) {
                return;
            }
            List<String> lines = EnclosedCounts.hoverLines(entry.blockMismatch);
            if (lines != null) {
                RenderUtils.drawHoverText(ctx, x, y + info.getTotalHeight() + 16, lines);
            }
        } catch (Throwable ignored) {
            // no count line; litematica's popup already rendered
        }
    }
}
