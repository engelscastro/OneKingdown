package grouponekingdown.onekingdown.network;

import grouponekingdown.onekingdown.Onekingdown;
import grouponekingdown.onekingdown.client.gui.KingdomMinimapOverlay;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenMapPayload(
        int castleX, int castleZ,
        int barracksX, int barracksZ,
        int mineX, int mineZ,
        int farmX, int farmZ,
        boolean hasInvasion,
        String invasionName,
        int citadelX, int citadelZ
) implements CustomPacketPayload {

    public static final Type<OpenMapPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Onekingdown.MODID, "open_map")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMapPayload> STREAM_CODEC =
            CustomPacketPayload.codec(OpenMapPayload::write, OpenMapPayload::read);

    private static void write(OpenMapPayload p, RegistryFriendlyByteBuf buf) {
        buf.writeInt(p.castleX);
        buf.writeInt(p.castleZ);
        buf.writeInt(p.barracksX);
        buf.writeInt(p.barracksZ);
        buf.writeInt(p.mineX);
        buf.writeInt(p.mineZ);
        buf.writeInt(p.farmX);
        buf.writeInt(p.farmZ);
        buf.writeBoolean(p.hasInvasion);
        buf.writeUtf(p.invasionName);
        buf.writeInt(p.citadelX);
        buf.writeInt(p.citadelZ);
    }

    private static OpenMapPayload read(RegistryFriendlyByteBuf buf) {
        return new OpenMapPayload(
                buf.readInt(), buf.readInt(),
                buf.readInt(), buf.readInt(),
                buf.readInt(), buf.readInt(),
                buf.readInt(), buf.readInt(),
                buf.readBoolean(),
                buf.readUtf(),
                buf.readInt(), buf.readInt()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnClient(OpenMapPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> KingdomMinimapOverlay.updateData(payload));
    }
}