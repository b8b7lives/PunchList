package com.b8b7.punchlist.mixin;

import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = SchematicVerifier.class, remap = false)
public interface SchematicVerifierAccessor {
    @Accessor("ACTIVE_VERIFIERS")
    static List<SchematicVerifier> punchlist$getActiveVerifiers() {
        throw new AssertionError();
    }
}
