package com.b8b7.punchlist;

import fi.dy.masa.malilib.gui.button.ButtonOnOff;

// The PunchList on/off toggle with the corner anchored tooltip
public class FilterToggleButton extends ButtonOnOff implements CornerTooltip {
    public FilterToggleButton(int x, int y, int width, boolean rightAlign,
            String translationKey, boolean isCurrentlyOn, String... hoverStrings) {
        super(x, y, width, rightAlign, translationKey, isCurrentlyOn, hoverStrings);
    }
}
