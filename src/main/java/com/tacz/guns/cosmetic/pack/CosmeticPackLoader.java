package com.tacz.guns.cosmetic.pack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tacz.guns.cosmetic.GunCosmeticsMod;
import com.tacz.guns.cosmetic.data.CosmeticRarity;
import com.tacz.guns.cosmetic.data.CosmeticAttachmentData;
import com.tacz.guns.cosmetic.data.KeychainDefinition;
import com.tacz.guns.cosmetic.data.SkinDefinition;
import com.tacz.guns.cosmetic.registry.KeychainRegistry;
import com.tacz.guns.cosmetic.registry.SkinRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Scans config/guncosmetics/packs/ for skin/keychain packs (folders or ZIPs).
 * Each pack contains pack.json, skins/*.json, keychains/*.json.
 */
public final class CosmeticPackLoader {

    private CosmeticPackLoader() {}

    public static void loadAllPacks(@Nullable MinecraftServer server) {
        Path gameDir = getGameDir(server);
        Path packsDir = getPacksDir(server);
        try {
            if (!Files.exists(packsDir)) {
                Files.createDirectories(packsDir);
            }
        } catch (IOException e) {
            GunCosmeticsMod.LOGGER.error("GunCosmetics: Failed to create external packs directory", e);
        }

        SkinRegistry.clear();
        KeychainRegistry.clear();
        CosmeticLocalization.clear();

        loadTaczGunPackCosmetics(gameDir.resolve("tacz"));

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(packsDir)) {
            for (Path entry : stream) {
                try {
                    if (Files.isDirectory(entry)) {
                        loadFolderPack(entry);
                    } else if (entry.toString().endsWith(".zip")) {
                        loadZipPack(entry);
                    }
                } catch (Exception e) {
                    GunCosmeticsMod.LOGGER.error("Failed to load pack: {}", entry, e);
                }
            }
        } catch (IOException e) {
            GunCosmeticsMod.LOGGER.error("Failed to scan packs directory", e);
        }

        GunCosmeticsMod.LOGGER.info("Loaded {} skins, {} keychains",
                SkinRegistry.size(), KeychainRegistry.size());
    }

    private static void loadTaczGunPackCosmetics(Path taczDir) {
        if (!Files.isDirectory(taczDir)) {
            return;
        }
        try (DirectoryStream<Path> gunPacks = Files.newDirectoryStream(taczDir)) {
            for (Path gunPack : gunPacks) {
                if (!Files.isDirectory(gunPack)) {
                    continue;
                }
                Path cosmeticsDir = gunPack.resolve("guncosmetics");
                if (!Files.isDirectory(cosmeticsDir)) {
                    continue;
                }
                try (DirectoryStream<Path> cosmeticPacks = Files.newDirectoryStream(cosmeticsDir)) {
                    for (Path entry : cosmeticPacks) {
                        try {
                            if (Files.isDirectory(entry)) {
                                loadFolderPack(entry);
                            } else if (entry.toString().endsWith(".zip")) {
                                loadZipPack(entry);
                            }
                        } catch (Exception e) {
                            GunCosmeticsMod.LOGGER.error("Failed to load TACZ cosmetic pack: {}", entry, e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            GunCosmeticsMod.LOGGER.warn("Failed to scan TACZ cosmetic packs: {}", taczDir, e);
        }
    }

    private static Path getGameDir(@Nullable MinecraftServer server) {
        return server != null
                ? server.getServerDirectory().toPath()
                : net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get();
    }

    private static Path getPacksDir(@Nullable MinecraftServer server) {
        return getGameDir(server).resolve("guncosmetics");
    }

    /**
     * Attaches local asset bytes to registry entries received from the server.
     * Server sync intentionally sends only metadata; the client then resolves
     * textures/models from its own guncosmetics pack directory.
     */
    public static int attachLocalAssetsToRegistries(Path packsDir) {
        if (!Files.isDirectory(packsDir)) return 0;

        Map<String, Path> folderPacks = new HashMap<>();
        Map<String, Path> zipPacks = new HashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(packsDir)) {
            for (Path entry : stream) {
                try {
                    if (Files.isDirectory(entry)) {
                        Path packJsonPath = entry.resolve("pack.json");
                        if (!Files.exists(packJsonPath)) continue;
                        JsonObject packJson = readJson(packJsonPath);
                        if (packJson.has("id")) {
                            folderPacks.put(packJson.get("id").getAsString(), entry);
                            loadFolderLang(entry);
                        }
                    } else if (entry.toString().endsWith(".zip")) {
                        try (ZipFile zip = new ZipFile(entry.toFile())) {
                            ZipEntry packEntry = findPackJson(zip);
                            if (packEntry == null) continue;
                            JsonObject packJson = readJsonFromStream(zip.getInputStream(packEntry));
                            if (packJson.has("id")) {
                                zipPacks.put(packJson.get("id").getAsString(), entry);
                                loadZipLang(zip);
                            }
                        }
                    }
                } catch (Exception e) {
                    GunCosmeticsMod.LOGGER.warn("Failed to read cosmetic pack metadata: {}", entry, e);
                }
            }
        } catch (IOException e) {
            GunCosmeticsMod.LOGGER.warn("Failed to scan local cosmetic packs: {}", packsDir, e);
            return 0;
        }

        int attached = 0;
        for (SkinDefinition skin : SkinRegistry.getAll()) {
            Path packDir = folderPacks.get(skin.getSkinId().getNamespace());
            if (packDir == null) continue;

            if (skin.getTexture() != null && skin.getTextureData() == null) {
                attached += attachSkinTexture(packDir, skin, skin.getTexture());
            }
            if (skin.getOverlayTexture() != null && skin.getTextureData() == null) {
                attached += attachSkinTexture(packDir, skin, skin.getOverlayTexture());
            }
        }

        for (SkinDefinition skin : SkinRegistry.getAll()) {
            Path zipPath = zipPacks.get(skin.getSkinId().getNamespace());
            if (zipPath == null || skin.getTextureData() != null) continue;
            try (ZipFile zip = new ZipFile(zipPath.toFile())) {
                ResourceLocation texture = skin.getTexture() != null ? skin.getTexture() : skin.getOverlayTexture();
                if (texture != null) {
                    attached += attachSkinTexture(zip, skin, texture);
                }
            } catch (IOException e) {
                GunCosmeticsMod.LOGGER.warn("Failed to read skin assets from ZIP: {}", zipPath, e);
            }
        }

        for (KeychainDefinition kc : KeychainRegistry.getAll()) {
            Path packDir = folderPacks.get(kc.getKeychainId().getNamespace());
            if (packDir == null) continue;

            if (kc.getTextureData() == null) {
                Path texPath = packDir.resolve("keychains").resolve(kc.getTexture().getPath());
                if (Files.exists(texPath)) {
                    try {
                        kc.setTextureData(Files.readAllBytes(texPath));
                        attached++;
                    } catch (IOException e) {
                        GunCosmeticsMod.LOGGER.warn("Failed to read keychain texture: {}", texPath, e);
                    }
                }
            }

            if (kc.getModelData() == null) {
                Path modelPath = packDir.resolve("keychains").resolve(kc.getModel().getPath());
                if (Files.exists(modelPath)) {
                    try {
                        kc.setModelData(Files.readAllBytes(modelPath));
                        attached++;
                    } catch (IOException e) {
                        GunCosmeticsMod.LOGGER.warn("Failed to read keychain model: {}", modelPath, e);
                    }
                }
            }
        }

        for (KeychainDefinition kc : KeychainRegistry.getAll()) {
            Path zipPath = zipPacks.get(kc.getKeychainId().getNamespace());
            if (zipPath == null) continue;
            try (ZipFile zip = new ZipFile(zipPath.toFile())) {
                if (kc.getTextureData() == null) {
                    String prefix = getZipRootPrefix(zip);
                    byte[] bytes = readZipBytes(zip, prefix + "keychains/" + kc.getTexture().getPath());
                    if (bytes != null) {
                        kc.setTextureData(bytes);
                        attached++;
                    }
                }
                if (kc.getModelData() == null) {
                    String prefix = getZipRootPrefix(zip);
                    byte[] bytes = readZipBytes(zip, prefix + "keychains/" + kc.getModel().getPath());
                    if (bytes != null) {
                        kc.setModelData(bytes);
                        attached++;
                    }
                }
            } catch (IOException e) {
                GunCosmeticsMod.LOGGER.warn("Failed to read keychain assets from ZIP: {}", zipPath, e);
            }
        }

        return attached;
    }

    private static int attachSkinTexture(Path packDir, SkinDefinition skin, ResourceLocation texture) {
        Path texPath = packDir.resolve("skins").resolve(texture.getPath());
        if (!Files.exists(texPath)) return 0;

        try {
            skin.setTextureData(Files.readAllBytes(texPath));
            return 1;
        } catch (IOException e) {
            GunCosmeticsMod.LOGGER.warn("Failed to read skin texture: {}", texPath, e);
            return 0;
        }
    }

    private static int attachSkinTexture(ZipFile zip, SkinDefinition skin, ResourceLocation texture) throws IOException {
        byte[] bytes = readZipBytes(zip, getZipRootPrefix(zip) + "skins/" + texture.getPath());
        if (bytes == null) return 0;
        skin.setTextureData(bytes);
        return 1;
    }

    // ── Folder pack ────────────────────────────────────

    private static void loadFolderPack(Path packDir) throws IOException {
        Path packJsonPath = packDir.resolve("pack.json");
        if (!Files.exists(packJsonPath)) {
            GunCosmeticsMod.LOGGER.warn("Pack {} missing pack.json, skipping", packDir);
            return;
        }

        JsonObject packJson = readJson(packJsonPath);
        String packId = packJson.get("id").getAsString();
        GunCosmeticsMod.LOGGER.info("Loading pack: {} ({})", packJson.get("name").getAsString(), packId);
        loadFolderLang(packDir);

        // Load skins
        Path skinsDir = packDir.resolve("skins");
        if (Files.isDirectory(skinsDir)) {
            try (DirectoryStream<Path> files = Files.newDirectoryStream(skinsDir, "*.json")) {
                for (Path skinFile : files) {
                    try {
                        SkinDefinition skin = parseSkin(readJson(skinFile), packId, packDir);
                        SkinRegistry.register(skin);
                    } catch (Exception e) {
                        GunCosmeticsMod.LOGGER.error("Failed to parse skin {}", skinFile, e);
                    }
                }
            }
        }

        // Load keychains
        Path keychainsDir = packDir.resolve("keychains");
        if (Files.isDirectory(keychainsDir)) {
            try (DirectoryStream<Path> files = Files.newDirectoryStream(keychainsDir, "*.json")) {
                for (Path kcFile : files) {
                    try {
                        KeychainDefinition kc = parseKeychain(readJson(kcFile), packId, packDir);
                        KeychainRegistry.register(kc);
                    } catch (Exception e) {
                        GunCosmeticsMod.LOGGER.error("Failed to parse keychain {}", kcFile, e);
                    }
                }
            }
        }
    }

    // ── ZIP pack ───────────────────────────────────────

    private static void loadZipPack(Path zipPath) throws IOException {
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            ZipEntry packEntry = findPackJson(zip);
            if (packEntry == null) {
                GunCosmeticsMod.LOGGER.warn("ZIP {} missing pack.json, skipping", zipPath);
                return;
            }

            String prefix = rootPrefixFromPackEntry(packEntry);
            JsonObject packJson = readJsonFromStream(zip.getInputStream(packEntry));
            String packId = packJson.get("id").getAsString();
            GunCosmeticsMod.LOGGER.info("Loading ZIP pack: {} ({})", packJson.get("name").getAsString(), packId);
            loadZipLang(zip);

            zip.stream().forEach(entry -> {
                String name = entry.getName();
                try {
                    if (isTopLevelPackJson(name, prefix + "skins/")) {
                        SkinDefinition skin = parseSkin(readJsonFromStream(zip.getInputStream(entry)), packId, null);
                        ResourceLocation texture = skin.getTexture() != null ? skin.getTexture() : skin.getOverlayTexture();
                        if (texture != null) attachSkinTexture(zip, skin, texture);
                        SkinRegistry.register(skin);
                    } else if (isTopLevelPackJson(name, prefix + "keychains/")) {
                        KeychainDefinition kc = parseKeychain(readJsonFromStream(zip.getInputStream(entry)), packId, null);
                        byte[] texture = readZipBytes(zip, prefix + "keychains/" + kc.getTexture().getPath());
                        if (texture != null) kc.setTextureData(texture);
                        byte[] model = readZipBytes(zip, prefix + "keychains/" + kc.getModel().getPath());
                        if (model != null) kc.setModelData(model);
                        KeychainRegistry.register(kc);
                    }
                } catch (Exception e) {
                    GunCosmeticsMod.LOGGER.error("Failed to parse {} in ZIP {}", name, zipPath, e);
                }
            });
        }
    }

    // ── Parsing ────────────────────────────────────────

    private static SkinDefinition parseSkin(JsonObject json, String packId, @Nullable Path packDir) {
        String name = getCosmeticName(json);
        ResourceLocation skinId = json.has("id")
                ? parsePackResourceLocation(packId, json.get("id").getAsString())
                : new ResourceLocation(packId, name);
        String typeStr = json.has("type") ? json.get("type").getAsString() : "specific";
        SkinDefinition.SkinType type = typeStr.equalsIgnoreCase("universal")
                ? SkinDefinition.SkinType.UNIVERSAL
                : SkinDefinition.SkinType.SPECIFIC;
        String displayName = json.has("display_name") ? json.get("display_name").getAsString() : name;
        CosmeticRarity rarity = CosmeticRarity.fromString(
                json.has("rarity") ? json.get("rarity").getAsString() : "common");

        ResourceLocation targetGun = null;
        ResourceLocation texture = null;
        ResourceLocation modelOverride = null;
        ResourceLocation overlayTexture = null;
        SkinDefinition.BlendMode blendMode = SkinDefinition.BlendMode.MULTIPLY;
        List<String> description = parseDescription(json);
        ResourceLocation icon = json.has("icon") ? parsePackResourceLocation(packId, json.get("icon").getAsString()) : null;

        if (type == SkinDefinition.SkinType.SPECIFIC) {
            if (json.has("target_gun")) {
                targetGun = ResourceLocation.tryParse(json.get("target_gun").getAsString());
            }
            if (json.has("texture")) {
                texture = new ResourceLocation(packId, json.get("texture").getAsString());
            }
            if (json.has("model_override")) {
                modelOverride = new ResourceLocation(packId, json.get("model_override").getAsString());
            }
        } else {
            if (json.has("overlay_texture")) {
                overlayTexture = new ResourceLocation(packId, json.get("overlay_texture").getAsString());
            }
            if (json.has("blend_mode")) {
                blendMode = SkinDefinition.BlendMode.fromString(json.get("blend_mode").getAsString());
            }
        }

        SkinDefinition skin = new SkinDefinition(skinId, type, displayName, rarity,
                targetGun, texture, modelOverride, overlayTexture, blendMode, description, icon,
                CosmeticAttachmentData.parse(json));

        // Load binary texture data if packDir available
        if (packDir != null && texture != null) {
            try {
                Path texPath = packDir.resolve("skins").resolve(json.get("texture").getAsString());
                if (Files.exists(texPath)) {
                    skin.setTextureData(Files.readAllBytes(texPath));
                }
            } catch (IOException ignored) {}
        }
        if (packDir != null && overlayTexture != null && json.has("overlay_texture")) {
            try {
                Path texPath = packDir.resolve("skins").resolve(json.get("overlay_texture").getAsString());
                if (Files.exists(texPath)) {
                    skin.setTextureData(Files.readAllBytes(texPath));
                }
            } catch (IOException ignored) {}
        }

        return skin;
    }

    private static KeychainDefinition parseKeychain(JsonObject json, String packId, @Nullable Path packDir) {
        String name = getCosmeticName(json);
        ResourceLocation keychainId = json.has("id")
                ? parsePackResourceLocation(packId, json.get("id").getAsString())
                : new ResourceLocation(packId, name);
        String displayName = json.has("display_name") ? json.get("display_name").getAsString() : name;
        CosmeticRarity rarity = CosmeticRarity.fromString(
                json.has("rarity") ? json.get("rarity").getAsString() : "common");

        ResourceLocation model = new ResourceLocation(packId,
                json.has("model") ? json.get("model").getAsString() : "models/" + name + ".geo.json");
        ResourceLocation texture = new ResourceLocation(packId,
                json.has("texture") ? json.get("texture").getAsString() : "textures/" + name + ".png");
        ResourceLocation animation = json.has("animation")
                ? new ResourceLocation(packId, json.get("animation").getAsString())
                : null;
        List<String> description = parseDescription(json);
        ResourceLocation icon = json.has("icon") ? parsePackResourceLocation(packId, json.get("icon").getAsString()) : null;

        String bone = "stock";
        Vector3f offset = new Vector3f(0, 0, 0);
        Vector3f rotation = new Vector3f(0, 0, 0);
        Vector3f scale = new Vector3f(1, 1, 1);

        if (json.has("default_attachment")) {
            JsonObject att = json.getAsJsonObject("default_attachment");
            bone = att.has("bone") ? att.get("bone").getAsString() : "stock";
            offset = parseVec3(att, "offset", 0, 0, 0);
            rotation = parseVec3(att, "rotation", 0, 0, 0);
            scale = parseVec3(att, "scale", 1, 1, 1);
        }

        KeychainDefinition kc = new KeychainDefinition(keychainId, displayName, rarity,
                model, texture, animation, bone, offset, rotation, scale, description, icon,
                CosmeticAttachmentData.parse(json));

        // Load binary data
        if (packDir != null) {
            try {
                if (json.has("texture")) {
                    Path texPath = packDir.resolve("keychains").resolve(json.get("texture").getAsString());
                    if (Files.exists(texPath)) kc.setTextureData(Files.readAllBytes(texPath));
                }
                if (json.has("model")) {
                    Path modelPath = packDir.resolve("keychains").resolve(json.get("model").getAsString());
                    if (Files.exists(modelPath)) kc.setModelData(Files.readAllBytes(modelPath));
                }
            } catch (IOException ignored) {}
        }

        return kc;
    }

    private static Vector3f parseVec3(JsonObject parent, String key, float dx, float dy, float dz) {
        if (!parent.has(key)) return new Vector3f(dx, dy, dz);
        JsonArray arr = parent.getAsJsonArray(key);
        return new Vector3f(
                arr.size() > 0 ? arr.get(0).getAsFloat() : dx,
                arr.size() > 1 ? arr.get(1).getAsFloat() : dy,
                arr.size() > 2 ? arr.get(2).getAsFloat() : dz
        );
    }

    private static List<String> parseDescription(JsonObject json) {
        if (!json.has("description")) return List.of();
        JsonElement el = json.get("description");
        if (el.isJsonArray()) {
            List<String> list = new java.util.ArrayList<>();
            el.getAsJsonArray().forEach(e -> list.add(e.getAsString()));
            return list;
        }
        return List.of(el.getAsString());
    }

    private static void loadFolderLang(Path packDir) {
        Path assetsDir = packDir.resolve("assets");
        if (!Files.isDirectory(assetsDir)) return;
        try (DirectoryStream<Path> namespaces = Files.newDirectoryStream(assetsDir)) {
            for (Path namespace : namespaces) {
                Path langDir = namespace.resolve("lang");
                if (!Files.isDirectory(langDir)) continue;
                try (DirectoryStream<Path> files = Files.newDirectoryStream(langDir, "*.json")) {
                    for (Path file : files) {
                        CosmeticLocalization.add(stripJsonExtension(file.getFileName().toString()), readLangMap(readJson(file)));
                    }
                }
            }
        } catch (IOException e) {
            GunCosmeticsMod.LOGGER.warn("Failed to load cosmetic pack localization: {}", packDir, e);
        }
    }

    private static void loadZipLang(ZipFile zip) {
        zip.stream().forEach(entry -> {
            String name = entry.getName().replace('\\', '/');
            if (entry.isDirectory() || !name.startsWith("assets/") || !name.contains("/lang/") || !name.endsWith(".json")) {
                return;
            }
            try {
                String locale = stripJsonExtension(name.substring(name.lastIndexOf('/') + 1));
                CosmeticLocalization.add(locale, readLangMap(readJsonFromStream(zip.getInputStream(entry))));
            } catch (Exception e) {
                GunCosmeticsMod.LOGGER.warn("Failed to load cosmetic ZIP localization entry: {}", name, e);
            }
        });
    }

    private static Map<String, String> readLangMap(JsonObject json) {
        Map<String, String> entries = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                entries.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return entries;
    }

    private static String stripJsonExtension(String fileName) {
        return fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - 5) : fileName;
    }

    private static String getCosmeticName(JsonObject json) {
        if (json.has("name")) return json.get("name").getAsString();
        if (json.has("id")) {
            String id = json.get("id").getAsString();
            int colon = id.indexOf(':');
            return colon >= 0 ? id.substring(colon + 1) : id;
        }
        throw new IllegalArgumentException("Cosmetic JSON must have id or name");
    }

    private static ResourceLocation parsePackResourceLocation(String packId, String id) {
        return id.contains(":") ? new ResourceLocation(id) : new ResourceLocation(packId, id);
    }

    private static boolean isTopLevelPackJson(String name, String prefix) {
        String normalized = name.replace('\\', '/');
        if (!normalized.startsWith(prefix) || !normalized.endsWith(".json")) return false;
        String rest = normalized.substring(prefix.length());
        return !rest.isEmpty() && !rest.contains("/");
    }

    @Nullable
    private static ZipEntry findPackJson(ZipFile zip) {
        ZipEntry root = zip.getEntry("pack.json");
        if (root != null) return root;
        return zip.stream()
                .filter(entry -> !entry.isDirectory())
                .filter(entry -> entry.getName().replace('\\', '/').endsWith("/pack.json"))
                .filter(entry -> entry.getName().replace('\\', '/').indexOf('/') == entry.getName().replace('\\', '/').lastIndexOf('/'))
                .findFirst()
                .orElse(null);
    }

    private static String getZipRootPrefix(ZipFile zip) {
        ZipEntry packEntry = findPackJson(zip);
        return packEntry == null ? "" : rootPrefixFromPackEntry(packEntry);
    }

    private static String rootPrefixFromPackEntry(ZipEntry packEntry) {
        String name = packEntry.getName().replace('\\', '/');
        int slash = name.lastIndexOf('/');
        return slash >= 0 ? name.substring(0, slash + 1) : "";
    }

    @Nullable
    private static byte[] readZipBytes(ZipFile zip, String path) throws IOException {
        ZipEntry entry = zip.getEntry(path.replace('\\', '/'));
        if (entry == null || entry.isDirectory()) return null;
        try (InputStream in = zip.getInputStream(entry)) {
            return in.readAllBytes();
        }
    }

    // ── JSON reading ───────────────────────────────────

    private static JsonObject readJson(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static JsonObject readJsonFromStream(InputStream is) throws IOException {
        try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
