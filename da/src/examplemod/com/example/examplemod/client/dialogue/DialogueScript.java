package com.example.examplemod.client.dialogue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class DialogueScript {
    public static final String DEFAULT_RESPONSE_GROUP = "default";
    private final String prompt;
    private final List<Option> options;
    private final Scope scope;
    private final List<PoolEntry> optionPoolPos;
    private final List<PoolEntry> optionPoolNeg;
    private final List<PoolEntry> optionPoolNeu;

    public DialogueScript(String prompt, List<Option> options, Scope scope,
                          List<PoolEntry> optionPoolPos, List<PoolEntry> optionPoolNeg, List<PoolEntry> optionPoolNeu) {
        this.prompt = Objects.requireNonNull(prompt, "prompt").trim();
        this.options = List.copyOf(Objects.requireNonNull(options, "options"));
        this.scope = Objects.requireNonNull(scope, "scope");
        this.optionPoolPos = List.copyOf(Objects.requireNonNullElse(optionPoolPos, List.of()));
        this.optionPoolNeg = List.copyOf(Objects.requireNonNullElse(optionPoolNeg, List.of()));
        this.optionPoolNeu = List.copyOf(Objects.requireNonNullElse(optionPoolNeu, List.of()));
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

    public List<PoolEntry> optionPoolPos() { return optionPoolPos; }
    public List<PoolEntry> optionPoolNeg() { return optionPoolNeg; }
    public List<PoolEntry> optionPoolNeu() { return optionPoolNeu; }

    public boolean hasOptionPools() {
        return !optionPoolPos.isEmpty() || !optionPoolNeg.isEmpty() || !optionPoolNeu.isEmpty();
    }

    public record PoolEntry(String question, String response) {}

    public List<Option> randomizeOptions() {
        record ShuffledEntry(int delta, String question, String response) {}

        List<ShuffledEntry> allEntries = new ArrayList<>();
        for (PoolEntry e : optionPoolPos) allEntries.add(new ShuffledEntry(1, e.question(), e.response()));
        for (PoolEntry e : optionPoolNeg) allEntries.add(new ShuffledEntry(-1, e.question(), e.response()));
        for (PoolEntry e : optionPoolNeu) allEntries.add(new ShuffledEntry(0, e.question(), e.response()));

        if (allEntries.isEmpty()) return List.of();

        Collections.shuffle(allEntries, ThreadLocalRandom.current());

        int count = Math.min(3, allEntries.size());

        boolean hasPos = false;
        for (int i = 0; i < count; i++) {
            if (allEntries.get(i).delta() == 1) { hasPos = true; break; }
        }
        if (!hasPos && !optionPoolPos.isEmpty()) {
            for (int i = count; i < allEntries.size(); i++) {
                if (allEntries.get(i).delta() == 1) {
                    ShuffledEntry temp = allEntries.get(0);
                    allEntries.set(0, allEntries.get(i));
                    allEntries.set(i, temp);
                    break;
                }
            }
        }

        List<Option> result = new ArrayList<>();
        String[] ids = {"a", "b", "c"};
        for (int i = 0; i < count; i++) {
            ShuffledEntry entry = allEntries.get(i);
            ResponseEntry response = new ResponseEntry(
                entry.response().isBlank() ? "..." : entry.response(), ""
            );
            result.add(new Option(ids[i], entry.question(), List.of(response), Map.of(), DEFAULT_RESPONSE_GROUP, 0, entry.delta()));
        }

        return result;
    }

    public boolean matchesNpc(String npcName, String npcUuid, String npcJob) {
        return scope.matches(npcName, npcUuid, npcJob);
    }

    public static DialogueScript empty(String prompt) {
        return new DialogueScript(prompt, List.of(), Scope.any(), List.of(), List.of(), List.of());
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
