package com.b8b7.punchlist.mixin;

import com.b8b7.punchlist.FilterState;
import fi.dy.masa.litematica.render.schematic.BlockModelRendererSchematic;
import fi.dy.masa.litematica.render.schematic.ChunkMeshDataSchematic;
import fi.dy.masa.litematica.render.schematic.ChunkRenderDataSchematic;
import fi.dy.masa.litematica.render.schematic.ChunkRenderDispatcherBuffers;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo;
import fi.dy.masa.litematica.render.schematic.FluidModelRendererSchematic;
import fi.dy.masa.litematica.render.schematic.IBlockOutputSchematic;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChunkRendererSchematicVbo.class, remap = false)
public class MixinChunkRendererSchematicVbo {
    // cancel skips both the ghost block and its overlay quads;
    // require = 0 — CompatCheck gates
    @Inject(method = "renderBlocksAndOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void punchlist$skipHidden(BlockModelRendererSchematic blockRenderer,
                                      FluidModelRendererSchematic fluidRenderer,
                                      BlockPos pos,
                                      ChunkRenderDataSchematic data,
                                      ChunkMeshDataSchematic chunkMeshData,
                                      ChunkRenderDispatcherBuffers pack,
                                      IBlockOutputSchematic blockOutput,
                                      Vec3 offset, VisGraph visGraph,
                                      CallbackInfo ci) {
        if (FilterState.hiddenInRender(pos)) {
            ci.cancel();
        }
    }
}
