package com.commandert3706.client.logic;

import com.commandert3706.BetterLitematicaMaterialsList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.sandrohc.schematic4j.SchematicLoader;
import net.sandrohc.schematic4j.schematic.Schematic;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MaterialTrackerManager {
    private static Map<String, Integer> activeMaterials = Collections.emptyMap();
    private static final Map<String, String> ALIASES = new HashMap<>();
    private static boolean isProcessing = false;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> CHECKED_ITEMS = new HashSet<>();
    private static String currentSchematicName = "";
    private static SortPreference activeSortOrder = SortPreference.ALPHABETICAL;

    public enum SortPreference {
        ALPHABETICAL,
        AMOUNT_ASC,
        AMOUNT_DESC
    }

    static {
        ALIASES.put("minecraft:water", "minecraft:water_bucket");
        ALIASES.put("minecraft:lava", "minecraft:lava_bucket");
        ALIASES.put("minecraft:powder_snow", "minecraft:powder_snow_bucket");
        ALIASES.put("minecraft:redstone_wire", "minecraft:redstone");
    }

    public static void loadSchematicAsync(File file) {
        if (file == null || !file.exists()) return;
        isProcessing = true;

        CompletableFuture.runAsync(() -> {
            try {
                Schematic schematic = SchematicLoader.load(file.getAbsolutePath());
                Map<String, Integer> tempCounts = new HashMap<>();

                schematic.blocks().forEach(block -> {
                    String rawName = block.right.name;

                    if (rawName.startsWith("minecraft:water") || rawName.startsWith("minecraft:lava")) {
                        if (rawName.contains("level=") && !rawName.contains("level=0")) {
                            return;
                        }
                    }

                    if (rawName.startsWith("minecraft:air") || rawName.startsWith("minecraft:cave_air") || rawName.startsWith("minecraft:piston_head")) return;

                    String cleanName = rawName.contains("[") ? rawName.substring(0, rawName.indexOf("[")) : rawName;

                    if (cleanName.contains("_wall_")) {
                        cleanName = cleanName.replace("_wall_", "_");
                    }

                    // Case 2: Regular Wall Torches (e.g., "minecraft:wall_torch" -> "minecraft:torch")
                    if (cleanName.contains(":wall_")) {
                        cleanName = cleanName.replace(":wall_", ":");
                    }

                    if (ALIASES.containsKey(cleanName)) {
                        cleanName = ALIASES.get(cleanName);
                    }

                    tempCounts.put(cleanName, tempCounts.getOrDefault(cleanName, 0) + 1);
                });

                activeMaterials = tempCounts;
                setCurrentSchematic(file.getName());

            } catch (Exception e) {
                BetterLitematicaMaterialsList.LOGGER.error("Error parsing schematic with schematic4j: " + file.getName());
                e.printStackTrace();
            } finally {
                isProcessing = false;
                assert Minecraft.getInstance().player != null;
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("Loaded schematic: " + file.getName()));
            }
        });
    }

    public static Map<String, Integer> getActiveMaterials() {
        return activeMaterials;
    }

    public static boolean isProcessing() {
        return isProcessing;
    }

    public static void clear() {
        activeMaterials = Collections.emptyMap();
    }

    public static void setCurrentSchematic(String fileName) {
        currentSchematicName = fileName.replace(".litematic", "");
        loadCheckedStates();
    }

    public static boolean isItemChecked(String rawId) {
        return CHECKED_ITEMS.contains(rawId);
    }

    public static void setItemCheckedState(String rawId, boolean checked) {
        if (checked) {
            CHECKED_ITEMS.add(rawId);
        } else {
            CHECKED_ITEMS.remove(rawId);
        }

        saveCheckedStates();
    }

    private static File getConfigFile() {
        File configDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "blml");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        return new File(configDir, currentSchematicName + "_progress.json");
    }

    private static void saveCheckedStates() {
        if (currentSchematicName.isEmpty()) return;

        new Thread(() -> {
            try (FileWriter writer = new FileWriter(getConfigFile())) {
                GSON.toJson(CHECKED_ITEMS, writer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void loadCheckedStates() {
        CHECKED_ITEMS.clear();
        File configFile = getConfigFile();
        if (!configFile.exists()) return;

        try (FileReader reader = new FileReader(configFile)) {
            Set<String> loadedData = GSON.fromJson(reader, new TypeToken<HashSet<String>>(){}.getType());
            if (loadedData != null) {
                CHECKED_ITEMS.addAll(loadedData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SortPreference getSortPreference() {
        return activeSortOrder;
    }

    public static void setSortPreference(SortPreference preference) {
        activeSortOrder = preference;
        saveGlobalSettings();
    }

    private static File getGlobalConfigFile() {
        File configDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "blml");
        if (!configDir.exists()) configDir.mkdirs();
        return new File(configDir, "global_settings.json");
    }

    public static void saveGlobalSettings() {
        new Thread(() -> {
            try (FileWriter writer = new FileWriter(getGlobalConfigFile())) {
                GSON.toJson(activeSortOrder, writer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void loadGlobalSettings() {
        File configFile = getGlobalConfigFile();
        if (!configFile.exists()) return;

        try (FileReader reader = new FileReader(configFile)) {
            SortPreference loadedSort = GSON.fromJson(reader, SortPreference.class);
            if (loadedSort != null) {
                activeSortOrder = loadedSort;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getSortPreferenceCaption() {
        return switch (activeSortOrder) {
            case AMOUNT_ASC -> "Sort: ASC";
            case AMOUNT_DESC -> "Sort: DESC";
            case ALPHABETICAL -> "Sort: A-Z";
        };
    }

    public static String formatCount(int totalCount) {
        int shulkers = totalCount / 1728;
        int remainderAfterShulkers = totalCount % 1728;

        int stacks = remainderAfterShulkers / 64;
        int items = remainderAfterShulkers % 64;

        String sbStr = (shulkers == 1) ? "box" : "boxes";
        String stStr = (stacks == 1) ? "stack" : "stacks";
        String itemStr = (items == 1) ? "item" : "items";

        if (totalCount >= 1728) {
            return String.format("%d %s, %d %s, %d %s", shulkers, sbStr, stacks, stStr, items, itemStr);
        } else if (totalCount >= 64) {
            return String.format("%d %s, %d %s", stacks, stStr, items, itemStr);
        } else {
            return String.format("%d %s", totalCount, itemStr);
        }
    }
}
