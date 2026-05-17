package com.example.examplemod.client.input;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.love.NPCLoveHandler;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("null")
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModKeyBindings {
    private static final String KEY_CATEGORY = "key.categories.advancedmod";
    private static final KeyMapping OPEN_TARGET_NPC_DIALOGUE_UI_KEY = new KeyMapping(
        "key.advancedmod.open_target_npc_dialogue_ui",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_V,
        KEY_CATEGORY
    );

    private ModKeyBindings() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_TARGET_NPC_DIALOGUE_UI_KEY);
    }

    @Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeClientEvents {
        private ForgeClientEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            while (OPEN_TARGET_NPC_DIALOGUE_UI_KEY.consumeClick()) {
                handleOpenTargetNpcDialogueUi(minecraft);
            }
        }

        private static void handleOpenTargetNpcDialogueUi(Minecraft minecraft) {
            if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
                return;
            }

            Entity targetNpc = resolveCrosshairNpc(minecraft);
            if (targetNpc == null) {
                return;
            }

            NPCLoveHandler.openInteractScreenForClient(minecraft.player, targetNpc);
        }

        private static Entity resolveCrosshairNpc(Minecraft minecraft) {
            if (!(minecraft.hitResult instanceof EntityHitResult entityHitResult)) {
                return null;
            }

            Entity target = entityHitResult.getEntity();
            return NPCLoveHandler.isSupportedNpc(target) ? target : null;
        }
    }
}
