package com.example.examplemod.love;

import net.minecraft.ChatFormatting;

public enum PersonalityType {
    LIVELY("活泼", ChatFormatting.YELLOW, "总是充满活力，喜欢热闹"),
    COLD("冷淡", ChatFormatting.AQUA, "性格冷淡，不易接近"),
    DEPENDENT("依赖", ChatFormatting.LIGHT_PURPLE, "渴望被关心，容易依赖他人"),
    POISONOUS("毒舌", ChatFormatting.GREEN, "说话刻薄，但内心善良"),
    VIOLENT("暴戾", ChatFormatting.RED, "脾气暴躁，需要小心对待");
    
    private final String displayName;
    private final ChatFormatting color;
    private final String description;
    
    PersonalityType(String displayName, ChatFormatting color, String description) {
        this.displayName = displayName;
        this.color = color;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public ChatFormatting getColor() {
        return color;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getFormattedName() {
        return color + displayName + ChatFormatting.RESET;
    }
    
    /**
     * 根据性格计算对话的好感度变化
     */
    public int calculateAffectionChange(DialogueType dialogueType, int baseChange, Love.AffectionLevel affectionLevel) {
        switch (this) {
            case LIVELY:
                return calculateLivelyChange(dialogueType, baseChange, affectionLevel);
            case COLD:
                return calculateColdChange(dialogueType, baseChange, affectionLevel);
            case DEPENDENT:
                return calculateDependentChange(dialogueType, baseChange, affectionLevel);
            case POISONOUS:
                return calculatePoisonousChange(dialogueType, baseChange, affectionLevel);
            case VIOLENT:
                return calculateViolentChange(dialogueType, baseChange, affectionLevel);
            default:
                return baseChange;
        }
    }
    
    private int calculateLivelyChange(DialogueType type, int base, Love.AffectionLevel level) {
        // 活泼性格：喜欢所有互动，调情和笑话效果更好
        switch (type) {
            case FLIRT:
                return base + 3; // 调情+8
            case JOKE:
                return base + 4; // 笑话+7
            case NORMAL:
            default:
                return base + 1; // 普通对话+1
        }
    }
    
    private int calculateColdChange(DialogueType type, int base, Love.AffectionLevel level) {
        // 冷淡性格：不喜欢调情，普通对话更安全
        switch (type) {
            case FLIRT:
                // 冷漠时调情会大幅降低好感
                if (level == Love.AffectionLevel.COLD) return -8;
                if (level == Love.AffectionLevel.NEUTRAL) return -3;
                return base - 2;
            case JOKE:
                // 冷淡性格不喜欢笑话
                return level == Love.AffectionLevel.COLD ? -3 : base - 1;
            case NORMAL:
            default:
                // 普通对话在友好之前效果很差
                if (level == Love.AffectionLevel.COLD) return 1;
                if (level == Love.AffectionLevel.NEUTRAL) return 2;
                return base;
        }
    }
    
    private int calculateDependentChange(DialogueType type, int base, Love.AffectionLevel level) {
        // 依赖性格：非常喜欢调情，渴望被关注
        switch (type) {
            case FLIRT:
                return base + 5; // 调情+10
            case JOKE:
                return base + 2; // 笑话+5
            case NORMAL:
            default:
                // 依赖性格需要持续的关心
                return level == Love.AffectionLevel.COLD ? 2 : base;
        }
    }
    
    private int calculatePoisonousChange(DialogueType type, int base, Love.AffectionLevel level) {
        // 毒舌性格：喜欢笑话，调情会被吐槽但会增加好感
        switch (type) {
            case FLIRT:
                // 毒舌会吐槽但内心开心
                return level == Love.AffectionLevel.COLD ? -2 : base + 2;
            case JOKE:
                // 毒舌最喜欢笑话
                return base + 4;
            case NORMAL:
            default:
                // 普通对话会被毒舌回应
                return level == Love.AffectionLevel.COLD ? 0 : base - 1;
        }
    }
    
    private int calculateViolentChange(DialogueType type, int base, Love.AffectionLevel level) {
        // 暴戾性格：很难相处，调情很危险
        switch (type) {
            case FLIRT:
                // 暴戾性格不喜欢被调情
                if (level == Love.AffectionLevel.COLD) return -10;
                if (level == Love.AffectionLevel.NEUTRAL) return -5;
                return base - 3;
            case JOKE:
                // 笑话可能缓解情绪
                return level == Love.AffectionLevel.COLD ? -2 : base;
            case NORMAL:
            default:
                // 普通对话效果很差
                if (level == Love.AffectionLevel.COLD) return -1;
                if (level == Love.AffectionLevel.NEUTRAL) return 1;
                return base - 1;
        }
    }
    
    /**
     * 获取性格的结婚难度系数
     * 越高表示越难达到结婚条件
     */
    public int getMarriageDifficulty() {
        switch (this) {
            case LIVELY: return 0;
            case DEPENDENT: return -10; // 依赖型更容易结婚
            case COLD: return 20;
            case POISONOUS: return 10;
            case VIOLENT: return 30; // 暴戾型最难结婚
            default: return 0;
        }
    }
    
    /**
     * 随机获取一种性格
     */
    public static PersonalityType getRandomPersonality() {
        PersonalityType[] types = values();
        return types[(int) (Math.random() * types.length)];
    }
}
