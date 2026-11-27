package grouponekingdown.onekingdown.network;

import grouponekingdown.onekingdown.Onekingdown;
import grouponekingdown.onekingdown.client.gui.RoomSetupScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenRoomSetupPayload(
        String roomCode,
        boolean roomAlreadyConfigured,
        int maxPlayers
) implements CustomPacketPayload {

    public static final Type<OpenRoomSetupPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Onekingdown.MODID, "open_room_setup"));

    public static final StreamCodec<FriendlyByteBuf, OpenRoomSetupPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeUtf(p.roomCode);
                        buf.writeBoolean(p.roomAlreadyConfigured);
                        buf.writeInt(p.maxPlayers);
                    },
                    buf -> new OpenRoomSetupPayload(
                            buf.readUtf(),
                            buf.readBoolean(),
                            buf.readInt()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnClient(OpenRoomSetupPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft.getInstance().setScreenAndShow(
                    new RoomSetupScreen(payload.roomCode(), payload.roomAlreadyConfigured(), payload.maxPlayers())
            );
        });
    }
}