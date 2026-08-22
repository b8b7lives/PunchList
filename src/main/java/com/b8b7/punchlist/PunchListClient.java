package com.b8b7.punchlist;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PunchListClient implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("punchlist");

    // harness probe: class load forces mixin application; run-loadcheck.sh greps the markers
    private static final boolean LOAD_CHECK = Boolean.getBoolean("punchlist.loadcheck");
    private static boolean loadCheckDone = false;

    @Override
    public void onInitializeClient() {
        // malilib also calls load() post-init; the direct call covers a
        // registration that lands after that pass
        ConfigManager.getInstance().registerConfigHandler("punchlist", new PunchListConfigs.ConfigHandler());
        PunchListConfigs.loadFromFile();

        registerRendermaticaTransformer();

        PunchListConfigs.FILTER_TOGGLE.getKeybind().setCallback((action, key) -> {
            if (action != KeyAction.PRESS) {
                return false;
            }
            FilterState.toggle();
            return true;
        });

        ClientTickEvents.END_CLIENT_TICK.register(FilterState::clientTick);

        if (LOAD_CHECK) {
            ClientTickEvents.END_CLIENT_TICK.register(PunchListClient::loadCheckTick);
        }
    }

    // rendermatica replaces the stock marker render path punchlist wraps;
    // the transformer keeps enclosed-marker filtering working there. The
    // two paths are mutually exclusive: the wrap fires only when stock
    // runs, the transformer only when rendermatica's cache runs.
    // Reflection keeps the repos build-independent.
    private static void registerRendermaticaTransformer() {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("rendermatica")) {
            return;
        }
        try {
            Class<?> api = Class.forName("com.b8b7.rendermatica.RendermaticaApi");
            java.util.function.UnaryOperator<java.util.List<fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.MismatchRenderPos>> transformer =
                    FilterState::filterMarkers;
            api.getMethod("registerMarkerTransformer", java.util.function.UnaryOperator.class)
                    .invoke(null, transformer);
            LOGGER.info("PunchList: registered marker transformer with rendermatica");
        } catch (Throwable t) {
            LOGGER.warn("PunchList: could not register with rendermatica; enclosed markers may show if its cached overlay is active", t);
        }
    }

    private static void loadCheckTick(net.minecraft.client.Minecraft mc) {
        if (loadCheckDone || mc.gui.screen() == null) {
            return;
        }
        loadCheckDone = true;
        String[] targets = {
                "fi.dy.masa.litematica.util.RayTraceUtils",
                "fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo",
                "fi.dy.masa.litematica.gui.GuiSchematicVerifier",
                "fi.dy.masa.litematica.schematic.verifier.SchematicVerifier",
                "fi.dy.masa.litematica.config.Hotkeys",
                "fi.dy.masa.litematica.render.OverlayRenderer",
                "fi.dy.masa.litematica.world.SchematicWorldHandler",
                "fi.dy.masa.litematica.gui.widgets.WidgetSchematicVerificationResult",
        };
        boolean ok = true;
        for (String name : targets) {
            try {
                Class.forName(name);
                LOGGER.info("punchlist loadcheck: {} OK", name);
            } catch (Throwable t) {
                ok = false;
                LOGGER.error("punchlist loadcheck: {} FAILED", name, t);
            }
        }
        LOGGER.info(ok ? "punchlist loadcheck PASS" : "punchlist loadcheck FAIL");
        mc.stop();
    }
}
