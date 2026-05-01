package com.tacz.guns.resource;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tacz.guns.api.vmlib.LuaGunLogicConstant;
import com.tacz.guns.api.vmlib.LuaLibrary;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.GunMod;
import com.tacz.guns.cosmetic.data.CosmeticRarity;
import com.tacz.guns.cosmetic.data.KeychainDefinition;
import com.tacz.guns.cosmetic.data.SkinDefinition;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.crafting.result.GunSmithTableResult;
import com.tacz.guns.init.ModRecipe;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageSyncGunPack;
import com.tacz.guns.resource.filter.RecipeFilter;
import com.tacz.guns.resource.index.CommonAmmoIndex;
import com.tacz.guns.resource.index.CommonAttachmentIndex;
import com.tacz.guns.resource.index.CommonBlockIndex;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.manager.*;
import com.tacz.guns.resource.network.CommonNetworkCache;
import com.tacz.guns.resource.network.DataType;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import com.tacz.guns.resource.pojo.data.block.BlockData;
import com.tacz.guns.resource.pojo.data.block.TabConfig;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.Ignite;
import com.tacz.guns.resource.serialize.*;
import com.tacz.guns.util.AllowAttachmentTagMatcher;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.luaj.vm2.LuaTable;

import java.util.*;
import java.util.function.Consumer;

@Mod.EventBusSubscriber
public class CommonAssetsManager implements ICommonResourceProvider {
    private static CommonAssetsManager INSTANCE;
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
            .registerTypeAdapter(Pair.class, new PairSerializer())
            .registerTypeAdapter(GunSmithTableIngredient.class, new GunSmithTableIngredientSerializer())
            .registerTypeAdapter(GunSmithTableResult.class, new GunSmithTableResultSerializer())
            .registerTypeAdapter(ExtraDamage.DistanceDamagePair.class, new DistanceDamagePairSerializer())
            .registerTypeAdapter(Vec3.class, new Vec3Serializer())
            .registerTypeAdapter(Ignite.class, new IgniteSerializer())
            .registerTypeAdapter(RecipeFilter.class, new RecipeFilter.Deserializer())
            .registerTypeAdapter(CommonGunIndex.class, new CommonGunIndexSerializer())
            .registerTypeAdapter(CommonAmmoIndex.class, new CommonAmmoIndexSerializer())
            .registerTypeAdapter(CommonAttachmentIndex.class, new CommonAttachmentIndexSerializer())
            .registerTypeAdapter(CommonBlockIndex.class, new CommonBlockIndexSerializer())
            .registerTypeAdapter(TabConfig.class, new TabConfig.Deserializer())
            .create();

    private final List<INetworkCacheReloadListener> listeners = new ArrayList<>();
    private CommonDataManager<GunData> gunData;
    private CommonDataManager<AttachmentData> attachmentData;
    private CommonDataManager<BlockData> blockData;
    private CommonDataManager<CommonAmmoIndex> ammoIndex;
    private CommonDataManager<CommonGunIndex> gunIndex;
    private CommonDataManager<CommonAttachmentIndex> attachmentIndex;
    private CommonDataManager<CommonBlockIndex> blockIndex;
    private RecipeFilterManager recipeFilterManager;

    private AttachmentsTagManager attachmentsTagManager;
    List<LuaLibrary> libList = List.of(new LuaGunLogicConstant());
    private final ScriptManager scriptManager = new ScriptManager(new FileToIdConverter("scripts", ".lua"), libList);

    public void reloadAndRegister(Consumer<PreparableReloadListener> register) {
        // 这里会顺序重载，所以需要把index这种依赖data的放在后面
        gunData = register(new CommonDataManager<>(DataType.GUN_DATA, GunData.class, GSON, "data/guns", "GunDataLoader"));
        attachmentData = register(new AttachmentDataManager());
        attachmentsTagManager = register(new AttachmentsTagManager());
        recipeFilterManager = register(new RecipeFilterManager());
        blockData = register(new CommonDataManager<>(DataType.BLOCK_DATA, BlockData.class, GSON, "data/blocks", "BlockDataLoader"));
        register.accept(scriptManager);

        ammoIndex = register(new CommonDataManager<>(DataType.AMMO_INDEX, CommonAmmoIndex.class, GSON, "index/ammo", "AmmoIndexLoader"));
        gunIndex = register(new CommonDataManager<>(DataType.GUN_INDEX, CommonGunIndex.class, GSON, "index/guns", "GunIndexLoader"));
        attachmentIndex = register(new CommonDataManager<>(DataType.ATTACHMENT_INDEX, CommonAttachmentIndex.class, GSON, "index/attachments", "AttachmentIndexLoader"));
        blockIndex = register(new CommonDataManager<>(DataType.BLOCK_INDEX, CommonBlockIndex.class, GSON, "index/blocks", "BlockIndexLoader"));

        listeners.forEach(register);
        register.accept((barrier, resourceManager, preparationProfiler, reloadProfiler, backgroundExecutor, gameExecutor) -> {
            return barrier
                    .wait(Void.TYPE)
                    .thenRunAsync(AllowAttachmentTagMatcher::resetCache, gameExecutor);
        });
    }

    private <T extends INetworkCacheReloadListener> T register(T listener) {
        listeners.add(listener);
        return listener;
    }

    public Map<DataType, Map<ResourceLocation, String>> getNetworkCache() {
        ImmutableMap.Builder<DataType, Map<ResourceLocation, String>> builder = ImmutableMap.builder();
        for (INetworkCacheReloadListener listener : listeners) {
            builder.put(listener.getType(), listener.getNetworkCache());
        }
        return builder.build();
    }

    @Nullable
    @Override
    public GunData getGunData(ResourceLocation id) {
        return gunData.getData(id);
    }

    @Nullable
    @Override
    public AttachmentData getAttachmentData(ResourceLocation id) {
        return attachmentData.getData(id);
    }

    @Nullable
    @Override
    public BlockData getBlockData(ResourceLocation id) {
        return blockData.getData(id);
    }

    @Override
    @Nullable
    public RecipeFilter getRecipeFilter(ResourceLocation id) {
        return recipeFilterManager.getFilter(id);
    }

    @Nullable
    @Override
    public CommonGunIndex getGunIndex(ResourceLocation gunId) {
        return gunIndex.getData(gunId);
    }

    @Override
    public Set<Map.Entry<ResourceLocation, CommonGunIndex>> getAllGuns() {
        return gunIndex.getAllData().entrySet();
    }

    @Nullable
    @Override
    public CommonAmmoIndex getAmmoIndex(ResourceLocation ammoId) {
        return ammoIndex.getData(ammoId);
    }

    @Override
    public Set<Map.Entry<ResourceLocation, CommonAmmoIndex>> getAllAmmos() {
        return ammoIndex.getAllData().entrySet();
    }

    @Nullable
    @Override
    public CommonAttachmentIndex getAttachmentIndex(ResourceLocation attachmentId) {
        return attachmentIndex.getData(attachmentId);
    }

    @Override
    public Set<Map.Entry<ResourceLocation, CommonAttachmentIndex>> getAllAttachments() {
        return attachmentIndex.getAllData().entrySet();
    }

    @Override
    public LuaTable getScript(ResourceLocation scriptId) {
        return scriptManager.getScript(scriptId);
    }

    @Nullable
    @Override
    public CommonBlockIndex getBlockIndex(ResourceLocation blockId) {
        return blockIndex.getData(blockId);
    }

    @Override
    public Set<Map.Entry<ResourceLocation, CommonBlockIndex>> getAllBlocks() {
        return blockIndex.getAllData().entrySet();
    }

    @Override
    public Set<String> getAttachmentTags(ResourceLocation registryName) {
        return attachmentsTagManager.getAttachmentTags(registryName);
    }

    @Override
    public Set<String> getAllowAttachmentTags(ResourceLocation registryName) {
        return attachmentsTagManager.getAllowAttachmentTags(registryName);
    }

    /**
     * 获取实例<br/>
     * 实例仅当内置服务器/专用服务器启动时才会被创建<br/>
     * 当客户端正连接到多人游戏时，该方法将返回 null
     * @return CommonAssetsManger实例
     */
    @Nullable
    public static CommonAssetsManager getInstance() {
        return INSTANCE;
    }

    /**
     * 根据当前环境选择合适的缓存<br/>
     * 当前环境为单人游戏或多人游戏的服务端时，返回CommonAssetsManger实例<br/>
     * 当前环境为多人游戏的客户端时，返回CommonNetworkCache实例
     * @return ICommonResourceProvider实例
     */
    public static ICommonResourceProvider get() {
        return INSTANCE == null ? CommonNetworkCache.INSTANCE : INSTANCE;
    }

    public static Optional<SkinDefinition> getSkinAttachment(ResourceLocation id) {
        CommonAttachmentIndex index = get().getAttachmentIndex(id);
        if (index == null || index.getType() != AttachmentType.SKIN) {
            return Optional.empty();
        }
        return createSkinAttachment(id, index);
    }

    public static List<SkinDefinition> getAllSkinAttachments() {
        List<SkinDefinition> result = new ArrayList<>();
        for (Map.Entry<ResourceLocation, CommonAttachmentIndex> entry : get().getAllAttachments()) {
            if (entry.getValue().getType() == AttachmentType.SKIN) {
                createSkinAttachment(entry.getKey(), entry.getValue()).ifPresent(result::add);
            }
        }
        return result;
    }

    public static List<SkinDefinition> getSkinAttachmentsForGun(ResourceLocation gunId) {
        return getAllSkinAttachments().stream().filter(skin -> skin.isApplicableTo(gunId)).toList();
    }

    public static Optional<KeychainDefinition> getKeychainAttachment(ResourceLocation id) {
        CommonAttachmentIndex index = get().getAttachmentIndex(id);
        if (index == null || index.getType() != AttachmentType.KEYCHAIN) {
            return Optional.empty();
        }
        return createKeychainAttachment(id, index);
    }

    public static Optional<AttachmentData> getCosmeticAttachmentData(AttachmentType type, ResourceLocation id) {
        if (type != AttachmentType.SKIN && type != AttachmentType.KEYCHAIN) {
            return Optional.empty();
        }
        CommonAttachmentIndex index = get().getAttachmentIndex(id);
        if (index == null || index.getType() != type || getCosmeticData(index) == null) {
            return Optional.empty();
        }
        return Optional.of(index.getData());
    }

    private static Optional<SkinDefinition> createSkinAttachment(ResourceLocation id, CommonAttachmentIndex index) {
        AttachmentData.CosmeticData cosmetic = getCosmeticData(index);
        if (cosmetic == null || cosmetic.getSkin() == null) {
            return Optional.empty();
        }

        try {
            AttachmentData.SkinData skin = cosmetic.getSkin();
            SkinDefinition.SkinType type = "universal".equalsIgnoreCase(skin.getType())
                    ? SkinDefinition.SkinType.UNIVERSAL
                    : SkinDefinition.SkinType.SPECIFIC;
            if (type == SkinDefinition.SkinType.SPECIFIC && (skin.getTargetGun() == null || skin.getTexture() == null)) {
                throw new IllegalArgumentException("Specific skin requires cosmetic.skin.target_gun and cosmetic.skin.texture");
            }
            if (type == SkinDefinition.SkinType.UNIVERSAL && skin.getOverlayTexture() == null) {
                throw new IllegalArgumentException("Universal skin requires cosmetic.skin.overlay_texture");
            }

            var pojo = index.getPojo();
            return Optional.of(new SkinDefinition(
                    id,
                    type,
                    pojo.getName(),
                    CosmeticRarity.fromString(cosmetic.getRarity()),
                    skin.getTargetGun(),
                    skin.getTexture(),
                    skin.getModelOverride(),
                    skin.getOverlayTexture(),
                    SkinDefinition.BlendMode.fromString(skin.getBlendMode()),
                    description(cosmetic, pojo),
                    cosmetic.getIcon(),
                    index.getData()
            ));
        } catch (Exception e) {
            GunMod.LOGGER.warn("Failed to read skin attachment {}", id, e);
            return Optional.empty();
        }
    }

    private static Optional<KeychainDefinition> createKeychainAttachment(ResourceLocation id, CommonAttachmentIndex index) {
        AttachmentData.CosmeticData cosmetic = getCosmeticData(index);
        if (cosmetic == null || cosmetic.getKeychain() == null) {
            return Optional.empty();
        }

        try {
            AttachmentData.KeychainData keychain = cosmetic.getKeychain();
            AttachmentData.KeychainAttachmentData attachment = keychain.getDefaultAttachment();
            ResourceLocation model = keychain.getModel();
            if (model == null) {
                model = new ResourceLocation(id.getNamespace(), "attachment/" + id.getPath() + "_geo");
            }
            ResourceLocation texture = keychain.getTexture();
            if (texture == null) {
                texture = new ResourceLocation(id.getNamespace(), "attachment/uv/" + id.getPath());
            }

            var pojo = index.getPojo();
            return Optional.of(new KeychainDefinition(
                    id,
                    pojo.getName(),
                    CosmeticRarity.fromString(cosmetic.getRarity()),
                    model,
                    texture,
                    attachment != null ? attachment.getBone() : "stock",
                    vec(attachment != null ? attachment.getOffset() : null, 0.0f, 0.0f, 0.0f),
                    vec(attachment != null ? attachment.getRotation() : null, 0.0f, 0.0f, 0.0f),
                    vec(attachment != null ? attachment.getScale() : null, 1.0f, 1.0f, 1.0f),
                    description(cosmetic, pojo),
                    cosmetic.getIcon(),
                    index.getData()
            ));
        } catch (Exception e) {
            GunMod.LOGGER.warn("Failed to read keychain attachment {}", id, e);
            return Optional.empty();
        }
    }

    @Nullable
    private static AttachmentData.CosmeticData getCosmeticData(CommonAttachmentIndex index) {
        return index.getData().getCosmeticData();
    }

    private static List<String> description(AttachmentData.CosmeticData cosmetic, com.tacz.guns.resource.pojo.AttachmentIndexPOJO pojo) {
        List<String> result = new ArrayList<>();
        String[] cosmeticDescription = cosmetic.getDescription();
        if (cosmeticDescription != null) {
            result.addAll(List.of(cosmeticDescription));
        }
        if (result.isEmpty() && pojo.getTooltip() != null) {
            result.add(pojo.getTooltip());
        }
        return result;
    }

    private static Vector3f vec(float[] values, float x, float y, float z) {
        return new Vector3f(
                values != null && values.length > 0 ? values[0] : x,
                values != null && values.length > 1 ? values[1] : y,
                values != null && values.length > 2 ? values[2] : z
        );
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        var commonAssetsManager = new CommonAssetsManager();
        commonAssetsManager.reloadAndRegister(event::addListener);
        INSTANCE = commonAssetsManager;
        INSTANCE.recipeManager = event.getServerResources().getRecipeManager();
    }

    public RecipeManager recipeManager;

    /**
     * 这个事件理论上会在server resource已经完成重载和传输到客户端之前触发<br/>
     * 尝试根据common data初始化延迟加载的配方
     * @param event
     */
    @SubscribeEvent
    public static void onReload(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD){
            if (getInstance() !=null && getInstance().recipeManager != null) {
                List<GunSmithTableRecipe> recipes = getInstance().recipeManager.getAllRecipesFor(ModRecipe.GUN_SMITH_TABLE_CRAFTING.get());
                for (GunSmithTableRecipe recipe : recipes) {
                    recipe.init();
                }
            }
        }
    }


    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        INSTANCE = null;
    }

    @SubscribeEvent
    public static void OnDatapackSync(OnDatapackSyncEvent event) {
        if (getInstance() == null) {
            return;
        }
        ServerMessageSyncGunPack message = new ServerMessageSyncGunPack(getInstance().getNetworkCache());
        if (event.getPlayer() != null) {
            NetworkHandler.sendToClientPlayer(message, event.getPlayer());
        } else {
            event.getPlayerList().getPlayers().forEach(player -> NetworkHandler.sendToClientPlayer(message, player));
        }
    }

    public static void reloadAllPack() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        PackRepository packrepository = server.getPackRepository();
        packrepository.reload();

        Collection<String> collection = packrepository.getSelectedIds();
        server.reloadResources(collection);
    }
}
