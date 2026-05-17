package com.example.examplemod.love;

import net.minecraft.network.chat.Component;

import java.util.*;

public class DialogueManager {

    private static final Random random = new Random();

    // ==================== 活泼性格对话 ====================
    private static final Map<Love.AffectionLevel, List<String>> LIVELY_NORMAL = new HashMap<>();
    private static final Map<Love.AffectionLevel, List<String>> LIVELY_FLIRT = new HashMap<>();
    private static final Map<Love.AffectionLevel, List<String>> LIVELY_JOKE = new HashMap<>();

    // ==================== 冷淡性格对话 ====================
    private static final Map<Love.AffectionLevel, List<String>> COLD_NORMAL = new HashMap<>();
    private static final Map<Love.AffectionLevel, List<String>> COLD_FLIRT = new HashMap<>();
    private static final Map<Love.AffectionLevel, List<String>> COLD_JOKE = new HashMap<>();

    // ==================== 依赖性格对话 ====================
    private static final Map<Love.AffectionLevel, List<String>> DEPENDENT_NORMAL = new HashMap<>();
    private static final Map<Love.AffectionLevel, List<String>> DEPENDENT_FLIRT = new HashMap<>();
    private static final Map<Love.AffectionLevel, List<String>> DEPENDENT_JOKE = new HashMap<>();

    // ==================== 毒舌性格对话 ====================
    private static final Map<Love.AffectionLevel, List<String>> POISONOUS_NORMAL = new HashMap<>();
    private static final Map<Love.AffectionLevel, List<String>> POISONOUS_FLIRT = new HashMap<>();
    private static final Map<Love.AffectionLevel, List<String>> POISONOUS_JOKE = new HashMap<>();

    // ==================== 暴戾性格对话 ====================
    private static final Map<Love.AffectionLevel, List<String>> VIOLENT_NORMAL = new HashMap<>();
    private static final Map<Love.AffectionLevel, List<String>> VIOLENT_FLIRT = new HashMap<>();
    private static final Map<Love.AffectionLevel, List<String>> VIOLENT_JOKE = new HashMap<>();

    static {
        initLivelyDialogues();
        initColdDialogues();
        initDependentDialogues();
        initPoisonousDialogues();
        initViolentDialogues();
    }

    // ==================== 初始化活泼性格对话 ====================
    private static void initLivelyDialogues() {
        // 普通对话
        LIVELY_NORMAL.put(Love.AffectionLevel.COLD, Arrays.asList(
            "嗨！你好呀！",
            "今天天气真不错呢！",
            "你是新来的吗？"
        ));
        LIVELY_NORMAL.put(Love.AffectionLevel.NEUTRAL, Arrays.asList(
            "见到你真开心！",
            "今天有什么好玩的事吗？",
            "一起去冒险吧！"
        ));
        LIVELY_NORMAL.put(Love.AffectionLevel.FRIENDLY, Arrays.asList(
            "哇！你来了！",
            "我一直在等你呢！",
            "今天想去做什么有趣的事？"
        ));
        LIVELY_NORMAL.put(Love.AffectionLevel.LOVE, Arrays.asList(
            "你来了！我等你好久了~",
            "每次见到你我都超开心的！",
            "你今天看起来特别精神呢！"
        ));
        LIVELY_NORMAL.put(Love.AffectionLevel.MARRIED, Arrays.asList(
            "亲爱的！今天过得怎么样？",
            "有你在我身边真好！",
            "我爱你！永远爱你！"
        ));

        // 调情对话
        LIVELY_FLIRT.put(Love.AffectionLevel.COLD, Arrays.asList(
            "诶？你在说什么呀？（脸红）",
            "哈哈，你真有趣！",
            "你真会逗人开心！"
        ));
        LIVELY_FLIRT.put(Love.AffectionLevel.NEUTRAL, Arrays.asList(
            "你真会说话！",
            "哈哈，你真讨厌~",
            "你这么说我会害羞的！"
        ));
        LIVELY_FLIRT.put(Love.AffectionLevel.FRIENDLY, Arrays.asList(
            "我也喜欢你！",
            "你真是的，总是说这种话~",
            "我的心跳得好快！"
        ));
        LIVELY_FLIRT.put(Love.AffectionLevel.LOVE, Arrays.asList(
            "我也超喜欢你的！",
            "你真是世界上最棒的人！",
            "和你在一起最开心了！"
        ));
        LIVELY_FLIRT.put(Love.AffectionLevel.MARRIED, Arrays.asList(
            "老公/老婆，你还是这么会说话~",
            "不管听多少次，我还是会心动！",
            "能嫁给你/娶到你是我最大的幸福！"
        ));

        // 笑话对话
        LIVELY_JOKE.put(Love.AffectionLevel.COLD, Arrays.asList(
            "哈哈哈！太好笑了！",
            "你真幽默！",
            "这个笑话好有趣！"
        ));
        LIVELY_JOKE.put(Love.AffectionLevel.NEUTRAL, Arrays.asList(
            "哈哈哈！我肚子都笑疼了！",
            "你真会讲笑话！",
            "太好笑了！"
        ));
        LIVELY_JOKE.put(Love.AffectionLevel.FRIENDLY, Arrays.asList(
            "哈哈哈！笑死我了！",
            "你讲笑话的样子真可爱！",
            "这个笑话太棒了！"
        ));
        LIVELY_JOKE.put(Love.AffectionLevel.LOVE, Arrays.asList(
            "只要有你在，什么都很开心！",
            "你讲笑话的样子最可爱了！",
            "哈哈哈，我爱你！"
        ));
        LIVELY_JOKE.put(Love.AffectionLevel.MARRIED, Arrays.asList(
            "这么多年了，你还是这么幽默~",
            "和你一起笑是最幸福的事！",
            "亲爱的，你总能让我开心！"
        ));
    }

    // ==================== 初始化冷淡性格对话 ====================
    private static void initColdDialogues() {
        // 普通对话
        COLD_NORMAL.put(Love.AffectionLevel.COLD, Arrays.asList(
            "...什么事？",
            "请说重点。",
            "我很忙。"
        ));
        COLD_NORMAL.put(Love.AffectionLevel.NEUTRAL, Arrays.asList(
            "你好。",
            "有事吗？",
            "...嗯。"
        ));
        COLD_NORMAL.put(Love.AffectionLevel.FRIENDLY, Arrays.asList(
            "...你来了。",
            "有事就说吧。",
            "...今天还行。"
        ));
        COLD_NORMAL.put(Love.AffectionLevel.LOVE, Arrays.asList(
            "...你来晚了。",
            "...我等你了。",
            "...别让我等太久。"
        ));
        COLD_NORMAL.put(Love.AffectionLevel.MARRIED, Arrays.asList(
            "...回来了。",
            "...今天过得怎么样。",
            "...欢迎回家。"
        ));

        // 调情对话
        COLD_FLIRT.put(Love.AffectionLevel.COLD, Arrays.asList(
            "...无聊。",
            "请不要说这种毫无意义的话。",
            "...你在浪费我的时间。"
        ));
        COLD_FLIRT.put(Love.AffectionLevel.NEUTRAL, Arrays.asList(
            "...你在说什么？",
            "...无聊的把戏。",
            "...随你便。"
        ));
        COLD_FLIRT.put(Love.AffectionLevel.FRIENDLY, Arrays.asList(
            "...别这样。",
            "...你很烦。",
            "...随便你。"
        ));
        COLD_FLIRT.put(Love.AffectionLevel.LOVE, Arrays.asList(
            "...笨蛋。",
            "...不要说这种让人害羞的话。",
            "...你真是的。"
        ));
        COLD_FLIRT.put(Love.AffectionLevel.MARRIED, Arrays.asList(
            "...都结婚了还说这种话。",
            "...真是拿你没办法。",
            "...我也...爱你。"
        ));

        // 笑话对话
        COLD_JOKE.put(Love.AffectionLevel.COLD, Arrays.asList(
            "...这有什么好笑的？",
            "...无聊。",
            "...（面无表情）"
        ));
        COLD_JOKE.put(Love.AffectionLevel.NEUTRAL, Arrays.asList(
            "...还行。",
            "...一般。",
            "...就这样？"
        ));
        COLD_JOKE.put(Love.AffectionLevel.FRIENDLY, Arrays.asList(
            "...有点意思。",
            "...不算太差。",
            "...勉勉强强。"
        ));
        COLD_JOKE.put(Love.AffectionLevel.LOVE, Arrays.asList(
            "...哼，还行吧。",
            "...也就那样。",
            "...（嘴角微微上扬）"
        ));
        COLD_JOKE.put(Love.AffectionLevel.MARRIED, Arrays.asList(
            "...你还是老样子。",
            "...也就你能让我笑一下。",
            "...笨蛋。"
        ));
    }

    // ==================== 初始化依赖性格对话 ====================
    private static void initDependentDialogues() {
        // 普通对话
        DEPENDENT_NORMAL.put(Love.AffectionLevel.COLD, Arrays.asList(
            "你...你会陪我吗？",
            "不要丢下我...",
            "我一个人好害怕..."
        ));
        DEPENDENT_NORMAL.put(Love.AffectionLevel.NEUTRAL, Arrays.asList(
            "你终于来了...",
            "我等了你好久...",
            "你会一直陪着我吗？"
        ));
        DEPENDENT_NORMAL.put(Love.AffectionLevel.FRIENDLY, Arrays.asList(
            "见到你真好...",
            "我一直在想你...",
            "不要离开我好吗？"
        ));
        DEPENDENT_NORMAL.put(Love.AffectionLevel.LOVE, Arrays.asList(
            "你来了...我好想你...",
            "没有你我该怎么办...",
            "你会永远陪着我吗？"
        ));
        DEPENDENT_NORMAL.put(Love.AffectionLevel.MARRIED, Arrays.asList(
            "亲爱的...你回来了...",
            "我每天都在等你...",
            "永远不要离开我..."
        ));

        // 调情对话
        DEPENDENT_FLIRT.put(Love.AffectionLevel.COLD, Arrays.asList(
            "诶？你...你是认真的吗？",
            "真的吗？你不会骗我吧？",
            "我...我可以相信你吗？"
        ));
        DEPENDENT_FLIRT.put(Love.AffectionLevel.NEUTRAL, Arrays.asList(
            "你...你真的这么想吗？",
            "我好开心...",
            "你会一直对我这么好吗？"
        ));
        DEPENDENT_FLIRT.put(Love.AffectionLevel.FRIENDLY, Arrays.asList(
            "我也喜欢你！最喜欢你了！",
            "你对我真好...",
            "我好幸福..."
        ));
        DEPENDENT_FLIRT.put(Love.AffectionLevel.LOVE, Arrays.asList(
            "我也爱你！超爱你的！",
            "没有你我就活不下去了...",
            "我们要永远在一起！"
        ));
        DEPENDENT_FLIRT.put(Love.AffectionLevel.MARRIED, Arrays.asList(
            "...我好爱你...",
            "能嫁给你/娶到你是我最大的幸福...",
            "我们要永远永远在一起..."
        ));

        // 笑话对话
        DEPENDENT_JOKE.put(Love.AffectionLevel.COLD, Arrays.asList(
            "哈...哈哈...",
            "你...你真有趣...",
            "谢谢...你让我开心了..."
        ));
        DEPENDENT_JOKE.put(Love.AffectionLevel.NEUTRAL, Arrays.asList(
            "哈哈哈...好好笑...",
            "你真会逗我开心...",
            "我好喜欢听你说话..."
        ));
        DEPENDENT_JOKE.put(Love.AffectionLevel.FRIENDLY, Arrays.asList(
            "哈哈哈！太好笑了！",
            "你总能让我开心！",
            "我好喜欢你的笑话！"
        ));
        DEPENDENT_JOKE.put(Love.AffectionLevel.LOVE, Arrays.asList(
            "哈哈哈！只要有你在我就开心！",
            "你讲笑话的样子真可爱！",
            "我爱你！"
        ));
        DEPENDENT_JOKE.put(Love.AffectionLevel.MARRIED, Arrays.asList(
            "哈哈哈！亲爱的你真幽默！",
            "和你一起笑好幸福...",
            "我最爱你了..."
        ));
    }

    // ==================== 初始化毒舌性格对话 ====================
    private static void initPoisonousDialogues() {
        // 普通对话
        POISONOUS_NORMAL.put(Love.AffectionLevel.COLD, Arrays.asList(
            "哟，这不是那个谁吗？",
            "找我有什么事？我很忙的。",
            "你看起来还是一样无趣。"
        ));
        POISONOUS_NORMAL.put(Love.AffectionLevel.NEUTRAL, Arrays.asList(
            "又来了？你还真闲啊。",
            "说吧，这次又是什么事？",
            "哼，还算准时。"
        ));
        POISONOUS_NORMAL.put(Love.AffectionLevel.FRIENDLY, Arrays.asList(
            "哟，终于舍得来了？",
            "我还以为你把我忘了呢，杂鱼。",
            "算你还有点良心。"
        ));
        POISONOUS_NORMAL.put(Love.AffectionLevel.LOVE, Arrays.asList(
            "来得这么晚，让我好等。",
            "哼，还算你知道来看我。",
            "你这家伙，真是让人没办法。"
        ));
        POISONOUS_NORMAL.put(Love.AffectionLevel.MARRIED, Arrays.asList(
            "回来得这么晚，去哪鬼混了？",
            "哼，还算知道有我这个人。",
            "真是的，没有我你怎么办。"
        ));

        // 调情对话
        POISONOUS_FLIRT.put(Love.AffectionLevel.COLD, Arrays.asList(
            "哈？你在说什么胡话？",
            "脑子进水了吗？",
            "就只知道h段子吗。"
        ));
        POISONOUS_FLIRT.put(Love.AffectionLevel.NEUTRAL, Arrays.asList(
            "油嘴滑舌，恶心。",
            "哼，算你会说话。",
            "...也就那样吧。"
        ));
        POISONOUS_FLIRT.put(Love.AffectionLevel.FRIENDLY, Arrays.asList(
            "哼，少贫嘴。",
            "...也就你觉得这种话有用。",
            "真是的，拿你没办法。"
        ));
        POISONOUS_FLIRT.put(Love.AffectionLevel.LOVE, Arrays.asList(
            "...笨蛋，说什么呢。",
            "哼，也就我会喜欢你这种笨蛋。",
            "...我也喜欢你啦，真是的。"
        ));
        POISONOUS_FLIRT.put(Love.AffectionLevel.MARRIED, Arrays.asList(
            "都结婚了还说这种肉麻的话。",
            "哼，也就我能忍受你了。",
            "...我也爱你啦，笨蛋。"
        ));

        // 笑话对话
        POISONOUS_JOKE.put(Love.AffectionLevel.COLD, Arrays.asList(
            "...这就是你的品味？",
            "无聊透顶。",
            "这种笑话也讲得出口？"
        ));
        POISONOUS_JOKE.put(Love.AffectionLevel.NEUTRAL, Arrays.asList(
            "...勉勉强强吧。",
            "也就那样。",
            "哼，还行。"
        ));
        POISONOUS_JOKE.put(Love.AffectionLevel.FRIENDLY, Arrays.asList(
            "哈哈哈！什么鬼！",
            "你这笑话也太烂了吧！",
            "...噗，还行。"
        ));
        POISONOUS_JOKE.put(Love.AffectionLevel.LOVE, Arrays.asList(
            "哈哈哈！你这笨蛋！",
            "也就你能讲出这种冷笑话！",
            "...哼，还挺有趣的。"
        ));
        POISONOUS_JOKE.put(Love.AffectionLevel.MARRIED, Arrays.asList(
            "哈哈哈！还是老样子！",
            "你这笑话讲得还是这么烂！",
            "...不过我喜欢。"
        ));
    }

    // ==================== 初始化暴戾性格对话 ====================
    private static void initViolentDialogues() {
        // 普通对话
        VIOLENT_NORMAL.put(Love.AffectionLevel.COLD, Arrays.asList(
            "滚开！",
            "别来烦我！",
            "你想找死吗？"
        ));
        VIOLENT_NORMAL.put(Love.AffectionLevel.NEUTRAL, Arrays.asList(
            "什么事？快说！",
            "有话快说！",
            "别浪费我的时间！"
        ));
        VIOLENT_NORMAL.put(Love.AffectionLevel.FRIENDLY, Arrays.asList(
            "哟，来了啊。",
            "还算准时。",
            "说吧，什么事？"
        ));
        VIOLENT_NORMAL.put(Love.AffectionLevel.LOVE, Arrays.asList(
            "来得这么慢！让我好等！",
            "下次再让我等这么久就揍你！",
            "...哼，算你还有点良心。"
        ));
        VIOLENT_NORMAL.put(Love.AffectionLevel.MARRIED, Arrays.asList(
            "快来陪陪我！",
            "...哼，还算知道回家。",
            "...过来，让我看看你。"
        ));

        // 调情对话
        VIOLENT_FLIRT.put(Love.AffectionLevel.COLD, Arrays.asList(
            "你找死吗？！",
            "想被揍吗？！",
            "...滚！"
        ));
        VIOLENT_FLIRT.put(Love.AffectionLevel.NEUTRAL, Arrays.asList(
            "...你脑子有问题吗？",
            "...少废话！",
            "...无聊。"
        ));
        VIOLENT_FLIRT.put(Love.AffectionLevel.FRIENDLY, Arrays.asList(
            "...少油嘴滑舌！",
            "...哼！",
            "...笨蛋！"
        ));
        VIOLENT_FLIRT.put(Love.AffectionLevel.LOVE, Arrays.asList(
            "...笨蛋！说什么呢！",
            "...哼，也就我会喜欢你！",
            "...我也喜欢你啦！"
        ));
        VIOLENT_FLIRT.put(Love.AffectionLevel.MARRIED, Arrays.asList(
            "...都结婚了还说这种话！",
            "...哼，我也爱你！",
            "...过来，让我抱抱！"
        ));

        // 笑话对话
        VIOLENT_JOKE.put(Love.AffectionLevel.COLD, Arrays.asList(
            "...这有什么好笑的？",
            "...无聊！",
            "...浪费时间！"
        ));
        VIOLENT_JOKE.put(Love.AffectionLevel.NEUTRAL, Arrays.asList(
            "...也就那样。",
            "...一般。",
            "...勉勉强强。"
        ));
        VIOLENT_JOKE.put(Love.AffectionLevel.FRIENDLY, Arrays.asList(
            "...哈哈哈！",
            "...还行吧！",
            "...再来一个！"
        ));
        VIOLENT_JOKE.put(Love.AffectionLevel.LOVE, Arrays.asList(
            "哈哈哈！你这笨蛋！",
            "...哼，还挺有趣的！",
            "...再来一个！"
        ));
        VIOLENT_JOKE.put(Love.AffectionLevel.MARRIED, Arrays.asList(
            "哈哈哈！你还是老样子！",
            "...哼，也就你能让我笑！",
            "...再来一个！快！"
        ));
    }

    public static DialogueResult performDialogue(Love.Relationship relationship, DialogueType type) {
        Love.AffectionLevel level = relationship.getAffectionLevel();
        PersonalityType personality = relationship.getPersonality();

        List<String> responses = getDialogues(personality, type, level);
        String response = responses.get(random.nextInt(responses.size()));

        // 根据性格计算好感度变化
        int baseChange = type.getBaseAffectionChange();
        int affectionChange = personality.calculateAffectionChange(type, baseChange, level);

        relationship.addAffection(affectionChange);

        return new DialogueResult(response, affectionChange, relationship.getAffection(), personality);
    }

    private static List<String> getDialogues(PersonalityType personality, DialogueType type, Love.AffectionLevel level) {
        switch (personality) {
            case LIVELY:
                return getDialoguesFromMap(type, LIVELY_NORMAL, LIVELY_FLIRT, LIVELY_JOKE, level);
            case COLD:
                return getDialoguesFromMap(type, COLD_NORMAL, COLD_FLIRT, COLD_JOKE, level);
            case DEPENDENT:
                return getDialoguesFromMap(type, DEPENDENT_NORMAL, DEPENDENT_FLIRT, DEPENDENT_JOKE, level);
            case POISONOUS:
                return getDialoguesFromMap(type, POISONOUS_NORMAL, POISONOUS_FLIRT, POISONOUS_JOKE, level);
            case VIOLENT:
                return getDialoguesFromMap(type, VIOLENT_NORMAL, VIOLENT_FLIRT, VIOLENT_JOKE, level);
            default:
                return LIVELY_NORMAL.get(level);
        }
    }

    private static List<String> getDialoguesFromMap(DialogueType type,
                                                     Map<Love.AffectionLevel, List<String>> normal,
                                                     Map<Love.AffectionLevel, List<String>> flirt,
                                                     Map<Love.AffectionLevel, List<String>> joke,
                                                     Love.AffectionLevel level) {
        switch (type) {
            case FLIRT:
                return flirt.getOrDefault(level, normal.get(level));
            case JOKE:
                return joke.getOrDefault(level, normal.get(level));
            case NORMAL:
            default:
                return normal.get(level);
        }
    }

    public static class DialogueResult {
        private final String npcResponse;
        private final int affectionChange;
        private final int currentAffection;
        private final PersonalityType personality;

        public DialogueResult(String npcResponse, int affectionChange, int currentAffection, PersonalityType personality) {
            this.npcResponse = npcResponse;
            this.affectionChange = affectionChange;
            this.currentAffection = currentAffection;
            this.personality = personality;
        }

        public String getNpcResponse() {
            return npcResponse;
        }

        public int getAffectionChange() {
            return affectionChange;
        }

        public int getCurrentAffection() {
            return currentAffection;
        }

        public PersonalityType getPersonality() {
            return personality;
        }

        public Component getFormattedResponse() {
            String changeText;
            if (affectionChange > 0) {
                changeText = "§a好感度+" + affectionChange;
            } else if (affectionChange < 0) {
                changeText = "§c好感度" + affectionChange;
            } else {
                changeText = "§7好感度无变化";
            }
            return Component.literal("§e[" + personality.getFormattedName() + "§e] §f" + npcResponse + "\n" + changeText + " §7(当前: " + currentAffection + "/100)");
        }
    }
}
