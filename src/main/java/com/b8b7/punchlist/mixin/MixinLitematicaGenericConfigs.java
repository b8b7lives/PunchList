package com.b8b7.punchlist.mixin;

import com.b8b7.punchlist.PunchListConfigs;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.malilib.config.IConfigBase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

// shadow-initializer reassign: the merged <clinit> appends our entries
// before any reader touches OPTIONS; litematica.json persists them (#14)
@Mixin(value = Configs.Generic.class, remap = false)
public class MixinLitematicaGenericConfigs {
    @Mutable
    @Final
    @Shadow
    public static ImmutableList<IConfigBase> OPTIONS = new ImmutableList.Builder<IConfigBase>()
            .addAll(PunchListConfigs.EXTENDED_OPTIONS)
            .addAll(Configs.Generic.OPTIONS)
            .build();
}
