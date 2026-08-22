package com.b8b7.punchlist.mixin;

import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(value = SchematicVerifier.class, remap = false)
public interface SchematicVerifierAccessor {
    @Accessor("ACTIVE_VERIFIERS")
    static List<SchematicVerifier> punchlist$getActiveVerifiers() {
        throw new AssertionError();
    }

    @Accessor("blockMismatches")
    Object2ObjectOpenHashMap<BlockPos, SchematicVerifier.BlockMismatch> punchlist$getBlockMismatches();

    @Invoker("updateMismatchOverlays")
    void punchlist$updateMismatchOverlays();
}
