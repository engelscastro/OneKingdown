package grouponekingdown.onekingdown.network;

import grouponekingdown.onekingdown.Onekingdown;
import grouponekingdown.onekingdown.invasion.InvasionManager;
import grouponekingdown.onekingdown.kingdom.KingdomData;
import grouponekingdown.onekingdown.kingdom.KingdomManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public record RequestMapPayload() implements CustomPacketPayload {

    public static final Type<RequestMapPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Onekingdown.MODID, "request_map")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestMapPayload> STREAM_CODEC =
            CustomPacketPayload.codec((payload, buf) -> {}, buf -> new RequestMapPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(RequestMapPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            int castleX = player.getBlockX(), castleZ = player.getBlockZ();
            int barracksX = castleX, barracksZ = castleZ;
            int mineX = castleX, mineZ = castleZ;
            int farmX = castleX, farmZ = castleZ;

            KingdomManager manager = KingdomManager.get();
            if (manager != null) {
                Optional<KingdomData.Kingdom> kingdomOpt = manager.getKingdomByPlayer(player.getUUID());
                if (kingdomOpt.isPresent()) {
                    KingdomData.Kingdom k = kingdomOpt.get();
                    castleX = k.centerX;
                    castleZ = k.centerZ;
                    BlockPos barracks = k.getBarracksPos();
                    BlockPos mine = k.getMinePos();
                    BlockPos farm = k.getFarmPos();
                    barracksX = barracks.getX(); barracksZ = barracks.getZ();
                    mineX = mine.getX(); mineZ = mine.getZ();
                    farmX = farm.getX(); farmZ = farm.getZ();
                }
            }

            boolean hasInvasion = InvasionManager.isRunning() && InvasionManager.getCitadelPos() != null;
            BlockPos citadel = InvasionManager.getCitadelPos();
            String invasionName = InvasionManager.getActiveInvasionName();

            PacketDistributor.sendToPlayer(player, new OpenMapPayload(
                    castleX, castleZ,
                    barracksX, barracksZ,
                    mineX, mineZ,
                    farmX, farmZ,
                    hasInvasion,
                    invasionName,
                    hasInvasion ? citadel.getX() : 0,
                    hasInvasion ? citadel.getZ() : 0
            ));
        });
    }
}