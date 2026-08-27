package com.b8b7.punchlist;

import fi.dy.masa.malilib.config.IConfigOptionList;
import fi.dy.masa.malilib.gui.button.ConfigButtonOptionList;

// The enclosed mode cycle button with the corner anchored tooltip
public class EnclosedCycleButton extends ConfigButtonOptionList implements CornerTooltip {
    public EnclosedCycleButton(int x, int y, int width, int height,
            IConfigOptionList config, String translationKey) {
        super(x, y, width, height, config, translationKey);
    }
}
