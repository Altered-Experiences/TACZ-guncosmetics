package com.tacz.guns.cosmetic.command;

import com.tacz.guns.cosmetic.data.KeychainDefinition;
import com.tacz.guns.cosmetic.data.SkinDefinition;
import com.tacz.guns.cosmetic.network.CosmeticsNetworkHandler;
import com.tacz.guns.cosmetic.network.message.SyncCosmeticsPacket;
import com.tacz.guns.cosmetic.pack.CosmeticPackLoader;
import com.tacz.guns.cosmetic.registry.KeychainRegistry;
import com.tacz.guns.cosmetic.registry.SkinRegistry;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * /guncosmetics command tree.
 */
public final class GunCosmeticsCommand {

    private GunCosmeticsCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("guncosmetics")
                // /guncosmetics open
                .then(Commands.literal("open")
                        .executes(GunCosmeticsCommand::openEditor))

                // /guncosmetics give <player> skin <skin_id>
                .then(Commands.literal("give")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("skin")
                                        .then(Commands.argument("skin_id", ResourceLocationArgument.id())
                                                .suggests(GunCosmeticsCommand::suggestSkins)
                                                .executes(GunCosmeticsCommand::giveSkin)))
                                .then(Commands.literal("keychain")
                                        .then(Commands.argument("keychain_id", ResourceLocationArgument.id())
                                                .suggests(GunCosmeticsCommand::suggestKeychains)
                                                .executes(GunCosmeticsCommand::giveKeychain)))))

                // /guncosmetics list skins [gun_id]
                .then(Commands.literal("list")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("skins")
                                .executes(ctx -> listSkins(ctx, null))
                                .then(Commands.argument("gun_id", ResourceLocationArgument.id())
                                        .executes(ctx -> listSkins(ctx,
                                                ResourceLocationArgument.getId(ctx, "gun_id")))))
                        .then(Commands.literal("keychains")
                                .executes(GunCosmeticsCommand::listKeychains)))

                // /guncosmetics reload
                .then(Commands.literal("reload")
                        .requires(s -> s.hasPermission(2))
                        .executes(GunCosmeticsCommand::reloadPacks))
        );
    }

    private static int openEditor(CommandContext<CommandSourceStack> ctx) {
        // Client-side only — the screen opening has to happen on client
        if (ctx.getSource().isPlayer()) {
            try {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                // We can't open GUI from server, but we can trigger it
                // In practice this is handled client-side; sending a packet would be needed
                ctx.getSource().sendSuccess(() -> Component.literal("Cosmetics are installed from the TACZ refit attachment screen."), false);
            } catch (Exception ignored) {}
        }
        return 1;
    }

    private static int giveSkin(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            ResourceLocation skinId = ResourceLocationArgument.getId(ctx, "skin_id");
            if (SkinRegistry.get(skinId).isEmpty()) {
                ctx.getSource().sendFailure(Component.literal("Unknown skin: " + skinId));
                return 0;
            }
            ItemStack stack = AttachmentItemBuilder.create().setId(skinId).build();
            if (!target.addItem(stack)) {
                target.drop(stack, false);
            }
            ctx.getSource().sendSuccess(() -> Component.translatable("command.guncosmetics.give.success",
                    skinId.toString(), target.getName().getString()), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int giveKeychain(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            ResourceLocation kcId = ResourceLocationArgument.getId(ctx, "keychain_id");
            if (KeychainRegistry.get(kcId).isEmpty()) {
                ctx.getSource().sendFailure(Component.literal("Unknown keychain: " + kcId));
                return 0;
            }
            ItemStack stack = AttachmentItemBuilder.create().setId(kcId).build();
            if (!target.addItem(stack)) {
                target.drop(stack, false);
            }
            ctx.getSource().sendSuccess(() -> Component.translatable("command.guncosmetics.give.success",
                    kcId.toString(), target.getName().getString()), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int listSkins(CommandContext<CommandSourceStack> ctx, ResourceLocation gunIdFilter) {
        var skins = gunIdFilter != null
                ? SkinRegistry.getForGun(gunIdFilter)
                : SkinRegistry.getAll();

        ctx.getSource().sendSuccess(() -> Component.translatable("command.guncosmetics.list.skins", skins.size()), false);
        for (SkinDefinition skin : skins) {
            ctx.getSource().sendSuccess(() -> Component.literal("  " + skin.getSkinId() + " - " + skin.getDisplayName()
                    + " [" + skin.getRarity().getId() + "] " + skin.getType()), false);
        }
        return skins.size();
    }

    private static int listKeychains(CommandContext<CommandSourceStack> ctx) {
        var keychains = KeychainRegistry.getAll();
        ctx.getSource().sendSuccess(() -> Component.translatable("command.guncosmetics.list.keychains", keychains.size()), false);
        for (KeychainDefinition kc : keychains) {
            ctx.getSource().sendSuccess(() -> Component.literal("  " + kc.getKeychainId() + " - " + kc.getDisplayName()
                    + " [" + kc.getRarity().getId() + "]"), false);
        }
        return keychains.size();
    }

    private static int reloadPacks(CommandContext<CommandSourceStack> ctx) {
        CosmeticPackLoader.loadAllPacks(ctx.getSource().getServer());
        // Sync to all players
        CosmeticsNetworkHandler.sendToAll(new SyncCosmeticsPacket());

        ctx.getSource().sendSuccess(() -> Component.translatable("command.guncosmetics.reload.success",
                SkinRegistry.size(), KeychainRegistry.size()), true);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestSkins(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(
                SkinRegistry.getAll().stream().map(SkinDefinition::getSkinId).collect(Collectors.toList()),
                builder);
    }

    private static CompletableFuture<Suggestions> suggestKeychains(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(
                KeychainRegistry.getAll().stream().map(KeychainDefinition::getKeychainId).collect(Collectors.toList()),
                builder);
    }
}
