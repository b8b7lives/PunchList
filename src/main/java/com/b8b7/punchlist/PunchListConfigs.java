package com.b8b7.punchlist;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.data.json.JsonUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

// hotkey binding persists via litematica's config; filter state is
// deliberately runtime-only, so the mod starts OFF every launch;
// options live in litematica's Generic tab and persist in
// litematica.json since 0.8.1 (#14)
public class PunchListConfigs {
    public static final ConfigHotkey FILTER_TOGGLE =
            (ConfigHotkey) new ConfigHotkey("punchListFilterToggle", "").apply("punchlist.hotkey");

    public static final ConfigOptionList ENCLOSED_MODE =
            new ConfigOptionList("enclosedMode", EnclosedMode.SHOW).apply("punchlist.config");
    public static final ConfigStringList ENCLOSED_SOFT_OCCLUDERS =
            new ConfigStringList("enclosedSoftOccluders",
                    ImmutableList.of("#minecraft:leaves")).apply("punchlist.config");
    public static final ConfigInteger FOLLOW_PLAYER_DISTANCE =
            new ConfigInteger("followPlayerDistance", 32, 0, 4096).apply("punchlist.config");
    public static final ConfigBoolean POCKET_FILL =
            new ConfigBoolean("pocketFill", true).apply("punchlist.config");
    public static final ConfigInteger POCKET_FILL_MAX_VOLUME =
            new ConfigInteger("pocketFillMaxVolume", 100000000, 0, 1000000000).apply("punchlist.config");

    public static final ImmutableList<ConfigHotkey> EXTENDED_HOTKEYS = ImmutableList.of(
            FILTER_TOGGLE
    );

    // injected into litematica's Generic OPTIONS list by
    // MixinLitematicaGenericConfigs; litematica serializes them
    public static final ImmutableList<IConfigBase> EXTENDED_OPTIONS = ImmutableList.of(
            ENCLOSED_MODE, ENCLOSED_SOFT_OCCLUDERS, FOLLOW_PLAYER_DISTANCE,
            POCKET_FILL, POCKET_FILL_MAX_VOLUME
    );

    private static final String LEGACY_CONFIG_FILE = "punchlist.json";

    // pre-0.8.1 store; applied once, then renamed so it can never
    // override litematica.json again
    public static void migrateLegacyFile() {
        try {
            Path legacy = FileUtils.getConfigDirectory().resolve(LEGACY_CONFIG_FILE);
            if (!Files.exists(legacy)) {
                return;
            }
            JsonElement el = JsonUtils.parseJsonFile(legacy);
            if (el != null && el.isJsonObject()) {
                ConfigUtils.readConfigBase(el.getAsJsonObject(), "Generic", EXTENDED_OPTIONS);
            }
            Files.move(legacy, legacy.resolveSibling(LEGACY_CONFIG_FILE + ".migrated"),
                    StandardCopyOption.REPLACE_EXISTING);
            PunchListClient.LOGGER.info(
                    "PunchList: migrated {} into the litematica.json store, renamed to .migrated",
                    LEGACY_CONFIG_FILE);
        } catch (Throwable t) {
            PunchListClient.LOGGER.warn("PunchList: legacy config migration failed, using defaults", t);
        }
    }
}
