package com.b8b7.punchlist.mixin;

import com.b8b7.punchlist.FilterState;
import com.b8b7.punchlist.PunchListClient;
import com.b8b7.punchlist.PunchListConfigs;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import fi.dy.masa.malilib.gui.button.ConfigButtonOptionList;
import fi.dy.masa.malilib.util.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// cosmetic mirror of the hotkey; fail-soft (require = 0, guarded).
// the enclosed cycle button has no hotkey by design: durable path is the
// config file
@Mixin(value = GuiSchematicVerifier.class, remap = false)
public abstract class MixinGuiSchematicVerifier {
    private static final int ENCLOSED_BUTTON_WIDTH = 140;

    @Inject(method = "initGui", at = @At("TAIL"), require = 0)
    private void punchlist$addFilterButton(CallbackInfo ci) {
        try {
            GuiSchematicVerifier self = (GuiSchematicVerifier) (Object) this;
            ButtonOnOff button = new com.b8b7.punchlist.FilterToggleButton(
                    self.getScreenWidth() - 12, 20, -1, true,
                    "punchlist.gui.button.filter", FilterState.isEnabled(),
                    StringUtils.translate("punchlist.gui.button.hover.filter"));
            ((GuiBase) self).addButton(button, (btn, mouseButton) -> {
                boolean on = FilterState.toggle();
                if (btn instanceof ButtonOnOff onOff) {
                    onOff.updateDisplayString(on);
                }
            });

            // ConfigButtonOptionList cycles the value itself (right-click =
            // backward); the listener only persists it
            ConfigButtonOptionList enclosedButton = new com.b8b7.punchlist.EnclosedCycleButton(
                    self.getScreenWidth() - 12 - button.getWidth() - 4 - ENCLOSED_BUTTON_WIDTH, 20,
                    ENCLOSED_BUTTON_WIDTH, 20,
                    PunchListConfigs.ENCLOSED_MODE, "punchlist.gui.button.enclosed");
            enclosedButton.setHoverStrings(StringUtils.translate("punchlist.gui.button.hover.enclosed"));
            ((GuiBase) self).addButton(enclosedButton,
                    (btn, mouseButton) -> {
                        // persist immediately; litematica also saves on GUI close
                        try {
                            ConfigManager.getInstance().onConfigsChanged("litematica");
                        } catch (Throwable t) {
                            // save on GUI close still covers it
                        }
                    });
        } catch (Throwable t) {
            PunchListClient.LOGGER.warn("PunchList: could not add verifier screen buttons (hotkey and config file still work)", t);
        }
    }
}
