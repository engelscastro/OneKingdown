package grouponekingdown.onekingdown.network;

import grouponekingdown.onekingdown.Onekingdown;
import grouponekingdown.onekingdown.kingdom.KingdomNpcManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HireUnitPayload(
        String recruiterRole,
        String unitType,
        int level
) implements CustomPacketPayload {

    public static final Type<HireUnitPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Onekingdown.MODID, "hire_unit")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, HireUnitPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, HireUnitPayload::recruiterRole,
                    ByteBufCodecs.STRING_UTF8, HireUnitPayload::unitType,
                    ByteBufCodecs.VAR_INT, HireUnitPayload::level,
                    HireUnitPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(HireUnitPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                var overworld = player.level().getServer().overworld();
                KingdomNpcManager.hireUnitWithLevel(
                        overworld, player, payload.unitType(), payload.level()
                );
            }
        });
    }
}