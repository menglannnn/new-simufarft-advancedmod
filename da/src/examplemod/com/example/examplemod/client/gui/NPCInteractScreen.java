package com.example.examplemod.client.gui;

import com.example.examplemod.client.dialogue.DialogueCommandExecutor;
import com.example.examplemod.client.dialogue.DialogueScript;
import com.example.examplemod.client.dialogue.DialogueScriptLoader;
import com.example.examplemod.integration.SimulatedNightmaresIntegration;
import com.example.examplemod.love.Love;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@SuppressWarnings("null")
public class NPCInteractScreen extends Screen {
    private static final int PANEL_MARGIN = 24;
    private static final int PANEL_HEIGHT = 182;
    private static final int PORTRAIT_WIDTH = 130;
    private static final int BUTTON_HEIGHT = 24;
    private static final int BUTTON_SPACING = 8;
    private static final int PANEL_BG = 0xB0000000;
    private static final int CARD_BG = 0x80000000;
    private static final int CARD_BG_HOVERED = 0xA0000000;
    private static final int BORDER_COLOR = 0x66F5D76E;
    private static final int TITLE_COLOR = 0xFFF5D76E;
    private static final int TEXT_COLOR = 0xFFF7E8A3;
    private static final int SUBTLE_TEXT_COLOR = 0xFFCBCBCB;
    private static final int HEART_COLOR = 0xFFD94141;
    private static final int HEART_DARK_COLOR = 0xFF8E1D1D;
    private static final float TEXT_REVEAL_SPEED = 1.15F;
    private static final int OPTIONS_PER_PAGE = 3;
    private static final String MENU_PROMPT = "请选择交互方式。";

    private final UUID npcId;
    private final Entity npcEntity;
    private final String npcName;
    private final Love.Relationship relationship;
    private DialogueScript dialogueScript;

    private DialogueButton dialogueButton;
    private DialogueButton identityButton;
    private DialogueButton closeButton;
    private DialogueButton backButton;
    private final List<DialogueButton> choiceButtons = new ArrayList<>();

    private int dialogueStage = 0;
    private int dialogueOptionPage = 0;
    private List<DialogueScript.ResponseEntry> activeResponses = List.of();
    private int activeResponseIndex = 0;
    private String fullNpcMessage = "";
    private int visibleCharacterCount = 0;
    private float revealProgress = 0.0F;

    public NPCInteractScreen(Player player, UUID npcId, String npcName) {
        this(player, npcId, npcName, null);
    }

    public NPCInteractScreen(Player player, UUID npcId, String npcName, Entity npcEntity) {
        super(Component.literal("与 " + npcName + " 互动"));
        this.npcId = npcId;
        this.npcEntity = npcEntity;
        this.npcName = npcName;
        this.relationship = Love.getRelationship(player, npcId, npcName);
        this.dialogueScript = DialogueScriptLoader.loadMainDialogue(npcEntity, npcName, npcId.toString());
        this.startMessageReveal(MENU_PROMPT);
    }

    public void reloadDialogueScript() {
        this.dialogueScript = DialogueScriptLoader.loadMainDialogue(this.npcEntity, this.npcName, this.npcId.toString());
        this.dialogueStage = 0;
        this.dialogueOptionPage = 0;
        this.activeResponses = List.of();
        this.activeResponseIndex = 0;
        this.startMessageReveal(MENU_PROMPT);
        this.rebuildWidgets();
    }

    @Override
    protected void init() {
        super.init();
        initButtons();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.visibleCharacterCount < this.fullNpcMessage.length()) {
            this.revealProgress += TEXT_REVEAL_SPEED;
            this.visibleCharacterCount = Math.min(this.fullNpcMessage.length(), Mth.floor(this.revealProgress));
        }
    }

    private void initButtons() {
        clearWidgets();
        choiceButtons.clear();

        int panelX = PANEL_MARGIN;
        int panelY = this.height - PANEL_HEIGHT - PANEL_MARGIN;
        int panelWidth = this.width - PANEL_MARGIN * 2;
        int buttonWidth = Math.min(232, panelWidth - PORTRAIT_WIDTH - 60);
        int buttonX = panelX + panelWidth - buttonWidth - 20;
        int baseY = panelY + 88;

        if (dialogueStage == 1) {
            initDialogueOptions(buttonX, baseY, buttonWidth);
        } else if (dialogueStage == 2) {
            initResultButtons(buttonX, baseY + 16, buttonWidth);
        } else {
            initMainButtons(buttonX, baseY, buttonWidth);
        }

        if (this.dialogueStage == 0 && this.dialogueButton != null) {
            this.setInitialFocus(this.dialogueButton);
        } else if (!this.choiceButtons.isEmpty()) {
            this.setInitialFocus(this.choiceButtons.get(0));
        } else if (this.backButton != null) {
            this.setInitialFocus(this.backButton);
        }
    }

    private void initMainButtons(int buttonX, int baseY, int buttonWidth) {
        this.dialogueButton = createActionButton("对话", buttonX, baseY, buttonWidth, button -> {
            this.dialogueStage = 1;
            this.dialogueOptionPage = 0;
            this.startMessageReveal(this.dialogueScript.prompt());
            this.rebuildWidgets();
        });
        this.dialogueButton.active = hasUnlockedOptions();

        this.identityButton = createActionButton("查看原版身份卡", buttonX, baseY + BUTTON_HEIGHT + BUTTON_SPACING, buttonWidth, button -> {
            openIdentityScreen();
        });

        this.closeButton = createActionButton("关闭", buttonX, baseY + (BUTTON_HEIGHT + BUTTON_SPACING) * 2, buttonWidth, button -> {
            this.onClose();
        });

        this.addRenderableWidget(this.dialogueButton);
        this.addRenderableWidget(this.identityButton);
        this.addRenderableWidget(this.closeButton);
    }

    private void initDialogueOptions(int buttonX, int baseY, int buttonWidth) {
        List<DialogueScript.Option> currentOptions = getCurrentPageOptions();
        if (currentOptions.isEmpty()) {
            this.backButton = createActionButton("返回", buttonX, baseY, buttonWidth, button -> {
                this.dialogueStage = 0;
                this.startMessageReveal(MENU_PROMPT);
                this.rebuildWidgets();
            });
            this.addRenderableWidget(this.backButton);
            this.startMessageReveal("当前对话脚本没有可用选项。");
            return;
        }

        for (int i = 0; i < currentOptions.size(); i++) {
            DialogueScript.Option option = currentOptions.get(i);
            DialogueButton choiceButton = createActionButton(option.displayText(), buttonX, baseY + i * (BUTTON_HEIGHT + BUTTON_SPACING), buttonWidth, button -> {
                openResponseSequence(option);
            });
            this.choiceButtons.add(choiceButton);
            this.addRenderableWidget(choiceButton);
        }
    }

    private void initResultButtons(int buttonX, int baseY, int buttonWidth) {
        boolean hasNextResponse = this.activeResponseIndex < this.activeResponses.size() - 1;
        this.backButton = createActionButton(hasNextResponse ? "下一句" : "返回选项", buttonX, baseY, buttonWidth, button -> {
            if (hasNextResponse) {
                this.activeResponseIndex++;
                showActiveResponse();
                this.rebuildWidgets();
                return;
            }

            this.dialogueStage = 1;
            this.activeResponses = List.of();
            this.activeResponseIndex = 0;
            this.startMessageReveal(this.dialogueScript.prompt());
            this.rebuildWidgets();
        });

        this.closeButton = createActionButton("关闭", buttonX, baseY + BUTTON_HEIGHT + BUTTON_SPACING, buttonWidth, button -> {
            this.onClose();
        });

        this.addRenderableWidget(this.backButton);
        this.addRenderableWidget(this.closeButton);
    }

    private List<DialogueScript.Option> getCurrentPageOptions() {
        if (this.dialogueScript.hasOptionPools()) {
            return this.dialogueScript.randomizeOptions();
        }

        List<DialogueScript.Option> unlockedOptions = getUnlockedOptions();
        int pageCount = getOptionPageCount();
        this.dialogueOptionPage = Mth.clamp(this.dialogueOptionPage, 0, Math.max(0, pageCount - 1));

        int startIndex = this.dialogueOptionPage * OPTIONS_PER_PAGE;
        int endIndex = Math.min(startIndex + OPTIONS_PER_PAGE, unlockedOptions.size());
        if (startIndex >= endIndex) {
            return List.of();
        }
        return unlockedOptions.subList(startIndex, endIndex);
    }

    private void openResponseSequence(DialogueScript.Option option) {
        int affectionBeforeChange = getCurrentAffection();
        applyAffectionChange(option);
        this.dialogueStage = 2;

        List<DialogueScript.ResponseEntry> resolvedResponses = option.responsesForCurrentAffection(affectionBeforeChange);
        this.activeResponses = resolvedResponses.isEmpty()
            ? List.of(option.firstResponseOrFallback(affectionBeforeChange))
            : resolvedResponses;

        this.activeResponseIndex = 0;
        showActiveResponse();
        this.rebuildWidgets();
    }

    private void showActiveResponse() {
        if (this.activeResponses.isEmpty()) {
            this.startMessageReveal("这个选项暂时还没有配置回复。");
            return;
        }

        DialogueScript.ResponseEntry responseEntry = this.activeResponses.get(Mth.clamp(this.activeResponseIndex, 0, this.activeResponses.size() - 1));
        this.startMessageReveal(responseEntry.text().isBlank() ? "......" : responseEntry.text());
        DialogueCommandExecutor.executeIfPresent(responseEntry);
    }

    private int getOptionPageCount() {
        if (this.dialogueScript.hasOptionPools()) {
            return 1;
        }
        int unlockedOptionCount = getUnlockedOptions().size();
        if (unlockedOptionCount <= 0) {
            return 1;
        }
        return (unlockedOptionCount + OPTIONS_PER_PAGE - 1) / OPTIONS_PER_PAGE;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.dialogueStage == 1 && getOptionPageCount() > 1) {
            int oldPage = this.dialogueOptionPage;
            if (delta < 0) {
                this.dialogueOptionPage = Math.min(getOptionPageCount() - 1, this.dialogueOptionPage + 1);
            } else if (delta > 0) {
                this.dialogueOptionPage = Math.max(0, this.dialogueOptionPage - 1);
            }

            if (oldPage != this.dialogueOptionPage) {
                this.rebuildWidgets();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void openIdentityScreen() {
        this.onClose();

        if (npcEntity != null) {
            SimulatedNightmaresIntegration.openIdentityCardScreen(npcEntity);
        } else {
            SimulatedNightmaresIntegration.openIdentityScreen(npcId);
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.fill(0, 0, this.width, this.height, 0x55000000);

        int panelX = PANEL_MARGIN;
        int panelY = this.height - PANEL_HEIGHT - PANEL_MARGIN;
        int panelWidth = this.width - PANEL_MARGIN * 2;
        int panelBottom = panelY + PANEL_HEIGHT;
        int portraitX = panelX + 16;
        int portraitY = panelY + 16;
        int portraitHeight = PANEL_HEIGHT - 32;
        int textBoxX = portraitX + PORTRAIT_WIDTH + 18;
        int textBoxY = panelY + 18;
        int textBoxWidth = panelWidth - PORTRAIT_WIDTH - 54;
        int textBoxHeight = 70;

        renderDialoguePanel(guiGraphics, panelX, panelY, panelWidth, PANEL_HEIGHT);
        renderPortraitPanel(guiGraphics, portraitX, portraitY, PORTRAIT_WIDTH, portraitHeight, mouseX, mouseY);
        renderTextPanel(guiGraphics, textBoxX, textBoxY, textBoxWidth, textBoxHeight);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (dialogueStage == 1 && getOptionPageCount() > 1) {
            guiGraphics.drawString(
                this.font,
                Component.literal((this.dialogueOptionPage + 1) + "/" + getOptionPageCount()),
                panelX + panelWidth - 28,
                panelBottom - 14,
                0x66FFFFFF,
                false
            );
        }
    }

    private DialogueButton createActionButton(String text, int x, int y, int width, Button.OnPress onPress) {
        return new DialogueButton(x, y, width, BUTTON_HEIGHT, Component.literal(text), onPress);
    }

    private void startMessageReveal(String newMessage) {
        this.fullNpcMessage = newMessage;
        this.visibleCharacterCount = 0;
        this.revealProgress = 0.0F;
    }

    private String getVisibleNpcMessage() {
        int endIndex = Math.min(this.visibleCharacterCount, this.fullNpcMessage.length());
        return this.fullNpcMessage.substring(0, endIndex);
    }

    private void renderDialoguePanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, PANEL_BG);
        guiGraphics.fill(x, y, x + width, y + 2, BORDER_COLOR);
        guiGraphics.fill(x, y, x + 1, y + height, BORDER_COLOR);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, BORDER_COLOR);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0x44FFFFFF);
    }

    private void renderPortraitPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        guiGraphics.fill(x, y, x + width, y + height, CARD_BG);
        guiGraphics.fill(x, y, x + width, y + 1, BORDER_COLOR);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, BORDER_COLOR);

        if (npcEntity instanceof LivingEntity livingEntity) {
            try {
                guiGraphics.enableScissor(x + 4, y + 4, x + width - 4, y + height - 4);
                renderPortraitEntity(guiGraphics, livingEntity, x, y, width, height, mouseX, mouseY);
                guiGraphics.disableScissor();
                return;
            } catch (Exception ignored) {
                guiGraphics.disableScissor();
            }
        }

        guiGraphics.drawCenteredString(this.font, Component.literal("NPC"), x + width / 2, y + 28, TITLE_COLOR);
        guiGraphics.drawCenteredString(this.font, Component.literal(this.npcName), x + width / 2, y + 54, TEXT_COLOR);
        guiGraphics.drawCenteredString(this.font, Component.literal("立绘加载中"), x + width / 2, y + height - 28, SUBTLE_TEXT_COLOR);
    }

    private void renderPortraitEntity(GuiGraphics guiGraphics, LivingEntity livingEntity, int x, int y, int width, int height, int mouseX, int mouseY) {
        float headYaw = Mth.clamp((x + width / 2.0F - mouseX) * 0.35F, -22.0F, 22.0F);
        float headPitch = Mth.clamp((y + height / 2.0F - mouseY) * 0.20F, -12.0F, 12.0F);
        float baseFacingYaw = 180.0F;

        float oldYRot = livingEntity.getYRot();
        float oldXRot = livingEntity.getXRot();
        float oldYBodyRot = livingEntity.yBodyRot;
        float oldYBodyRotO = livingEntity.yBodyRotO;
        float oldYHeadRot = livingEntity.getYHeadRot();
        float oldYHeadRotO = livingEntity.yHeadRotO;

        livingEntity.setYRot(baseFacingYaw);
        livingEntity.setXRot(headPitch * 0.35F);
        livingEntity.setYBodyRot(baseFacingYaw);
        livingEntity.yBodyRotO = baseFacingYaw;
        livingEntity.setYHeadRot(baseFacingYaw + headYaw);
        livingEntity.yHeadRotO = baseFacingYaw + headYaw;

        Quaternionf bodyRotation = Axis.ZP.rotationDegrees(180.0F);
        Quaternionf cameraRotation = Axis.XP.rotationDegrees(headPitch * 0.35F);

        InventoryScreen.renderEntityInInventory(
            guiGraphics,
            x + width / 2,
            y + height + 72,
            92,
            bodyRotation,
            cameraRotation,
            livingEntity
        );

        livingEntity.setYRot(oldYRot);
        livingEntity.setXRot(oldXRot);
        livingEntity.setYBodyRot(oldYBodyRot);
        livingEntity.yBodyRotO = oldYBodyRotO;
        livingEntity.setYHeadRot(oldYHeadRot);
        livingEntity.yHeadRotO = oldYHeadRotO;
    }

    private void renderTextPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, CARD_BG);
        guiGraphics.fill(x, y, x + width, y + 1, BORDER_COLOR);
        guiGraphics.drawString(this.font, Component.literal(this.npcName), x + 10, y + 8, TITLE_COLOR, false);
        renderAffectionHeart(guiGraphics, x + width - 66, y + 6, getCurrentAffection());

        String visibleText = getVisibleNpcMessage();
        List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(Component.literal(visibleText), width - 20);
        int lineY = y + 28;
        int maxLines = Math.max(1, (height - 34) / 10);
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            guiGraphics.drawString(this.font, lines.get(i), x + 10, lineY + i * 10, TEXT_COLOR, false);
        }
    }

    private List<DialogueScript.Option> getUnlockedOptions() {
        int currentAffection = getCurrentAffection();
        return this.dialogueScript.options().stream()
            .filter(option -> option.isUnlocked(currentAffection))
            .toList();
    }

    private boolean hasUnlockedOptions() {
        if (this.dialogueScript.hasOptionPools()) {
            return true;
        }
        return !getUnlockedOptions().isEmpty();
    }

    private int getCurrentAffection() {
        if (this.relationship == null) {
            return 0;
        }
        return Mth.clamp(this.relationship.getAffection(), 0, Love.MAX_LOVE);
    }

    private void applyAffectionChange(DialogueScript.Option option) {
        if (this.relationship == null || option.affectionChange() == 0) {
            return;
        }
        this.relationship.addAffection(option.affectionChange());
    }

    private void renderAffectionHeart(GuiGraphics guiGraphics, int x, int y, int affection) {
        guiGraphics.fill(x + 10, y + 2, x + 24, y + 16, HEART_COLOR);
        guiGraphics.fill(x + 24, y + 2, x + 38, y + 16, HEART_COLOR);
        guiGraphics.fill(x + 6, y + 10, x + 42, y + 24, HEART_COLOR);
        guiGraphics.fill(x + 10, y + 24, x + 38, y + 34, HEART_COLOR);
        guiGraphics.fill(x + 14, y + 34, x + 34, y + 42, HEART_COLOR);
        guiGraphics.fill(x + 18, y + 42, x + 30, y + 48, HEART_COLOR);

        guiGraphics.fill(x + 10, y + 2, x + 24, y + 3, HEART_DARK_COLOR);
        guiGraphics.fill(x + 24, y + 2, x + 38, y + 3, HEART_DARK_COLOR);
        guiGraphics.fill(x + 6, y + 10, x + 7, y + 24, HEART_DARK_COLOR);
        guiGraphics.fill(x + 41, y + 10, x + 42, y + 24, HEART_DARK_COLOR);

        int centerX = x + 24;
        guiGraphics.drawCenteredString(this.font, Component.literal("好感"), centerX, y + 14, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.literal(String.valueOf(affection)), centerX, y + 26, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private static final class DialogueButton extends Button {
        private DialogueButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int backgroundColor = this.isHoveredOrFocused() ? CARD_BG_HOVERED : CARD_BG;
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, backgroundColor);
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, BORDER_COLOR);
            guiGraphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, BORDER_COLOR);

            int textColor = this.active ? TITLE_COLOR : 0x88F5D76E;
            int textY = this.getY() + (this.height - 8) / 2;
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width / 2, textY, textColor);
        }
    }
}
