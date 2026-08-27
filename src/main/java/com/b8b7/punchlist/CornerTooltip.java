package com.b8b7.punchlist;

import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.render.GuiContext;
import java.util.ArrayList;
import net.minecraft.client.gui.Font;

// Marker for verifier-screen buttons whose tooltip anchors its
// top right corner just below the button's bottom right corner,
// extending down and left. MixinGuiBaseButtonTooltip routes hover
// draws for implementors here; any failure falls back to the stock
// cursor draw at the call site.
public interface CornerTooltip {
    int PAD = 3;
    int GAP = 2;
    int BG = 0xF0100010;
    int BORDER = 0xFF505054;

    static void draw(GuiContext ctx, ButtonBase button) {
        // hover strings carry embedded newlines from the lang file; the
        // stock draw splits them and so must we
        ArrayList<String> lines = new ArrayList<>();
        for (String s : button.getHoverStrings()) {
            for (String part : s.split("\n")) {
                lines.add(part);
            }
        }
        if (lines.isEmpty()) {
            return;
        }
        Font font = ctx.fontRenderer();
        int lineH = font.lineHeight + 1;
        int textW = 0;
        for (String s : lines) {
            textW = Math.max(textW, font.width(s));
        }
        int boxW = textW + PAD * 2;
        int boxH = lines.size() * lineH - 1 + PAD * 2;
        int left = Math.max(2, button.getX() + button.getWidth() - boxW);
        int top = button.getY() + button.getHeight() + GAP;

        ctx.fill(left - 1, top - 1, left + boxW + 1, top + boxH + 1, BORDER);
        ctx.fill(left, top, left + boxW, top + boxH, BG);
        int ty = top + PAD;
        for (String s : lines) {
            ctx.text(font, s, left + PAD, ty, 0xFFFFFFFF, true);
            ty += lineH;
        }
    }
}
