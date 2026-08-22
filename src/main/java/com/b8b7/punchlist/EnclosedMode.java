package com.b8b7.punchlist;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum EnclosedMode implements IConfigOptionListEntry {
    SHOW("show", "punchlist.gui.label.enclosed_mode.show"),
    HIDE("hide", "punchlist.gui.label.enclosed_mode.hide"),
    HIDE_STRICT("hide_strict", "punchlist.gui.label.enclosed_mode.hide_strict");

    public static final ImmutableList<EnclosedMode> VALUES = ImmutableList.copyOf(values());

    private final String configString;
    private final String translationKey;

    EnclosedMode(String configString, String translationKey) {
        this.configString = configString;
        this.translationKey = translationKey;
    }

    @Override
    public String getStringValue() {
        return this.configString;
    }

    @Override
    public String getDisplayName() {
        return StringUtils.translate(this.translationKey);
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward) {
        int size = VALUES.size();
        return VALUES.get(((this.ordinal() + (forward ? 1 : -1)) % size + size) % size);
    }

    @Override
    public EnclosedMode fromString(String value) {
        for (EnclosedMode mode : VALUES) {
            if (mode.configString.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return SHOW;
    }
}
