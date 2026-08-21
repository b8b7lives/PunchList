package com.b8b7.punchlist.mixin;

import com.b8b7.punchlist.FilterState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.world.WorldSchematic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Filtered positions read as air so the ray walks through them (same
 * mechanism LayerRange uses via respectLayerRange). The WorldSchematic
 * guard leaves vanilla-world traces untouched. require = 0 throughout;
 * CompatCheck gates.
 */
@Mixin(value = RayTraceUtils.class, remap = false)
public class MixinRayTraceUtils {
    // walk methods + pick-block adjacency fallbacks
    @WrapOperation(method = {"rayTraceBlocks", "rayTraceBlocksToList",
            "getFurthestSchematicWorldBlockBeforeVanilla", "getPickBlockLastTrace"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"),
            require = 0)
    private static BlockState punchlist$filterBlockState(Level world, BlockPos pos, Operation<BlockState> original) {
        if (world instanceof WorldSchematic && FilterState.hiddenInTrace(pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return original.call(world, pos);
    }

    @WrapOperation(method = {"rayTraceBlocks", "rayTraceBlocksToList"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"),
            require = 0)
    private static FluidState punchlist$filterFluidState(Level world, BlockPos pos, Operation<FluidState> original) {
        if (world instanceof WorldSchematic && FilterState.hiddenInTrace(pos)) {
            return Fluids.EMPTY.defaultFluidState();
        }
        return original.call(world, pos);
    }

    // third walker uses malilib's collision check, not Level.getBlockState;
    // suppressing the hit keeps the walk going
    @WrapOperation(method = "rayTraceSchematicWorldBlocksToList",
            at = @At(value = "INVOKE",
                    target = "Lfi/dy/masa/malilib/util/game/RayTraceUtils;checkRayCollision(Lfi/dy/masa/malilib/util/game/RayTraceUtils$RayTraceCalculationData;Lnet/minecraft/world/level/Level;Z)Z"),
            require = 0)
    private static boolean punchlist$filterThirdWalker(fi.dy.masa.malilib.util.game.RayTraceUtils.RayTraceCalculationData data,
                                                       Level world, boolean respectLayerRange, Operation<Boolean> original) {
        boolean hit = original.call(data, world, respectLayerRange);
        if (hit && world instanceof WorldSchematic
                && data.trace instanceof BlockHitResult blockHit
                && FilterState.hiddenInTrace(blockHit.getBlockPos())) {
            return false;
        }
        return hit;
    }
}
