package com.b8b7.punchlist.mixin;

import com.b8b7.punchlist.FilterState;
import fi.dy.masa.litematica.world.WorldSchematic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Schematic content can change mid-session (rebuild mode). Every
 * mutation routes through this override; the generation bump
 * invalidates enclosure caches so a stale verdict can never wrongly
 * hide a now-visible block. Normal world building never calls this.
 */
@Mixin(value = WorldSchematic.class, remap = false)
public class MixinWorldSchematic {
    @Inject(method = "setBlock", at = @At("TAIL"), require = 0)
    private void punchlist$onSchematicEdit(BlockPos pos, BlockState state, int flags,
                                           CallbackInfoReturnable<Boolean> cir) {
        FilterState.bumpSchematicGeneration();
    }
}
