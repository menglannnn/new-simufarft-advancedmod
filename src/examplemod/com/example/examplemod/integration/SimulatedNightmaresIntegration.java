package com.example.examplemod.integration;

import com.example.examplemod.love.PersonalityType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private static final String INDUSTRIAL_CLIENT_DATA_CLASS = "com.xiaoliang.simukraft.client.gui.IndustrialClientData";
    private static final String COMMERCIAL_CLIENT_DATA_CLASS = "com.xiaoliang.simukraft.client.gui.CommercialClientData";
    private static final String INDUSTRIAL_CONTROL_BOX_SCREEN_CLASS = "com.xiaoliang.simukraft.client.gui.IndustrialControlBoxLDLibScreen";
    private static final String COMMERCIAL_CONTROL_BOX_SCREEN_CLASS = "com.xiaoliang.simukraft.client.gui.CommercialControlBoxScreen";
    private static final String COMMERCIAL_TRADE_SELECT_SCREEN_CLASS = "com.xiaoliang.simukraft.client.gui.CommercialTradeSelectScreen";
    private static final String NPC_SELL_INFO_SCREEN_CLASS = "com.xiaoliang.simukraft.client.gui.NPCSellInfoScreen";
    private static final String PLACED_BUILDING_MANAGER_CLASS = "com.xiaoliang.simukraft.building.PlacedBuildingManager";
    private static final String BUILDING_DATA_MANAGER_CLASS = "com.xiaoliang.simukraft.utils.BuildingDataManager";

    private static boolean isSimulatedNightmaresLoaded = false;
    private static boolean classLookupAttempted = false;
    private static boolean jobUiLookupAttempted = false;
    private static Class<?> customEntityClass = null;
    private static Class<?> npcCardScreenClass = null;
    private static Class<?> industrialClientDataClass = null;
    private static Class<?> commercialClientDataClass = null;
    private static Class<?> placedBuildingManagerClass = null;
    private static Constructor<?> industrialControlBoxScreenConstructor = null;
    private static Constructor<?> commercialControlBoxScreenConstructor = null;
    private static Constructor<?> commercialTradeSelectScreenConstructor = null;
    private static Constructor<?> npcSellInfoScreenConstructor = null;
    private static Method customEntityGetJobMethod = null;
    private static Method industrialJobNameMethod = null;
    private static Method commercialJobNameMethod = null;
    private static Method industrialGetAllJobTypesMethod = null;
    private static Method industrialGetHiredEmployeeUUIDMethod = null;
    private static Method industrialGetBuildingFileNameMethod = null;
    private static Method commercialGetAllJobTypesMethod = null;
    private static Method commercialGetHiredEmployeeUUIDMethod = null;
    private static Method commercialGetBuildingFileNameMethod = null;
    private static Method commercialGetShopModeMethod = null;
    private static Method placedBuildingGetBuildingAtPosMethod = null;
    private static Method placedBuildingGetBuildingByControlBoxMethod = null;
    private static Method buildingDataManagerGetFileNameByDisplayNameMethod = null;
    private static Field placedBuildingControlBoxPosField = null;
    private static Field placedBuildingCategoryField = null;
    private static Field placedBuildingNameField = null;

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
        industrialClientDataClass = null;
        commercialClientDataClass = null;
        placedBuildingManagerClass = null;
        industrialControlBoxScreenConstructor = null;
        commercialControlBoxScreenConstructor = null;
        commercialTradeSelectScreenConstructor = null;
        npcSellInfoScreenConstructor = null;
        customEntityGetJobMethod = null;
        industrialJobNameMethod = null;
        commercialJobNameMethod = null;
        industrialGetAllJobTypesMethod = null;
        industrialGetHiredEmployeeUUIDMethod = null;
        industrialGetBuildingFileNameMethod = null;
        commercialGetAllJobTypesMethod = null;
        commercialGetHiredEmployeeUUIDMethod = null;
        commercialGetBuildingFileNameMethod = null;
        commercialGetShopModeMethod = null;
        placedBuildingGetBuildingAtPosMethod = null;
        placedBuildingGetBuildingByControlBoxMethod = null;
        buildingDataManagerGetFileNameByDisplayNameMethod = null;
        placedBuildingControlBoxPosField = null;
        placedBuildingCategoryField = null;
        placedBuildingNameField = null;
        jobUiLookupAttempted = false;
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

    /**
     * 判断当前 NPC 是否应显示职业 UI 按钮
     */
    public static boolean shouldShowJobUIScreenButton(Entity entity) {
        return resolveJobUiCategory(entity) != JobUiCategory.NONE;
    }

    /**
     * 打开工业/商业职业 UI
     */
    public static boolean openJobUIScreen(Entity entity) {
        return openJobUIScreen(entity, true);
    }

    /**
     * 静默尝试打开工业/商业职业 UI，失败时不提示，交由调用方决定如何回退。
     */
    public static boolean openJobUIScreenSilently(Entity entity) {
        return openJobUIScreen(entity, false);
    }

    private static boolean openJobUIScreen(Entity entity, boolean showFailureMessage) {
        if (!isSimulatedNightmaresLoaded) {
            return failOpenJobUIScreen(showFailureMessage, "§c新模拟大都市模组未加载，无法打开职业UI面板");
        }
        if (entity == null) {
            return failOpenJobUIScreen(showFailureMessage, "§c当前NPC实体不存在，无法打开职业UI面板");
        }

        JobUiContext context = resolveJobUiContext(entity);
        if (context.category() == JobUiCategory.NONE) {
            return failOpenJobUIScreen(showFailureMessage, "§e该NPC没有可打开的工业/商业职业UI");
        }
        if (context.controlBoxPos() == null) {
            return failOpenJobUIScreen(showFailureMessage, "§e未找到该NPC对应的职业控制盒数据，请靠近其工作建筑后再试");
        }
        if (!ensureJobUiReflectionLoaded()) {
            return failOpenJobUIScreen(showFailureMessage, "§c职业UI联动类尚未就绪，无法打开职业面板");
        }

        try {
            Screen screen = createJobScreen(context);
            if (screen == null) {
                return failOpenJobUIScreen(showFailureMessage, "§c未识别到可用的职业UI面板");
            }

            Minecraft.getInstance().setScreen(screen);
            return true;
        } catch (Exception e) {
            if (showFailureMessage) {
                showClientMessage("§c打开职业UI面板失败: " + e.getMessage());
            }
            e.printStackTrace();
            return false;
        }
    }

    private static boolean failOpenJobUIScreen(boolean showFailureMessage, String message) {
        if (showFailureMessage) {
            showClientMessage(message);
        }
        return false;
    }

    private static Screen createJobScreen(JobUiContext context) throws Exception {
        return switch (context.category()) {
            case INDUSTRIAL -> (Screen) industrialControlBoxScreenConstructor.newInstance(
                context.controlBoxPos(),
                context.buildingFileName()
            );
            case COMMERCIAL -> createCommercialJobScreen(context);
            case NONE -> null;
        };
    }

    private static Screen createCommercialJobScreen(JobUiContext context) throws Exception {
        if (context.controlBoxPos() == null) {
            return null;
        }

        String buildingFileName = context.buildingFileName();
        if (buildingFileName == null || buildingFileName.isBlank()) {
            return (Screen) commercialControlBoxScreenConstructor.newInstance(
                context.controlBoxPos(),
                ""
            );
        }

        String shopModeName = resolveCommercialShopModeName(buildingFileName);
        if ("NPC_SELL".equals(shopModeName) && npcSellInfoScreenConstructor != null) {
            return (Screen) npcSellInfoScreenConstructor.newInstance(context.controlBoxPos(), buildingFileName);
        }
        if (commercialTradeSelectScreenConstructor != null) {
            return (Screen) commercialTradeSelectScreenConstructor.newInstance(context.controlBoxPos(), buildingFileName);
        }
        return (Screen) commercialControlBoxScreenConstructor.newInstance(context.controlBoxPos(), buildingFileName);
    }

    private static JobUiContext resolveJobUiContext(Entity entity) {
        JobUiCategory category = resolveJobUiCategory(entity);
        if (category == JobUiCategory.NONE) {
            return JobUiContext.EMPTY;
        }

        BlockPos controlBoxPos = findJobControlBoxPos(entity, entity.getUUID(), category);
        String buildingFileName = controlBoxPos == null ? "" : getBuildingFileName(entity, category, controlBoxPos);
        return new JobUiContext(category, controlBoxPos, buildingFileName == null ? "" : buildingFileName);
    }

    private static JobUiCategory resolveJobUiCategory(Entity entity) {
        String jobType = getRawJobType(entity);
        if (jobType == null || jobType.isBlank()) {
            return JobUiCategory.NONE;
        }

        String industrialDisplayName = invokeJobDisplayMethod(INDUSTRIAL_CLIENT_DATA_CLASS, "getJobNameByJobType", jobType, true);
        if (industrialDisplayName != null && !industrialDisplayName.isBlank()) {
            return JobUiCategory.INDUSTRIAL;
        }

        String commercialDisplayName = invokeJobDisplayMethod(COMMERCIAL_CLIENT_DATA_CLASS, "getJobNameByJobType", jobType, false);
        if (commercialDisplayName != null && !commercialDisplayName.isBlank()) {
            return JobUiCategory.COMMERCIAL;
        }

        return JobUiCategory.NONE;
    }

    private static String getRawJobType(Entity entity) {
        if (!isSimulatedNightmaresLoaded || entity == null || !ensureClassesLoaded()) {
            return "";
        }

        Object npc = getAsNPC(entity);
        if (npc == null) {
            return "";
        }

        try {
            if (customEntityGetJobMethod == null) {
                customEntityGetJobMethod = customEntityClass.getMethod("getJob");
            }
            Object result = customEntityGetJobMethod.invoke(npc);
            return result instanceof String string ? string : "";
        } catch (Exception e) {
            return "";
        }
    }

    private static BlockPos findJobControlBoxPos(Entity entity, UUID npcId, JobUiCategory category) {
        if (npcId == null || !ensureJobUiReflectionLoaded()) {
            return null;
        }

        BlockPos placedBuildingControlBoxPos = findControlBoxPosFromPlacedBuilding(entity, category);
        if (placedBuildingControlBoxPos != null) {
            return placedBuildingControlBoxPos;
        }

        try {
            Map<?, ?> jobTypes = switch (category) {
                case INDUSTRIAL -> invokeStaticMap(industrialGetAllJobTypesMethod);
                case COMMERCIAL -> invokeStaticMap(commercialGetAllJobTypesMethod);
                case NONE -> Map.of();
            };

            for (Object key : jobTypes.keySet()) {
                if (!(key instanceof BlockPos blockPos)) {
                    continue;
                }

                UUID hiredNpcId = switch (category) {
                    case INDUSTRIAL -> invokeUuidGetter(industrialGetHiredEmployeeUUIDMethod, blockPos);
                    case COMMERCIAL -> invokeUuidGetter(commercialGetHiredEmployeeUUIDMethod, blockPos);
                    case NONE -> null;
                };
                if (npcId.equals(hiredNpcId)) {
                    return blockPos;
                }
            }
        } catch (Exception e) {
            System.err.println("[LoveMod] 查找职业控制盒位置失败: " + e.getMessage());
        }

        return null;
    }

    private static BlockPos findControlBoxPosFromPlacedBuilding(Entity entity, JobUiCategory category) {
        if (entity == null || !ensureJobUiReflectionLoaded()) {
            return null;
        }

        String worldId = entity.level().dimension().location().toString();
        for (BlockPos candidatePos : collectBuildingLookupPositions(entity.blockPosition())) {
            Object placedBuilding = getPlacedBuildingAtPos(candidatePos, worldId);
            if (placedBuilding == null || !matchesBuildingCategory(placedBuilding, category)) {
                continue;
            }

            try {
                Object result = placedBuildingControlBoxPosField.get(placedBuilding);
                if (result instanceof BlockPos blockPos) {
                    return blockPos;
                }
            } catch (IllegalAccessException ignored) {
                return null;
            }
        }

        return null;
    }

    private static List<BlockPos> collectBuildingLookupPositions(BlockPos originPos) {
        List<BlockPos> candidatePositions = new ArrayList<>();
        candidatePositions.add(originPos);
        candidatePositions.add(originPos.below());
        candidatePositions.add(originPos.above());
        candidatePositions.add(originPos.north());
        candidatePositions.add(originPos.south());
        candidatePositions.add(originPos.west());
        candidatePositions.add(originPos.east());
        return candidatePositions;
    }

    private static Object getPlacedBuildingAtPos(BlockPos blockPos, String worldId) {
        if (blockPos == null || worldId == null || worldId.isBlank() || !ensureJobUiReflectionLoaded()) {
            return null;
        }

        try {
            return placedBuildingGetBuildingAtPosMethod.invoke(null, blockPos, worldId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean matchesBuildingCategory(Object placedBuilding, JobUiCategory category) {
        if (placedBuilding == null || category == JobUiCategory.NONE || placedBuildingCategoryField == null) {
            return false;
        }

        try {
            Object result = placedBuildingCategoryField.get(placedBuilding);
            if (!(result instanceof String buildingCategory)) {
                return false;
            }
            return switch (category) {
                case INDUSTRIAL -> "industrial".equalsIgnoreCase(buildingCategory);
                case COMMERCIAL -> "commercial".equalsIgnoreCase(buildingCategory);
                case NONE -> false;
            };
        } catch (IllegalAccessException ignored) {
            return false;
        }
    }

    private static String getBuildingFileName(Entity entity, JobUiCategory category, BlockPos blockPos) {
        if (blockPos == null || !ensureJobUiReflectionLoaded()) {
            return "";
        }

        try {
            Object result = switch (category) {
                case INDUSTRIAL -> industrialGetBuildingFileNameMethod.invoke(null, blockPos);
                case COMMERCIAL -> commercialGetBuildingFileNameMethod.invoke(null, blockPos);
                case NONE -> "";
            };
            if (result instanceof String string && !string.isBlank()) {
                return string;
            }
        } catch (Exception e) {
            // 忽略并继续走建筑范围数据兜底
        }

        return getBuildingFileNameFromPlacedBuilding(entity, category, blockPos);
    }

    private static String getBuildingFileNameFromPlacedBuilding(Entity entity, JobUiCategory category, BlockPos controlBoxPos) {
        if (entity == null || controlBoxPos == null || !ensureJobUiReflectionLoaded()) {
            return "";
        }

        try {
            Object placedBuilding = placedBuildingGetBuildingByControlBoxMethod.invoke(null, controlBoxPos);
            if (placedBuilding == null || !matchesBuildingCategory(placedBuilding, category)) {
                return "";
            }

            Object nameResult = placedBuildingNameField.get(placedBuilding);
            Object categoryResult = placedBuildingCategoryField.get(placedBuilding);
            if (!(nameResult instanceof String buildingName) || !(categoryResult instanceof String buildingCategory)) {
                return "";
            }

            Object fileName = buildingDataManagerGetFileNameByDisplayNameMethod.invoke(null, buildingCategory, buildingName);
            return fileName instanceof String string ? string : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String resolveCommercialShopModeName(String buildingFileName) {
        if (buildingFileName == null || buildingFileName.isBlank() || commercialGetShopModeMethod == null) {
            return "";
        }

        try {
            Object result = commercialGetShopModeMethod.invoke(null, buildingFileName);
            if (result instanceof Enum<?> enumValue) {
                return enumValue.name();
            }
        } catch (Exception ignored) {
            return "";
        }
        return "";
    }

    private static Map<?, ?> invokeStaticMap(Method method) throws Exception {
        Object result = method.invoke(null);
        return result instanceof Map<?, ?> map ? map : Map.of();
    }

    private static UUID invokeUuidGetter(Method method, BlockPos blockPos) throws Exception {
        Object result = method.invoke(null, blockPos);
        return result instanceof UUID uuid ? uuid : null;
    }

    private static synchronized boolean ensureJobUiReflectionLoaded() {
        if (!isSimulatedNightmaresLoaded) {
            return false;
        }
        if (industrialClientDataClass != null
            && commercialClientDataClass != null
            && industrialControlBoxScreenConstructor != null
            && commercialControlBoxScreenConstructor != null
            && commercialTradeSelectScreenConstructor != null
            && npcSellInfoScreenConstructor != null
            && commercialGetShopModeMethod != null) {
            return true;
        }
        if (jobUiLookupAttempted) {
            return false;
        }

        jobUiLookupAttempted = true;
        try {
            ClassLoader classLoader = SimulatedNightmaresIntegration.class.getClassLoader();
            industrialClientDataClass = Class.forName(INDUSTRIAL_CLIENT_DATA_CLASS, false, classLoader);
            commercialClientDataClass = Class.forName(COMMERCIAL_CLIENT_DATA_CLASS, false, classLoader);
            placedBuildingManagerClass = Class.forName(PLACED_BUILDING_MANAGER_CLASS, false, classLoader);
            Class<?> buildingDataManagerClass = Class.forName(BUILDING_DATA_MANAGER_CLASS, false, classLoader);
            Class<?> industrialControlBoxScreenClass = Class.forName(INDUSTRIAL_CONTROL_BOX_SCREEN_CLASS, false, classLoader);
            Class<?> commercialControlBoxScreenClass = Class.forName(COMMERCIAL_CONTROL_BOX_SCREEN_CLASS, false, classLoader);
            Class<?> commercialTradeSelectScreenClass = Class.forName(COMMERCIAL_TRADE_SELECT_SCREEN_CLASS, false, classLoader);
            Class<?> npcSellInfoScreenClass = Class.forName(NPC_SELL_INFO_SCREEN_CLASS, false, classLoader);

            industrialGetAllJobTypesMethod = industrialClientDataClass.getMethod("getAllJobTypes");
            industrialGetHiredEmployeeUUIDMethod = industrialClientDataClass.getMethod("getHiredEmployeeUUID", BlockPos.class);
            industrialGetBuildingFileNameMethod = industrialClientDataClass.getMethod("getBuildingFileName", BlockPos.class);
            commercialGetAllJobTypesMethod = commercialClientDataClass.getMethod("getAllJobTypes");
            commercialGetHiredEmployeeUUIDMethod = commercialClientDataClass.getMethod("getHiredEmployeeUUID", BlockPos.class);
            commercialGetBuildingFileNameMethod = commercialClientDataClass.getMethod("getBuildingFileName", BlockPos.class);
            commercialGetShopModeMethod = commercialClientDataClass.getMethod("getShopMode", String.class);
            placedBuildingGetBuildingAtPosMethod = placedBuildingManagerClass.getMethod("getBuildingAtPos", BlockPos.class, String.class);
            placedBuildingGetBuildingByControlBoxMethod = placedBuildingManagerClass.getMethod("getBuildingByControlBox", BlockPos.class);
            buildingDataManagerGetFileNameByDisplayNameMethod = buildingDataManagerClass.getMethod("getFileNameByDisplayName", String.class, String.class);

            industrialControlBoxScreenConstructor = industrialControlBoxScreenClass.getConstructor(BlockPos.class, String.class);
            commercialControlBoxScreenConstructor = commercialControlBoxScreenClass.getConstructor(BlockPos.class, String.class);
            commercialTradeSelectScreenConstructor = commercialTradeSelectScreenClass.getConstructor(BlockPos.class, String.class);
            npcSellInfoScreenConstructor = npcSellInfoScreenClass.getConstructor(BlockPos.class, String.class);

            Class<?> placedBuildingDataClass = Class.forName(
                "com.xiaoliang.simukraft.building.PlacedBuildingManager$PlacedBuildingData",
                false,
                classLoader
            );
            placedBuildingControlBoxPosField = placedBuildingDataClass.getField("controlBoxPos");
            placedBuildingCategoryField = placedBuildingDataClass.getField("category");
            placedBuildingNameField = placedBuildingDataClass.getField("buildingName");
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            System.err.println("[LoveMod] 无法加载职业UI联动类: " + e.getMessage());
            industrialClientDataClass = null;
            commercialClientDataClass = null;
            placedBuildingManagerClass = null;
            industrialControlBoxScreenConstructor = null;
            commercialControlBoxScreenConstructor = null;
            commercialTradeSelectScreenConstructor = null;
            npcSellInfoScreenConstructor = null;
            industrialGetAllJobTypesMethod = null;
            industrialGetHiredEmployeeUUIDMethod = null;
            industrialGetBuildingFileNameMethod = null;
            commercialGetAllJobTypesMethod = null;
            commercialGetHiredEmployeeUUIDMethod = null;
            commercialGetBuildingFileNameMethod = null;
            commercialGetShopModeMethod = null;
            placedBuildingGetBuildingAtPosMethod = null;
            placedBuildingGetBuildingByControlBoxMethod = null;
            buildingDataManagerGetFileNameByDisplayNameMethod = null;
            placedBuildingControlBoxPosField = null;
            placedBuildingCategoryField = null;
            placedBuildingNameField = null;
            return false;
        }
    }

    private static void showClientMessage(String message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal(message));
        }
    }

    private enum JobUiCategory {
        NONE,
        INDUSTRIAL,
        COMMERCIAL
    }

    private record JobUiContext(JobUiCategory category, BlockPos controlBoxPos, String buildingFileName) {
        private static final JobUiContext EMPTY = new JobUiContext(JobUiCategory.NONE, null, "");
    }
}
