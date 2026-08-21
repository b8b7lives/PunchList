package com.b8b7.punchlist;

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
