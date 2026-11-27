package grouponekingdown.onekingdown.invasion;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public final class InvasionCommand {
    private InvasionCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("invasao")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("start")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    InvasionRegistry.all().forEach(def -> builder.suggest(def.id()));
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "id");
                                    ServerLevel level = ctx.getSource().getLevel();
                                    BlockPos pos = BlockPos.containing(ctx.getSource().getPosition());

                                    return InvasionRegistry.get(id).map(def -> {
                                        boolean started = InvasionManager.startInvasion(level, pos, def);
                                        if (started) {
                                            ctx.getSource().sendSuccess(() -> Component.literal("Invasão iniciada: " + def.displayName()), true);
                                            return 1;
                                        }
                                        ctx.getSource().sendFailure(Component.literal("Não foi possível iniciar (já há uma ativa ou o .nbt não foi encontrado)."));
                                        return 0;
                                    }).orElseGet(() -> {
                                        ctx.getSource().sendFailure(Component.literal("Invasão desconhecida: " + id));
                                        return 0;
                                    });
                                })))
                .then(Commands.literal("stop")
                        .executes(ctx -> {
                            InvasionManager.stopInvasion(ctx.getSource().getLevel());
                            return 1;
                        }))
                .then(Commands.literal("status")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal(InvasionManager.getStatusText()), false);
                            return 1;
                        }))
        );
    }
}