package com.b8b7.punchlist.mixin;

import com.b8b7.punchlist.FilterState;
import com.b8b7.punchlist.PunchListClient;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import fi.dy.masa.malilib.util.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// cosmetic mirror of the hotkey; fail-soft (require = 0, guarded)
@Mixin(value = GuiSchematicVerifier.class, remap = false)
public abstract class MixinGuiSchematicVerifier {
    @Inject(method = "initGui", at = @At("TAIL"), require = 0)
    private void punchlist$addFilterButton(CallbackInfo ci) {
        try {
            GuiSchematicVerifier self = (GuiSchematicVerifier) (Object) this;
            ButtonOnOff button = new ButtonOnOff(
                    self.getScreenWidth() - 12, 20, -1, true,
                    "punchlist.gui.button.filter", FilterState.isEnabled(),
                    StringUtils.translate("punchlist.gui.button.hover.filter"));
            ((GuiBase) self).addButton(button, (btn, mouseButton) -> {
                boolean on = FilterState.toggle();
                if (btn instanceof ButtonOnOff onOff) {
                    onOff.updateDisplayString(on);
                }
            });
        } catch (Throwable t) {
            PunchListClient.LOGGER.warn("PunchList: could not add verifier screen button (hotkey still works)", t);
        }
    }
}
