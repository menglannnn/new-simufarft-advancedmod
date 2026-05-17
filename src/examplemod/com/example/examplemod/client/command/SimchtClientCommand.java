package com.example.examplemod.client.command;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.client.dialogue.DialogueScriptLoader;
import com.example.examplemod.client.gui.NPCInteractScreen;
import com.example.examplemod.integration.SimulatedNightmaresIntegration;
import com.mojang.brigadier.Command;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
@SuppressWarnings("null")
/**
 * 客户端聊天指令：手动重载对话脚本，便于开发时快速验证文本改动。
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SimchtClientCommand {
    private SimchtClientCommand() {
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("simcht")
                .then(Commands.literal("r")
                    .executes(context -> reloadDialogue(context.getSource())))
                .then(Commands.literal("allr")
                    .executes(context -> reloadAll(context.getSource())))
                .then(Commands.literal("uuid")
                    .executes(context -> listNearbyNpcUuid(context.getSource())))
                .executes(context -> showHelp(context.getSource()))
        );
    }

    private static int showHelp(CommandSourceStack source) {
        source.sendSuccess(
            () -> literal(
                "本mod为新:模拟大都市附属,由menglannnn重构发行,如果你是付费拿到的mod那么恭喜你被骗了\n"
                    + "------------------------------------\n"
                    + "simcht r -重载对话框\n"
                    + "simcht allr -重载整个本mod\n"
                    + "simcht uuid -查找玩家附近3格内的NPC UUID"
            ),
            false
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadDialogue(CommandSourceStack source) {
        DialogueScriptLoader.ReloadResult result = DialogueScriptLoader.reloadForCommand();
        if (!result.success()) {
            source.sendFailure(literal("对话脚本重载失败，请检查 quanbu.txt 写法。"));
            return 0;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof NPCInteractScreen npcInteractScreen) {
            npcInteractScreen.reloadDialogueScript();
        }

        String scopeSummary = summarizeScope(result.scope());
        source.sendSuccess(
            () -> literal("对话脚本已重载: " + result.source() + "，共 " + result.optionCount() + " 个选项，作用范围: " + scopeSummary),
            false
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadAll(CommandSourceStack source) {
        Minecraft minecraft = Minecraft.getInstance();
        source.sendSuccess(() -> literal("开始重载本mod资源，请稍候..."), false);

        minecraft.reloadResourcePacks().whenComplete((unused, throwable) -> {
            minecraft.execute(() -> {
                if (throwable != null) {
                    source.sendFailure(literal("本mod资源重载失败: " + throwable.getMessage()));
                    return;
                }

                DialogueScriptLoader.ReloadResult result = DialogueScriptLoader.reloadForCommand();
                if (minecraft.screen instanceof NPCInteractScreen npcInteractScreen) {
                    npcInteractScreen.reloadDialogueScript();
                }

                if (!result.success()) {
                    source.sendFailure(literal("资源已重载，但对话脚本重新解析失败，请检查 quanbu.txt。"));
                    return;
                }

                source.sendSuccess(
                    () -> literal("本mod资源已重载完成，对话脚本来源: " + result.source() + "，共 " + result.optionCount() + " 个选项。"),
                    false
                );
            });
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int listNearbyNpcUuid(CommandSourceStack source) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            source.sendFailure(literal("当前未进入世界，无法查找附近NPC。"));
            return 0;
        }

        AABB searchBox = player.getBoundingBox().inflate(3.0D);
        List<Entity> nearbyNpcList = minecraft.level.getEntities(player, searchBox, SimchtClientCommand::isTargetNpc).stream()
            .sorted(Comparator.comparingDouble(player::distanceToSqr))
            .toList();

        if (nearbyNpcList.isEmpty()) {
            source.sendFailure(literal("玩家附近3格内未找到可识别的NPC。"));
            return 0;
        }

        source.sendSuccess(() -> literal("附近3格内NPC UUID如下:"), false);
        for (Entity entity : nearbyNpcList) {
            String npcName = SimulatedNightmaresIntegration.getNPCName(entity);
            String npcUuid = entity.getUUID().toString();
            source.sendSuccess(() -> literal("[" + npcName + "]+" + npcUuid), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static String summarizeScope(com.example.examplemod.client.dialogue.DialogueScript.Scope scope) {
        if (scope.allNpc()) {
            return "全部NPC";
        }
        if (!scope.npcUuid().isBlank()) {
            return "UUID=" + scope.npcUuid();
        }
        if (!scope.npcName().isBlank()) {
            return "名字=" + scope.npcName();
        }
        if ("all".equalsIgnoreCase(scope.npcJob())) {
            return "全部职业";
        }
        if (scope.npcJob().isBlank()) {
            return "无职业NPC";
        }
        return "职业=" + scope.npcJob();
    }

    private static Component literal(String text) {
        return Objects.requireNonNull(Component.literal(text));
    }

    private static boolean isTargetNpc(Entity entity) {
        if (SimulatedNightmaresIntegration.isSimulatedNightmaresNPC(entity)) {
            return true;
        }

        String entityName = entity.getName().getString();
        return entityName.contains("居民") || entityName.contains("NPC") || entityName.contains("市民");
    }
}
