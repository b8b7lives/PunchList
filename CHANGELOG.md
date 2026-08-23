# Changelog

## 0.8.2+26.2 (2026-08-23)

- Fix attempt: the Enclosed button's tooltip is drawn at a fixed spot
  below the button row instead of following the cursor. Field note, it
  can still overlap the button. A corner anchored version is planned.

## 0.8.1+26.2 (2026-08-23)

- Feature: all settings now appear in Litematica's config screen under
  the Generic tab (enclosedMode, enclosedSoftOccluders,
  followPlayerDistance, pocketFill, pocketFillMaxVolume), searchable
  like any Litematica option, persisted in litematica.json by
  Litematica's own config handling. The old config/punchlist.json is
  read once at first launch, applied, and renamed to
  punchlist.json.migrated.
- The Enclosed cycle button on the verifier screen now saves the mode
  through the same store immediately.

## 0.8.0+26.2 (2026-08-22)

- Feature: canopy aware hiding. Interior leaves now count as hidden.
  The pocket flood deliberately travels through leaves so logs and
  branches inside a canopy stay visible, but that also meant every
  interior air gap counted as seen from outside, so almost every leaf
  next to one counted as visible work. A second flood that treats
  leaves as opaque now serves leaf targets only. On a large organic
  tree the leaves counts invert from nearly all visible to nearly all
  hidden, and the marker window and hover counts follow. Strict mode
  is unchanged.

## 0.7.0+26.2 (2026-08-22)

- Feature: the verifier marker window is now filter aware. Enclosure
  hidden positions are dropped before the closest sort, so the window
  budget (verifierErrorHilightMaxPositions) only counts blocks you can
  actually work on. Previously the closest N could be entirely interior
  positions, leaving the screen empty while placeable work sat just
  past the window edge, and the workaround was raising the cap or
  flying until a re-center rolled a better window. The nearest visible
  work now always renders, and a genuinely empty screen means
  everything left in range is enclosed. The info overlay's closest
  position lists inherit the filtering. When the window is found
  holding hidden markers (fresh enclosure results landing), it refills
  automatically within half a second.

## 0.6.5+26.2 (2026-08-22)

- Fix: lowering verifierErrorHilightMaxPositions now takes effect within
  half a second. Litematica only re-sorts the marker window on block
  changes or verifier GUI interaction, so a lowered cap left a stale
  oversized window (and its performance cost) in place until something
  else triggered a re-sort. Recovery previously required resetting the
  verifier. The mod now watches the config value and refreshes the
  window when it changes.

## 0.6.4+26.2 (2026-08-21)

- Audit fixes (adversarial review): the working-set cache now hashes
  actual position content, so a same-size window swap can never serve
  a stale set even if a render-list hook fails to apply; a pocket-fill
  computation finishing after its inputs changed (or after the feature
  was turned off) is discarded instead of resurrecting stale
  reachability data; any input change drops to plain enclosure (fail
  open) until the new flood publishes rather than mixing old
  reachability with new geometry; failed background computations no
  longer retry every tick; the enclosure memo's clear is atomic with
  its generation bump; counts staleness detection strengthened; the
  render-list hooks and flood stack growth gained probes/guards.

## 0.6.3+26.2 (2026-08-21)

- Fix: ghosts now refill in real time while placing. When the verifier
  window (verifierErrorHilightMaxPositions) refilled after placements,
  the new positions sat in chunks that never re-meshed, and the full
  re-mesh debounce reset on every change. Steady placing postponed it
  indefinitely, so the punchlist visually "ran out" of blocks until
  you moved away or toggled the filter. Window changes now re-mesh
  only the affected chunks, immediately for small deltas; large deltas
  (GUI mass-selection changes) stay debounced with a hard deadline so
  they can neither storm nor stall forever.
- Side effect: selection changes no longer trigger full-schematic
  re-meshes at all - category clicks and re-centers got much cheaper.

## 0.6.2+26.2 (2026-08-21)

- Perf: the filter's working set is no longer re-copied and re-compared
  every tick. Render-list change hooks (with a size-check fallback)
  drive rebuilds event-wise, and unchanged sets stay
  instance-identical so per-tick comparisons are O(1). Removes
  constant client-thread overhead that scaled with
  verifierErrorHilightMaxPositions and worsened placement lag spikes
  at raised caps.

## 0.6.1+26.2 (2026-08-21)

- Enclosed button tooltip rewritten: five short lines instead of a
  wall of text, mode names highlighted, content current for 0.6
  (pocket fill, counts-vs-hiding split). Smaller tooltip box also
  covers less of the button row. Config comments updated to match.

## 0.6.0+26.2 (2026-08-21)

- Pocket fill: Hide modes now also hide positions whose exposed faces
  only open into sealed interior pockets - air spaces in the finished
  build with no path to the outside. An exterior flood fill over the
  schematic's intended states decides reachability; leaves and glass
  count as see-through, so structure inside foliage still shows and
  canopy behavior is unchanged. Configurable (pocketFill, on by
  default; pocketFillMaxVolume guard). Hover counts include pocket
  hiding automatically.
- Enclosure memo: every position's hidden verdict is computed once per
  placement state instead of on every selection change, keyed to
  placement transforms (origin/rotation/mirror moves now invalidate
  everything correctly), schematic edits, mode/config changes, and
  flood updates. Removes the per-placement recompute cost during
  building; hover counts settle in ~0.5s instead of ~2s.
- Fail open throughout: flood not yet computed, missing hooks, volume
  cap exceeded, or any error means plain 0.5.x enclosure behavior.

## 0.5.1+26.2 (2026-08-21)

- Fix: the enclosed hidden/visible hover line no longer overlaps the
  verifier popup's block-state property lines. malilib's hover-text
  helper draws about 14px above its anchor point; the anchor now
  compensates, placing the line just below the popup.

## 0.5.0+26.2 (2026-08-21)

- Rendermatica integration: when Rendermatica's cached verifier overlay
  is installed, PunchList registers its enclosed-marker filter through
  Rendermatica's marker transformer API, so enclosed positions stay
  hidden in the cached overlay too. Without Rendermatica nothing
  changes. (Rendermatica disables its cache when it detects a PunchList
  older than this version.)

## 0.4.1+26.2 (2026-08-21)

- Fix: editing the schematic mid-session (rebuild mode - removing,
  replacing, or re-rotating schematic blocks) now invalidates the
  enclosed-block caches immediately. Previously a stale enclosure
  verdict could keep hiding a block that a schematic edit had made
  visible, until the selection happened to change. Hover counts pick
  up schematic edits the same way.
- No behavior change for sessions that never edit the schematic.

## 0.4.0+26.2 (2026-08-21)

- Follow the player: the Schematic Verifier's closest-N marker window
  re-centers on the player after moving followPlayerDistance blocks
  (default 32; 0 restores stock behavior). Stock litematica only
  re-sorts the window on block changes and GUI interaction, leaving it
  anchored wherever you last placed a block. Stock triggers are
  untouched; litematica's own re-sort logic is reused wholesale.
- Fail open: if the hook is unavailable the feature disables itself
  and marker behavior is exactly stock.

## 0.3.0+26.2 (2026-08-21)

- Adjusted counts: with any Enclosed Hide mode set, hovering a row in
  the Schematic Verifier list shows how many of that row's positions
  are enclosed and how many remain visible ("Enclosed: 396,000 hidden,
  4,000 visible"). Computed over the full mismatch set on a background
  thread with the same enclosure rules as the hiding; counts appear
  even with the PunchList filter off (information only - hiding still
  requires the filter). Verifier data, sorting, and selection are
  untouched.
- Shows "Enclosed: computing..." briefly after a scan on large
  schematics; counts settle once the verifier stops changing.
- Fail open: missing hook targets or any computation error removes the
  hover line and nothing else.

## 0.2.0+26.2 (2026-08-21)

- Enclosed-block layer: optionally hide punchlist positions that cannot
  be seen in the finished build (every face occluded per vanilla
  opaque-full-cube semantics, checked against the schematic's intended
  states). Leaves act as soft occluders in Hide mode: they hide only
  other leaves-class blocks, so canopy interiors hollow while logs and
  branches threading through foliage always stay listed. Hide (strict)
  restricts occlusion to opaque full cubes only.
- Verifier mismatch markers respect the layer (no floating marker boxes
  on hidden positions); the verifier GUI list and counts are unchanged.
- New cycle button on the verifier screen (left-click forward,
  right-click back); mode persists in config/punchlist.json. No hotkey
  by design. Soft-occluder classes configurable
  (enclosedSoftOccluders: '#namespace:tag' or 'namespace:block'
  entries, default '#minecraft:leaves').
- Fail open throughout: missing hook targets, absent schematic world,
  or any error in the enclosure check disables only the subtraction
  and falls back to 0.1 behavior.

## 0.1.2+26.2 (2026-08-20)

- Mod icon.
- fabric.mod.json contact metadata (homepage, sources, issues).
- No functional changes.

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
