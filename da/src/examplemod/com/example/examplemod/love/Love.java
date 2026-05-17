package com.example.examplemod.love;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
@SuppressWarnings({"null"})
public class Love {
    private static final Map<UUID, Map<UUID, Relationship>> playerRelationships = new HashMap<>();
    
    public static final int MAX_LOVE = 100;
    public static final int MARRIAGE_THRESHOLD = 100;
    public static final int FRIENDLY_THRESHOLD = 50;
    
    public static class Relationship {
        private int affection;
        private boolean isMarried;
        private final UUID npcId;
        private final String npcName;
        private PersonalityType personality;
        
        public Relationship(UUID npcId, String npcName) {
            this.npcId = npcId;
            this.npcName = npcName;
            this.affection = 0;
            this.isMarried = false;
            this.personality = PersonalityType.getRandomPersonality();
        }
        
        public Relationship(UUID npcId, String npcName, PersonalityType personality) {
            this.npcId = npcId;
            this.npcName = npcName;
            this.affection = 0;
            this.isMarried = false;
            this.personality = personality;
        }
        
        public PersonalityType getPersonality() {
            return personality;
        }
        
        public void setPersonality(PersonalityType personality) {
            this.personality = personality;
        }
        
        public int getAffection() {
            return affection;
        }
        
        public void addAffection(int amount) {
            if (!isMarried) {
                this.affection = Math.min(MAX_LOVE, Math.max(0, this.affection + amount));
            }
        }
        
        public boolean isMarried() {
            return isMarried;
        }
        
        public void setMarried(boolean married) {
            this.isMarried = married;
            if (married) {
                this.affection = MAX_LOVE;
            }
        }
        
        public boolean canMarry() {
            return affection >= MARRIAGE_THRESHOLD && !isMarried;
        }
        
        public UUID getNpcId() {
            return npcId;
        }
        
        public String getNpcName() {
            return npcName;
        }
        
        public AffectionLevel getAffectionLevel() {
            if (isMarried) return AffectionLevel.MARRIED;
            if (affection >= MARRIAGE_THRESHOLD) return AffectionLevel.LOVE;
            if (affection >= FRIENDLY_THRESHOLD) return AffectionLevel.FRIENDLY;
            if (affection >= 20) return AffectionLevel.NEUTRAL;
            return AffectionLevel.COLD;
        }
        
        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Affection", affection);
            tag.putBoolean("Married", isMarried);
            tag.putUUID("NpcId", npcId);
            tag.putString("NpcName", npcName);
            tag.putString("Personality", personality.name());
            return tag;
        }
        
        public static Relationship load(CompoundTag tag) {
            UUID npcId = tag.getUUID("NpcId");
            String npcName = tag.getString("NpcName");
            PersonalityType personality = PersonalityType.LIVELY;
            if (tag.contains("Personality")) {
                try {
                    personality = PersonalityType.valueOf(tag.getString("Personality"));
                } catch (IllegalArgumentException e) {
                    personality = PersonalityType.getRandomPersonality();
                }
            }
            Relationship rel = new Relationship(npcId, npcName, personality);
            rel.affection = tag.getInt("Affection");
            rel.isMarried = tag.getBoolean("Married");
            return rel;
        }
    }
    
    public enum AffectionLevel {
        COLD("冷漠", 0),
        NEUTRAL("普通", 20),
        FRIENDLY("友好", 50),
        LOVE("爱慕", 100),
        MARRIED("已婚", 100);
        
        private final String displayName;
        private final int threshold;
        
        AffectionLevel(String displayName, int threshold) {
            this.displayName = displayName;
            this.threshold = threshold;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public int getThreshold() {
            return threshold;
        }
    }
    
    public static Relationship getRelationship(Player player, UUID npcId, String npcName) {
        UUID playerId = player.getUUID();
        playerRelationships.putIfAbsent(playerId, new HashMap<>());
        Map<UUID, Relationship> relationships = playerRelationships.get(playerId);
        
        if (!relationships.containsKey(npcId)) {
            relationships.put(npcId, new Relationship(npcId, npcName));
        }
        return relationships.get(npcId);
    }
    
    public static Map<UUID, Relationship> getPlayerRelationships(Player player) {
        return playerRelationships.getOrDefault(player.getUUID(), new HashMap<>());
    }
    
    public static void clearRelationships(Player player) {
        playerRelationships.remove(player.getUUID());
    }
}
