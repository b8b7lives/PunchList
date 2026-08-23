package com.b8b7.punchlist;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.world.WorldSchematic;
import net.minecraft.core.BlockPos;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Exterior visibility floods: 6-connected traversal of non-solid cells
 * over the union of enclosing boxes of verifier'd placements, expanded
 * by one; the expanded shell seeds each flood. Two floods per epoch
 * (v0.8): standard traverses soft occluders and glass (see-through;
 * conservative = fewer hidden); hard treats full-cube soft occluders
 * as opaque and serves soft-occluder targets only. With no soft
 * entries the pair aliases one set. null result = tier-1-only behavior
 * (fail open). Computed per generation base on a worker, debounced;
 * the previous pair stays in use until the new one publishes.
 */
public final class PocketFill {
    // ~2s at 20 tps: rebuild-mode edit spam must not thrash the worker
    private static final int STABLE_TICKS = 40;

    static final class ReachedSet implements EnclosureTest.Reached {
        private final int minX;
        private final int minY;
        private final int minZ;
        private final int sizeX;
        private final int sizeY;
        private final int sizeZ;
        private final long[] bits;

        ReachedSet(int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ, long[] bits) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.bits = bits;
        }

        @Override
        public boolean isReached(int x, int y, int z) {
            int dx = x - minX;
            int dy = y - minY;
            int dz = z - minZ;
            if (dx < 0 || dy < 0 || dz < 0 || dx >= sizeX || dy >= sizeY || dz >= sizeZ) {
                return true;
            }
            int idx = (dy * sizeZ + dz) * sizeX + dx;
            return (bits[idx >> 6] & (1L << (idx & 63))) != 0;
        }
    }

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "punchlist-pocket");
        t.setDaemon(true);
        return t;
    });

    private static volatile EnclosureTest.Floods current = null;
    private static final AtomicLong EPOCH = new AtomicLong(0);
    // bumped on every clear/disable; in-flight computes captured an older
    // ticket and discard their result instead of resurrecting stale data
    private static final AtomicLong TICKET = new AtomicLong(0);
    private static volatile boolean computing = false;
    private static volatile long computedBase = Long.MIN_VALUE;

    // client-thread only
    private static long lastBase = Long.MIN_VALUE;
    private static int stableTicks = 0;
    private static boolean warnedVolume = false;
    private static boolean warnedThisSession = false;

    private PocketFill() {}

    static EnclosureTest.Floods current() {
        return current;
    }

    static long epoch() {
        return EPOCH.get();
    }

    static void clientTick(EnclosureTest.Spec spec, WorldSchematic world, long base) {
        try {
            if (spec.mode() == EnclosedMode.SHOW || world == null
                    || !PunchListConfigs.POCKET_FILL.getBooleanValue() || !CompatCheck.pocketOk()) {
                clearCurrent();
                computedBase = Long.MIN_VALUE;
                return;
            }
            if (base != lastBase) {
                lastBase = base;
                stableTicks = 0;
                // inputs changed: the old reached-set describes geometry
                // that no longer exists; drop to tier-1 (fail open) until
                // the new flood publishes
                clearCurrent();
                return;
            }
            if (base == computedBase || computing || ++stableTicks < STABLE_TICKS) {
                return;
            }

            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            boolean any = false;
            for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements()) {
                if (!placement.hasVerifier()) {
                    continue;
                }
                Box box = placement.getEclosingBox();
                if (box == null || box.getPos1() == null || box.getPos2() == null) {
                    continue;
                }
                BlockPos p1 = box.getPos1();
                BlockPos p2 = box.getPos2();
                minX = Math.min(minX, Math.min(p1.getX(), p2.getX()));
                minY = Math.min(minY, Math.min(p1.getY(), p2.getY()));
                minZ = Math.min(minZ, Math.min(p1.getZ(), p2.getZ()));
                maxX = Math.max(maxX, Math.max(p1.getX(), p2.getX()));
                maxY = Math.max(maxY, Math.max(p1.getY(), p2.getY()));
                maxZ = Math.max(maxZ, Math.max(p1.getZ(), p2.getZ()));
                any = true;
            }
            if (!any) {
                clearCurrent();
                return;
            }
            int sx = maxX - minX + 3;
            int sy = maxY - minY + 3;
            int sz = maxZ - minZ + 3;
            long volume = (long) sx * sy * sz;
            long cap = PunchListConfigs.POCKET_FILL_MAX_VOLUME.getIntegerValue();
            if (volume > cap || volume > Integer.MAX_VALUE) {
                if (!warnedVolume) {
                    warnedVolume = true;
                    PunchListClient.LOGGER.warn(
                            "PunchList: pocket fill skipped, bounds volume {} exceeds pocketFillMaxVolume {} - enclosure runs tier-1 only",
                            volume, cap);
                }
                clearCurrent();
                computedBase = base;
                return;
            }
            computing = true;
            int fMinX = minX - 1;
            int fMinY = minY - 1;
            int fMinZ = minZ - 1;
            long ticket = TICKET.get();
            WORKER.submit(() -> compute(world, spec, fMinX, fMinY, fMinZ, sx, sy, sz, base, ticket));
        } catch (Throwable t) {
            warnOnce(t);
        }
    }

    private static void clearCurrent() {
        TICKET.incrementAndGet();
        if (current != null) {
            current = null;
            EPOCH.incrementAndGet();
        }
    }

    private static void compute(WorldSchematic world, EnclosureTest.Spec spec, int minX, int minY, int minZ,
                                int sx, int sy, int sz, long base, long ticket) {
        try {
            boolean wantHard = !spec.softTags().isEmpty() || !spec.softBlocks().isEmpty();
            long[] std = flood(world, null, minX, minY, minZ, sx, sy, sz);
            long[] hard = wantHard ? flood(world, spec, minX, minY, minZ, sx, sy, sz) : std;
            if (ticket == TICKET.get()) {
                ReachedSet stdSet = new ReachedSet(minX, minY, minZ, sx, sy, sz, std);
                ReachedSet hardSet = hard == std ? stdSet : new ReachedSet(minX, minY, minZ, sx, sy, sz, hard);
                current = new EnclosureTest.Floods(stdSet, hardSet);
                computedBase = base;
                EPOCH.incrementAndGet();
            }
            // superseded results are simply dropped
        } catch (Throwable t) {
            // mark attempted so a failing input does not retry every tick
            computedBase = base;
            warnOnce(t);
        } finally {
            computing = false;
        }
    }

    // hardSpec null = standard passability; non-null adds soft-occluder opacity
    private static long[] flood(WorldSchematic world, EnclosureTest.Spec hardSpec,
                                int minX, int minY, int minZ, int sx, int sy, int sz) {
        {
            int volume = sx * sy * sz;
            long[] visited = new long[(volume + 63) >> 6];
            int[] stack = new int[1 << 16];
            int sp = 0;
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

            // seed: every passable cell on the expanded shell is exterior
            for (int y = 0; y < sy; y++) {
                boolean yEdge = y == 0 || y == sy - 1;
                for (int z = 0; z < sz; z++) {
                    boolean zEdge = z == 0 || z == sz - 1;
                    for (int x = 0; x < sx; x++) {
                        if (!yEdge && !zEdge && x != 0 && x != sx - 1) {
                            x = sx - 2;
                            continue;
                        }
                        if (passable(world, m, hardSpec, minX + x, minY + y, minZ + z)) {
                            int idx = (y * sz + z) * sx + x;
                            if ((visited[idx >> 6] & (1L << (idx & 63))) == 0) {
                                visited[idx >> 6] |= 1L << (idx & 63);
                                if (sp == stack.length) {
                                    stack = Arrays.copyOf(stack, (int) Math.min((long) stack.length << 1, volume));
                                }
                                stack[sp++] = idx;
                            }
                        }
                    }
                }
            }

            while (sp > 0) {
                int idx = stack[--sp];
                int x = idx % sx;
                int rest = idx / sx;
                int z = rest % sz;
                int y = rest / sz;
                for (int d = 0; d < 6; d++) {
                    int nx = x + (d == 0 ? 1 : d == 1 ? -1 : 0);
                    int ny = y + (d == 2 ? 1 : d == 3 ? -1 : 0);
                    int nz = z + (d == 4 ? 1 : d == 5 ? -1 : 0);
                    if (nx < 0 || ny < 0 || nz < 0 || nx >= sx || ny >= sy || nz >= sz) {
                        continue;
                    }
                    int nidx = (ny * sz + nz) * sx + nx;
                    if ((visited[nidx >> 6] & (1L << (nidx & 63))) != 0) {
                        continue;
                    }
                    if (!passable(world, m, hardSpec, minX + nx, minY + ny, minZ + nz)) {
                        continue;
                    }
                    visited[nidx >> 6] |= 1L << (nidx & 63);
                    if (sp == stack.length) {
                        stack = Arrays.copyOf(stack, (int) Math.min((long) stack.length << 1, volume));
                    }
                    stack[sp++] = nidx;
                }
            }

            return visited;
        }
    }

    private static boolean passable(WorldSchematic world, BlockPos.MutableBlockPos m,
                                    EnclosureTest.Spec hardSpec, int x, int y, int z) {
        m.set(x, y, z);
        var state = world.getBlockState(m);
        if (state.isSolidRender()) {
            return false;
        }
        return hardSpec == null || !EnclosureTest.blocksHardFlood(world, m, state, hardSpec);
    }

    private static void warnOnce(Throwable t) {
        if (!warnedThisSession) {
            warnedThisSession = true;
            PunchListClient.LOGGER.warn("PunchList: pocket fill failed, enclosure runs tier-1 only", t);
        }
    }
}
