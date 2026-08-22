#!/usr/bin/env bash
# Static mixin-target verification against arbitrary litematica/malilib jars.
# Checks signatures AND call sites (javap -c), which runtime reflection cannot.
# Usage: verify-targets.sh [litematica.jar] [malilib.jar]   (defaults: libs/)
set -uo pipefail
cd "$(dirname "$0")/.."

LITEMATICA=${1:-$(ls libs/litematica-*.jar 2>/dev/null | head -1)}
MALILIB=${2:-$(ls libs/malilib-*.jar 2>/dev/null | head -1)}
[ -f "$LITEMATICA" ] && [ -f "$MALILIB" ] || { echo "FAIL: jars not found (litematica='$LITEMATICA' malilib='$MALILIB')"; exit 1; }
echo "litematica: $LITEMATICA"
echo "malilib:    $MALILIB"

docker build -q -f Dockerfile.build -t punchlist-build:local . >/dev/null

OUT=$(docker run --rm -v "$PWD":/work -w /work -u "$(id -u):$(id -g)" punchlist-build:local sh -c "
L='$LITEMATICA'; M='$MALILIB'
sig() { javap -p -classpath \"\$1\" \"\$2\" 2>/dev/null; }
dis() { javap -c -p -classpath \"\$1\" \"\$2\" 2>/dev/null; }

RTU=\$(sig \"\$L\" fi.dy.masa.litematica.util.RayTraceUtils)
RTUC=\$(dis \"\$L\" fi.dy.masa.litematica.util.RayTraceUtils)
VBO=\$(sig \"\$L\" fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo)
SV=\$(sig \"\$L\" fi.dy.masa.litematica.schematic.verifier.SchematicVerifier)
SWR=\$(sig \"\$L\" fi.dy.masa.litematica.util.SchematicWorldRefresher)
HK=\$(sig \"\$L\" fi.dy.masa.litematica.config.Hotkeys)
GSV=\$(sig \"\$L\" fi.dy.masa.litematica.gui.GuiSchematicVerifier)
MRT=\$(sig \"\$M\" fi.dy.masa.malilib.util.game.RayTraceUtils)
OVR=\$(sig \"\$L\" fi.dy.masa.litematica.render.OverlayRenderer)
OVRC=\$(dis \"\$L\" fi.dy.masa.litematica.render.OverlayRenderer)
SWH=\$(sig \"\$L\" fi.dy.masa.litematica.world.SchematicWorldHandler)
WVR=\$(sig \"\$L\" fi.dy.masa.litematica.gui.widgets.WidgetSchematicVerificationResult)
WVRC=\$(dis \"\$L\" fi.dy.masa.litematica.gui.widgets.WidgetSchematicVerificationResult)
BMI=\$(sig \"\$L\" 'fi.dy.masa.litematica.gui.widgets.WidgetSchematicVerificationResult\$BlockMismatchInfo')

r() { echo \"\$2\" | grep -qE \"\$3\" && echo \"PASS: \$1\" || echo \"FAIL: \$1\"; }

r 'rayTraceBlocks signature'          \"\$RTU\" 'rayTraceBlocks\(net.minecraft.world.level.Level, net.minecraft.world.phys.Vec3, net.minecraft.world.phys.Vec3, net.minecraft.world.level.ClipContext.Fluid, boolean, boolean, boolean, int\)'
r 'rayTraceBlocksToList signature'    \"\$RTU\" 'rayTraceBlocksToList\(net.minecraft.world.level.Level, net.minecraft.world.phys.Vec3, net.minecraft.world.phys.Vec3, net.minecraft.world.level.ClipContext.Fluid, boolean, boolean, boolean, int\)'
r 'getFurthestSchematicWorldBlockBeforeVanilla' \"\$RTU\" 'getFurthestSchematicWorldBlockBeforeVanilla\(net.minecraft.world.level.Level, net.minecraft.world.entity.Entity, double, boolean\)'
r 'getPickBlockLastTrace signature'   \"\$RTU\" 'getPickBlockLastTrace\(net.minecraft.world.level.Level, net.minecraft.world.entity.Entity, double, boolean\)'
r 'rayTraceSchematicWorldBlocksToList' \"\$RTU\" 'rayTraceSchematicWorldBlocksToList\(net.minecraft.world.level.Level, net.minecraft.world.phys.Vec3, net.minecraft.world.phys.Vec3, int\)'
r 'renderBlocksAndOverlay signature'  \"\$VBO\" 'renderBlocksAndOverlay\(fi.dy.masa.litematica.render.schematic.BlockModelRendererSchematic, fi.dy.masa.litematica.render.schematic.FluidModelRendererSchematic, net.minecraft.core.BlockPos,'
r 'getSelectedMismatchBlockPositionsForRender' \"\$SV\" 'getSelectedMismatchBlockPositionsForRender\(\)'
r 'ACTIVE_VERIFIERS field'            \"\$SV\" 'ACTIVE_VERIFIERS'
r 'SchematicWorldRefresher.updateAll' \"\$SWR\" 'public void updateAll\(\)'
r 'markSchematicChunkForRenderUpdate' \"\$SWR\" 'public void markSchematicChunkForRenderUpdate\(net.minecraft.core.BlockPos\)'
r 'Hotkeys.HOTKEY_LIST field'         \"\$HK\" 'HOTKEY_LIST'
r 'GuiSchematicVerifier.initGui'      \"\$GSV\" 'public void initGui\(\)'
r 'malilib checkRayCollision'         \"\$MRT\" 'checkRayCollision\(fi.dy.masa.malilib.util.game.RayTraceUtils.RayTraceCalculationData, net.minecraft.world.level.Level, boolean\)'
r 'getSelectedMismatchPositionsForRender' \"\$SV\" 'getSelectedMismatchPositionsForRender\(\)'
r 'SchematicWorldHandler.getSchematicWorld' \"\$SWH\" 'public static fi.dy.masa.litematica.world.WorldSchematic getSchematicWorld\(\)'
WSC=\$(sig \"\$L\" fi.dy.masa.litematica.world.WorldSchematic)
r 'WorldSchematic.setBlock override' \"\$WSC\" 'public boolean setBlock\(net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState, int\)'
SPM=\$(sig \"\$L\" fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager)
SP=\$(sig \"\$L\" fi.dy.masa.litematica.schematic.placement.SchematicPlacement)
SBX=\$(sig \"\$L\" fi.dy.masa.litematica.selection.Box)
r 'getAllSchematicsPlacements' \"\$SPM\" 'getAllSchematicsPlacements\(\)'
r 'SchematicPlacement.getOrigin' \"\$SP\" 'public net.minecraft.core.BlockPos getOrigin\(\)'
r 'SchematicPlacement.getRotation' \"\$SP\" 'getRotation\(\)'
r 'SchematicPlacement.getMirror' \"\$SP\" 'getMirror\(\)'
r 'SchematicPlacement.getEclosingBox' \"\$SP\" 'getEclosingBox\(\)'
r 'Box.getPos1/getPos2' \"\$SBX\" 'getPos1\(\)'
r 'renderSchematicVerifierMismatches signature' \"\$OVR\" 'public void renderSchematicVerifierMismatches\(net.minecraft.util.profiling.ProfilerFiller\)'
r 'SchematicVerifier.blockMismatches field' \"\$SV\" 'blockMismatches'
r 'SchematicVerifier.updateMismatchOverlays' \"\$SV\" 'private void updateMismatchOverlays\(\)'
r 'SchematicVerifier.clearActiveMismatchRenderPositions' \"\$SV\" 'private void clearActiveMismatchRenderPositions\(\)'
r 'SchematicVerifier.clearData' \"\$SV\" 'private void clearData\(\)'
r 'SchematicVerifier.combineClosestPositions' \"\$SV\" 'private void combineClosestPositions\(net.minecraft.core.BlockPos, int\)'
r 'SchematicVerifier.clearActiveVerifiers' \"\$SV\" 'public static void clearActiveVerifiers\(\)'
r 'postRenderHovered signature' \"\$WVR\" 'public void postRenderHovered\(fi.dy.masa.malilib.render.GuiContext, int, int, boolean\)'
r 'BlockMismatchInfo.render signature' \"\$BMI\" 'public void render\(fi.dy.masa.malilib.render.GuiContext, int, int\)'

# call sites: getBlockState 2x in each walk method + >=1 in each fallback;
# getFluidState 2x per walk method; checkRayCollision 1x in third walker
gbs=\$(echo \"\$RTUC\" | grep -c 'Method net/minecraft/world/level/Level.getBlockState')
gfs=\$(echo \"\$RTUC\" | grep -c 'Method net/minecraft/world/level/Level.getFluidState')
crc=\$(echo \"\$RTUC\" | grep -c 'Method fi/dy/masa/malilib/util/game/RayTraceUtils.checkRayCollision')
gsm=\$(echo \"\$OVRC\" | grep -c 'Method fi/dy/masa/litematica/schematic/verifier/SchematicVerifier.getSelectedMismatchPositionsForRender')
bmr=\$(echo \"\$WVRC\" | grep -c 'Method fi/dy/masa/litematica/gui/widgets/WidgetSchematicVerificationResult\$BlockMismatchInfo.render')
[ \"\$gbs\" -ge 6 ] && echo \"PASS: Level.getBlockState call sites (\$gbs >= 6)\" || echo \"FAIL: Level.getBlockState call sites (\$gbs < 6)\"
[ \"\$gfs\" -ge 4 ] && echo \"PASS: Level.getFluidState call sites (\$gfs >= 4)\" || echo \"FAIL: Level.getFluidState call sites (\$gfs < 4)\"
[ \"\$crc\" -ge 1 ] && echo \"PASS: checkRayCollision call site (\$crc >= 1)\" || echo \"FAIL: checkRayCollision call site (\$crc < 1)\"
[ \"\$gsm\" -ge 1 ] && echo \"PASS: getSelectedMismatchPositionsForRender call site (\$gsm >= 1)\" || echo \"FAIL: getSelectedMismatchPositionsForRender call site (\$gsm < 1)\"
[ \"\$bmr\" -ge 1 ] && echo \"PASS: BlockMismatchInfo.render call site (\$bmr >= 1)\" || echo \"FAIL: BlockMismatchInfo.render call site (\$bmr < 1)\"
" 2>&1)

echo "$OUT"
echo "$OUT" | grep -q '^FAIL' && exit 1 || exit 0
