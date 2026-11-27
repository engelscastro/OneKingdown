package grouponekingdown.onekingdown.kingdom;

import grouponekingdown.onekingdown.Onekingdown;
import grouponekingdown.onekingdown.network.HireUnitPayload;
import grouponekingdown.onekingdown.network.OpenMapPayload;
import grouponekingdown.onekingdown.network.OpenRecruiterPayload;
import grouponekingdown.onekingdown.network.RequestMapPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record KingdomHudPayload(
        int ores,
        int wood,
        int food,
        int guards,
        int miners,
        int farmers,
        int animals,
        int monsters,
        int level,
        int tier
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<KingdomHudPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Onekingdown.MODID, "kingdom_hud"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KingdomHudPayload> STREAM_CODEC =
            CustomPacketPayload.codec(KingdomHudPayload::write, KingdomHudPayload::read);

    public static volatile KingdomHudPayload clientState = null;

    private static void write(KingdomHudPayload payload, RegistryFriendlyByteBuf buf) {
        buf.writeInt(payload.ores);
        buf.writeInt(payload.wood);
        buf.writeInt(payload.food);
        buf.writeInt(payload.guards);
        buf.writeInt(payload.miners);
        buf.writeInt(payload.farmers);
        buf.writeInt(payload.animals);
        buf.writeInt(payload.monsters);
        buf.writeInt(payload.level);
        buf.writeInt(payload.tier);
    }

    private static KingdomHudPayload read(RegistryFriendlyByteBuf buf) {
        return new KingdomHudPayload(
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(TYPE, STREAM_CODEC, (payload, context) -> clientState = payload);
        registrar.playToClient(
                OpenRecruiterPayload.TYPE,
                OpenRecruiterPayload.STREAM_CODEC,
                OpenRecruiterPayload::handleOnClient
        );
        registrar.playToServer(
                HireUnitPayload.TYPE,
                HireUnitPayload.STREAM_CODEC,
                HireUnitPayload::handleOnServer
        );
        registrar.playToServer(
                RequestMapPayload.TYPE,
                RequestMapPayload.STREAM_CODEC,
                RequestMapPayload::handleOnServer
        );
        registrar.playToClient(
                OpenMapPayload.TYPE,
                OpenMapPayload.STREAM_CODEC,
                OpenMapPayload::handleOnClient
        );
    }
}