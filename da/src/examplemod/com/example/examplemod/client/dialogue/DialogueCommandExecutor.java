package com.example.examplemod.client.dialogue;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * 对话命令执行器
 * 仅处理脚本里的命令片段，统一走单人世界控制台权限执行。
 */@SuppressWarnings("null")
public final class DialogueCommandExecutor {
    private static final Logger LOGGER = LogUtils.getLogger();

    private DialogueCommandExecutor() {
    }

    public static void executeIfPresent(DialogueScript.ResponseEntry responseEntry) {
        if (responseEntry == null || !responseEntry.hasCommand()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.hasSingleplayerServer()) {
            if (minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.literal("§c当前不是单人世界，无法执行对话命令。"));
            }
            return;
        }

        MinecraftServer server = minecraft.getSingleplayerServer();
        LocalPlayer player = minecraft.player;
        if (server == null || player == null) {
            return;
        }

        ServerPlayer serverPlayer = server.getPlayerList().getPlayer(player.getUUID());
        if (serverPlayer == null) {
            return;
        }

        String resolvedCommand = replacePlayerPlaceholder(responseEntry.command(), player.getGameProfile().getName());
        CommandSourceStack sourceStack = server.createCommandSourceStack()
            .withSuppressedOutput()
            .withPermission(4)
            .withEntity(serverPlayer)
            .withLevel(serverPlayer.serverLevel())
            .withPosition(new Vec3(serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ()));

        try {
            server.getCommands().performPrefixedCommand(sourceStack, resolvedCommand);
        } catch (Exception e) {
            LOGGER.error("执行对话命令失败: {}", resolvedCommand, e);
            player.sendSystemMessage(Component.literal("§c执行对话命令失败: " + resolvedCommand));
        }
    }

    private static String replacePlayerPlaceholder(String command, String playerName) {
        return command.replaceAll("\\bplayer\\b", playerName);
    }
}
