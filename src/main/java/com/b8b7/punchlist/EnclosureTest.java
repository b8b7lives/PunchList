package com.b8b7.punchlist;

import fi.dy.masa.litematica.world.WorldSchematic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Shared enclosure predicate for the v0.2 subtraction and the v0.3
 * counts, so the two can never drift apart. A position is enclosed when
 * every face neighbor occludes per vanilla opaque-full-cube semantics;
 * soft occluders (HIDE mode) occlude only members of their own class,
 * and only while shaped as full cubes. Out-of-bounds neighbors read as
 * air in the schematic world, so build boundaries stay exposed.
 */
public final class EnclosureTest {
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Predicate<BlockBehaviour.BlockStateBase> ANY_STATE = s -> true;

    /** Parsed occlusion rules; immutable, safe to hand to a worker thread. */
    public record Spec(EnclosedMode mode, List<TagKey<Block>> softTags, List<Identifier> softBlocks) {}

    /** Exterior flood-fill membership; cells outside the computed bounds are reached. */
    public interface Reached {
        boolean isReached(int x, int y, int z);
    }

    /**
     * Flood pair (v0.8): standard traverses soft occluders (logs stay
     * visible through canopy), hard treats full-cube soft occluders as
     * opaque and is consulted only when the judged block is itself a
     * soft occluder — own-kind occlusion extended from adjacent faces
     * to line-of-sight range. With no soft entries both refer to the
     * same set.
     */
    public record Floods(Reached standard, Reached hard) {}

    private EnclosureTest() {}

    // entries: "#namespace:tag" or "namespace:block"; unparseable entries skipped
    public static Spec spec(EnclosedMode mode, List<String> softCfg) {
        List<TagKey<Block>> tags = new ArrayList<>();
        List<Identifier> blocks = new ArrayList<>();
        if (mode == EnclosedMode.HIDE) {
            for (String entry : softCfg) {
                String s = entry.trim();
                if (s.startsWith("#")) {
                    Identifier id = Identifier.tryParse(s.substring(1));
                    if (id != null) {
                        tags.add(TagKey.create(Registries.BLOCK, id));
                    }
                } else if (!s.isEmpty()) {
                    Identifier id = Identifier.tryParse(s);
                    if (id != null) {
                        blocks.add(id);
                    }
                }
            }
        }
        return new Spec(mode, List.copyOf(tags), List.copyOf(blocks));
    }

    /**
     * Composed predicate (v0.6): hidden iff every face is occluded
     * (tier-1 rules) OR opens onto an unreached cell. floods == null
     * degrades to tier-1-only (fail open). Soft-occluder targets consult
     * the hard flood (v0.8), everything else the standard one.
     */
    public static boolean isHidden(WorldSchematic world, BlockPos pos, Spec spec, Floods floods) {
        boolean selfSoft = spec.mode() == EnclosedMode.HIDE
                && isSoft(world.getBlockState(pos), spec);
        Reached reached = floods == null ? null : (selfSoft ? floods.hard() : floods.standard());
        for (Direction dir : DIRECTIONS) {
            BlockPos npos = pos.relative(dir);
            BlockState neighbor = world.getBlockState(npos);
            if (neighbor.isSolidRender()) {
                continue;
            }
            // soft occluder must be a full-cube shape: a tagged but
            // carpet-shaped block never vouches for a face
            if (selfSoft && isSoft(neighbor, spec)
                    && Block.isShapeFullBlock(neighbor.getCollisionShape(world, npos))) {
                continue;
            }
            if (reached != null && !reached.isReached(npos.getX(), npos.getY(), npos.getZ())) {
                continue;
            }
            return false;
        }
        return true;
    }

    /** Hard-flood opacity: a full-cube soft occluder blocks the hard flood. */
    static boolean blocksHardFlood(WorldSchematic world, BlockPos pos, BlockState state, Spec spec) {
        return isSoft(state, spec) && Block.isShapeFullBlock(state.getCollisionShape(world, pos));
    }

    static boolean isSoft(BlockState state, Spec spec) {
        for (TagKey<Block> tag : spec.softTags()) {
            if (state.is(tag, ANY_STATE)) {
                return true;
            }
        }
        for (Identifier id : spec.softBlocks()) {
            if (state.getBlock().builtInRegistryHolder().is(id)) {
                return true;
            }
        }
        return false;
    }
}
