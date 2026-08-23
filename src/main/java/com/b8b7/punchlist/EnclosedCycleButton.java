package com.b8b7.punchlist;

import fi.dy.masa.malilib.config.IConfigOptionList;
import fi.dy.masa.malilib.gui.button.ConfigButtonOptionList;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;

// #15: stock hover text follows the cursor and covers the button, so
// the mode label is unreadable while clicking. Anchor the tooltip
// below the button row instead (above is off-screen, the button sits
// at the top edge). drawHoverText paints ~14px above its anchor
// (0.5.1 measurement), compensated in the offset.
public class EnclosedCycleButton extends ConfigButtonOptionList {
    private static final int BELOW_OFFSET = 18;

    public EnclosedCycleButton(int x, int y, int width, int height,
            IConfigOptionList config, String translationKey) {
        super(x, y, width, height, config, translationKey);
    }

    @Override
    public void postRenderHovered(GuiContext ctx, int mouseX, int mouseY, boolean selected) {
        try {
            if (this.hasHoverText()) {
                RenderUtils.drawHoverText(ctx, this.getX(),
                        this.getY() + this.getHeight() + BELOW_OFFSET, this.getHoverStrings());
            }
        } catch (Throwable t) {
            // fall back to the cursor-anchored stock draw
            super.postRenderHovered(ctx, mouseX, mouseY, selected);
        }
    }
}
