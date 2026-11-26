package grouponekingdown.onekingdown.network;

import grouponekingdown.onekingdown.Onekingdown;
import grouponekingdown.onekingdown.client.gui.RecruiterScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenRecruiterPayload(
        String recruiterRole, // "GUARD", "MINE", "FARM"
        int ores,
        int wood,
        int food
) implements CustomPacketPayload {

    public static final Type<OpenRecruiterPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Onekingdown.MODID, "open_recruiter")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenRecruiterPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, OpenRecruiterPayload::recruiterRole,
                    ByteBufCodecs.VAR_INT, OpenRecruiterPayload::ores,
                    ByteBufCodecs.VAR_INT, OpenRecruiterPayload::wood,
                    ByteBufCodecs.VAR_INT, OpenRecruiterPayload::food,
                    OpenRecruiterPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnClient(OpenRecruiterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft.getInstance().setScreenAndShow(new RecruiterScreen(
                    payload.recruiterRole(),
                    payload.ores(),
                    payload.wood(),
                    payload.food()
            ));
        });
    }
}