package grouponekingdown.onekingdown.kingdom;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import grouponekingdown.onekingdown.Config;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Optional;

public class KingdomEvents {

    public static void onServerStarting(ServerStartingEvent event) {
        KingdomManager.init(event.getServer());
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        KingdomManager.shutdown();
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        KingdomManager manager = KingdomManager.get();
        if (manager != null) {
            manager.onServerTick(event.getServer());
        }
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (KingdomManager.get() == null) return;

        ServerLevel overworld = player.level().getServer().overworld();
        if (KingdomNpcManager.handleNpcInteract(overworld, player, event.getTarget())) {
            event.setCanceled(true);
        }
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        KingdomManager manager = KingdomManager.get();
        if (manager == null) return;

        ServerLevel overworld = player.level().getServer().overworld();
        boolean isNewPlayer = manager.getKingdomByPlayer(player.getUUID()).isEmpty();
        KingdomData.Kingdom kingdom = manager.getOrCreateKingdom(overworld, player);

        if (isNewPlayer) {
            manager.teleportToKingdom(overworld, player, kingdom);
            player.sendSystemMessage(Component.literal(
                    "§6[OneKingdown] Bem-vindo, General! Seu reino na Sala #" + kingdom.roomId
                            + " foi erguido com Castelo, Quartel (Leste), Mina (Oeste) e Plantacao (Norte)."
            ));
            player.sendSystemMessage(Component.literal(
                    "§7Converse com o §6Rei§7 e os gerenciadores de cada construcao para recrutar tropas e trabalhadores!"
            ));
        } else {
            manager.sendHudSnapshot(overworld, player, kingdom);
        }
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        KingdomManager manager = KingdomManager.get();
        if (manager == null) return;

        var server = player.level().getServer();
        ServerLevel overworld = server.overworld();
        KingdomData.Kingdom kingdom = manager.getOrCreateKingdom(overworld, player);

        server.execute(() -> {
            manager.teleportToKingdom(overworld, player, kingdom);
            player.sendSystemMessage(Component.literal("§6[OneKingdown] Voce renasceu no seu castelo!"));
        });
    }

    public static void onBlockBreak(BreakBlockEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (player.isCreative()) return;

        KingdomManager manager = KingdomManager.get();
        if (manager == null) return;

        Optional<KingdomData.Kingdom> ownerKingdom = manager.findKingdomAt(event.getPos());
        if (ownerKingdom.isPresent() && !ownerKingdom.get().ownerUuid.equals(player.getUUID().toString())) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal(
                    "§c[OneKingdown] Este territorio pertence ao reino de " + ownerKingdom.get().ownerName + "!"
            ));
        }
    }

    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isCreative()) return;

        KingdomManager manager = KingdomManager.get();
        if (manager == null) return;

        Optional<KingdomData.Kingdom> ownerKingdom = manager.findKingdomAt(event.getPos());
        if (ownerKingdom.isPresent() && !ownerKingdom.get().ownerUuid.equals(player.getUUID().toString())) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal(
                    "§c[OneKingdown] Voce nao pode construir no reino de " + ownerKingdom.get().ownerName + "!"
            ));
        }
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("reino")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "§6[OneKingdown] Comandos: §f/reino info§7, §f/reino casa§7, §f/reino contratar <guarda|mineiro|fazendeiro>§7, §f/reino listar§7, §f/reino addxp <qtd>"
                            ), false);
                            return 1;
                        })
                        .then(Commands.literal("info").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            KingdomManager manager = KingdomManager.get();
                            ServerLevel overworld = ctx.getSource().getServer().overworld();
                            KingdomData.Kingdom k = manager.getOrCreateKingdom(overworld, player);
                            KingdomData.Room room = manager.getRoom(k.roomId).orElse(null);

                            String roomCount = room != null ? (room.kingdoms.size() + "/" + room.maxPlayers) : "?";
                            int nextUpgradeLevel = k.currentTier * 10;

                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "§6=== Reino do General " + k.ownerName + " ===\n"
                                            + "§7Sala: §f#" + k.roomId + " (" + roomCount + " jogadores)\n"
                                            + "§7Nivel: §f" + k.level + " §7(XP: §f" + k.xp + "/" + Config.xpPerLevel + "§7) | §eTier " + k.currentTier + " §7(prox. nivel " + nextUpgradeLevel + ")\n"
                                            + "§9Guardas: §f" + k.guardsCount + "/" + k.getMaxGuards()
                                            + " §7| §bMineiros: §f" + k.minersCount + "/" + k.getMaxMiners()
                                            + " §7| §aFazendeiros: §f" + k.farmersCount + "/" + k.getMaxFarmers() + "\n"
                                            + "§7Construcoes: Castelo (Centro), Quartel (+20 X), Mina (-20 X), Plantacao (-20 Z)"
                            ), false);
                            return 1;
                        }))
                        .then(Commands.literal("casa").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            KingdomManager manager = KingdomManager.get();
                            ServerLevel overworld = ctx.getSource().getServer().overworld();
                            KingdomData.Kingdom k = manager.getOrCreateKingdom(overworld, player);
                            manager.teleportToKingdom(overworld, player, k);
                            ctx.getSource().sendSuccess(() -> Component.literal("§a[OneKingdown] Teleportado para o trono do seu castelo!"), false);
                            return 1;
                        }))
                        .then(Commands.literal("contratar")
                                .then(Commands.argument("unidade", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("guarda");
                                            builder.suggest("mineiro");
                                            builder.suggest("fazendeiro");
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String unit = StringArgumentType.getString(ctx, "unidade");
                                            ServerLevel overworld = ctx.getSource().getServer().overworld();
                                            KingdomData.Kingdom k = KingdomManager.get().getOrCreateKingdom(overworld, player);
                                            KingdomNpcManager.hireUnit(overworld, player, k, unit);
                                            return 1;
                                        })))
                        .then(Commands.literal("listar").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            KingdomManager manager = KingdomManager.get();
                            ServerLevel overworld = ctx.getSource().getServer().overworld();
                            KingdomData.Kingdom myKingdom = manager.getOrCreateKingdom(overworld, player);
                            Optional<KingdomData.Room> roomOpt = manager.getRoom(myKingdom.roomId);

                            if (roomOpt.isEmpty()) return 0;
                            KingdomData.Room room = roomOpt.get();
                            StringBuilder sb = new StringBuilder("§6=== Sala #" + room.roomId
                                    + " (" + room.kingdoms.size() + "/" + room.maxPlayers + ") ===");
                            for (KingdomData.Kingdom k : room.kingdoms) {
                                sb.append("\n§e- ").append(k.ownerName)
                                        .append(" §7| Nivel ").append(k.level)
                                        .append(" (Tier ").append(k.currentTier).append(")")
                                        .append(" | Guardas: ").append(k.guardsCount);
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                            return 1;
                        }))
                        .then(Commands.literal("addxp")
                                .then(Commands.argument("quantidade", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            int amount = IntegerArgumentType.getInteger(ctx, "quantidade");
                                            ServerLevel overworld = ctx.getSource().getServer().overworld();
                                            KingdomManager.get().addXpAndCheckUpgrade(overworld, player, amount);
                                            return 1;
                                        })))
                        .then(Commands.literal("sala_max")
                                .then(Commands.argument("limite", IntegerArgumentType.integer(1, 50))
                                        .executes(ctx -> {
                                            int novoLimite = IntegerArgumentType.getInteger(ctx, "limite");
                                            Config.maxPlayersPerRoom = novoLimite;
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "§a[OneKingdown] Limite das proximas salas configurado para " + novoLimite + " jogadores."
                                            ), true);
                                            return 1;
                                        })))
        );
    }
}