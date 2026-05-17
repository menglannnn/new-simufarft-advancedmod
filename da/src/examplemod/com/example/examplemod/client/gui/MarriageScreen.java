package com.example.examplemod.client.gui;

import com.example.examplemod.love.Love;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;
import java.util.UUID;

@SuppressWarnings("null")
public class MarriageScreen extends Screen {
    private static final ResourceLocation BACKGROUND =
        ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/demo_background.png");
    
    private final String npcName;
    private final Love.Relationship relationship;
    private final Runnable onMarriageComplete;
    
    private Button yesButton;
    private Button noButton;
    
    public MarriageScreen(Player player, UUID npcId, String npcName, Runnable onMarriageComplete) {
        super(Component.literal("§c§l求婚"));
        this.npcName = npcName;
        this.relationship = Love.getRelationship(player, npcId, npcName);
        this.onMarriageComplete = onMarriageComplete;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // 确认按钮
        this.yesButton = Button.builder(Component.literal("§a§l我愿意！"), button -> {
            relationship.setMarried(true);
            if (onMarriageComplete != null) {
                onMarriageComplete.run();
            }
            this.onClose();
        })
        .pos(centerX - 90, centerY + 20)
        .size(80, 25)
        .build();
        
        // 取消按钮
        this.noButton = Button.builder(Component.literal("§c再等等"), button -> {
            this.onClose();
        })
        .pos(centerX + 10, centerY + 20)
        .size(80, 25)
        .build();
        
        this.addRenderableWidget(this.yesButton);
        this.addRenderableWidget(this.noButton);
    }
    
    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // 绘制背景
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(BACKGROUND, centerX - 130, centerY - 70, 0, 0, 260, 140);
        
        // 绘制标题
        guiGraphics.drawCenteredString(this.font, this.title, centerX, centerY - 55, 0xFFFFFF);
        
        // 绘制求婚台词
        guiGraphics.drawCenteredString(this.font, Component.literal("§e" + npcName + "§f，"), centerX, centerY - 30, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.literal("我们的好感度已经达到了§c100§f！"), centerX, centerY - 15, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.literal("你愿意嫁给我/娶我吗？"), centerX, centerY, 0xFFFFFF);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
