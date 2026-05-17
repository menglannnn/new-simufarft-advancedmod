package com.example.examplemod.client.dialogue;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 对话脚本数据模型，界面层只依赖这个结果，不再关心 txt 的具体写法。
 */
public final class DialogueScript {
    public static final String DEFAULT_RESPONSE_GROUP = "default";
    public static final String UNSET_NPC_JOB = "__unset_npc_job__";
    private final String prompt;
    private final List<Option> options;
    private final Scope scope;

    public DialogueScript(String prompt, List<Option> options, Scope scope) {
        this.prompt = Objects.requireNonNull(prompt, "prompt").trim();
        this.options = List.copyOf(Objects.requireNonNull(options, "options"));
        this.scope = Objects.requireNonNull(scope, "scope");
    }

    public String prompt() {
        return prompt;
    }

    public List<Option> options() {
        return options;
    }

    public boolean hasOptions() {
        return !options.isEmpty();
    }

    public Scope scope() {
        return scope;
    }

    public boolean matchesNpc(String npcName, String npcUuid, String npcJob) {
        return scope.matches(npcName, npcUuid, npcJob);
    }

    public static DialogueScript empty(String prompt) {
        return new DialogueScript(prompt, List.of(), Scope.any());
    }

    public record Option(String id, String question, List<ResponseEntry> responses,
                         Map<String, List<ResponseEntry>> groupedResponses,
                         String responseGroup, int requiredAffection, int affectionChange) {
        public Option {
            id = Objects.requireNonNull(id, "id").trim();
            question = Objects.requireNonNull(question, "question").trim();
            responses = List.copyOf(Objects.requireNonNull(responses, "responses"));
            groupedResponses = Map.copyOf(Objects.requireNonNull(groupedResponses, "groupedResponses"));
            responseGroup = normalizeResponseGroup(responseGroup);
        }

        public String displayText() {
            if (id.isBlank() || !id.chars().allMatch(Character::isDigit)) {
                return question;
            }
            return id + ". " + question;
        }

        public boolean hasResponses() {
            return !responses.isEmpty();
        }

        public boolean isUnlocked(int currentAffection) {
            return currentAffection >= requiredAffection;
        }

        public List<ResponseEntry> responsesForCurrentAffection(int currentAffection) {
            List<ResponseEntry> selectedResponses = groupedResponses.get(responseGroup);
            if (selectedResponses != null && !selectedResponses.isEmpty()) {
                return selectedResponses;
            }
            return responses;
        }

        public ResponseEntry firstResponseOrFallback(int currentAffection) {
            List<ResponseEntry> resolvedResponses = responsesForCurrentAffection(currentAffection);
            if (resolvedResponses.isEmpty()) {
                return new ResponseEntry("这个选项暂时还没有配置回复。", "");
            }
            return resolvedResponses.get(0);
        }

        private static String normalizeResponseGroup(String rawGroup) {
            if (rawGroup == null) {
                return DEFAULT_RESPONSE_GROUP;
            }
            String normalized = rawGroup.trim().toLowerCase();
            return normalized.isBlank() ? DEFAULT_RESPONSE_GROUP : normalized;
        }
    }

    public record ResponseEntry(String text, String command) {
        public ResponseEntry {
            text = Objects.requireNonNull(text, "text").trim();
            command = Objects.requireNonNull(command, "command").trim();
        }

        public boolean hasCommand() {
            return !command.isBlank();
        }
    }

    public record Scope(boolean allNpc, String npcName, String npcUuid, String npcJob) {
        public Scope {
            npcName = normalize(npcName);
            npcUuid = normalize(npcUuid);
            npcJob = normalize(npcJob);
        }

        public static Scope any() {
            return new Scope(true, "", "", "all");
        }

        public static Scope customized() {
            return new Scope(false, "", "", UNSET_NPC_JOB);
        }

        public boolean matches(String actualNpcName, String actualNpcUuid, String actualNpcJob) {
            if (allNpc) {
                return true;
            }

            String normalizedName = normalize(actualNpcName);
            String normalizedUuid = normalize(actualNpcUuid);
            String normalizedJob = normalize(actualNpcJob);

            if (!npcName.isBlank() && !matchesName(npcName, normalizedName)) {
                return false;
            }

            if (!npcUuid.isBlank() && !npcUuid.equalsIgnoreCase(normalizedUuid)) {
                return false;
            }

            if (npcName.isBlank() && npcUuid.isBlank() && UNSET_NPC_JOB.equalsIgnoreCase(npcJob)) {
                return false;
            }

            if (UNSET_NPC_JOB.equalsIgnoreCase(npcJob)) {
                return true;
            }

            if ("all".equalsIgnoreCase(npcJob)) {
                return true;
            }

            if (npcJob.isBlank()) {
                return normalizedJob.isBlank() || "unemployed".equalsIgnoreCase(normalizedJob);
            }

            return npcJob.equalsIgnoreCase(normalizedJob);
        }

        private static String normalize(String value) {
            return value == null ? "" : value.trim();
        }

        private static boolean matchesName(String expectedName, String actualName) {
            String compactExpected = compact(expectedName);
            String compactActual = compact(actualName);
            return compactExpected.equalsIgnoreCase(compactActual);
        }

        private static String compact(String value) {
            return normalize(value).replace(" ", "").replace("\u3000", "");
        }
    }
}
