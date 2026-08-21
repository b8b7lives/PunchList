package com.b8b7.punchlist;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Soft-fail gate: mixins ship require = 0, so this check verifies each hook
 * target reflectively; any core target missing disables the filter with a
 * visible message instead of crashing. Signatures only — call-site injection
 * is verified statically by harness/verify-targets.sh.
 */
public final class CompatCheck {
    private static Boolean coreOk = null;
    private static final List<String> missingCore = new ArrayList<>();
    private static final List<String> missingOptional = new ArrayList<>();

    private CompatCheck() {}

    /** Lazy, idempotent; safe once the client is fully constructed. */
    public static synchronized boolean ensureRun() {
        if (coreOk != null) {
            return coreOk;
        }
        // core: absence disables the filter
        core("RayTraceUtils.rayTraceBlocks", () ->
                fi.dy.masa.litematica.util.RayTraceUtils.class.getDeclaredMethod("rayTraceBlocks",
                        Level.class, Vec3.class, Vec3.class, ClipContext.Fluid.class,
                        boolean.class, boolean.class, boolean.class, int.class));
        core("RayTraceUtils.rayTraceBlocksToList", () ->
                fi.dy.masa.litematica.util.RayTraceUtils.class.getDeclaredMethod("rayTraceBlocksToList",
                        Level.class, Vec3.class, Vec3.class, ClipContext.Fluid.class,
                        boolean.class, boolean.class, boolean.class, int.class));
        core("RayTraceUtils.getFurthestSchematicWorldBlockBeforeVanilla", () ->
                fi.dy.masa.litematica.util.RayTraceUtils.class.getDeclaredMethod("getFurthestSchematicWorldBlockBeforeVanilla",
                        Level.class, Entity.class, double.class, boolean.class));
        core("RayTraceUtils.getPickBlockLastTrace", () ->
                fi.dy.masa.litematica.util.RayTraceUtils.class.getDeclaredMethod("getPickBlockLastTrace",
                        Level.class, Entity.class, double.class, boolean.class));
        core("RayTraceUtils.rayTraceSchematicWorldBlocksToList", () ->
                fi.dy.masa.litematica.util.RayTraceUtils.class.getDeclaredMethod("rayTraceSchematicWorldBlocksToList",
                        Level.class, Vec3.class, Vec3.class, int.class));
        core("malilib RayTraceUtils.checkRayCollision", () ->
                fi.dy.masa.malilib.util.game.RayTraceUtils.class.getDeclaredMethod("checkRayCollision",
                        fi.dy.masa.malilib.util.game.RayTraceUtils.RayTraceCalculationData.class,
                        Level.class, boolean.class));
        core("ChunkRendererSchematicVbo.renderBlocksAndOverlay", () ->
                fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo.class.getDeclaredMethod("renderBlocksAndOverlay",
                        fi.dy.masa.litematica.render.schematic.BlockModelRendererSchematic.class,
                        fi.dy.masa.litematica.render.schematic.FluidModelRendererSchematic.class,
                        BlockPos.class,
                        fi.dy.masa.litematica.render.schematic.ChunkRenderDataSchematic.class,
                        fi.dy.masa.litematica.render.schematic.ChunkMeshDataSchematic.class,
                        fi.dy.masa.litematica.render.schematic.ChunkRenderDispatcherBuffers.class,
                        fi.dy.masa.litematica.render.schematic.IBlockOutputSchematic.class,
                        Vec3.class,
                        net.minecraft.client.renderer.chunk.VisGraph.class));
        core("SchematicVerifier.getSelectedMismatchBlockPositionsForRender", () ->
                fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.class.getDeclaredMethod("getSelectedMismatchBlockPositionsForRender"));
        core("SchematicVerifier.ACTIVE_VERIFIERS", () ->
                fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.class.getDeclaredField("ACTIVE_VERIFIERS"));
        core("SchematicWorldRefresher.updateAll", () ->
                fi.dy.masa.litematica.util.SchematicWorldRefresher.class.getDeclaredMethod("updateAll"));

        // optional: absence degrades UI only
        optional("Hotkeys.HOTKEY_LIST", () ->
                fi.dy.masa.litematica.config.Hotkeys.class.getDeclaredField("HOTKEY_LIST"));
        optional("GuiSchematicVerifier.initGui", () ->
                fi.dy.masa.litematica.gui.GuiSchematicVerifier.class.getDeclaredMethod("initGui"));

        coreOk = missingCore.isEmpty();
        if (!coreOk) {
            PunchListClient.LOGGER.error("PunchList disabled: incompatible Litematica/MaLiLib. Missing core targets: {}", missingCore);
        }
        if (!missingOptional.isEmpty()) {
            PunchListClient.LOGGER.warn("PunchList: optional targets missing (degraded UI, filter unaffected): {}", missingOptional);
        }
        return coreOk;
    }

    private interface Probe {
        Object get() throws Throwable;
    }

    private static void core(String name, Probe probe) {
        if (!present(probe)) {
            missingCore.add(name);
        }
    }

    private static void optional(String name, Probe probe) {
        if (!present(probe)) {
            missingOptional.add(name);
        }
    }

    private static boolean present(Probe probe) {
        try {
            return probe.get() != null;
        } catch (Throwable t) {
            return false;
        }
    }
}
