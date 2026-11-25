package grouponekingdown.onekingdown;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = Onekingdown.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue MAX_PLAYERS_PER_ROOM = BUILDER
            .comment("Quantidade maxima de jogadores simultaneos por sala")
            .defineInRange("maxPlayersPerRoom", 5, 1, 50);

    private static final ModConfigSpec.IntValue KINGDOM_SIDE_SIZE = BUILDER
            .comment("Lado do terreno quadrado de cada reino em blocos (64x64 comporta castelo + quartel + mina + plantacao)")
            .defineInRange("kingdomSideSize", 64, 48, 160);

    private static final ModConfigSpec.IntValue ROOM_SPACING = BUILDER
            .comment("Distancia em blocos entre salas diferentes para manter isolamento")
            .defineInRange("roomSpacing", 10000, 1000, 100000);

    private static final ModConfigSpec.IntValue KINGDOM_SPACING = BUILDER
            .comment("Raio de distribuicao dos reinos dentro da mesma sala")
            .defineInRange("kingdomSpacing", 160, 80, 1000);

    private static final ModConfigSpec.IntValue XP_PER_LEVEL = BUILDER
            .comment("XP necessario para subir cada nivel do reino")
            .defineInRange("xpPerLevel", 100, 10, 100000);

    private static final ModConfigSpec.IntValue PRODUCTION_INTERVAL_SECONDS = BUILDER
            .comment("Intervalo em segundos para mineiros e fazendeiros gerarem recursos nos baus")
            .defineInRange("productionIntervalSeconds", 30, 5, 600);

    private static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", false);

    private static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("Introduction message")
            .define("magicNumberIntroduction", "The magic number is... ");

    private static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static int maxPlayersPerRoom = 5;
    public static int kingdomSideSize = 64;
    public static int roomSpacing = 10000;
    public static int kingdomSpacing = 160;
    public static int xpPerLevel = 100;
    public static int productionIntervalSeconds = 30;

    public static boolean logDirtBlock = false;
    public static int magicNumber = 42;
    public static String magicNumberIntroduction = "The magic number is... ";
    public static Set<Item> items = Set.of();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(Identifier.parse(itemName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        maxPlayersPerRoom = MAX_PLAYERS_PER_ROOM.get();
        kingdomSideSize = KINGDOM_SIDE_SIZE.get();
        roomSpacing = ROOM_SPACING.get();
        kingdomSpacing = KINGDOM_SPACING.get();
        xpPerLevel = XP_PER_LEVEL.get();
        productionIntervalSeconds = PRODUCTION_INTERVAL_SECONDS.get();

        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();
        items = ITEM_STRINGS.get().stream()
                .map(itemName -> BuiltInRegistries.ITEM.getValue(Identifier.parse(itemName)))
                .collect(Collectors.toSet());
    }
}