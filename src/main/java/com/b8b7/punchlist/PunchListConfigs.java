package com.b8b7.punchlist;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigHotkey;

// binding persists via litematica's config; filter state is deliberately
// runtime-only, so the mod starts OFF every launch
public class PunchListConfigs {
    public static final ConfigHotkey FILTER_TOGGLE =
            (ConfigHotkey) new ConfigHotkey("punchListFilterToggle", "").apply("punchlist.hotkey");

    public static final ImmutableList<ConfigHotkey> EXTENDED_HOTKEYS = ImmutableList.of(
            FILTER_TOGGLE
    );
}
