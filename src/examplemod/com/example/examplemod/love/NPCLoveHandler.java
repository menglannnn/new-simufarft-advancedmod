package com.example.examplemod.love;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.client.gui.NPCInteractScreen;
import com.example.examplemod.integration.SimulatedNightmaresIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class NPCLoveHandler {

    private static final String DATA_KEY = "NPCLoveData";

    /**
     * 判断实体是否是新模拟大都市的NPC
     */
    public static boolean isSupportedNpc(Entity entity) {
        // 首先检查是否为新模拟大都市的NPC（使用实际的CustomEntity类）
        if (SimulatedNightmaresIntegration.isSimulatedNightmaresNPC(entity)) {
            return true;
        }

        // 备用方案：检查实体名称
        String entityName = entity.getName().getString();
        return entityName.contains("居民") || entityName.contains("NPC") || entityName.contains("市民");
    }

    /**
     * 获取NPC的UUID
     */
    private static UUID getNPCUUID(Entity npc) {
        return SimulatedNightmaresIntegration.getNPCUUID(npc);
    }

    /**
     * 获取NPC的名称
     */
    private static String getNPCName(Entity npc) {
        // 优先使用集成类获取名字
        if (SimulatedNightmaresIntegration.isSimulatedNightmaresNPC(npc)) {
            return SimulatedNightmaresIntegration.getNPCName(npc);
        }
        return npc.getName().getString();
    }

    @OnlyIn(Dist.CLIENT)
    public static void openInteractScreenForClient(Player player, Entity npc) {
        UUID npcId = getNPCUUID(npc);
        String npcName = getNPCName(npc);

        // 获取或创建关系
        Love.Relationship relationship = Love.getRelationship(player, npcId, npcName);

        // 如果是新关系，尝试从新模拟大都市获取性格
        if (relationship.getAffection() == 0 && !relationship.isMarried()) {
            // 尝试从实体直接获取性格
            PersonalityType snPersonality = SimulatedNightmaresIntegration.getNPCPersonalityType(npc);
            if (snPersonality != null) {
                relationship.setPersonality(snPersonality);
            }
        }

        // 传递实体对象，用于打开身份卡界面
        Minecraft.getInstance().setScreen(new NPCInteractScreen(player, npcId, npcName, npc));
    }

    /**
     * 玩家登录时加载数据
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            loadPlayerData(player);
        }
    }

    /**
     * 玩家退出时保存数据
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            savePlayerData(player);
        }
    }

    /**
     * 保存玩家恋爱数据到NBT
     */
    public static void savePlayerData(ServerPlayer player) {
        CompoundTag playerData = player.getPersistentData();
        CompoundTag modData = new CompoundTag();

        ListTag relationshipsTag = new ListTag();

        for (Love.Relationship rel : Love.getPlayerRelationships(player).values()) {
            relationshipsTag.add(rel.save());
        }

        modData.put("Relationships", relationshipsTag);
        playerData.put(DATA_KEY, modData);
    }

    /**
     * 从NBT加载玩家恋爱数据
     */
    public static void loadPlayerData(ServerPlayer player) {
        CompoundTag playerData = player.getPersistentData();

        if (playerData.contains(DATA_KEY)) {
            CompoundTag modData = playerData.getCompound(DATA_KEY);
            ListTag relationshipsTag = modData.getList("Relationships", 10);

            for (int i = 0; i < relationshipsTag.size(); i++) {
                CompoundTag relTag = relationshipsTag.getCompound(i);
                Love.Relationship rel = Love.Relationship.load(relTag);
                Love.getPlayerRelationships(player).put(rel.getNpcId(), rel);
            }
        }
    }
}
