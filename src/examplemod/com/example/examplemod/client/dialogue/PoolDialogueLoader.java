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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 读取全局 pool 语言池，并按当前好感度随机生成 3 个可用对话选项。
 */
@SuppressWarnings("null")
public final class PoolDialogueLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation POOL_LOCATION =
        ResourceLocation.fromNamespaceAndPath("advancedmod", "client/puhs/pool.txt");
    private static final Path DEV_POOL_RELATIVE_PATH = Path.of("src", "examplemod", "assets", "advancedmod", "client", "puhs", "pool.txt");
    private static final Path BUILD_POOL_RELATIVE_PATH = Path.of("build", "resources", "main", "assets", "advancedmod", "client", "puhs", "pool.txt");
    private static final Pattern HEADER_PATTERN = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\s*:\\s*(.*)$");
    private static final Pattern PROMPT_PATTERN = Pattern.compile("^npc-->\\s*(.+?);?$");
    private static final Pattern POOL_SECTION_PATTERN = Pattern.compile("^pool\\s*(?:=)?\\s*([+-]?\\d+)\\s*:$", Pattern.CASE_INSENSITIVE);
    private static final Pattern IF_PATTERN = Pattern.compile("^if\\s*(>=|<=|==|>|<)\\s*<\\s*([+-]?\\d+)\\s*>\\s*:$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ENTRY_PATTERN = Pattern.compile("^-\\s*(.+?)\\s*\\|\\s*(.+)$");
    private static final Pattern COMMAND_SUFFIX_PATTERN = Pattern.compile("^(.*\\S)\\s+->\\(\"([^\"]+)\"\\)<-$");
    private static final int DISPLAY_OPTION_COUNT = 3;

    private PoolDialogueLoader() {
    }

    public static DialogueScript loadPoolDialogue(Entity npcEntity, String npcName, String npcUuid, int currentAffection) {
        DialogueScript developmentScript = loadPoolFromDevelopmentPath(npcEntity, npcName, npcUuid, currentAffection);
        if (developmentScript != null) {
            return developmentScript;
        }

        List<Resource> resources = Minecraft.getInstance().getResourceManager().getResourceStack(poolLocation());
        if (resources.isEmpty()) {
            return null;
        }

        try (BufferedReader reader = resources.get(0).openAsReader()) {
            return parse(reader, npcEntity, npcName, npcUuid, currentAffection);
        } catch (IOException e) {
            LOGGER.error("读取 pool 语言池失败: {}", POOL_LOCATION, e);
            return null;
        }
    }

    private static DialogueScript loadPoolFromDevelopmentPath(Entity npcEntity, String npcName, String npcUuid, int currentAffection) {
        Path poolPath = resolveDevelopmentPoolPath();
        if (poolPath == null) {
            return null;
        }

        try (BufferedReader reader = Files.newBufferedReader(poolPath, StandardCharsets.UTF_8)) {
            return parse(reader, npcEntity, npcName, npcUuid, currentAffection);
        } catch (IOException e) {
            LOGGER.error("读取开发态 pool 语言池失败: {}", poolPath, e);
            return null;
        }
    }

    private static DialogueScript parse(BufferedReader reader, Entity npcEntity, String npcName, String npcUuid, int currentAffection) throws IOException {
        String prompt = "";
        DialogueScript.Scope scope = DialogueScript.Scope.any();
        HeaderState headerState = new HeaderState();
        List<PoolEntry> allEntries = new ArrayList<>();
        Integer currentPoolDelta = null;
        Condition currentCondition = Condition.alwaysTrue();
        String line;

        while ((line = reader.readLine()) != null) {
            String normalizedLine = normalizeLine(line);
            if (normalizedLine.isEmpty()) {
                continue;
            }

            Matcher headerMatcher = HEADER_PATTERN.matcher(normalizedLine);
            if (headerMatcher.matches()) {
                scope = updateScope(scope, headerState, headerMatcher.group(1), headerMatcher.group(2));
                continue;
            }

            Matcher promptMatcher = PROMPT_PATTERN.matcher(normalizedLine);
            if (promptMatcher.matches()) {
                prompt = promptMatcher.group(1).trim();
                continue;
            }

            Matcher sectionMatcher = POOL_SECTION_PATTERN.matcher(normalizedLine);
            if (sectionMatcher.matches()) {
                currentPoolDelta = parseInteger(sectionMatcher.group(1), 0);
                currentCondition = Condition.alwaysTrue();
                continue;
            }

            Matcher ifMatcher = IF_PATTERN.matcher(normalizedLine);
            if (ifMatcher.matches()) {
                currentCondition = Condition.of(ifMatcher.group(1), parseInteger(ifMatcher.group(2), 0));
                continue;
            }

            Matcher entryMatcher = ENTRY_PATTERN.matcher(normalizedLine);
            if (entryMatcher.matches()) {
                if (currentPoolDelta == null) {
                    LOGGER.warn("忽略未归属任何 pool 段落的语言池配置: {}", normalizedLine);
                    continue;
                }
                allEntries.add(new PoolEntry(
                    entryMatcher.group(1).trim(),
                    parseResponseEntry(entryMatcher.group(2).trim()),
                    currentPoolDelta,
                    currentCondition
                ));
            }
        }

        String npcJob = npcEntity != null ? SimulatedNightmaresIntegration.getNPCProfession(npcEntity) : "";
        if (!scope.matches(npcName, npcUuid, npcJob)) {
            return null;
        }

        List<PoolEntry> unlockedEntries = allEntries.stream()
            .filter(entry -> entry.condition.matches(currentAffection))
            .toList();
        if (unlockedEntries.isEmpty()) {
            return null;
        }

        return buildScript(prompt, selectEntries(unlockedEntries), scope);
    }

    private static DialogueScript buildScript(String prompt, List<PoolEntry> selectedEntries, DialogueScript.Scope scope) {
        List<DialogueScript.Option> options = new ArrayList<>();
        for (int index = 0; index < selectedEntries.size(); index++) {
            PoolEntry entry = selectedEntries.get(index);
            options.add(new DialogueScript.Option(
                String.valueOf(index + 1),
                entry.question,
                List.of(entry.response),
                Map.of(),
                DialogueScript.DEFAULT_RESPONSE_GROUP,
                0,
                entry.affectionChange
            ));
        }

        String resolvedPrompt = prompt.isBlank() ? "你想说些什么？" : prompt;
        return new DialogueScript(resolvedPrompt, options, scope);
    }

    private static List<PoolEntry> selectEntries(List<PoolEntry> unlockedEntries) {
        List<PoolEntry> selectedEntries = new ArrayList<>(DISPLAY_OPTION_COUNT);
        List<PoolEntry> positiveEntries = unlockedEntries.stream()
            .filter(entry -> entry.affectionChange > 0)
            .toList();

        if (!positiveEntries.isEmpty()) {
            PoolEntry guaranteedPositive = positiveEntries.get(ThreadLocalRandom.current().nextInt(positiveEntries.size()));
            selectedEntries.add(guaranteedPositive);
        }

        List<PoolEntry> remainingEntries = new ArrayList<>(unlockedEntries);
        if (!selectedEntries.isEmpty()) {
            remainingEntries.remove(selectedEntries.get(0));
        }
        Collections.shuffle(remainingEntries, ThreadLocalRandom.current());

        for (PoolEntry entry : remainingEntries) {
            if (selectedEntries.size() >= DISPLAY_OPTION_COUNT) {
                break;
            }
            selectedEntries.add(entry);
        }

        Collections.shuffle(selectedEntries, ThreadLocalRandom.current());
        return List.copyOf(selectedEntries);
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

    private static String normalizeLine(String rawLine) {
        String line = rawLine.replace("\uFEFF", "").trim();
        if (line.startsWith("#") || line.startsWith("//")) {
            return "";
        }
        return line;
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

    private static int parseInteger(String value, int fallbackValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallbackValue;
        }
    }

    private static Path resolveDevelopmentPoolPath() {
        Path gameDir = Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath().normalize();
        Path projectDir = gameDir.getParent() != null && gameDir.getParent().getParent() != null
            ? gameDir.getParent().getParent()
            : null;
        if (projectDir == null) {
            return null;
        }

        Path sourcePath = projectDir.resolve(DEV_POOL_RELATIVE_PATH).normalize();
        if (Files.isRegularFile(sourcePath)) {
            return sourcePath;
        }

        Path buildPath = projectDir.resolve(BUILD_POOL_RELATIVE_PATH).normalize();
        if (Files.isRegularFile(buildPath)) {
            return buildPath;
        }

        return null;
    }

    private static ResourceLocation poolLocation() {
        return Objects.requireNonNull(POOL_LOCATION);
    }

    private static final class HeaderState {
        private boolean allNpcDefined;
        private boolean npcNameDefined;
        private boolean npcUuidDefined;
        private boolean npcJobDefined;
    }

    private record PoolEntry(String question, DialogueScript.ResponseEntry response, int affectionChange, Condition condition) {
    }

    private record Condition(String operator, int expectedValue) {
        private static Condition alwaysTrue() {
            return new Condition("always", 0);
        }

        private static Condition of(String operator, int expectedValue) {
            return new Condition(operator == null ? "always" : operator.trim(), expectedValue);
        }

        private boolean matches(int actualValue) {
            return switch (operator) {
                case "always" -> true;
                case ">=" -> actualValue >= expectedValue;
                case "<=" -> actualValue <= expectedValue;
                case "==" -> actualValue == expectedValue;
                case ">" -> actualValue > expectedValue;
                case "<" -> actualValue < expectedValue;
                default -> true;
            };
        }
    }
}
