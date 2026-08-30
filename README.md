![PunchList](gallery/banner.webp)

# PunchList

Download on [Modrinth](https://modrinth.com/mod/punchlist),
[CurseForge](https://www.curseforge.com/minecraft/mc-mods/punchlist), or
[GitHub releases](https://github.com/b8b7lives/PunchList/releases).

A Litematica companion mod for Fabric. Pick the categories you want to
work on in the Schematic Verifier (missing, wrong block, wrong state,
extra), toggle the filter, and only those positions render as ghost
blocks. Every other ghost is hidden, and hidden ghosts do not block your
aim, so a flagged block behind unbuilt ghosts can be targeted without
moving the camera into the schematic.

## Features

- **Verifier filter** shows only the positions in your selected verifier
  categories as ghosts. Hidden ghosts do not block Easy Place or pick
  block.
- **Enclosed block hiding** adds a cycle button on the verifier screen. It
  hides positions that are invisible in the finished build, either because
  every face is covered or because they face a sealed interior pocket.
  Leaves only hide other leaves, so canopy hollows and logs threading
  through foliage stay visible. The modes are Show, Hide, and Hide strict.
- **Adjusted counts** appear when you hover a verifier row, showing how
  many of that material are hidden and how many are visible. This works
  even with the filter off.
- **Follow the player** re-centers the verifier marker window as you move.
  Stock Litematica only re-sorts when you place a block or touch the GUI.
  The distance is configurable, defaults to 32 blocks, and 0 turns it off.

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

Pairs with bHold easyPlace. With the filter on, the only ghosts Easy
Place can target are the ones on your list, and hold mode places them
without holding the use key.

## Build

No host JDK required. Everything runs in docker.

```
docker build -f Dockerfile.build -t punchlist-build:local .
docker run --rm -v "$PWD/.gradle-home":/gradle -v "$PWD":/work -w /work \
  -e GRADLE_USER_HOME=/gradle -e CI=true -u "$(id -u):$(id -g)" \
  punchlist-build:local ./gradlew build --no-daemon
```

`libs/` needs the pinned litematica + malilib jars (gitignored).

## Verify

- `harness/verify-targets.sh [litematica.jar] [malilib.jar]` runs a static check
  of every mixin target signature and call site. Run against a new
  Litematica release before widening the supported range.
- `harness/run-loadcheck.sh` boots the production jar with a full mod
  stack (`STACK=<dir>` to point at a mods directory) under xvfb. An in-mod
  probe force-loads every mixin-target class and the script greps for apply
  failures.
