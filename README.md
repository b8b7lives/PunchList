![PunchList](gallery/banner.webp)

# PunchList

A Litematica companion mod for Fabric. Work the Schematic Verifier's punch
list. While the filter is on, only the mismatch positions selected in the
verifier GUI render as ghost blocks. Easy Place and pick block pass through
everything else, so flagged blocks behind unbuilt ghosts can be targeted
directly.

A punch list is the construction-industry term for the inspection's list of
remaining defects: fix only these to finish the job. The verifier is the
inspection; this mod lets you work the list.

## Features

- **Verifier filter** — while on, only the mismatch positions selected in
  the verifier render as ghosts; traces pass through everything else.
- **Enclosed-block hiding** — a cycle button on the verifier screen hides
  positions that are invisible in the finished build: every face covered,
  or facing a sealed interior pocket no player can see into. Leaves only
  hide other leaves, so canopy hollows and logs threading through foliage
  stay visible. Three modes: Show, Hide, Hide (strict).
- **Adjusted counts** — hovering a verifier row shows the hidden/visible
  split for that material ("396,000 hidden, 4,000 visible"), so counts
  reflect the work that actually shows. Works even with the filter off.
- **Follow the player** — the verifier's closest-N marker window
  re-centers as you move (default every 32 blocks; stock only re-sorts
  when you place a block or touch the GUI). Configurable, 0 disables.

Everything fails open: no verifier, empty selection, or any internal error
means the schematic renders exactly as stock Litematica would.

## Usage

1. Run the Schematic Verifier on a placement and select the categories or
   entries to work on (same selection that drives the verifier's overlay
   markers).
2. Toggle the filter: hotkey `punchListFilterToggle` (Litematica > Hotkeys,
   unbound by default) or the `PunchList` button on the verifier screen.
3. With nothing selected in the verifier, the filter does nothing. The
   filter always starts off at launch.

On an incompatible Litematica/MaLiLib version the mod disables itself with
a chat message instead of crashing.

## Targets

- Minecraft 26.2, Fabric
- Litematica 0.28.4 + MaLiLib 0.29.3 (sakura-ryoko builds)

## Build

No host JDK required: everything runs in docker.

```
docker build -f Dockerfile.build -t punchlist-build:local .
docker run --rm -v "$PWD/.gradle-home":/gradle -v "$PWD":/work -w /work \
  -e GRADLE_USER_HOME=/gradle -e CI=true -u "$(id -u):$(id -g)" \
  punchlist-build:local ./gradlew build --no-daemon
```

`libs/` needs the pinned litematica + malilib jars (gitignored).

## Verify

- `harness/verify-targets.sh [litematica.jar] [malilib.jar]` — static check
  of every mixin target signature and call site. Run against a new
  Litematica release before widening the supported range.
- `harness/run-loadcheck.sh` — boots the production jar with a full mod
  stack (`STACK=<dir>` to point at a mods directory) under xvfb; an in-mod
  probe force-loads every mixin-target class and the script greps for apply
  failures.
