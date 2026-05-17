package com.example.examplemod.client.dialogue;

import com.example.examplemod.integration.SimulatedNightmaresIntegration;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("null")
public final class DialogueScriptLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation SCRIPT_LOCATION =
        ResourceLocation.fromNamespaceAndPath("advancedmod", "client/menglan/quanbu.txt");
    private static final Path DEV_SCRIPT_RELATIVE_PATH = Path.of("src", "examplemod", "assets", "advancedmod", "client", "menglan", "quanbu.txt");
    private static final Path BUILD_SCRIPT_RELATIVE_PATH = Path.of("build", "resources", "main", "assets", "advancedmod", "client", "menglan", "quanbu.txt");
    private static final Path RUNTIME_SCRIPT_RELATIVE_PATH = Path.of("menglan", "quanbu.txt");
    private static final String BUNDLED_SCRIPT_CLASSPATH = "assets/advancedmod/client/menglan/quanbu.txt";
    private static final String LEGACY_BUNDLED_SCRIPT_CLASSPATH = "client/menglan/quanbu.txt";

    private static final Pattern LEGACY_PROMPT_PATTERN = Pattern.compile("^npc-->\\s*(.+?);?$");
    private static final Pattern LEGACY_OPTION_PATTERN = Pattern.compile("^(?:me-->\\s*)?(.+?)\\s*=\\s*\\{(.+?)};?(?:\\s*<([+-]?\\d+)>)?$");
    private static final Pattern LEGACY_GROUP_RESPONSE_PATTERN = Pattern.compile("^player([A-Za-z0-9_]*)(?:\\[([^\\]]+)])?\\(([^)]+)\\)-->\\s*(.+)$");
    private static final Pattern LEGACY_GROUP_RESPONSE_BLOCK_START_PATTERN = Pattern.compile("^player([A-Za-z0-9_]*)(?:\\[([^\\]]+)])?\\(([^)]+)\\)-->\\s*\\{(.*)$");
    private static final Pattern COMMAND_SUFFIX_PATTERN = Pattern.compile("^(.*\\S)\\s+->\\(\"([^\"]+)\"\\)<-$");
    private static final Pattern HEADER_PATTERN = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\s*:\\s*(.*)$");
    private static final Pattern GROUPED_OPTION_ID_PATTERN = Pattern.compile("^\\[([^\\]]+)]\\+?\\(([^)]+)\\)$");
    private static final Pattern OPTION_ID_PATTERN = Pattern.compile("^\\(([^)]+)\\)$");

    private DialogueScriptLoader() {
    }

    public static void initializeRuntimeScriptIfNeeded() {
        ensureRuntimeScriptExists();
    }

    public static ReloadResult reloadForCommand() {
        Path scriptPath = resolveDevelopmentScriptPath();
        if (scriptPath == null) {
            scriptPath = ensureRuntimeScriptExists();
        }
        if (scriptPath != null) {
            try (BufferedReader reader = Files.newBufferedReader(scriptPath, StandardCharsets.UTF_8)) {
                DialogueScript script = parse(reader);
                return new ReloadResult(true, scriptPath.toString(), script.options().size(), script.scope());
            } catch (IOException e) {
                LOGGER.error("通过指令重载开发态对话脚本失败: {}", scriptPath, e);
                return new ReloadResult(false, scriptPath.toString(), 0, DialogueScript.Scope.any());
            }
        }

        List<Resource> resources = Minecraft.getInstance().getResourceManager().getResourceStack(scriptLocation());
        if (resources.isEmpty()) {
            LOGGER.warn("通过指令重载时未找到对话脚本资源: {}", SCRIPT_LOCATION);
            return new ReloadResult(false, SCRIPT_LOCATION.toString(), 0, DialogueScript.Scope.any());
        }

        try (BufferedReader reader = resources.get(0).openAsReader()) {
            DialogueScript script = parse(reader);
            return new ReloadResult(true, SCRIPT_LOCATION.toString(), script.options().size(), script.scope());
        } catch (IOException e) {
            LOGGER.error("通过指令重载对话脚本失败: {}", SCRIPT_LOCATION, e);
            return new ReloadResult(false, SCRIPT_LOCATION.toString(), 0, DialogueScript.Scope.any());
        }
    }

    public static DialogueScript loadMainDialogue(Entity npcEntity, String npcName, String npcUuid) {
        DialogueScript devScript = loadDevelopmentDialogue(npcEntity, npcName, npcUuid);
        if (devScript != null) {
            return devScript;
        }

        DialogueScript runtimeScript = loadRuntimeDialogue(npcEntity, npcName, npcUuid);
        if (runtimeScript != null) {
            return runtimeScript;
        }

        List<Resource> resources = Minecraft.getInstance().getResourceManager().getResourceStack(scriptLocation());
        if (resources.isEmpty()) {
            LOGGER.warn("未找到对话脚本资源: {}", SCRIPT_LOCATION);
            return DialogueScript.empty("对话脚本未找到。");
        }

        try (BufferedReader reader = resources.get(0).openAsReader()) {
            DialogueScript script = parse(reader);
            String npcJob = npcEntity != null ? SimulatedNightmaresIntegration.getNPCProfession(npcEntity) : "";
            if (script.matchesNpc(npcName, npcUuid, npcJob)) {
                return script;
            }
            return DialogueScript.empty("这个 NPC 没有配置可用对话。");
        } catch (IOException e) {
            LOGGER.error("读取对话脚本失败: {}", SCRIPT_LOCATION, e);
            return DialogueScript.empty("对话脚本读取失败。");
        }
    }

    private static DialogueScript loadDevelopmentDialogue(Entity npcEntity, String npcName, String npcUuid) {
        Path scriptPath = resolveDevelopmentScriptPath();
        if (scriptPath == null) {
            return null;
        }

        return loadDialogueFromPath(scriptPath, npcEntity, npcName, npcUuid, "开发态");
    }

    private static DialogueScript loadRuntimeDialogue(Entity npcEntity, String npcName, String npcUuid) {
        Path scriptPath = ensureRuntimeScriptExists();
        if (scriptPath == null) {
            return null;
        }

        return loadDialogueFromPath(scriptPath, npcEntity, npcName, npcUuid, "运行时");
    }

    private static DialogueScript loadDialogueFromPath(Path scriptPath, Entity npcEntity, String npcName, String npcUuid, String sourceName) {
        try (BufferedReader reader = Files.newBufferedReader(scriptPath, StandardCharsets.UTF_8)) {
            DialogueScript script = parse(reader);
            String npcJob = npcEntity != null ? SimulatedNightmaresIntegration.getNPCProfession(npcEntity) : "";
            if (script.matchesNpc(npcName, npcUuid, npcJob)) {
                return script;
            }
            return DialogueScript.empty("这个 NPC 没有配置可用对话。");
        } catch (IOException e) {
            LOGGER.error("读取{}对话脚本失败: {}", sourceName, scriptPath, e);
            return DialogueScript.empty("对话脚本读取失败。");
        }
    }

    private static DialogueScript parse(BufferedReader reader) throws IOException {
        String prompt = "";
        Map<String, MutableOption> options = new LinkedHashMap<>();
        DialogueScript.Scope scope = DialogueScript.Scope.any();
        HeaderState headerState = new HeaderState();
        MutableOption currentResponseBlock = null;
        String currentResponseGroup = DialogueScript.DEFAULT_RESPONSE_GROUP;
        ActivePool activePool = ActivePool.NONE;
        List<DialogueScript.PoolEntry> optionPoolPos = new ArrayList<>();
        List<DialogueScript.PoolEntry> optionPoolNeg = new ArrayList<>();
        List<DialogueScript.PoolEntry> optionPoolNeu = new ArrayList<>();
        String line;

        while ((line = reader.readLine()) != null) {
            String normalizedLine = normalizeLine(line);
            if (normalizedLine.isEmpty() || normalizedLine.equals("}") || normalizedLine.equals("npc {")) {
                if (currentResponseBlock != null && normalizedLine.equals("}")) {
                    currentResponseBlock = null;
                    currentResponseGroup = DialogueScript.DEFAULT_RESPONSE_GROUP;
                }
                activePool = ActivePool.NONE;
                continue;
            }

            if (normalizedLine.startsWith("- ") && activePool != ActivePool.NONE) {
                String entryText = normalizedLine.substring(2).trim();
                if (!entryText.isEmpty()) {
                    int sepIndex = entryText.indexOf('|');
                    String question = sepIndex > 0 ? entryText.substring(0, sepIndex).trim() : entryText;
                    String response = sepIndex > 0 ? entryText.substring(sepIndex + 1).trim() : "";
                    DialogueScript.PoolEntry poolEntry = new DialogueScript.PoolEntry(question, response);
                    switch (activePool) {
                        case OPTION_POS -> optionPoolPos.add(poolEntry);
                        case OPTION_NEG -> optionPoolNeg.add(poolEntry);
                        case OPTION_NEU -> optionPoolNeu.add(poolEntry);
                        default -> {}
                    }
                }
                continue;
            }

            if (normalizedLine.equals("pool+1:")) { activePool = ActivePool.OPTION_POS; continue; }
            if (normalizedLine.equals("pool-1:")) { activePool = ActivePool.OPTION_NEG; continue; }
            if (normalizedLine.equals("pool0:")) { activePool = ActivePool.OPTION_NEU; continue; }

            if (activePool != ActivePool.NONE) {
                activePool = ActivePool.NONE;
            }

            if (currentResponseBlock != null) {
                addResponseEntry(currentResponseBlock, parseResponseEntry(normalizedLine), currentResponseGroup);
                continue;
            }

            Matcher headerMatcher = HEADER_PATTERN.matcher(normalizedLine);
            if (headerMatcher.matches()) {
                scope = updateScope(scope, headerState, headerMatcher.group(1), headerMatcher.group(2));
                continue;
            }

            if (normalizedLine.startsWith("@prompt=")) {
                prompt = normalizedLine.substring("@prompt=".length()).trim();
                continue;
            }

            if (normalizedLine.startsWith("@option=")) {
                parseStructuredOption(normalizedLine.substring("@option=".length()).trim(), options);
                continue;
            }

            Matcher promptMatcher = LEGACY_PROMPT_PATTERN.matcher(normalizedLine);
            if (promptMatcher.matches()) {
                prompt = promptMatcher.group(1).trim();
                continue;
            }

            Matcher optionMatcher = LEGACY_OPTION_PATTERN.matcher(normalizedLine);
            if (optionMatcher.matches()) {
                ParsedOptionDefinition parsedOption = parseOptionDefinition(optionMatcher.group(1).trim());
                String optionStorageKey = buildOptionStorageKey(parsedOption.responseGroup(), parsedOption.id());
                MutableOption option = options.computeIfAbsent(optionStorageKey, ignored -> new MutableOption(parsedOption.id()));
                option.question = optionMatcher.group(2).trim();
                option.requiredAffection = parsedOption.requiredAffection();
                option.affectionChange = parseAffectionDelta(optionMatcher.group(3));
                option.responseGroup = parsedOption.responseGroup();
                continue;
            }

            Matcher responseMatcher = LEGACY_GROUP_RESPONSE_PATTERN.matcher(normalizedLine);
            if (responseMatcher.matches()) {
                Matcher responseBlockStartMatcher = LEGACY_GROUP_RESPONSE_BLOCK_START_PATTERN.matcher(normalizedLine);
                if (responseBlockStartMatcher.matches()) {
                    String responseGroup = normalizeResponseGroup(responseBlockStartMatcher.group(1));
                    String optionStorageKey = buildOptionStorageKey(responseGroup, responseBlockStartMatcher.group(3).trim());
                    MutableOption option = options.computeIfAbsent(optionStorageKey, ignored -> new MutableOption(responseBlockStartMatcher.group(3).trim()));
                    String inlineContent = responseBlockStartMatcher.group(4).trim();
                    if (!inlineContent.isEmpty() && !inlineContent.equals("}")) {
                        addResponseEntry(option, parseResponseEntry(inlineContent), responseGroup);
                    }
                    currentResponseBlock = option;
                    currentResponseGroup = responseGroup;
                    continue;
                }

                String responseGroup = normalizeResponseGroup(responseMatcher.group(1));
                String optionStorageKey = buildOptionStorageKey(responseGroup, responseMatcher.group(3).trim());
                MutableOption option = options.computeIfAbsent(optionStorageKey, ignored -> new MutableOption(responseMatcher.group(3).trim()));
                addResponseEntry(option, parseResponseEntry(responseMatcher.group(4).trim()), responseGroup);
            }
        }

        return buildScript(prompt, options, scope, optionPoolPos, optionPoolNeg, optionPoolNeu);
    }

    private static void parseStructuredOption(String optionBody, Map<String, MutableOption> options) {
        String[] segments = optionBody.split("\\|", 3);
        if (segments.length < 3) {
            LOGGER.warn("忽略无法识别的对话选项配置: {}", optionBody);
            return;
        }

        String id = segments[0].trim();
        String optionStorageKey = buildOptionStorageKey(DialogueScript.DEFAULT_RESPONSE_GROUP, id);
        MutableOption option = options.computeIfAbsent(optionStorageKey, ignored -> new MutableOption(id));
        option.question = segments[1].trim();
        addResponseEntry(option, parseResponseEntry(segments[2].trim()), DialogueScript.DEFAULT_RESPONSE_GROUP);
    }

    private static DialogueScript buildScript(String prompt, Map<String, MutableOption> options, DialogueScript.Scope scope,
                                               List<DialogueScript.PoolEntry> optionPoolPos, List<DialogueScript.PoolEntry> optionPoolNeg, List<DialogueScript.PoolEntry> optionPoolNeu) {
        String resolvedPrompt = prompt.isBlank() ? "这个 NPC 暂时没有准备好台词。" : prompt;
        List<DialogueScript.Option> resolvedOptions = new ArrayList<>();

        for (MutableOption option : options.values()) {
            if (option.question == null || option.question.isBlank()) {
                continue;
            }

            List<DialogueScript.ResponseEntry> responses = option.responses.isEmpty()
                ? List.of(new DialogueScript.ResponseEntry("", ""))
                : List.copyOf(option.responses);
            resolvedOptions.add(new DialogueScript.Option(
                option.id,
                option.question,
                responses,
                copyGroupedResponses(option.groupedResponses),
                option.responseGroup,
                option.requiredAffection,
                option.affectionChange
            ));
        }

        return new DialogueScript(resolvedPrompt, resolvedOptions, scope, optionPoolPos, optionPoolNeg, optionPoolNeu);
    }

    private static DialogueScript.ResponseEntry parseResponseEntry(String rawLine) {
        String normalizedLine = rawLine == null ? "" : rawLine.trim();
        Matcher commandMatcher = COMMAND_SUFFIX_PATTERN.matcher(normalizedLine);
        if (commandMatcher.matches()) {
            String text = commandMatcher.group(1).trim();
            String command = commandMatcher.group(2).trim();
            return new DialogueScript.ResponseEntry(text, command);
        }
        return new DialogueScript.ResponseEntry(normalizedLine, "");
    }

    private static ParsedOptionDefinition parseOptionDefinition(String rawIdentifier) {
        Matcher groupedMatcher = GROUPED_OPTION_ID_PATTERN.matcher(rawIdentifier);
        if (groupedMatcher.matches()) {
            return parseGroupedOptionDefinition(groupedMatcher.group(1).trim(), groupedMatcher.group(2).trim());
        }

        Matcher wrappedMatcher = OPTION_ID_PATTERN.matcher(rawIdentifier);
        if (wrappedMatcher.matches()) {
            return new ParsedOptionDefinition(wrappedMatcher.group(1).trim(), 0, DialogueScript.DEFAULT_RESPONSE_GROUP);
        }

        return new ParsedOptionDefinition(rawIdentifier.trim(), 0, DialogueScript.DEFAULT_RESPONSE_GROUP);
    }

    private static int parseAffectionDelta(String rawDelta) {
        if (rawDelta == null || rawDelta.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(rawDelta.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void addResponseEntry(MutableOption option, DialogueScript.ResponseEntry responseEntry, String responseGroup) {
        String normalizedGroup = normalizeResponseGroup(responseGroup);
        if (DialogueScript.DEFAULT_RESPONSE_GROUP.equals(normalizedGroup)) {
            option.responses.add(responseEntry);
            return;
        }
        option.groupedResponses.computeIfAbsent(normalizedGroup, key -> new ArrayList<>()).add(responseEntry);
    }

    private static DialogueScript.Scope updateScope(DialogueScript.Scope currentScope, HeaderState headerState, String rawKey, String rawValue) {
        String key = rawKey.trim().toLowerCase();
        String value = rawValue == null ? "" : rawValue.trim();

        return switch (key) {
            case "allnpc" -> headerState.allNpcDefined
                ? currentScope
                : defineAllNpc(currentScope, headerState, value);
            case "npcname" -> headerState.npcNameDefined
                ? currentScope
                : defineNpcName(currentScope, headerState, value);
            case "npcuuid" -> headerState.npcUuidDefined
                ? currentScope
                : defineNpcUuid(currentScope, headerState, value);
            case "npcjob" -> headerState.npcJobDefined
                ? currentScope
                : defineNpcJob(currentScope, headerState, value);
            default -> currentScope;
        };
    }

    private static DialogueScript.Scope defineAllNpc(DialogueScript.Scope currentScope, HeaderState headerState, String value) {
        headerState.allNpcDefined = true;
        return new DialogueScript.Scope(parseBoolean(value), currentScope.npcName(), currentScope.npcUuid(), currentScope.npcJob());
    }

    private static DialogueScript.Scope defineNpcName(DialogueScript.Scope currentScope, HeaderState headerState, String value) {
        headerState.npcNameDefined = true;
        return new DialogueScript.Scope(currentScope.allNpc(), value, currentScope.npcUuid(), currentScope.npcJob());
    }

    private static DialogueScript.Scope defineNpcUuid(DialogueScript.Scope currentScope, HeaderState headerState, String value) {
        headerState.npcUuidDefined = true;
        return new DialogueScript.Scope(currentScope.allNpc(), currentScope.npcName(), value, currentScope.npcJob());
    }

    private static DialogueScript.Scope defineNpcJob(DialogueScript.Scope currentScope, HeaderState headerState, String value) {
        headerState.npcJobDefined = true;
        return new DialogueScript.Scope(currentScope.allNpc(), currentScope.npcName(), currentScope.npcUuid(), value);
    }

    private static boolean parseBoolean(String value) {
        return "true".equalsIgnoreCase(value)
            || "yes".equalsIgnoreCase(value)
            || "1".equalsIgnoreCase(value)
            || "on".equalsIgnoreCase(value);
    }

    private static String normalizeLine(String rawLine) {
        String line = rawLine.replace("\uFEFF", "").trim();
        if (line.startsWith("#") || line.startsWith("//")) {
            return "";
        }
        if (line.startsWith("npc {")) {
            line = line.substring("npc {".length()).trim();
        }
        return line;
    }

    private static ParsedOptionDefinition parseGroupedOptionDefinition(String rawSelector, String optionId) {
        String selector = rawSelector.trim();
        if (selector.matches("[+-]?\\d+")) {
            return new ParsedOptionDefinition(optionId, Integer.parseInt(selector), "love");
        }

        int separatorIndex = selector.indexOf(':');
        if (separatorIndex > 0 && separatorIndex < selector.length() - 1) {
            String groupName = normalizeResponseGroup(selector.substring(0, separatorIndex));
            String groupValue = selector.substring(separatorIndex + 1).trim();
            if ("love".equals(groupName) && groupValue.matches("[+-]?\\d+")) {
                return new ParsedOptionDefinition(optionId, Integer.parseInt(groupValue), groupName);
            }
            return new ParsedOptionDefinition(optionId, 0, groupName);
        }

        return new ParsedOptionDefinition(optionId, 0, normalizeResponseGroup(selector));
    }

    private static String normalizeResponseGroup(String rawGroup) {
        if (rawGroup == null) {
            return DialogueScript.DEFAULT_RESPONSE_GROUP;
        }
        String normalized = rawGroup.trim().toLowerCase();
        return normalized.isBlank() ? DialogueScript.DEFAULT_RESPONSE_GROUP : normalized;
    }

    private static Map<String, List<DialogueScript.ResponseEntry>> copyGroupedResponses(Map<String, List<DialogueScript.ResponseEntry>> groupedResponses) {
        Map<String, List<DialogueScript.ResponseEntry>> copied = new LinkedHashMap<>();
        for (Map.Entry<String, List<DialogueScript.ResponseEntry>> entry : groupedResponses.entrySet()) {
            copied.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return copied;
    }

    private static String buildOptionStorageKey(String responseGroup, String optionId) {
        return normalizeResponseGroup(responseGroup) + ":" + optionId.trim();
    }

    private static Path resolveDevelopmentScriptPath() {
        Path gameDir = Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath().normalize();
        Path projectDir = gameDir.getParent() != null && gameDir.getParent().getParent() != null
            ? gameDir.getParent().getParent()
            : null;
        if (projectDir == null) {
            return null;
        }

        Path sourcePath = projectDir.resolve(DEV_SCRIPT_RELATIVE_PATH).normalize();
        if (Files.isRegularFile(sourcePath)) {
            return sourcePath;
        }

        Path buildPath = projectDir.resolve(BUILD_SCRIPT_RELATIVE_PATH).normalize();
        if (Files.isRegularFile(buildPath)) {
            return buildPath;
        }

        return null;
    }

    private static Path ensureRuntimeScriptExists() {
        Path runtimeScriptPath = resolveRuntimeScriptPath();
        if (runtimeScriptPath == null) {
            return null;
        }

        try {
            Files.createDirectories(runtimeScriptPath.getParent());
            if (!Files.isRegularFile(runtimeScriptPath)) {
                try (InputStream inputStream = openBundledScriptStream()) {
                    Files.copy(inputStream, runtimeScriptPath, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("已生成默认对话脚本: {}", runtimeScriptPath);
                }
            }
            return runtimeScriptPath;
        } catch (IOException e) {
            LOGGER.error("创建运行时对话脚本失败: {}", runtimeScriptPath, e);
            return null;
        }
    }

    private static Path resolveRuntimeScriptPath() {
        Path gameDir = Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath().normalize();
        return gameDir.resolve(RUNTIME_SCRIPT_RELATIVE_PATH).normalize();
    }

    private static InputStream openBundledScriptStream() throws IOException {
        InputStream inputStream = DialogueScriptLoader.class.getClassLoader().getResourceAsStream(BUNDLED_SCRIPT_CLASSPATH);
        if (inputStream != null) {
            return inputStream;
        }

        InputStream legacyInputStream = DialogueScriptLoader.class.getClassLoader().getResourceAsStream(LEGACY_BUNDLED_SCRIPT_CLASSPATH);
        if (legacyInputStream != null) {
            return legacyInputStream;
        }

        Path developmentScriptPath = resolveDevelopmentScriptPath();
        if (developmentScriptPath != null && Files.isRegularFile(developmentScriptPath)) {
            return Files.newInputStream(developmentScriptPath);
        }

        Path runtimeBuildScriptPath = resolveRuntimeBuildScriptPath();
        if (runtimeBuildScriptPath != null && Files.isRegularFile(runtimeBuildScriptPath)) {
            return Files.newInputStream(runtimeBuildScriptPath);
        }

        throw new IOException("未找到内置默认对话脚本，也未找到可兜底的默认模板来源: " + BUNDLED_SCRIPT_CLASSPATH);
    }

    private static Path resolveRuntimeBuildScriptPath() {
        Path gameDir = Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath().normalize();
        Path projectDir = gameDir.getParent() != null && gameDir.getParent().getParent() != null
            ? gameDir.getParent().getParent()
            : null;
        if (projectDir == null) {
            return null;
        }
        return projectDir.resolve(BUILD_SCRIPT_RELATIVE_PATH).normalize();
    }

    private static ResourceLocation scriptLocation() {
        return Objects.requireNonNull(SCRIPT_LOCATION);
    }

    private enum ActivePool {
        NONE, OPTION_POS, OPTION_NEG, OPTION_NEU
    }

    private static final class MutableOption {
        private final String id;
        private String question;
        private final List<DialogueScript.ResponseEntry> responses = new ArrayList<>();
        private final Map<String, List<DialogueScript.ResponseEntry>> groupedResponses = new LinkedHashMap<>();
        private String responseGroup = DialogueScript.DEFAULT_RESPONSE_GROUP;
        private int requiredAffection;
        private int affectionChange;

        private MutableOption(String id) {
            this.id = id;
        }
    }

    public record ReloadResult(boolean success, String source, int optionCount, DialogueScript.Scope scope) {
    }

    private static final class HeaderState {
        private boolean allNpcDefined;
        private boolean npcNameDefined;
        private boolean npcUuidDefined;
        private boolean npcJobDefined;
    }

    private record ParsedOptionDefinition(String id, int requiredAffection, String responseGroup) {
    }
}
