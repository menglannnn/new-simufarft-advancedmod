package com.example.examplemod.love;

public enum DialogueType {
    NORMAL("普通对话", 0),
    FLIRT("调情", 5),
    JOKE("讲笑话", 3);
    
    private final String displayName;
    private final int baseAffectionChange;
    
    DialogueType(String displayName, int baseAffectionChange) {
        this.displayName = displayName;
        this.baseAffectionChange = baseAffectionChange;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getBaseAffectionChange() {
        return baseAffectionChange;
    }
}
