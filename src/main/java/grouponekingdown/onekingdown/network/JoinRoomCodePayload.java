package grouponekingdown.onekingdown.network;

import grouponekingdown.onekingdown.Onekingdown;
import grouponekingdown.onekingdown.kingdom.KingdomManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record JoinRoomCodePayload(String enteredCode) implements CustomPacketPayload {

    public static final Type<JoinRoomCodePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Onekingdown.MODID, "join_room_code"));

    public static final StreamCodec<FriendlyByteBuf, JoinRoomCodePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> buf.writeUtf(p.enteredCode),
                    buf -> new JoinRoomCodePayload(buf.readUtf())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(JoinRoomCodePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                KingdomManager.joinRoomWithCode(player, payload.enteredCode());
            }
        });
    }
}