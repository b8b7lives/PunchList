package com.b8b7.punchlist.mixin;

import com.b8b7.punchlist.PunchListConfigs;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

// shadow-initializer reassign: the merged <clinit> appends our entries
// before any reader touches HOTKEY_LIST
@Mixin(value = Hotkeys.class, remap = false)
public class MixinLitematicaHotkeys {
    @Mutable
    @Final
    @Shadow
    public static List<ConfigHotkey> HOTKEY_LIST = new ImmutableList.Builder<ConfigHotkey>()
            .addAll(PunchListConfigs.EXTENDED_HOTKEYS)
            .addAll(Hotkeys.HOTKEY_LIST)
            .build();
}
