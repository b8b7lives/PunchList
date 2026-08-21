# Changelog

## 0.1.1+26.2 (2026-08-20)

- Soft-fail compatibility posture: all mixins require = 0, mixin config
  non-required; runtime CompatCheck reflectively verifies every hook target
  and disables the filter (with a visible message) on incompatible
  Litematica/MaLiLib instead of crashing the game.
- Trace-coverage audit fixes: the two pick-block adjacency fallbacks
  (getFurthestSchematicWorldBlockBeforeVanilla, getPickBlockLastTrace) and
  the third walker (rayTraceSchematicWorldBlocksToList via malilib
  checkRayCollision) now respect the filter; previously these pick-block
  paths could target hidden ghosts.
- harness/verify-targets.sh: static per-release verification of all mixin
  target signatures and call sites against arbitrary litematica/malilib jars.

## 0.1.0+26.2 (2026-08-20)

- Initial release: verifier-filtered schematic rendering + Easy Place /
  pick-block trace pass-through, hotkey + verifier-screen toggle button.
