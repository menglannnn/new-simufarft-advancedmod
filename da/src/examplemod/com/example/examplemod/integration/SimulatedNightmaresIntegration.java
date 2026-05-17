package com.example.examplemod.integration;

import com.example.examplemod.love.PersonalityType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 新模拟大都市(SimuKraft)模组集成类
 * 使用反射避免硬依赖
 */
@SuppressWarnings("null")
public class SimulatedNightmaresIntegration {

    private static final String MOD_ID = "simukraft";
    private static final String CUSTOM_ENTITY_CLASS = "com.xiaoliang.simukraft.entity.CustomEntity";
    private static final String NPC_CARD_SCREEN_CLASS = "com.xiaoliang.simukraft.client.gui.NPCCardScreen";

    private static boolean isSimulatedNightmaresLoaded = false;
    private static boolean classLookupAttempted = false;
    private static Class<?> customEntityClass = null;
    private static Class<?> npcCardScreenClass = null;
    private static Method customEntityGetJobMethod = null;
    private static Method industrialJobNameMethod = null;
    private static Method commercialJobNameMethod = null;

    /**
     * 初始化集成
     * 在模组加载时调用
     */
    public static void init() {
        // 检查新模拟大都市是否加载
        isSimulatedNightmaresLoaded = ModList.get().isLoaded(MOD_ID);
        classLookupAttempted = false;
        customEntityClass = null;
        npcCardScreenClass = null;
        customEntityGetJobMethod = null;
        industrialJobNameMethod = null;
        commercialJobNameMethod = null;
    }

    /**
     * 检查新模拟大都市是否已加载
     */
    public static boolean isLoaded() {
        return isSimulatedNightmaresLoaded;
    }

    /**
     * 检查实体是否是新模拟大都市的NPC
     */
    public static boolean isSimulatedNightmaresNPC(Entity entity) {
        if (!ensureClassesLoaded()) {
            return false;
        }

        // 使用反射检查是否是CustomEntity的实例
        return customEntityClass.isInstance(entity);
    }

    /**
     * 将实体转换为CustomEntity（返回Object避免硬依赖）
     *
     * @param entity 实体
     * @return CustomEntity实例，如果不是则返回null
     */
    public static Object getAsNPC(Entity entity) {
        if (!ensureClassesLoaded()) {
            return null;
        }

        if (customEntityClass.isInstance(entity)) {
            return entity;
        }
        return null;
    }

    /**
     * 打开新模拟大都市的身份卡界面
     *
     * @param entity NPC实体
     */
    public static void openIdentityCardScreen(Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isSimulatedNightmaresLoaded) {
            if (minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.literal("§c新模拟大都市模组未加载，无法查看身份信息"));
            }
            return;
        }

        if (!ensureClassesLoaded()) {
            if (minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.literal("§c无法加载新模拟大都市联动类"));
            }
            return;
        }

        // 检查是否是CustomEntity
        if (!customEntityClass.isInstance(entity)) {
            if (minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.literal("§c无法打开身份卡：不是新模拟大都市的NPC"));
            }
            return;
        }

        try {
            // 使用反射创建NPCCardScreen实例
            Object npcCardScreen = npcCardScreenClass
                .getConstructor(customEntityClass)
                .newInstance(entity);

            // 设置屏幕
            minecraft.setScreen((net.minecraft.client.gui.screens.Screen) npcCardScreen);
        } catch (Exception e) {
            if (minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.literal("§c打开身份卡界面失败: " + e.getMessage()));
            }
            e.printStackTrace();
        }
    }

    /**
     * 打开新模拟大都市的身份查看界面（通过UUID）
     *
     * @param npcId NPC的UUID
     */
    public static void openIdentityScreen(UUID npcId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isSimulatedNightmaresLoaded) {
            if (minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.literal("§c新模拟大都市模组未加载，无法查看身份信息"));
            }
            return;
        }

        if (!ensureClassesLoaded()) {
            if (minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.literal("§c新模拟大都市联动类尚未就绪"));
            }
            return;
        }

        var level = minecraft.level;
        if (level == null) {
            if (minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.literal("§c当前客户端世界未加载，暂时无法打开身份卡"));
            }
            return;
        }

        // 尝试在客户端世界寻找该实体的引用
        Entity entity = null;
        for (Entity e : level.entitiesForRendering()) {
            if (e.getUUID().equals(npcId)) {
                entity = e;
                break;
            }
        }

        if (entity != null) {
            openIdentityCardScreen(entity);
        } else {
            if (minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.literal("§c无法在当前范围内找到该NPC，请靠近后再试"));
            }
        }
    }

    /**
     * 获取NPC的身份信息
     *
     * @param npcId NPC的UUID
     * @return 身份信息字符串
     */
    public static String getNPCIdentityInfo(UUID npcId) {
        if (!isSimulatedNightmaresLoaded || !ensureClassesLoaded()) {
            return "新模拟大都市模组未加载";
        }

        return "暂无身份信息";
    }

    /**
     * 获取NPC的职业
     *
     * @param entity NPC实体
     * @return 职业名称
     */
    public static String getNPCProfession(Entity entity) {
        if (!isSimulatedNightmaresLoaded || !ensureClassesLoaded()) {
            return "未知职业";
        }

        Object npc = getAsNPC(entity);
        if (npc == null) {
            return "未知职业";
        }

        try {
            if (customEntityGetJobMethod == null) {
                customEntityGetJobMethod = customEntityClass.getMethod("getJob");
            }

            String job = (String) customEntityGetJobMethod.invoke(npc);
            if (job == null || job.isBlank()) {
                return "";
            }

            String industrialJobName = invokeJobDisplayMethod(
                "com.xiaoliang.simukraft.client.gui.IndustrialClientData",
                "getJobNameByJobType",
                job,
                true
            );
            if (industrialJobName != null && !industrialJobName.isBlank()) {
                return industrialJobName;
            }

            String commercialJobName = invokeJobDisplayMethod(
                "com.xiaoliang.simukraft.client.gui.CommercialClientData",
                "getJobNameByJobType",
                job,
                false
            );
            if (commercialJobName != null && !commercialJobName.isBlank()) {
                return commercialJobName;
            }

            String translatedJob = Component.translatable("job." + job).getString();
            if (!translatedJob.equals("job." + job)) {
                return translatedJob;
            }

            return job;
        } catch (Exception e) {
            return "未知职业";
        }
    }

    /**
     * 获取NPC的性格
     *
     * @param entity NPC实体
     * @return 性格名称
     */
    public static String getNPCPersonality(Entity entity) {
        if (!isSimulatedNightmaresLoaded || !ensureClassesLoaded()) {
            return "未知性格";
        }
        return "未知性格";
    }

    /**
     * 获取NPC的性格类型
     * 如果新模拟大都市有自己的性格系统，可以映射到我们的PersonalityType
     *
     * @param entity NPC实体
     * @return PersonalityType 性格类型，如果没有则返回null
     */
    public static PersonalityType getNPCPersonalityType(Entity entity) {
        if (!isSimulatedNightmaresLoaded || !ensureClassesLoaded()) {
            return null;
        }

        Object npc = getAsNPC(entity);
        if (npc != null) {
            // 当前附属还未读取主模组的真实性格字段，这里先用本地默认值兜底。
            return PersonalityType.getRandomPersonality();
        }

        return null;
    }

    /**
     * 根据NPC UUID获取性格类型
     *
     * @param npcId NPC的UUID
     * @return PersonalityType 性格类型
     */
    public static PersonalityType getNPCPersonalityType(UUID npcId) {
        if (!isSimulatedNightmaresLoaded || !ensureClassesLoaded()) {
            return null;
        }

        return null;
    }

    /**
     * 映射新模拟大都市的性格到我们的PersonalityType
     *
     * @param snPersonality 新模拟大都市的性格字符串
     * @return 对应的PersonalityType
     */
    public static PersonalityType mapPersonality(String snPersonality) {
        if (snPersonality == null) {
            return PersonalityType.getRandomPersonality();
        }

        // 根据新模拟大都市的性格名称映射到我们的性格
        switch (snPersonality.toLowerCase()) {
            case "开朗":
            case "活泼":
            case "lively":
            case "cheerful":
                return PersonalityType.LIVELY;
            case "冷漠":
            case "冷淡":
            case "cold":
            case "indifferent":
                return PersonalityType.COLD;
            case "黏人":
            case "依赖":
            case "dependent":
            case "clingy":
                return PersonalityType.DEPENDENT;
            case "刻薄":
            case "毒舌":
            case "poisonous":
            case "tsundere":
            case "sharp":
                return PersonalityType.POISONOUS;
            case "暴躁":
            case "暴戾":
            case "violent":
            case "aggressive":
            case "hot":
                return PersonalityType.VIOLENT;
            default:
                return PersonalityType.getRandomPersonality();
        }
    }

    /**
     * 获取NPC的背景故事
     *
     * @param entity NPC实体
     * @return 背景故事
     */
    public static String getNPCBackground(Entity entity) {
        if (!isSimulatedNightmaresLoaded || !ensureClassesLoaded()) {
            return "暂无背景信息";
        }
        return "暂无背景信息";
    }

    /**
     * 按需加载联动类，并避免在模组构造阶段触发主模组类初始化。
     */
    private static synchronized boolean ensureClassesLoaded() {
        if (!isSimulatedNightmaresLoaded) {
            return false;
        }
        if (customEntityClass != null && npcCardScreenClass != null) {
            return true;
        }
        if (classLookupAttempted) {
            return false;
        }

        classLookupAttempted = true;
        try {
            ClassLoader classLoader = SimulatedNightmaresIntegration.class.getClassLoader();
            customEntityClass = Class.forName(CUSTOM_ENTITY_CLASS, false, classLoader);
            npcCardScreenClass = Class.forName(NPC_CARD_SCREEN_CLASS, false, classLoader);
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            System.err.println("[LoveMod] 无法延迟加载新模拟大都市联动类: " + e.getMessage());
            customEntityClass = null;
            npcCardScreenClass = null;
            return false;
        }
    }

    private static String invokeJobDisplayMethod(String className, String methodName, String job, boolean industrial) {
        try {
            Method method;
            if (industrial) {
                if (industrialJobNameMethod == null) {
                    Class<?> clazz = Class.forName(className, false, SimulatedNightmaresIntegration.class.getClassLoader());
                    industrialJobNameMethod = clazz.getMethod(methodName, String.class);
                }
                method = industrialJobNameMethod;
            } else {
                if (commercialJobNameMethod == null) {
                    Class<?> clazz = Class.forName(className, false, SimulatedNightmaresIntegration.class.getClassLoader());
                    commercialJobNameMethod = clazz.getMethod(methodName, String.class);
                }
                method = commercialJobNameMethod;
            }

            Object result = method.invoke(null, job);
            return result instanceof String string ? string : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 获取NPC的名字
     *
     * @param entity NPC实体
     * @return NPC名字
     */
    public static String getNPCName(Entity entity) {
        return entity.getName().getString();
    }

    /**
     * 获取NPC的UUID
     *
     * @param entity NPC实体
     * @return UUID
     */
    public static UUID getNPCUUID(Entity entity) {
        return entity.getUUID();
    }
}
