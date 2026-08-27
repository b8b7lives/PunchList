package com.b8b7.punchlist.mixin;

import com.b8b7.punchlist.CornerTooltip;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.render.GuiContext;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// drawButtonHoverTexts is the real button-tooltip path (verified
// in bytecode; postRenderHovered is never called for buttons). Wrap
// its drawHoverText call so the Enclosed button gets the corner
// anchored draw; every other button keeps stock behavior, and any
// failure falls through to the original call.
@Mixin(value = GuiBase.class, remap = false)
public class MixinGuiBaseButtonTooltip {
    @WrapOperation(method = "drawButtonHoverTexts",
            at = @At(value = "INVOKE",
                    target = "Lfi/dy/masa/malilib/render/RenderUtils;drawHoverText(Lfi/dy/masa/malilib/render/GuiContext;IILjava/util/List;)V"),
            require = 0)
    private void punchlist$cornerAnchoredTooltip(GuiContext ctx, int x, int y,
            List<String> lines, Operation<Void> original,
            @Local ButtonBase button) {
        if (button instanceof CornerTooltip) {
            try {
                CornerTooltip.draw(ctx, button);
                return;
            } catch (Throwable t) {
                // fall through to the stock cursor draw
            }
        }
        original.call(ctx, x, y, lines);
    }
}
