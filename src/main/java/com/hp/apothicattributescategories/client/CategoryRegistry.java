package com.hp.apothicattributescategories.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.hp.apothicattributescategories.ApothicAttributesCategories;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CategoryRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(CategoryRegistry.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String CONFIG_FILE_NAME = "apothic_attributes_categories.json";
    private static final String ICONS_DIR_NAME = "apothic_attributes_categories_icons";

    private static List<CategoryDefinition> categories = Collections.emptyList();
    private static final Map<ResourceLocation, String> ATTRIBUTE_CACHE = new HashMap<>();
    private static final Map<String, ItemStack> ICON_CACHE = new HashMap<>();
    private static final Map<String, Optional<ResourceLocation>> TEXTURE_CACHE = new HashMap<>();
    private static final Set<ResourceLocation> DYNAMIC_TEXTURES = new HashSet<>();
    private static final Map<String, Set<String>> EXACT_MAP = new HashMap<>();
    private static final Map<String, Set<String>> NAMESPACE_MAP = new HashMap<>();
    private static final Map<String, List<String>> KEYWORD_MAP = new HashMap<>();
    private static FileTime lastModified = null;
    private static boolean initialized = false;
    private static int dynamicVersion = 0;

    private CategoryRegistry() {
    }

    public static void ensureLoaded() {
        Path path = getConfigPath();
        if (!initialized) {
            initialized = true;
            ensureIconsDir();
            if (!Files.exists(path)) {
                writeDefaultConfig(path);
            }
            loadFromDisk(path);
            return;
        }
        try {
            if (Files.exists(path)) {
                FileTime current = Files.getLastModifiedTime(path);
                if (lastModified == null || !current.equals(lastModified)) {
                    loadFromDisk(path);
                }
            }
        } catch (IOException ignored) {
        }
    }

    public static List<CategoryDefinition> getCategories() {
        ensureLoaded();
        return categories;
    }

    public static ItemStack getIcon(CategoryDefinition def) {
        return ICON_CACHE.computeIfAbsent(def.id(), key -> def.createIconStack());
    }

    public static ResourceLocation getIconTexture(CategoryDefinition def) {
        Optional<ResourceLocation> cached = TEXTURE_CACHE.get(def.id());
        if (cached != null) {
            return cached.orElse(null);
        }
        ResourceLocation resolved = resolveIconTexture(def);
        TEXTURE_CACHE.put(def.id(), Optional.ofNullable(resolved));
        return resolved;
    }

    public static String resolveCategory(Attribute attribute) {
        ensureLoaded();
        ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(attribute);
        if (id == null) {
            return CategoryDefinition.ID_OTHER;
        }
        return ATTRIBUTE_CACHE.computeIfAbsent(id, CategoryRegistry::computeCategory);
    }

    private static ResourceLocation resolveIconTexture(CategoryDefinition def) {
        String spec = def.resolvedTextureSpec();
        if (spec.isEmpty()) {
            return null;
        }
        if (spec.startsWith("file:")) {
            return loadLocalTexture(def.id(), spec.substring(5).trim());
        }
        if (!spec.contains(":")) {
            return loadLocalTexture(def.id(), spec);
        }
        ResourceLocation loc = ResourceLocation.tryParse(spec);
        if (loc != null) {
            return loc;
        }
        return loadLocalTexture(def.id(), spec);
    }

    private static ResourceLocation loadLocalTexture(String categoryId, String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }
        Path configDir = FMLPaths.CONFIGDIR.get().toAbsolutePath().normalize();
        Path filePath = Path.of(rawPath);
        if (!filePath.isAbsolute()) {
            Path direct = configDir.resolve(rawPath).normalize();
            if (Files.isRegularFile(direct)) {
                filePath = direct;
            } else {
                filePath = configDir.resolve(ICONS_DIR_NAME).resolve(rawPath).normalize();
            }
        }
        if (!Files.isRegularFile(filePath)) {
            LOGGER.warn("Custom category icon file not found for '{}': {}", categoryId, filePath);
            return null;
        }
        try (InputStream in = Files.newInputStream(filePath)) {
            NativeImage image = NativeImage.read(in);
            DynamicTexture texture = new DynamicTexture(image);
            String safeId = categoryId.replaceAll("[^a-z0-9_.-]", "_");
            ResourceLocation texLoc = ResourceLocation.fromNamespaceAndPath(
                ApothicAttributesCategories.MOD_ID,
                "dynamic/category_" + safeId + "_" + (++dynamicVersion)
            );
            Minecraft.getInstance().getTextureManager().register(texLoc, texture);
            DYNAMIC_TEXTURES.add(texLoc);
            return texLoc;
        } catch (IOException e) {
            LOGGER.error("Failed to load custom category icon '{}' from {}", categoryId, filePath, e);
            return null;
        }
    }

    private static void releaseDynamicTextures() {
        if (DYNAMIC_TEXTURES.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getTextureManager() != null) {
            for (ResourceLocation loc : DYNAMIC_TEXTURES) {
                mc.getTextureManager().release(loc);
            }
        }
        DYNAMIC_TEXTURES.clear();
    }

    private static String computeCategory(ResourceLocation id) {
        String fullId = id.toString().toLowerCase(Locale.ROOT);
        String namespace = id.getNamespace().toLowerCase(Locale.ROOT);
        String path = id.getPath().toLowerCase(Locale.ROOT);

        for (CategoryDefinition def : categories) {
            if (def.isSpecial()) {
                continue;
            }
            Set<String> exacts = EXACT_MAP.get(def.id());
            if (exacts != null && exacts.contains(fullId)) {
                return def.id();
            }
        }
        for (CategoryDefinition def : categories) {
            if (def.isSpecial()) {
                continue;
            }
            Set<String> namespaces = NAMESPACE_MAP.get(def.id());
            if (namespaces != null && namespaces.contains(namespace)) {
                return def.id();
            }
        }
        for (CategoryDefinition def : categories) {
            if (def.isSpecial()) {
                continue;
            }
            List<String> keywords = KEYWORD_MAP.get(def.id());
            if (keywords != null) {
                for (String keyword : keywords) {
                    if (path.contains(keyword)) {
                        return def.id();
                    }
                }
            }
        }
        return CategoryDefinition.ID_OTHER;
    }

    private static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE_NAME);
    }

    private static void ensureIconsDir() {
        try {
            Files.createDirectories(FMLPaths.CONFIGDIR.get().resolve(ICONS_DIR_NAME));
        } catch (IOException ignored) {
        }
    }

    private static void loadFromDisk(Path path) {
        List<CategoryDefinition> loaded = null;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ConfigFileData data = GSON.fromJson(reader, ConfigFileData.class);
            if (data != null && data.categories != null && !data.categories.isEmpty()) {
                loaded = sanitize(data.categories);
            }
            lastModified = Files.getLastModifiedTime(path);
        } catch (IOException | JsonParseException e) {
            LOGGER.error("Failed to read {}, falling back to default categories", path, e);
        }
        if (loaded == null || loaded.isEmpty()) {
            loaded = createDefaultCategories();
        }
        applyCategories(loaded);
    }

    private static List<CategoryDefinition> sanitize(List<CategoryEntry> entries) {
        List<CategoryDefinition> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        boolean hasAll = false;
        boolean hasOther = false;

        for (CategoryEntry entry : entries) {
            if (entry == null || entry.id == null || entry.id.isBlank()) {
                continue;
            }
            String id = entry.id.trim().toLowerCase(Locale.ROOT);
            if (!seen.add(id)) {
                continue;
            }
            if (CategoryDefinition.ID_ALL.equals(id)) {
                hasAll = true;
            }
            if (CategoryDefinition.ID_OTHER.equals(id)) {
                hasOther = true;
            }
            result.add(new CategoryDefinition(
                id,
                entry.name == null ? "" : entry.name.trim(),
                entry.icon == null ? "" : entry.icon.trim(),
                entry.iconTexture == null ? "" : entry.iconTexture.trim(),
                cleanList(entry.attributes),
                cleanList(entry.namespaces),
                cleanList(entry.keywords)
            ));
        }

        if (!hasAll) {
            result.add(0, defaultAllCategory());
        }
        if (!hasOther) {
            result.add(defaultOtherCategory());
        }
        return Collections.unmodifiableList(result);
    }

    private static List<String> cleanList(List<String> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> cleaned = new ArrayList<>();
        for (String item : input) {
            if (item != null && !item.isBlank()) {
                cleaned.add(item.trim().toLowerCase(Locale.ROOT));
            }
        }
        return Collections.unmodifiableList(cleaned);
    }

    private static void applyCategories(List<CategoryDefinition> list) {
        releaseDynamicTextures();
        categories = list;
        ATTRIBUTE_CACHE.clear();
        ICON_CACHE.clear();
        TEXTURE_CACHE.clear();
        EXACT_MAP.clear();
        NAMESPACE_MAP.clear();
        KEYWORD_MAP.clear();
        for (CategoryDefinition def : list) {
            if (!def.attributes().isEmpty()) {
                EXACT_MAP.put(def.id(), new HashSet<>(def.attributes()));
            }
            if (!def.namespaces().isEmpty()) {
                NAMESPACE_MAP.put(def.id(), new HashSet<>(def.namespaces()));
            }
            if (!def.keywords().isEmpty()) {
                KEYWORD_MAP.put(def.id(), def.keywords());
            }
        }
    }

    private static void writeDefaultConfig(Path path) {
        ConfigFileData fileData = new ConfigFileData();
        fileData._readme = List.of(
            "You can add, remove, or reorder categories here. Changes reload automatically when reopening the Attributes GUI.",
            "name: literal display text (e.g. \"Elemental\") or \"lang:<translation_key>\".",
            "icon: item ID (e.g. \"minecraft:diamond_sword\").",
            "iconTexture: optional 16x16 texture path, either a resource path (e.g. \"minecraft:textures/mob_effect/strength.png\") or a local PNG under config/apothic_attributes_categories_icons/ (e.g. \"file:my_icon.png\").",
            "attributes / namespaces / keywords: matching priority is exact attributes -> namespaces -> path keywords -> other.",
            "可在此自由新增、删改或调整分类顺序，保存后重新打开属性界面自动热重载；iconTexture 支持资源路径或 config/apothic_attributes_categories_icons/ 下的本地 PNG 图片。"
        );
        fileData.categories = new ArrayList<>();
        for (CategoryDefinition def : createDefaultCategories()) {
            CategoryEntry entry = new CategoryEntry();
            entry.id = def.id();
            entry.name = def.name();
            entry.icon = def.icon();
            entry.iconTexture = def.iconTexture();
            entry.attributes = new ArrayList<>(def.attributes());
            entry.namespaces = new ArrayList<>(def.namespaces());
            entry.keywords = new ArrayList<>(def.keywords());
            fileData.categories.add(entry);
        }
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(fileData, writer);
            }
            lastModified = Files.getLastModifiedTime(path);
        } catch (IOException e) {
            LOGGER.error("Failed to write default {}", path, e);
        }
    }

    private static CategoryDefinition defaultAllCategory() {
        return new CategoryDefinition(
            CategoryDefinition.ID_ALL,
            "lang:apothic_attributes_categories.category.all",
            "minecraft:book",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );
    }

    private static CategoryDefinition defaultOtherCategory() {
        return new CategoryDefinition(
            CategoryDefinition.ID_OTHER,
            "lang:apothic_attributes_categories.category.other",
            "minecraft:compass",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );
    }

    private static List<CategoryDefinition> createDefaultCategories() {
        List<CategoryDefinition> defaults = new ArrayList<>();
        defaults.add(defaultAllCategory());
        defaults.add(new CategoryDefinition(
            "combat",
            "lang:apothic_attributes_categories.category.combat",
            "minecraft:diamond_sword",
            "",
            List.of(
                "minecraft:generic.attack_damage",
                "minecraft:generic.attack_speed",
                "minecraft:generic.attack_knockback",
                "minecraft:player.sweeping_damage_ratio",
                "apothic_attributes:armor_pierce",
                "apothic_attributes:armor_shred",
                "apothic_attributes:prot_pierce",
                "apothic_attributes:prot_shred",
                "apothic_attributes:crit_chance",
                "apothic_attributes:crit_damage",
                "apothic_attributes:arrow_damage",
                "apothic_attributes:arrow_velocity",
                "apothic_attributes:draw_speed",
                "apothic_attributes:cold_damage",
                "apothic_attributes:fire_damage",
                "apothic_attributes:current_hp_damage",
                "apothic_attributes:life_steal",
                "apothic_attributes:overheal",
                "apothic_attributes:projectile_damage"
            ),
            Collections.emptyList(),
            List.of(
                "attack",
                "damage",
                "crit",
                "arrow",
                "draw_speed",
                "pierce",
                "shred",
                "sweep",
                "life_steal",
                "overheal",
                "projectile",
                "ranged",
                "melee"
            )
        ));
        defaults.add(new CategoryDefinition(
            "defense",
            "lang:apothic_attributes_categories.category.defense",
            "minecraft:shield",
            "",
            List.of(
                "minecraft:generic.armor",
                "minecraft:generic.armor_toughness",
                "minecraft:generic.knockback_resistance",
                "minecraft:generic.explosion_knockback_resistance",
                "minecraft:generic.burning_time",
                "apothic_attributes:dodge_chance",
                "apothic_attributes:ghost_health"
            ),
            Collections.emptyList(),
            List.of(
                "armor",
                "toughness",
                "dodge",
                "resistance",
                "burn",
                "shield",
                "parry",
                "block_chance",
                "thorns",
                "mitigation",
                "ward",
                "defen"
            )
        ));
        defaults.add(new CategoryDefinition(
            "survival",
            "lang:apothic_attributes_categories.category.survival",
            "minecraft:golden_apple",
            "",
            List.of(
                "minecraft:generic.max_health",
                "minecraft:generic.max_absorption",
                "minecraft:generic.oxygen_bonus",
                "minecraft:generic.luck",
                "minecraft:generic.fall_damage_multiplier",
                "minecraft:generic.safe_fall_distance",
                "apothic_attributes:healing_received",
                "apothic_attributes:experience_gained"
            ),
            Collections.emptyList(),
            List.of(
                "health",
                "absorption",
                "heal",
                "regen",
                "oxygen",
                "breath",
                "luck",
                "experience",
                "fall",
                "hunger",
                "saturation",
                "stamina"
            )
        ));
        defaults.add(new CategoryDefinition(
            "mobility",
            "lang:apothic_attributes_categories.category.mobility",
            "minecraft:iron_pickaxe",
            "",
            List.of(
                "minecraft:generic.movement_speed",
                "minecraft:generic.flying_speed",
                "minecraft:generic.jump_strength",
                "minecraft:generic.gravity",
                "minecraft:generic.step_height",
                "minecraft:generic.movement_efficiency",
                "minecraft:generic.water_movement_efficiency",
                "minecraft:generic.scale",
                "minecraft:player.sneaking_speed",
                "minecraft:player.block_break_speed",
                "minecraft:player.mining_efficiency",
                "minecraft:player.submerged_mining_speed",
                "minecraft:player.block_interaction_range",
                "minecraft:player.entity_interaction_range",
                "neoforge:swim_speed",
                "neoforge:creative_flight",
                "apothic_attributes:elytra_flight",
                "apothic_attributes:mining_speed"
            ),
            Collections.emptyList(),
            List.of(
                "speed",
                "movement",
                "jump",
                "gravity",
                "step",
                "swim",
                "flight",
                "fly",
                "mining",
                "break_speed",
                "interaction_range",
                "reach",
                "scale",
                "climb"
            )
        ));
        defaults.add(new CategoryDefinition(
            "magic",
            "lang:apothic_attributes_categories.category.magic",
            "minecraft:amethyst_shard",
            "",
            Collections.emptyList(),
            List.of(
                "irons_spellbooks",
                "ars_nouveau",
                "botania",
                "bloodmagic",
                "mahoutsukai",
                "malum",
                "traveloptics",
                "iss_magicfromtheeast"
            ),
            List.of(
                "spell",
                "mana",
                "magic",
                "cast",
                "cooldown",
                "arcane",
                "eldritch",
                "evocation",
                "holy",
                "lightning",
                "summon",
                "elemental",
                "soul"
            )
        ));
        defaults.add(defaultOtherCategory());
        return Collections.unmodifiableList(defaults);
    }

    private static final class ConfigFileData {
        List<String> _readme;
        List<CategoryEntry> categories;
    }

    private static final class CategoryEntry {
        String id;
        String name;
        String icon;
        String iconTexture;
        List<String> attributes;
        List<String> namespaces;
        List<String> keywords;
    }
}
