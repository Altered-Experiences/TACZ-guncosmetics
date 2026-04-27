package com.tacz.guns.cosmetic.client.gui;

import com.tacz.guns.cosmetic.data.CosmeticNBTHelper;
import com.tacz.guns.cosmetic.data.KeychainDefinition;
import com.tacz.guns.cosmetic.data.SkinDefinition;
import com.tacz.guns.cosmetic.network.CosmeticsNetworkHandler;
import com.tacz.guns.cosmetic.network.message.ApplyCosmeticPacket;
import com.tacz.guns.cosmetic.network.message.RemoveCosmeticPacket;
import com.tacz.guns.cosmetic.network.message.UpdateKeychainTransformPacket;
import com.tacz.guns.cosmetic.pack.CosmeticLocalization;
import com.tacz.guns.cosmetic.registry.KeychainRegistry;
import com.tacz.guns.cosmetic.registry.SkinRegistry;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.util.RenderDistance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CosmeticsEditorScreen extends Screen {
    private enum Tab { SKINS, KEYCHAINS }

    private Tab currentTab = Tab.SKINS;
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private List<SkinDefinition> availableSkins = new ArrayList<>();
    private List<KeychainDefinition> availableKeychains = new ArrayList<>();

    private float kcOffsetX = 0, kcOffsetY = 0, kcOffsetZ = 0;
    private float kcRotX = 0, kcRotY = 0, kcRotZ = 0;
    private float kcScale = 1.0f;
    private String kcBone = "";

    private List<String> availableBones = new ArrayList<>();
    private int currentBoneIndex = -1;

    private String selectedPack = "all";
    private String sortMode = "name"; // name, rarity

    private int previewX, previewY, previewW, previewH;
    private boolean draggingPreview;
    private int dragButton = -1;
    private boolean selectingPack = true;
    private float previewRotX = 14.0f;
    private float previewRotY = 90.0f;
    private float previewZoom = 1.0f;

    public CosmeticsEditorScreen() {
        super(Component.translatable("gui.guncosmetics.title"));
    }

    @Override
    protected void init() {
        this.clearWidgets();
        loadAvailableCosmetics();
        selectCurrentCosmeticIfNeeded();

        int listWidth = getListWidth();
        int controlY = getControlY();

        this.addRenderableWidget(Button.builder(Component.translatable("gui.guncosmetics.tab.skins"),
                b -> { currentTab = Tab.SKINS; selectedIndex = -1; scrollOffset = 0; selectingPack = false; init(); })
                .bounds(10, 5, 90, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.guncosmetics.tab.keychains"),
                b -> { currentTab = Tab.KEYCHAINS; selectedIndex = -1; scrollOffset = 0; selectingPack = false; init(); })
                .bounds(106, 5, 112, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.guncosmetics.close"),
                b -> this.onClose())
                .bounds(this.width - 66, 5, 60, 20).build());

        if (selectingPack) {
            this.addRenderableWidget(Button.builder(Component.literal("Select a Pack"), b -> {}).bounds(10, 28, listWidth, 20).build()).active = false;
        } else {
            this.addRenderableWidget(Button.builder(Component.literal("< Back to Packs"),
                    b -> { selectingPack = true; init(); }).bounds(10, 28, listWidth / 2 - 2, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("Sort: " + sortMode),
                    b -> { sortMode = "name".equals(sortMode) ? "rarity" : "name"; init(); })
                    .bounds(10 + listWidth / 2 + 2, 28, listWidth / 2 - 2, 20).build());
        }

        int maxVisible = Math.max(1, (getActionY() - 60) / 20);
        
        if (selectingPack) {
            List<String> packList = getAvailablePacks();
            for (int i = 0; i < Math.min(maxVisible, packList.size() - scrollOffset); i++) {
                int idx = i + scrollOffset;
                String packId = packList.get(idx);
                String packName = "all".equals(packId) ? Component.translatable("gui.guncosmetics.pack.all.name").getString() : tr("pack." + packId + ".name");
                this.addRenderableWidget(Button.builder(Component.literal(packName),
                        b -> { selectedPack = packId; selectingPack = false; scrollOffset = 0; selectedIndex = -1; init(); })
                        .bounds(10, 52 + i * 20, listWidth, 18).build());
            }
        } else {
            List<?> items = currentTab == Tab.SKINS ? availableSkins : availableKeychains;
            for (int i = 0; i < Math.min(maxVisible, items.size() - scrollOffset); i++) {
                int idx = i + scrollOffset;
                String label;
                int color;
                if (currentTab == Tab.SKINS) {
                    SkinDefinition skin = availableSkins.get(idx);
                    label = tr(skin.getDisplayName());
                    color = skin.getRarity().getColor();
                } else {
                    KeychainDefinition kc = availableKeychains.get(idx);
                    label = tr(kc.getDisplayName());
                    color = kc.getRarity().getColor();
                }

                final int finalIdx = idx;
                Button entry = Button.builder(Component.literal(label).withStyle(s -> s.withColor(color)),
                        b -> { selectIndex(finalIdx); init(); })
                        .bounds(10, 52 + i * 20, listWidth, 18).build();
                if (idx == selectedIndex) entry.active = false;
                this.addRenderableWidget(entry);
            }
        }

        int bottomY = getActionY();
        int actionW = Math.max(54, (listWidth - 8) / 3);
        this.addRenderableWidget(Button.builder(Component.translatable("gui.guncosmetics.apply"),
                b -> applySelected()).bounds(10, bottomY, actionW, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.guncosmetics.reset"),
                b -> { selectedIndex = -1; init(); }).bounds(14 + actionW, bottomY, actionW, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.guncosmetics.remove"),
                b -> removeCurrentCosmetic()).bounds(18 + actionW * 2, bottomY, actionW, 20).build());

        if (currentTab == Tab.KEYCHAINS && selectedIndex >= 0) {
            int x = getRightX() + 10;
            int y = getControlY();
            int rightWidth = getRightWidth() - 20;
            int colGap = 8;
            int colW = Math.max(80, (rightWidth - colGap) / 2);
            int row = 17;
            int x2 = x + colW + colGap;
            addTransformSlider(x, y, colW, "X", -16.0f, 16.0f, () -> kcOffsetX, v -> kcOffsetX = v, "%.2f");
            addTransformSlider(x, y + row, colW, "Y", -16.0f, 16.0f, () -> kcOffsetY, v -> kcOffsetY = v, "%.2f");
            addTransformSlider(x, y + row * 2, colW, "Z", -16.0f, 16.0f, () -> kcOffsetZ, v -> kcOffsetZ = v, "%.2f");
            addTransformSlider(x, y + row * 3, colW, "S", 0.05f, 4.0f, () -> kcScale, v -> kcScale = Math.max(0.05f, v), "%.2f");
            addTransformSlider(x2, y, colW, "RX", -180.0f, 180.0f, () -> kcRotX, v -> kcRotX = v, "%.0f");
            addTransformSlider(x2, y + row, colW, "RY", -180.0f, 180.0f, () -> kcRotY, v -> kcRotY = v, "%.0f");
            addTransformSlider(x2, y + row * 2, colW, "RZ", -180.0f, 180.0f, () -> kcRotZ, v -> kcRotZ = v, "%.0f");
            this.addRenderableWidget(Button.builder(Component.literal("Auto"),
                    b -> { resetToDefaultKeychainTransform(); syncKeychainTransform(); init(); })
                    .bounds(x2, y + row * 3, colW, 16).build());

            // Bone cycle button
            int boneY = y - 19;
            this.addRenderableWidget(Button.builder(Component.literal("Bone: " + (kcBone.isEmpty() ? "Auto" : kcBone)),
                    b -> {
                        if (availableBones.isEmpty()) return;
                        currentBoneIndex = (currentBoneIndex + 1) % availableBones.size();
                        kcBone = availableBones.get(currentBoneIndex);
                        syncKeychainTransform();
                        init();
                    }).bounds(x, boneY, rightWidth, 16).build());
        }
    }

    private void addTransformSlider(int x, int y, int width, String label, float min, float max,
                                    java.util.function.Supplier<Float> getter,
                                    java.util.function.Consumer<Float> setter, String format) {
        this.addRenderableWidget(new TransformSlider(x, y, width, 16, label, min, max, getter, setter, format));
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        int listWidth = getListWidth();
        graphics.fill(5, 28, listWidth + 15, this.height - 6, 0x90000000);
        graphics.fill(listWidth + 18, 28, this.width - 5, this.height - 6, 0x88000000);

        graphics.fill(currentTab == Tab.SKINS ? 10 : 106, 25,
                currentTab == Tab.SKINS ? 100 : 218, 27, 0xFFFFAA00);

        previewX = listWidth + 24;
        previewY = 35;
        previewW = Math.max(80, this.width - previewX - 14);
        previewH = getPreviewHeight();
        graphics.fill(previewX, previewY, previewX + previewW, previewY + previewH, 0x9909090C);
        renderPreviewModel(previewX, previewY, previewW, previewH);

        if (currentTab == Tab.KEYCHAINS && selectedIndex >= 0) {
            int controlY = getControlY() - 23;
            graphics.fill(previewX, controlY, previewX + previewW, this.height - 6, 0xAA050507);
        }

        drawSelectionInfo(graphics, previewX + 10, previewY + 8);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawSelectionInfo(GuiGraphics graphics, int x, int y) {
        if (selectedIndex < 0) return;
        List<String> lines = new ArrayList<>();
        if (selectingPack && selectedIndex >= 0 && selectedIndex < getAvailablePacks().size()) {
            String packId = getAvailablePacks().get(selectedIndex);
            if (!"all".equals(packId)) {
                lines.add(tr("pack." + packId + ".name"));
                lines.add("");
                lines.add("§o" + tr("pack." + packId + ".desc") + "§r");
            } else {
                lines.add(Component.translatable("gui.guncosmetics.pack.all.name").getString());
                lines.add("");
                lines.add("§o" + Component.translatable("gui.guncosmetics.pack.all.desc").getString() + "§r");
            }
        } else if (!selectingPack && currentTab == Tab.SKINS && selectedIndex >= 0 && selectedIndex < availableSkins.size()) {
            SkinDefinition skin = availableSkins.get(selectedIndex);
            lines.add(tr(skin.getDisplayName()) + " [" + skin.getRarity().getId() + "]");
            lines.add(skin.getType() == SkinDefinition.SkinType.UNIVERSAL ? "Paintjob overlay" : "Skin texture");
            if (skin.getTargetGun() != null) lines.add("For: " + skin.getTargetGun());
        } else if (!selectingPack && currentTab == Tab.KEYCHAINS && selectedIndex >= 0 && selectedIndex < availableKeychains.size()) {
            KeychainDefinition kc = availableKeychains.get(selectedIndex);
            lines.add(tr(kc.getDisplayName()) + " [" + kc.getRarity().getId() + "]");
            lines.add("Bone: " + kc.getBone());
        }
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(this.font, lines.get(i), x, y + i * 14, 0xDDDDDD);
        }
    }

    private int getListWidth() {
        return Math.min(260, Math.max(210, this.width / 3));
    }

    private String tr(String key) {
        if (key == null || key.isEmpty()) return "";
        String locale = Minecraft.getInstance().getLanguageManager().getSelected();
        String translated = CosmeticLocalization.translate(key, locale);
        if (!translated.equals(key)) return translated;
        String vanilla = Component.translatable(key).getString();
        return vanilla.equals(key) ? key : vanilla;
    }

    private int getActionY() {
        return this.height - 31;
    }

    private int getControlY() {
        return Math.max(112, this.height - 88);
    }

    private int getRightX() {
        return getListWidth() + 24;
    }

    private int getRightWidth() {
        return Math.max(80, this.width - getRightX() - 14);
    }

    private int getPreviewHeight() {
        if (currentTab == Tab.KEYCHAINS && selectedIndex >= 0) {
            return Math.max(90, getControlY() - previewY - 27);
        }
        return this.height - previewY - 46;
    }

    private List<String> getAvailablePacks() {
        java.util.Set<String> packs = new java.util.HashSet<>();
        SkinRegistry.getAll().forEach(s -> packs.add(s.getSkinId().getNamespace()));
        KeychainRegistry.getAll().forEach(k -> packs.add(k.getKeychainId().getNamespace()));
        List<String> packList = new ArrayList<>(packs);
        packList.sort(String::compareTo);
        packList.add(0, "all");
        return packList;
    }

    @SuppressWarnings("deprecation")
    private void renderPreviewModel(int x, int y, int width, int height) {
        ItemStack previewStack = buildPreviewStack();
        if (previewStack.isEmpty() || IGun.getIGunOrNull(previewStack) == null) return;

        RenderDistance.markGuiRenderTimestamp();
        Window window = Minecraft.getInstance().getWindow();
        double guiScale = window.getGuiScale();
        RenderSystem.enableScissor((int) (x * guiScale),
                (int) (window.getHeight() - ((y + height) * guiScale)),
                (int) (width * guiScale), (int) (height * guiScale));

        Minecraft.getInstance().textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS).setFilter(false, false);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.translate(x + width / 2.0F, y + height / 2.0F + 8.0F, 250.0F);
        modelView.scale(1.0F, -1.0F, 1.0F);
        float scale = Math.max(38.0F, Math.min(width, height) * 0.54F) * previewZoom;
        modelView.scale(scale, scale, scale);
        modelView.mulPose(Axis.XP.rotationDegrees(previewRotX));
        modelView.mulPose(Axis.YP.rotationDegrees(previewRotY));
        RenderSystem.applyModelViewMatrix();

        PoseStack pose = new PoseStack();
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        Lighting.setupForFlatItems();
        Minecraft.getInstance().getItemRenderer().renderStatic(previewStack,
                ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                pose, buffer, null, 0);
        buffer.endBatch();
        Lighting.setupFor3DItems();

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.disableScissor();
        RenderSystem.enableDepthTest();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isInPreview(mouseX, mouseY)) {
            previewZoom = Math.max(0.35f, Math.min(2.75f, previewZoom + (float) delta * 0.08f));
            return true;
        }
        List<?> items = currentTab == Tab.SKINS ? availableSkins : availableKeychains;
        int maxVisible = Math.max(1, (getActionY() - 60) / 20);
        if (delta > 0 && scrollOffset > 0) {
            scrollOffset--;
            init();
        } else if (delta < 0 && scrollOffset + maxVisible < items.size()) {
            scrollOffset++;
            init();
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInPreview(mouseX, mouseY)) {
            if (button == 0 || button == 1) {
                draggingPreview = true;
                dragButton = button;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingPreview && button == dragButton) {
            if (currentTab == Tab.KEYCHAINS && selectedIndex >= 0 && Screen.hasShiftDown()) {
                kcOffsetZ += (float) dragY * 0.035f;
                syncKeychainTransform();
            } else if (currentTab == Tab.KEYCHAINS && selectedIndex >= 0 && button == 1) {
                kcOffsetX += (float) dragX * 0.035f;
                kcOffsetY -= (float) dragY * 0.035f;
                syncKeychainTransform();
            } else {
                previewRotY += (float) dragX * 0.45f;
                previewRotX += (float) dragY * 0.45f;
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingPreview && button == dragButton) {
            draggingPreview = false;
            dragButton = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isInPreview(double mouseX, double mouseY) {
        return mouseX >= previewX && mouseX <= previewX + previewW && mouseY >= previewY && mouseY <= previewY + previewH;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void loadAvailableCosmetics() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack gunStack = player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(gunStack);
        availableSkins = iGun != null ? SkinRegistry.getForGun(iGun.getGunId(gunStack)) : SkinRegistry.getAll();
        availableKeychains = KeychainRegistry.getAll();

        if (!"all".equals(selectedPack)) {
            availableSkins.removeIf(skin -> !skin.getSkinId().getNamespace().equals(selectedPack));
            availableKeychains.removeIf(kc -> !kc.getKeychainId().getNamespace().equals(selectedPack));
        }

        if ("rarity".equals(sortMode)) {
            availableSkins.sort(java.util.Comparator.comparingInt(s -> -s.getRarity().ordinal()));
            availableKeychains.sort(java.util.Comparator.comparingInt(k -> -k.getRarity().ordinal()));
        } else {
            availableSkins.sort(java.util.Comparator.comparing(SkinDefinition::getDisplayName));
            availableKeychains.sort(java.util.Comparator.comparing(KeychainDefinition::getDisplayName));
        }

        loadGunBones(gunStack);
    }

    private void loadGunBones(ItemStack gunStack) {
        availableBones.clear();
        com.tacz.guns.api.TimelessAPI.getGunDisplay(gunStack).ifPresent(display -> {
            com.tacz.guns.client.model.BedrockGunModel model = display.getGunModel();
            if (model != null) {
                model.getIndexBones().forEach((name, bone) -> {
                    if (bone != null && !name.toLowerCase(java.util.Locale.ROOT).contains("camera") &&
                        !name.toLowerCase(java.util.Locale.ROOT).contains("muzzle") &&
                        !name.toLowerCase(java.util.Locale.ROOT).contains("bullet")) {
                        availableBones.add(name);
                    }
                });
            }
        });
        availableBones.sort(String::compareTo);
    }

    private void selectCurrentCosmeticIfNeeded() {
        if (selectingPack || selectedIndex >= 0) return;
        ItemStack gunStack = getGunStack();
        if (currentTab == Tab.SKINS) {
            ResourceLocation currentSkin = CosmeticNBTHelper.getSkinId(gunStack);
            if (currentSkin == null) return;
            for (int i = 0; i < availableSkins.size(); i++) {
                if (availableSkins.get(i).getSkinId().equals(currentSkin)) {
                    selectedIndex = i;
                    keepSelectedVisible();
                    return;
                }
            }
        } else {
            ResourceLocation currentKeychain = CosmeticNBTHelper.getKeychainId(gunStack);
            if (currentKeychain == null) return;
            for (int i = 0; i < availableKeychains.size(); i++) {
                if (availableKeychains.get(i).getKeychainId().equals(currentKeychain)) {
                    selectIndex(i);
                    keepSelectedVisible();
                    return;
                }
            }
        }
    }

    private void keepSelectedVisible() {
        if (selectedIndex < 0) return;
        int maxVisible = Math.max(1, (getActionY() - 60) / 20);
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + maxVisible) {
            scrollOffset = Math.max(0, selectedIndex - maxVisible + 1);
        }
    }

    private void selectIndex(int index) {
        selectedIndex = index;
        if (currentTab != Tab.KEYCHAINS || selectedIndex < 0 || selectedIndex >= availableKeychains.size()) return;

        CompoundTag transform = CosmeticNBTHelper.getKeychainTransform(getGunStack());
        if (isUsableTransform(transform)) {
            kcOffsetX = transform.getFloat("OffsetX");
            kcOffsetY = transform.getFloat("OffsetY");
            kcOffsetZ = transform.getFloat("OffsetZ");
            kcRotX = transform.getFloat("RotX");
            kcRotY = transform.getFloat("RotY");
            kcRotZ = transform.getFloat("RotZ");
            kcScale = Math.max(0.75f, transform.getFloat("ScaleX"));
            kcBone = transform.contains("Bone") ? transform.getString("Bone") : "";
            currentBoneIndex = availableBones.indexOf(kcBone);
        } else {
            applyDefaultKeychainTransform(availableKeychains.get(selectedIndex));
        }
    }

    private boolean isUsableTransform(CompoundTag transform) {
        if (transform == null) return false;
        float x = transform.getFloat("OffsetX");
        float y = transform.getFloat("OffsetY");
        float z = transform.getFloat("OffsetZ");
        float scale = transform.getFloat("ScaleX");
        return Math.abs(x) <= 32.0f && Math.abs(y) <= 32.0f && Math.abs(z) <= 32.0f && scale >= 0.05f && scale <= 8.0f;
    }

    private void resetToDefaultKeychainTransform() {
        if (currentTab != Tab.KEYCHAINS || selectedIndex < 0 || selectedIndex >= availableKeychains.size()) return;
        applyDefaultKeychainTransform(availableKeychains.get(selectedIndex));
    }

    private void applyDefaultKeychainTransform(KeychainDefinition kc) {
        kcOffsetX = kc.getOffset().x();
        kcOffsetY = kc.getOffset().y();
        kcOffsetZ = kc.getOffset().z();
        kcRotX = kc.getRotation().x();
        kcRotY = kc.getRotation().y();
        kcRotZ = kc.getRotation().z();
        kcScale = Math.max(0.75f, kc.getScale().x());
        kcBone = kc.getBone() != null ? kc.getBone() : "stock";
        currentBoneIndex = availableBones.indexOf(kcBone);
    }

    private ItemStack getGunStack() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? player.getMainHandItem() : ItemStack.EMPTY;
    }

    private ItemStack buildPreviewStack() {
        ItemStack gunStack = getGunStack();
        if (gunStack.isEmpty()) return ItemStack.EMPTY;
        ItemStack preview = gunStack.copy();
        if (currentTab == Tab.SKINS && selectedIndex >= 0 && selectedIndex < availableSkins.size()) {
            CosmeticNBTHelper.setSkinId(preview, availableSkins.get(selectedIndex).getSkinId());
        } else if (currentTab == Tab.KEYCHAINS && selectedIndex >= 0 && selectedIndex < availableKeychains.size()) {
            CosmeticNBTHelper.setKeychainId(preview, availableKeychains.get(selectedIndex).getKeychainId());
            writePreviewTransform(preview);
        }
        return preview;
    }

    private void writePreviewTransform(ItemStack stack) {
        CosmeticNBTHelper.setKeychainTransform(stack, kcBone, kcOffsetX, kcOffsetY, kcOffsetZ,
                kcRotX, kcRotY, kcRotZ, kcScale, kcScale, kcScale);
    }

    private void syncKeychainTransform() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || IGun.getIGunOrNull(player.getMainHandItem()) == null) return;
        writePreviewTransform(player.getMainHandItem());
        CosmeticsNetworkHandler.sendToServer(new UpdateKeychainTransformPacket(player.getInventory().selected,
                kcBone, kcOffsetX, kcOffsetY, kcOffsetZ, kcRotX, kcRotY, kcRotZ, kcScale));
    }

    private void applySelected() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || selectedIndex < 0) return;
        if (IGun.getIGunOrNull(player.getMainHandItem()) == null) {
            player.sendSystemMessage(Component.translatable("gui.guncosmetics.no_gun_in_hand"));
            return;
        }
        int slot = player.getInventory().selected;
        if (currentTab == Tab.SKINS && selectedIndex < availableSkins.size()) {
            CosmeticsNetworkHandler.sendToServer(new ApplyCosmeticPacket(slot,
                    ApplyCosmeticPacket.CosmeticType.SKIN, availableSkins.get(selectedIndex).getSkinId(), null));
        } else if (currentTab == Tab.KEYCHAINS && selectedIndex < availableKeychains.size()) {
            writePreviewTransform(player.getMainHandItem());
            CosmeticsNetworkHandler.sendToServer(new ApplyCosmeticPacket(slot,
                    ApplyCosmeticPacket.CosmeticType.KEYCHAIN, availableKeychains.get(selectedIndex).getKeychainId(), null));
            syncKeychainTransform();
        }
    }

    private void removeCurrentCosmetic() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || IGun.getIGunOrNull(player.getMainHandItem()) == null) return;
        CosmeticsNetworkHandler.sendToServer(new RemoveCosmeticPacket(player.getInventory().selected,
                currentTab == Tab.SKINS ? ApplyCosmeticPacket.CosmeticType.SKIN : ApplyCosmeticPacket.CosmeticType.KEYCHAIN));
    }

    private final class TransformSlider extends AbstractSliderButton {
        private final String label;
        private final float min;
        private final float max;
        private final java.util.function.Supplier<Float> getter;
        private final java.util.function.Consumer<Float> setter;
        private final String format;

        private TransformSlider(int x, int y, int width, int height, String label, float min, float max,
                                java.util.function.Supplier<Float> getter,
                                java.util.function.Consumer<Float> setter, String format) {
            super(x, y, width, height, Component.empty(), toSliderValue(getter.get(), min, max));
            this.label = label;
            this.min = min;
            this.max = max;
            this.getter = getter;
            this.setter = setter;
            this.format = format;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(label + ": " + String.format(Locale.ROOT, format, getter.get())));
        }

        @Override
        protected void applyValue() {
            float value = (float) Mth.lerp(this.value, min, max);
            setter.accept(value);
            syncKeychainTransform();
            updateMessage();
        }
    }

    private static double toSliderValue(float value, float min, float max) {
        if (max <= min) return 0.0;
        return Mth.clamp((value - min) / (max - min), 0.0f, 1.0f);
    }
}
