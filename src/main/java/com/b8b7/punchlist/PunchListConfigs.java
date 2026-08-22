package com.b8b7.punchlist;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.data.json.JsonUtils;

import java.nio.file.Path;
import java.util.List;

// hotkey binding persists via litematica's config; filter state is
// deliberately runtime-only, so the mod starts OFF every launch;
// enclosed-layer options persist via config/punchlist.json (safe: the
// mode is inert until the filter is toggled on)
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

    private static final String CONFIG_FILE = "punchlist.json";
    private static final List<IConfigBase> PERSISTED = ImmutableList.of(
            ENCLOSED_MODE, ENCLOSED_SOFT_OCCLUDERS, FOLLOW_PLAYER_DISTANCE,
            POCKET_FILL, POCKET_FILL_MAX_VOLUME
    );

    public static void loadFromFile() {
        try {
            JsonElement el = JsonUtils.parseJsonFile(configPath());
            if (el != null && el.isJsonObject()) {
                ConfigUtils.readConfigBase(el.getAsJsonObject(), "Generic", PERSISTED);
            }
        } catch (Throwable t) {
            PunchListClient.LOGGER.warn("PunchList: config load failed, using defaults", t);
        }
    }

    public static void saveToFile() {
        try {
            JsonObject root = new JsonObject();
            ConfigUtils.writeConfigBase(root, "Generic", PERSISTED);
            JsonUtils.writeJsonToFile(root, configPath());
        } catch (Throwable t) {
            PunchListClient.LOGGER.warn("PunchList: config save failed", t);
        }
    }

    private static Path configPath() {
        return FileUtils.getConfigDirectory().resolve(CONFIG_FILE);
    }

    public static class ConfigHandler implements IConfigHandler {
        @Override
        public void load() {
            loadFromFile();
        }

        @Override
        public void save() {
            saveToFile();
        }
    }
}
