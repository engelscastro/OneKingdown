package grouponekingdown.onekingdown.network;

import grouponekingdown.onekingdown.Onekingdown;
import grouponekingdown.onekingdown.kingdom.KingdomManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ConfirmRoomSetupPayload(
        int maxPlayers,
        int[] themeOrdinals,
        int[] bannerOrdinals
) implements CustomPacketPayload {

    public static final Type<ConfirmRoomSetupPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Onekingdown.MODID, "confirm_room_setup"));

    public static final StreamCodec<FriendlyByteBuf, ConfirmRoomSetupPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeInt(p.maxPlayers);
                        buf.writeVarIntArray(p.themeOrdinals);
                        buf.writeVarIntArray(p.bannerOrdinals);
                    },
                    buf -> new ConfirmRoomSetupPayload(
                            buf.readInt(),
                            buf.readVarIntArray(),
                            buf.readVarIntArray()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(ConfirmRoomSetupPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                KingdomManager.applyRoomSetup(
                        player,
                        payload.maxPlayers(),
                        payload.themeOrdinals(),
                        payload.bannerOrdinals()
                );
            }
        });
    }
}