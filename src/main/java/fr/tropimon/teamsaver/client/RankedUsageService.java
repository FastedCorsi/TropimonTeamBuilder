package fr.tropimon.teamsaver.client;

import com.google.gson.JsonSyntaxException;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import fr.tropimon.teamsaver.TeamJson;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import net.fabricmc.loader.api.FabricLoader;

/** Read-only client for the public Ranked Tropimon statistics API. */
final class RankedUsageService {
    static final RankedUsageService INSTANCE = new RankedUsageService();
    private static final String API = "https://rankedapi.tropimon.fr/api";
    private static final long CACHE_MS = Duration.ofMinutes(10).toMillis();
    private static final long DISK_CACHE_MAX_MS = Duration.ofDays(14).toMillis();
    private static final Set<String> RECOVERY_MOVES = Set.of(
            "recover", "roost", "softboiled", "slackoff", "synthesis", "moonlight",
            "morningsun", "wish", "rest", "strengthsap", "shoreup", "lifedew");
    private static final Set<String> HAZARD_MOVES = Set.of(
            "stealthrock", "spikes", "toxicspikes", "stickyweb", "ceaselessedge", "stoneaxe");
    private static final Set<String> REMOVAL_MOVES = Set.of(
            "rapidspin", "defog", "mortalspin", "tidyup", "courtchange");
    private static final Set<String> PIVOT_MOVES = Set.of(
            "uturn", "voltswitch", "flipturn", "partingshot", "teleport", "chillyreception");
    private static final Set<String> SPEED_CONTROL_MOVES = Set.of(
            "thunderwave", "glare", "tailwind", "icywind", "electroweb", "trickroom");
    private static final Set<String> SETUP_MOVES = Set.of(
            "swordsdance", "nastyplot", "calmmind", "dragondance", "quiverdance", "shellsmash",
            "bulkup", "coil", "agility", "rockpolish", "shiftgear", "bellydrum", "tidyup");
    private static final int BEAM_WIDTH = 6;
    private static final int BEAM_BRANCHING = 6;
    private static final int THREAT_COUNT = 18;
    private final ExecutorService executor = Executors.newFixedThreadPool(3, runnable -> {
        Thread thread = new Thread(runnable, "Tropimon-Team-Helper");
        thread.setDaemon(true);
        return thread;
    });
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final Map<String, CachedResponse> cache = new LinkedHashMap<>();
    private boolean diskCacheLoaded;

    private RankedUsageService() {
    }

    UsageIndex cachedUsageIndex(String requestedSeason) {
        ensureDiskCacheLoaded();
        try {
            Season[] rawSeasons = cachedValue("/seasons", Season[].class, true);
            if (rawSeasons == null || rawSeasons.length == 0) return null;
            List<SeasonInfo> seasons = new ArrayList<>();
            for (Season season : rawSeasons) {
                if (season != null && season.name != null && !season.name.isBlank()) {
                    seasons.add(new SeasonInfo(season.name, season.active));
                }
            }
            if (seasons.isEmpty()) return null;
            String season = resolveSeason(requestedSeason, seasons);
            String path = speciesListPath(season);
            UsageEntry[] entries = cachedValue(path, UsageEntry[].class, true);
            if (entries == null) return null;
            return new UsageIndex(season, List.copyOf(seasons), normalizedUsageEntries(entries));
        } catch (RuntimeException exception) {
            TropimonTeamSaverClient.LOGGER.debug("Cache Ranked local ignoré car invalide", exception);
            return null;
        }
    }

    CompletableFuture<UsageIndex> loadUsageIndex(String requestedSeason) {
        return supplyCancellable(() -> {
            List<SeasonInfo> seasons = seasons();
            String season = resolveSeason(requestedSeason, seasons);
            return new UsageIndex(season, seasons, speciesList(season));
        });
    }

    CompletableFuture<UsageIndex> refreshUsageIndex(String requestedSeason) {
        return supplyCancellable(() -> {
            List<SeasonInfo> seasons = seasons(true);
            String season = resolveSeason(requestedSeason, seasons);
            return new UsageIndex(season, seasons, speciesList(season, true));
        });
    }

    CompletableFuture<Map<String, Double>> loadMoveUsage(String requestedSeason, String requestedPokemon) {
        String pokemon = key(requestedPokemon);
        if (pokemon.isBlank()) return CompletableFuture.completedFuture(Map.of());
        return supplyCancellable(() -> {
            List<SeasonInfo> availableSeasons = seasons();
            String season = resolveSeason(requestedSeason, availableSeasons);
            UsageEntry match = speciesList(season).stream()
                    .filter(entry -> key(entry.name).equals(pokemon))
                    .findFirst().orElse(null);
            if (match == null) return Map.<String, Double>of();
            SpeciesStats stats = speciesDetails(season, match.name);
            Map<String, Double> normalized = new LinkedHashMap<>();
            stats.moves.forEach((move, usage) -> {
                String moveKey = key(move);
                if (!moveKey.isBlank() && usage != null) normalized.put(moveKey, usage);
            });
            return Map.copyOf(normalized);
        }).orTimeout(10, TimeUnit.SECONDS);
    }

    CompletableFuture<Recommendation> recommend(List<String> existingShowdownIds,
                                                 Map<String, CandidateProfile> candidateProfiles,
                                                 int maximumSize, String requestedSeason,
                                                 long variation, BuildStyle style, GenerationMode generationMode) {
        List<String> existing = existingShowdownIds == null ? List.of() : List.copyOf(existingShowdownIds);
        Map<String, CandidateProfile> profiles = normalizeProfiles(candidateProfiles);
        BuildStyle selectedStyle = style == null ? BuildStyle.BALANCED : style;
        GenerationMode selectedMode = generationMode == null ? GenerationMode.MIXED : generationMode;
        return supplyCancellable(
                () -> recommendBlocking(existing, profiles, maximumSize, requestedSeason, variation,
                        selectedStyle, selectedMode)).orTimeout(15, TimeUnit.SECONDS);
    }

    private <T> CompletableFuture<T> supplyCancellable(Supplier<T> supplier) {
        CompletableFuture<T> result = new CompletableFuture<>();
        Future<?> task = executor.submit(() -> {
            try {
                checkInterrupted();
                result.complete(supplier.get());
            } catch (Throwable error) {
                result.completeExceptionally(error);
            }
        });
        result.whenComplete((value, error) -> {
            if (result.isCancelled() || error instanceof TimeoutException) task.cancel(true);
        });
        return result;
    }

    private Recommendation recommendBlocking(List<String> existingShowdownIds,
                                              Map<String, CandidateProfile> profiles, int maximumSize,
                                              String requestedSeason, long variation, BuildStyle style,
                                              GenerationMode generationMode) {
        String season = resolveSeason(requestedSeason, seasons());
        Map<String, CandidateProfile> catalogueProfiles = profiles;
        List<UsageEntry> usage = speciesList(season).stream()
                .filter(entry -> catalogueProfiles.isEmpty() || catalogueProfiles.containsKey(key(entry.name)))
                .toList();
        if (usage.isEmpty()) throw new RankedException("No compatible species in Ranked Tropimon data");
        ShowdownGen9SetService.INSTANCE.ensureLoaded();
        Map<String, CandidateProfile> competitiveProfiles = competitiveProfiles(catalogueProfiles, style);

        Map<String, UsageEntry> usageByKey = new LinkedHashMap<>();
        for (UsageEntry entry : usage) usageByKey.putIfAbsent(key(entry.name), entry);
        Set<String> meta = metaKeys(usage);

        LinkedHashSet<String> selected = new LinkedHashSet<>();
        for (String raw : existingShowdownIds) {
            String normalized = key(raw);
            if (!normalized.isBlank() && (competitiveProfiles.containsKey(normalized)
                    || usageByKey.containsKey(normalized))) {
                selected.add(normalized);
            }
        }

        Map<String, SpeciesStats> details = new LinkedHashMap<>();
        Map<String, SpeciesStats> detailCache = new LinkedHashMap<>();
        Map<String, CandidateProfile> resolvedProfiles = new LinkedHashMap<>(competitiveProfiles);
        for (String selectedKey : selected) {
            UsageEntry entry = usageByKey.get(selectedKey);
            if (entry != null) {
                SpeciesStats stats = speciesDetails(season, entry.name);
                details.put(selectedKey, stats);
                detailCache.put(selectedKey, stats);
                CandidateProfile base = competitiveProfiles.get(selectedKey);
                if (base != null) resolvedProfiles.put(selectedKey, actualProfile(base, stats, style));
            }
        }

        int target = Math.max(0, Math.min(6, maximumSize));
        int lockedCount = selected.size();
        List<TeamBranch> beam = List.of(new TeamBranch(List.copyOf(selected), Map.copyOf(details), 0.0D));
        List<UsageEntry> threats = usage.stream().limit(THREAT_COUNT).toList();
        while (!beam.isEmpty() && beam.get(0).selected.size() < target) {
            checkInterrupted();
            List<TeamBranch> expanded = new ArrayList<>();
            for (TeamBranch branch : beam) {
                LinkedHashSet<String> chosen = new LinkedHashSet<>(branch.selected);
                List<UsageEntry> available = usage.stream()
                        .filter(entry -> !chosen.contains(key(entry.name))).toList();
                available = prioritizeForStyle(available, chosen, resolvedProfiles, style, target);
                available = prioritizeForRoles(available, chosen, resolvedProfiles, style);
                available = prioritizeForMode(available, chosen, meta, target, generationMode);
                Map<String, Integer> typeCounts = typeCounts(chosen, resolvedProfiles);
                Map<String, Integer> archetypeCounts = archetypeCounts(chosen, resolvedProfiles);
                boolean safeChoice = available.stream().anyMatch(entry ->
                        !exceedsTypeLimit(resolvedProfiles.get(key(entry.name)), typeCounts));
                SpeciesStats previous = previousDetail(branch.details, chosen);
                Comparator<UsageEntry> byPreliminaryScore = Comparator.comparingDouble(entry -> {
                    CandidateProfile candidate = resolvedProfiles.get(key(entry.name));
                    return candidateScore(entry, previous, branch.details.values(), candidate,
                            typeCounts, archetypeCounts, variation, chosen.size(), style)
                            + modeScore(entry, meta.contains(key(entry.name)), generationMode)
                            + teamRoleScore(candidate, chosen, resolvedProfiles, style)
                            + defensiveSynergyScore(candidate, chosen, resolvedProfiles);
                });
                List<UsageEntry> candidates = available.stream()
                        .filter(entry -> !safeChoice
                                || !exceedsTypeLimit(resolvedProfiles.get(key(entry.name)), typeCounts))
                        .sorted(byPreliminaryScore.reversed().thenComparingInt(entry -> entry.rank))
                        .limit(BEAM_BRANCHING).toList();
                for (UsageEntry candidate : candidates) {
                    checkInterrupted();
                    String candidateKey = key(candidate.name);
                    List<String> roster = new ArrayList<>(branch.selected);
                    roster.add(candidateKey);
                    Map<String, SpeciesStats> branchDetails = new LinkedHashMap<>(branch.details);
                    SpeciesStats cachedStats = detailCache.get(candidateKey);
                    if (cachedStats != null) branchDetails.put(candidateKey, cachedStats);
                    double score = teamScore(roster, branchDetails, usageByKey, resolvedProfiles,
                            threats, meta, style, generationMode, variation, roster.size() >= target);
                    expanded.add(new TeamBranch(List.copyOf(roster), Map.copyOf(branchDetails), score));
                }
            }
            beam = bestDistinctBranches(expanded, BEAM_WIDTH);
        }
        if (beam.isEmpty()) throw new RankedException("Unable to generate a coherent team");
        TeamBranch best = beam.get(0);
        Map<String, SpeciesStats> hydratedDetails = new LinkedHashMap<>(best.details);
        for (String selectedKey : best.selected) {
            checkInterrupted();
            UsageEntry entry = usageByKey.get(selectedKey);
            if (entry == null) continue;
            SpeciesStats stats = detailCache.computeIfAbsent(selectedKey,
                    ignored -> speciesDetails(season, entry.name));
            hydratedDetails.put(selectedKey, stats);
            CandidateProfile base = competitiveProfiles.get(selectedKey);
            if (base != null) resolvedProfiles.put(selectedKey, actualProfile(base, stats, style));
        }
        best = new TeamBranch(best.selected, Map.copyOf(hydratedDetails),
                teamScore(best.selected, hydratedDetails, usageByKey, resolvedProfiles,
                        threats, meta, style, generationMode, variation, true));
        best = repairBranch(best, lockedCount, usage, usageByKey, competitiveProfiles, resolvedProfiles,
                detailCache, threats, meta, season, style, generationMode, variation);
        List<SpeciesStats> additions = new ArrayList<>();
        for (int index = lockedCount; index < best.selected.size(); index++) {
            SpeciesStats stats = best.details.get(best.selected.get(index));
            if (stats != null) additions.add(stats);
        }
        return new Recommendation(season, orderedDetails(best), List.copyOf(additions));
    }

    private static Map<String, CandidateProfile> competitiveProfiles(Map<String, CandidateProfile> profiles,
                                                                      BuildStyle style) {
        if (profiles.isEmpty()) return profiles;
        Map<String, CandidateProfile> result = new LinkedHashMap<>();
        profiles.forEach((species, profile) -> {
            ShowdownGen9SetService.CompetitiveSet set = ShowdownGen9SetService.INSTANCE
                    .bestSet(species, style, profile.archetype);
            if (set == null) {
                result.put(species, provisionalProfile(profile, style));
                return;
            }
            Set<String> moves = new LinkedHashSet<>(set.moves);
            Set<String> abilities = set.ability == null ? Set.of() : Set.of(set.ability);
            result.put(species, new CandidateProfile(profile.types, profile.archetype, profile.baseSpeed,
                    abilities, moves).normalized());
        });
        return Map.copyOf(result);
    }

    private static CandidateProfile provisionalProfile(CandidateProfile base, BuildStyle style) {
        List<String> moves = new ArrayList<>(base.moves);
        moves.sort(Comparator.comparingDouble((String move) -> provisionalMoveScore(move, base, style)).reversed()
                .thenComparing(RankedUsageService::key));
        Set<String> selectedMoves = new LinkedHashSet<>();
        for (String move : moves) {
            selectedMoves.add(move);
            if (selectedMoves.size() >= 4) break;
        }
        String ability = base.abilities.stream().sorted(Comparator
                .comparing((String value) -> isStyleAbility(value, style)).reversed()
                .thenComparing(RankedUsageService::key)).findFirst().orElse(null);
        return new CandidateProfile(base.types, base.archetype, base.baseSpeed,
                ability == null ? Set.of() : Set.of(ability), selectedMoves).normalized();
    }

    private static double provisionalMoveScore(String rawMove, CandidateProfile profile, BuildStyle style) {
        String move = key(rawMove);
        double score = isStyleMove(move, style) ? 600.0D : 0.0D;
        if (HAZARD_MOVES.contains(move) || REMOVAL_MOVES.contains(move)) score += 260.0D;
        if (RECOVERY_MOVES.contains(move)) score += isBulky(profile) || style == BuildStyle.STALL ? 250.0D : 80.0D;
        if (PIVOT_MOVES.contains(move) || SPEED_CONTROL_MOVES.contains(move) || SETUP_MOVES.contains(move)) {
            score += 150.0D;
        }
        try {
            MoveTemplate template = Moves.getByName(move);
            if (template != null && template.getPower() > 0.0D) {
                score += Math.min(150.0D, template.getPower());
                String type = key(template.getElementalType().getName());
                if (profile.types.contains(type)) score += 70.0D;
                String category = key(template.getDamageCategory().getName());
                if (profile.archetype.equals("physical") && category.equals("physical")
                        || profile.archetype.equals("special") && category.equals("special")) score += 45.0D;
            }
        } catch (RuntimeException | LinkageError ignored) {
            // The deterministic role ordering above remains a safe pre-registry fallback.
        }
        return score;
    }

    /** Resolves one indivisible Pokémon + set profile, preferring current Tropimon data over Gen 9 Showdown. */
    private static CandidateProfile actualProfile(CandidateProfile base, SpeciesStats stats, BuildStyle style) {
        if (base == null) return null;
        if (stats != null && stats.moves != null && stats.moves.size() >= 4
                && stats.abilities != null && !stats.abilities.isEmpty()) {
            List<Map.Entry<String, Double>> rankedMoves = new ArrayList<>(stats.moves.entrySet());
            rankedMoves.sort(Comparator
                    .comparing((Map.Entry<String, Double> entry) -> isStyleMove(entry.getKey(), style)).reversed()
                    .thenComparing(Map.Entry<String, Double>::getValue, Comparator.reverseOrder()));
            Set<String> moves = new LinkedHashSet<>();
            for (Map.Entry<String, Double> entry : rankedMoves) {
                String move = key(entry.getKey());
                if (!move.isBlank()) moves.add(move);
                if (moves.size() >= 4) break;
            }
            String ability = stats.abilities.entrySet().stream()
                    .sorted(Comparator
                            .comparing((Map.Entry<String, Double> entry) -> isStyleAbility(entry.getKey(), style))
                            .reversed().thenComparing(Map.Entry<String, Double>::getValue, Comparator.reverseOrder()))
                    .map(Map.Entry::getKey).findFirst().orElse(null);
            if (moves.size() >= 4 && ability != null) {
                return new CandidateProfile(base.types, base.archetype, base.baseSpeed,
                        Set.of(ability), moves).normalized();
            }
        }
        ShowdownGen9SetService.CompetitiveSet set = ShowdownGen9SetService.INSTANCE
                .bestSet(stats == null || stats.name == null ? "" : stats.name, style, base.archetype);
        if (set == null) return base;
        return new CandidateProfile(base.types, base.archetype, base.baseSpeed,
                set.ability == null ? Set.of() : Set.of(set.ability), new LinkedHashSet<>(set.moves)).normalized();
    }

    private static List<TeamBranch> bestDistinctBranches(List<TeamBranch> branches, int maximum) {
        Map<Set<String>, TeamBranch> distinct = new LinkedHashMap<>();
        for (TeamBranch branch : branches) {
            Set<String> roster = Set.copyOf(branch.selected);
            TeamBranch previous = distinct.get(roster);
            if (previous == null || branch.score > previous.score) distinct.put(roster, branch);
        }
        return distinct.values().stream().sorted(Comparator.comparingDouble(TeamBranch::score).reversed())
                .limit(maximum).toList();
    }

    private static Map<String, SpeciesStats> orderedDetails(TeamBranch branch) {
        Map<String, SpeciesStats> result = new LinkedHashMap<>();
        for (String species : branch.selected) {
            SpeciesStats stats = branch.details.get(species);
            if (stats != null) result.put(species, stats);
        }
        return java.util.Collections.unmodifiableMap(result);
    }

    private TeamBranch repairBranch(TeamBranch initial, int lockedCount, List<UsageEntry> usage,
                                    Map<String, UsageEntry> usageByKey,
                                    Map<String, CandidateProfile> baseProfiles,
                                    Map<String, CandidateProfile> resolvedProfiles,
                                    Map<String, SpeciesStats> detailCache, List<UsageEntry> threats,
                                    Set<String> meta, String season, BuildStyle style,
                                    GenerationMode mode, long variation) {
        TeamBranch current = initial;
        for (int pass = 0; pass < 2; pass++) {
            checkInterrupted();
            LinkedHashSet<String> chosen = new LinkedHashSet<>(current.selected);
            List<UsageEntry> pool = usage.stream().filter(entry -> !chosen.contains(key(entry.name))).toList();
            pool = prioritizeForStyle(pool, chosen, resolvedProfiles, style, current.selected.size());
            pool = prioritizeForRoles(pool, chosen, resolvedProfiles, style);
            pool = prioritizeForMode(pool, chosen, meta, current.selected.size(), mode);
            pool = pool.stream().limit(12).toList();
            TeamBranch bestCandidate = current;
            UsageEntry bestEntry = null;
            String replacedKey = null;
            for (int slot = lockedCount; slot < current.selected.size(); slot++) {
                for (UsageEntry candidate : pool) {
                    checkInterrupted();
                    String candidateKey = key(candidate.name);
                    if (current.selected.contains(candidateKey)) continue;
                    List<String> roster = new ArrayList<>(current.selected);
                    roster.set(slot, candidateKey);
                    Map<String, SpeciesStats> details = new LinkedHashMap<>(current.details);
                    details.remove(current.selected.get(slot));
                    double score = teamScore(roster, details, usageByKey, resolvedProfiles, threats,
                            meta, style, mode, variation, true);
                    if (score > bestCandidate.score + 1.0D) {
                        bestCandidate = new TeamBranch(List.copyOf(roster), Map.copyOf(details), score);
                        bestEntry = candidate;
                        replacedKey = candidateKey;
                    }
                }
            }
            if (bestEntry == null || replacedKey == null) break;
            String candidateKey = replacedKey;
            UsageEntry candidate = bestEntry;
            SpeciesStats stats = detailCache.computeIfAbsent(candidateKey,
                    ignored -> speciesDetails(season, candidate.name));
            CandidateProfile base = baseProfiles.get(candidateKey);
            if (base != null) resolvedProfiles.put(candidateKey, actualProfile(base, stats, style));
            Map<String, SpeciesStats> details = new LinkedHashMap<>(bestCandidate.details);
            details.put(candidateKey, stats);
            double hydratedScore = teamScore(bestCandidate.selected, details, usageByKey, resolvedProfiles,
                    threats, meta, style, mode, variation, true);
            if (hydratedScore <= current.score + 1.0D) break;
            current = new TeamBranch(bestCandidate.selected, Map.copyOf(details), hydratedScore);
        }
        return current;
    }

    private static double teamScore(List<String> roster, Map<String, SpeciesStats> details,
                                    Map<String, UsageEntry> usageByKey,
                                    Map<String, CandidateProfile> profiles, List<UsageEntry> threats,
                                    Set<String> meta, BuildStyle style, GenerationMode mode,
                                    long variation, boolean complete) {
        double score = 0.0D;
        int metaCount = 0;
        for (int index = 0; index < roster.size(); index++) {
            String species = roster.get(index);
            UsageEntry usage = usageByKey.get(species);
            CandidateProfile profile = profiles.get(species);
            if (usage != null) score += usage.usagePercent * 0.62D + modeScore(usage, meta.contains(species), mode);
            score += styleScore(profile, style) * 0.34D;
            score += stableVariation(species, variation, index) * 0.35D;
            if (meta.contains(species)) metaCount++;
        }
        for (int first = 0; first < roster.size(); first++) {
            SpeciesStats source = details.get(roster.get(first));
            for (int second = first + 1; second < roster.size(); second++) {
                SpeciesStats partner = details.get(roster.get(second));
                score += teammateAffinity(source, roster.get(second)) * 1.35D;
                score += teammateAffinity(partner, roster.get(first)) * 1.35D;
            }
        }
        score += teamDiversityScore(roster, profiles);
        score += defensiveTeamScore(roster, profiles);
        score += offensiveCoverageScore(roster, profiles, threats);
        score += teamRoleCompletionScore(roster, profiles, style, complete);
        score += conditionalSynergyScore(roster, profiles, style);
        if (complete) {
            int desiredMeta = mode == GenerationMode.META ? 4 : mode == GenerationMode.MIXED ? 3 : 2;
            score -= Math.abs(metaCount - desiredMeta) * (mode == GenerationMode.META ? 8.0D : 5.0D);
        }
        return score;
    }

    private static double teamDiversityScore(List<String> roster, Map<String, CandidateProfile> profiles) {
        Map<String, Integer> typeCounts = typeCounts(new LinkedHashSet<>(roster), profiles);
        Map<String, Integer> archetypes = archetypeCounts(new LinkedHashSet<>(roster), profiles);
        Set<String> sharedTypes = sharedTypes(roster, profiles);
        double score = 0.0D;
        for (Map.Entry<String, Integer> entry : typeCounts.entrySet()) {
            if (sharedTypes.contains(entry.getKey())) continue;
            int count = entry.getValue();
            if (count == 1) score += 2.0D;
            else if (count > 2) score -= (count - 2) * 24.0D;
        }
        for (int count : archetypes.values()) if (count > 3) score -= (count - 3) * 9.0D;
        return score;
    }

    static Set<String> sharedTypes(List<String> roster, Map<String, CandidateProfile> profiles) {
        if (roster == null || roster.size() < 2 || profiles == null) return Set.of();
        Set<String> shared = null;
        for (String species : roster) {
            CandidateProfile profile = profiles.get(species);
            if (profile == null || profile.types.isEmpty()) return Set.of();
            if (shared == null) shared = new LinkedHashSet<>(profile.types);
            else shared.retainAll(profile.types);
            if (shared.isEmpty()) return Set.of();
        }
        return shared == null ? Set.of() : Set.copyOf(shared);
    }

    static Map<String, CandidateProfile> profilesForType(Map<String, CandidateProfile> profiles,
                                                          String requestedType) {
        if (profiles == null || profiles.isEmpty()) return Map.of();
        String type = key(requestedType);
        if (type.isBlank()) return Map.of();
        Map<String, CandidateProfile> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, CandidateProfile> entry : profiles.entrySet()) {
            CandidateProfile profile = entry.getValue();
            if (profile != null && profile.types.stream().map(RankedUsageService::key).anyMatch(type::equals)) {
                filtered.put(entry.getKey(), profile);
            }
        }
        return Map.copyOf(filtered);
    }

    static double defensiveTeamScore(List<String> roster, Map<String, CandidateProfile> profiles) {
        double score = 0.0D;
        for (String attack : TYPE_NAMES) {
            int weak = 0;
            int answers = 0;
            for (String species : roster) {
                double multiplier = defensiveMultiplier(profiles.get(species), attack);
                if (multiplier > 1.0D) weak++;
                if (multiplier < 1.0D) answers++;
            }
            if (weak >= 2 && answers == 0) score -= 24.0D + (weak - 2) * 9.0D;
            else if (weak > answers + 1) score -= (weak - answers - 1) * 8.0D;
            if (weak > 0 && answers > 0) score += Math.min(answers, 2) * 2.5D;
        }
        return score;
    }

    static double offensiveCoverageScore(List<String> roster, Map<String, CandidateProfile> profiles,
                                         List<UsageEntry> threats) {
        if (threats == null || threats.isEmpty()) return 0.0D;
        double score = 0.0D;
        for (UsageEntry threat : threats) {
            CandidateProfile defender = profiles.get(key(threat.name));
            if (defender == null || defender.types.isEmpty()) continue;
            boolean covered = false;
            for (String species : roster) {
                for (String attack : coverageTypes(profiles.get(species))) {
                    if (defensiveMultiplier(defender, attack) > 1.0D) {
                        covered = true;
                        break;
                    }
                }
                if (covered) break;
            }
            double weight = 1.0D + Math.min(2.5D, Math.max(0.0D, threat.usagePercent) / 12.0D);
            score += covered ? weight * 2.0D : -weight * 2.8D;
        }
        return score;
    }

    private static Set<String> coverageTypes(CandidateProfile profile) {
        if (profile == null) return Set.of();
        Set<String> result = new LinkedHashSet<>();
        for (String move : profile.moves) {
            try {
                MoveTemplate template = Moves.getByName(move);
                if (template != null && template.getPower() > 0.0D) {
                    result.add(key(template.getElementalType().getName()));
                }
            } catch (RuntimeException | LinkageError ignored) {
                // Cobblemon registries can still be warming up while the cached catalogue is restored.
            }
        }
        if (result.isEmpty()) result.addAll(profile.types);
        return Set.copyOf(result);
    }

    static double teamRoleCompletionScore(List<String> roster, Map<String, CandidateProfile> profiles,
                                          BuildStyle style, boolean complete) {
        Map<Role, Integer> minimums = switch (style == null ? BuildStyle.BALANCED : style) {
            case OFFENSE -> Map.of(Role.HAZARDS, 1, Role.SPEED_CONTROL, 1, Role.WIN_CONDITION, 1,
                    Role.PHYSICAL_BREAKER, 1, Role.SPECIAL_BREAKER, 1);
            case BULKY_OFFENSE -> Map.of(Role.HAZARDS, 1, Role.REMOVAL, 1, Role.PIVOT, 1,
                    Role.PHYSICAL_BREAKER, 1, Role.SPECIAL_BREAKER, 1);
            case STALL -> Map.of(Role.HAZARDS, 1, Role.REMOVAL, 1, Role.RECOVERY, 3);
            case TRICK_ROOM -> Map.of(Role.TRICK_ROOM, 2, Role.PHYSICAL_BREAKER, 1, Role.SPECIAL_BREAKER, 1);
            default -> Map.of(Role.HAZARDS, 1, Role.REMOVAL, 1, Role.SPEED_CONTROL, 1,
                    Role.PHYSICAL_BREAKER, 1, Role.SPECIAL_BREAKER, 1);
        };
        double score = 0.0D;
        for (Map.Entry<Role, Integer> requirement : minimums.entrySet()) {
            int present = roleCount(roster, profiles, requirement.getKey());
            score += Math.min(present, requirement.getValue()) * 11.0D;
            if (complete && present < requirement.getValue()) {
                score -= (requirement.getValue() - present) * 48.0D;
            }
        }
        if (style == BuildStyle.TRICK_ROOM) {
            int slow = countProfiles(new LinkedHashSet<>(roster), profiles, RankedUsageService::isSlow);
            score += Math.min(slow, 4) * 7.0D;
            if (complete && slow < 4) score -= (4 - slow) * 24.0D;
        }
        if (style == BuildStyle.RAIN || style == BuildStyle.SUN || style == BuildStyle.SAND
                || style == BuildStyle.SNOW || style == BuildStyle.AURORA_VEIL) {
            Set<String> selected = new LinkedHashSet<>(roster);
            int setters = countProfiles(selected, profiles, profile -> isStyleSetter(profile, style));
            int beneficiaries = countProfiles(selected, profiles, profile -> isWeatherBeneficiary(profile, style));
            score += Math.min(setters, 1) * 24.0D + Math.min(beneficiaries, 2) * 18.0D;
            if (complete && setters < 1) score -= 85.0D;
            if (complete && beneficiaries < 2) score -= (2 - beneficiaries) * 55.0D;
            if (style == BuildStyle.AURORA_VEIL) {
                int veil = countProfiles(selected, profiles, RankedUsageService::isAuroraVeilUser);
                score += Math.min(veil, 1) * 30.0D;
                if (complete && veil < 1) score -= 90.0D;
            }
        }
        return score;
    }

    static double conditionalSynergyScore(List<String> roster, Map<String, CandidateProfile> profiles,
                                          BuildStyle style) {
        Set<String> moves = new LinkedHashSet<>();
        int pivots = 0;
        int physical = 0;
        int setup = 0;
        boolean bulkyWithoutRecovery = false;
        for (String species : roster) {
            CandidateProfile profile = profiles.get(species);
            if (profile == null) continue;
            moves.addAll(profile.moves);
            if (hasRole(profile, Role.PIVOT)) pivots++;
            if (hasRole(profile, Role.PHYSICAL_BREAKER)) physical++;
            if (hasRole(profile, Role.WIN_CONDITION)) setup++;
            if (isBulky(profile) && !hasRole(profile, Role.RECOVERY)) bulkyWithoutRecovery = true;
        }
        double score = 0.0D;
        if (moves.contains("futuresight") && physical > 0) score += 16.0D;
        if (pivots >= 2) score += 12.0D + Math.min(2, pivots - 2) * 3.0D;
        if (intersects(moves, HAZARD_MOVES) && moves.contains("knockoff")) score += 12.0D;
        if (moves.contains("toxicspikes") && (moves.contains("hex") || moves.contains("venoshock"))) score += 13.0D;
        if (moves.contains("wish") && bulkyWithoutRecovery) score += 10.0D;
        if (style == BuildStyle.AURORA_VEIL && moves.contains("auroraveil") && setup > 0) score += 18.0D;
        return score;
    }

    private static int roleCount(List<String> roster, Map<String, CandidateProfile> profiles, Role role) {
        int count = 0;
        for (String species : roster) if (hasRole(profiles.get(species), role)) count++;
        return count;
    }

    private static void checkInterrupted() {
        if (Thread.currentThread().isInterrupted()) throw new RankedException("Ranked team generation cancelled");
    }

    private static Map<String, CandidateProfile> normalizeProfiles(Map<String, CandidateProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) return Map.of();
        Map<String, CandidateProfile> result = new LinkedHashMap<>();
        for (Map.Entry<String, CandidateProfile> entry : profiles.entrySet()) {
            String species = key(entry.getKey());
            CandidateProfile profile = entry.getValue();
            if (!species.isBlank() && profile != null) result.putIfAbsent(species, profile.normalized());
        }
        return Map.copyOf(result);
    }

    static double candidateScore(UsageEntry entry, SpeciesStats previous,
                                 Iterable<SpeciesStats> selected, CandidateProfile profile,
                                 Map<String, Integer> typeCounts,
                                 Map<String, Integer> archetypeCounts,
                                 long variation, int step) {
        return candidateScore(entry, previous, selected, profile, typeCounts, archetypeCounts,
                variation, step, BuildStyle.BALANCED);
    }

    static double candidateScore(UsageEntry entry, SpeciesStats previous,
                                 Iterable<SpeciesStats> selected, CandidateProfile profile,
                                 Map<String, Integer> typeCounts,
                                 Map<String, Integer> archetypeCounts,
                                 long variation, int step, BuildStyle style) {
        String candidate = key(entry.name);
        double directPartner = previous == null ? 0.0D : teammateAffinity(previous, candidate);
        double diversityPenalty = diversityPenalty(profile, typeCounts, archetypeCounts);
        return recommendationScore(entry, selected) + directPartner * 2.75D - diversityPenalty
                + stableVariation(candidate, variation, step) + styleScore(profile, style);
    }

    static double styleScore(CandidateProfile profile, BuildStyle style) {
        if (profile == null || style == null || style == BuildStyle.BALANCED
                || style == BuildStyle.MONOTYPE) return 0.0D;
        String archetype = key(profile.archetype);
        return switch (style) {
            case OFFENSE -> archetype.equals("fastoffense") ? 38.0D
                    : archetype.equals("physical") || archetype.equals("special") ? 24.0D : -12.0D;
            case BULKY_OFFENSE -> archetype.equals("bulky") ? 30.0D
                    : isOffensive(profile) ? 24.0D : 4.0D;
            case STALL -> isStallProfile(profile) ? 48.0D
                    : archetype.equals("bulky") ? 34.0D : profile.baseSpeed >= 100 ? -30.0D : -10.0D;
            case TRICK_ROOM -> isStyleSetter(profile, style) ? 48.0D
                    : Math.max(-35.0D, Math.min(35.0D, (75.0D - profile.baseSpeed) * 0.70D));
            case RAIN, SUN, SAND, SNOW, AURORA_VEIL -> weatherScore(profile, style);
            case BALANCED, MONOTYPE -> 0.0D;
        };
    }

    static List<UsageEntry> prioritizeForStyle(List<UsageEntry> available, Set<String> selected,
                                               Map<String, CandidateProfile> profiles,
                                               BuildStyle style, int targetSize) {
        if (available == null || available.isEmpty() || style == null) return List.of();
        Set<String> chosen = selected == null ? Set.of() : selected;
        int remaining = Math.max(0, targetSize - chosen.size());
        if (remaining == 0) return available;

        return switch (style) {
            case BALANCED, MONOTYPE -> {
                if (countProfiles(chosen, profiles, RankedUsageService::isBulky) < 1) {
                    yield prefer(available, profiles, RankedUsageService::isBulky);
                }
                if (countProfiles(chosen, profiles, RankedUsageService::isOffensive) < 2) {
                    yield prefer(available, profiles, RankedUsageService::isOffensive);
                }
                yield available;
            }
            case OFFENSE -> countProfiles(chosen, profiles, RankedUsageService::isOffensive) < 4
                    ? prefer(available, profiles, RankedUsageService::isOffensive) : available;
            case BULKY_OFFENSE -> {
                if (countProfiles(chosen, profiles, RankedUsageService::isBulky) < 2) {
                    yield prefer(available, profiles, RankedUsageService::isBulky);
                }
                if (countProfiles(chosen, profiles, RankedUsageService::isOffensive) < 3) {
                    yield prefer(available, profiles, RankedUsageService::isOffensive);
                }
                yield available;
            }
            case STALL -> {
                if (countProfiles(chosen, profiles, RankedUsageService::isStallProfile) < 3) {
                    List<UsageEntry> recovery = prefer(available, profiles, RankedUsageService::isStallProfile);
                    if (recovery != available) yield recovery;
                }
                yield countProfiles(chosen, profiles, RankedUsageService::isBulky) < 4
                        ? prefer(available, profiles, RankedUsageService::isBulky) : available;
            }
            case TRICK_ROOM -> {
                if (countProfiles(chosen, profiles, profile -> isStyleSetter(profile, style)) < 2) {
                    List<UsageEntry> setters = prefer(available, profiles,
                            profile -> isStyleSetter(profile, style));
                    if (setters != available) yield setters;
                }
                yield countProfiles(chosen, profiles, RankedUsageService::isSlow) < 4
                        ? prefer(available, profiles, RankedUsageService::isSlow) : available;
            }
            case RAIN, SUN, SAND, SNOW -> {
                if (countProfiles(chosen, profiles, profile -> isStyleSetter(profile, style)) < 1) {
                    List<UsageEntry> automaticSetters = prefer(available, profiles,
                            profile -> isAutomaticWeatherSetter(profile, style));
                    if (automaticSetters != available) yield automaticSetters;
                    List<UsageEntry> setters = prefer(available, profiles,
                            profile -> isStyleSetter(profile, style));
                    if (setters != available) yield setters;
                }
                if (countProfiles(chosen, profiles, profile -> isWeatherBeneficiary(profile, style)) < 2) {
                    List<UsageEntry> beneficiaries = prefer(available, profiles,
                            profile -> isWeatherBeneficiary(profile, style));
                    if (beneficiaries != available) yield beneficiaries;
                }
                yield countProfiles(chosen, profiles, profile -> isWeatherSynergy(profile, style)) < 4
                        ? prefer(available, profiles, profile -> isWeatherSynergy(profile, style)) : available;
            }
            case AURORA_VEIL -> {
                if (countProfiles(chosen, profiles, profile -> isStyleSetter(profile, style)) < 1) {
                    List<UsageEntry> automaticSetters = prefer(available, profiles,
                            profile -> isAutomaticWeatherSetter(profile, style));
                    if (automaticSetters != available) yield automaticSetters;
                    List<UsageEntry> setters = prefer(available, profiles,
                            profile -> isStyleSetter(profile, style));
                    if (setters != available) yield setters;
                }
                if (countProfiles(chosen, profiles, RankedUsageService::isAuroraVeilUser) < 1) {
                    List<UsageEntry> veilUsers = prefer(available, profiles,
                            RankedUsageService::isAuroraVeilUser);
                    if (veilUsers != available) yield veilUsers;
                }
                if (countProfiles(chosen, profiles, profile -> isWeatherBeneficiary(profile, style)) < 2) {
                    List<UsageEntry> beneficiaries = prefer(available, profiles,
                            profile -> isWeatherBeneficiary(profile, style));
                    if (beneficiaries != available) yield beneficiaries;
                }
                yield countProfiles(chosen, profiles, profile -> isWeatherSynergy(profile, style)) < 4
                        ? prefer(available, profiles, profile -> isWeatherSynergy(profile, style)) : available;
            }
        };
    }

    static List<UsageEntry> prioritizeForMode(List<UsageEntry> available, Set<String> selected,
                                              Set<String> meta, int targetSize,
                                              GenerationMode generationMode) {
        if (generationMode == GenerationMode.META || available == null || available.isEmpty()) return available;
        Set<String> chosen = selected == null ? Set.of() : selected;
        Set<String> metaKeys = meta == null ? Set.of() : meta;
        int metaCount = 0;
        for (String candidate : chosen) if (metaKeys.contains(candidate)) metaCount++;
        int anchors = generationMode == GenerationMode.MIXED ? 3 : 2;
        int neededMeta = Math.max(0, anchors - metaCount);
        int remaining = Math.max(0, targetSize - chosen.size());
        boolean requestMeta = neededMeta > 0 && (remaining <= neededMeta
                || generationMode == GenerationMode.MIXED && chosen.size() < 2
                || generationMode == GenerationMode.ORIGINAL && chosen.size() % 2 == 0);
        List<UsageEntry> preferred = available.stream()
                .filter(entry -> metaKeys.contains(key(entry.name)) == requestMeta)
                .toList();
        return preferred.isEmpty() ? available : preferred;
    }

    static List<UsageEntry> prioritizeForRoles(List<UsageEntry> available, Set<String> selected,
                                               Map<String, CandidateProfile> profiles, BuildStyle style) {
        if (available == null || available.isEmpty()) return List.of();
        Set<Role> present = roles(selected, profiles);
        List<Role> priorities = switch (style == null ? BuildStyle.BALANCED : style) {
            case STALL -> List.of(Role.REMOVAL, Role.RECOVERY, Role.HAZARDS, Role.PIVOT);
            case OFFENSE -> List.of(Role.SPEED_CONTROL, Role.WIN_CONDITION, Role.HAZARDS, Role.PIVOT);
            case TRICK_ROOM -> List.of(Role.TRICK_ROOM, Role.WIN_CONDITION, Role.HAZARDS, Role.REMOVAL);
            case RAIN, SUN, SAND, SNOW, AURORA_VEIL ->
                    List.of(Role.WEATHER_SETTER, Role.SPEED_CONTROL, Role.HAZARDS, Role.REMOVAL);
            default -> List.of(Role.HAZARDS, Role.REMOVAL, Role.SPEED_CONTROL, Role.WIN_CONDITION, Role.PIVOT);
        };
        for (Role wanted : priorities) {
            if (present.contains(wanted)) continue;
            List<UsageEntry> preferred = prefer(available, profiles, profile -> hasRole(profile, wanted));
            if (preferred != available) return preferred;
        }
        return available;
    }

    private static Set<String> metaKeys(List<UsageEntry> usage) {
        if (usage == null || usage.isEmpty()) return Set.of();
        int count = Math.max(12, Math.min(30, (int) Math.ceil(usage.size() * 0.12D)));
        Set<String> result = new LinkedHashSet<>();
        for (UsageEntry entry : usage) {
            String candidate = key(entry.name);
            if (!candidate.isBlank()) result.add(candidate);
            if (result.size() >= count) break;
        }
        return Set.copyOf(result);
    }

    private static double originalityScore(UsageEntry entry, boolean meta) {
        double usage = Math.max(0.0D, entry.usagePercent);
        if (meta) return 8.0D - Math.min(16.0D, usage * 0.35D);
        return Math.min(22.0D, 10.0D + Math.max(0.0D, 8.0D - usage));
    }

    private static double modeScore(UsageEntry entry, boolean meta, GenerationMode mode) {
        if (mode == null || mode == GenerationMode.META) return meta ? 14.0D : 0.0D;
        if (mode == GenerationMode.MIXED) return meta ? 8.0D : originalityScore(entry, false) * 0.35D;
        return originalityScore(entry, meta);
    }

    private static double teamRoleScore(CandidateProfile candidate, Set<String> selected,
                                        Map<String, CandidateProfile> profiles, BuildStyle style) {
        if (candidate == null) return 0.0D;
        Set<Role> present = roles(selected, profiles);
        double score = 0.0D;
        for (Role role : candidate.roles) {
            if (!present.contains(role)) score += switch (role) {
                case HAZARDS, REMOVAL -> 22.0D;
                case SPEED_CONTROL, WIN_CONDITION -> 18.0D;
                case PIVOT, RECOVERY -> 12.0D;
                case WEATHER_SETTER -> style == BuildStyle.RAIN || style == BuildStyle.SUN
                        || style == BuildStyle.SAND || style == BuildStyle.SNOW
                        || style == BuildStyle.AURORA_VEIL ? 28.0D : 3.0D;
                case TRICK_ROOM -> style == BuildStyle.TRICK_ROOM ? 32.0D : 0.0D;
                default -> 7.0D;
            };
        }
        return Math.min(42.0D, score);
    }

    private static Set<Role> roles(Set<String> selected, Map<String, CandidateProfile> profiles) {
        Set<Role> result = new LinkedHashSet<>();
        if (selected == null) return result;
        for (String species : selected) {
            CandidateProfile profile = profiles.get(species);
            if (profile != null) result.addAll(profile.roles);
        }
        return result;
    }

    private static boolean hasRole(CandidateProfile profile, Role role) {
        return profile != null && profile.roles.contains(role);
    }

    private static List<UsageEntry> prefer(List<UsageEntry> available,
                                           Map<String, CandidateProfile> profiles,
                                           java.util.function.Predicate<CandidateProfile> predicate) {
        List<UsageEntry> preferred = available.stream()
                .filter(entry -> predicate.test(profiles.get(key(entry.name))))
                .toList();
        return preferred.isEmpty() ? available : preferred;
    }

    private static int countProfiles(Set<String> selected, Map<String, CandidateProfile> profiles,
                                     java.util.function.Predicate<CandidateProfile> predicate) {
        int count = 0;
        for (String candidate : selected) if (predicate.test(profiles.get(candidate))) count++;
        return count;
    }

    private static boolean isOffensive(CandidateProfile profile) {
        if (profile == null) return false;
        String archetype = key(profile.archetype);
        return archetype.equals("fastoffense") || archetype.equals("physical") || archetype.equals("special");
    }

    private static boolean isBulky(CandidateProfile profile) {
        return profile != null && key(profile.archetype).equals("bulky");
    }

    private static boolean isStallProfile(CandidateProfile profile) {
        if (!isBulky(profile)) return false;
        for (String move : profile.moves) if (RECOVERY_MOVES.contains(move)) return true;
        return false;
    }

    private static boolean isSlow(CandidateProfile profile) {
        return profile != null && profile.baseSpeed <= 70;
    }

    private static boolean isStyleSetter(CandidateProfile profile, BuildStyle style) {
        if (profile == null) return false;
        if (style == BuildStyle.TRICK_ROOM) return profile.moves.contains("trickroom");
        return switch (style) {
            case RAIN -> profile.abilities.contains("drizzle") || profile.moves.contains("raindance");
            case SUN -> profile.abilities.contains("drought") || profile.abilities.contains("orichalcumpulse")
                    || profile.moves.contains("sunnyday");
            case SAND -> profile.abilities.contains("sandstream") || profile.abilities.contains("sandspit")
                    || profile.moves.contains("sandstorm");
            case SNOW -> profile.abilities.contains("snowwarning") || profile.moves.contains("snowscape")
                    || profile.moves.contains("hail");
            case AURORA_VEIL -> profile.abilities.contains("snowwarning") || profile.moves.contains("snowscape")
                    || profile.moves.contains("hail");
            default -> false;
        };
    }

    private static boolean isAutomaticWeatherSetter(CandidateProfile profile, BuildStyle style) {
        if (profile == null) return false;
        return switch (style) {
            case RAIN -> profile.abilities.contains("drizzle");
            case SUN -> profile.abilities.contains("drought") || profile.abilities.contains("orichalcumpulse");
            case SAND -> profile.abilities.contains("sandstream") || profile.abilities.contains("sandspit");
            case SNOW -> profile.abilities.contains("snowwarning");
            case AURORA_VEIL -> profile.abilities.contains("snowwarning");
            default -> false;
        };
    }

    private static boolean isAuroraVeilUser(CandidateProfile profile) {
        return profile != null && profile.moves.contains("auroraveil");
    }

    private static boolean isWeatherSynergy(CandidateProfile profile, BuildStyle style) {
        if (profile == null) return false;
        if (isStyleSetter(profile, style)) return true;
        return switch (style) {
            case RAIN -> intersects(profile.types, Set.of("water", "electric"))
                    || intersects(profile.abilities, Set.of("swiftswim", "raindish", "hydration", "dryskin"));
            case SUN -> intersects(profile.types, Set.of("fire", "grass"))
                    || intersects(profile.abilities, Set.of("chlorophyll", "solarpower", "protosynthesis"));
            case SAND -> intersects(profile.types, Set.of("rock", "ground", "steel"))
                    || intersects(profile.abilities, Set.of("sandrush", "sandforce", "sandveil"));
            case SNOW -> profile.types.contains("ice")
                    || intersects(profile.abilities, Set.of("slushrush", "icebody", "snowcloak"));
            case AURORA_VEIL -> profile.types.contains("ice") || isAuroraVeilUser(profile)
                    || intersects(profile.abilities, Set.of("slushrush", "icebody", "snowcloak"));
            default -> false;
        };
    }

    private static boolean isWeatherBeneficiary(CandidateProfile profile, BuildStyle style) {
        if (profile == null || isStyleSetter(profile, style)) return false;
        return switch (style) {
            case RAIN -> profile.types.contains("water")
                    || intersects(profile.abilities, Set.of("swiftswim", "raindish", "hydration", "dryskin"))
                    || profile.moves.contains("thunder") || profile.moves.contains("hurricane");
            case SUN -> profile.types.contains("fire")
                    || intersects(profile.abilities, Set.of("chlorophyll", "solarpower", "protosynthesis"))
                    || profile.moves.contains("solarbeam") || profile.moves.contains("solarblade");
            case SAND -> profile.types.contains("rock")
                    || intersects(profile.abilities, Set.of("sandrush", "sandforce", "sandveil"));
            case SNOW, AURORA_VEIL -> profile.types.contains("ice")
                    || intersects(profile.abilities, Set.of("slushrush", "icebody", "snowcloak"));
            default -> false;
        };
    }

    static boolean isStyleAbility(String rawAbility, BuildStyle style) {
        String ability = key(rawAbility);
        if (ability.isBlank() || style == null) return false;
        return switch (style) {
            case RAIN -> Set.of("drizzle", "swiftswim", "raindish", "hydration", "dryskin").contains(ability);
            case SUN -> Set.of("drought", "orichalcumpulse", "chlorophyll", "solarpower", "protosynthesis")
                    .contains(ability);
            case SAND -> Set.of("sandstream", "sandspit", "sandrush", "sandforce", "sandveil").contains(ability);
            case SNOW -> Set.of("snowwarning", "slushrush", "icebody", "snowcloak").contains(ability);
            case AURORA_VEIL -> Set.of("snowwarning", "slushrush", "icebody", "snowcloak")
                    .contains(ability);
            case STALL -> Set.of("regenerator", "poisonheal", "unaware", "magicguard", "multiscale")
                    .contains(ability);
            default -> false;
        };
    }

    static boolean isStyleMove(String rawMove, BuildStyle style) {
        String move = key(rawMove);
        if (move.isBlank() || style == null) return false;
        return switch (style) {
            case TRICK_ROOM -> move.equals("trickroom");
            case RAIN -> move.equals("raindance");
            case SUN -> move.equals("sunnyday");
            case SAND -> move.equals("sandstorm");
            case SNOW -> move.equals("snowscape") || move.equals("hail");
            case AURORA_VEIL -> move.equals("auroraveil") || move.equals("snowscape") || move.equals("hail");
            case STALL -> RECOVERY_MOVES.contains(move);
            default -> false;
        };
    }

    private static boolean intersects(Set<String> values, Set<String> expected) {
        for (String value : values) if (expected.contains(value)) return true;
        return false;
    }

    private static double weatherScore(CandidateProfile profile, BuildStyle style) {
        if (isAutomaticWeatherSetter(profile, style)) return 60.0D;
        if (isStyleSetter(profile, style)) return 45.0D;
        if (style == BuildStyle.AURORA_VEIL && isAuroraVeilUser(profile)) return 42.0D;
        return isWeatherSynergy(profile, style) ? 30.0D : -18.0D;
    }

    private static double teammateAffinity(SpeciesStats source, String candidate) {
        if (source == null || source.teammates == null) return 0.0D;
        for (Map.Entry<String, Double> teammate : source.teammates.entrySet()) {
            if (key(teammate.getKey()).equals(candidate)) return teammate.getValue();
        }
        return 0.0D;
    }

    private static Map<String, Integer> typeCounts(Set<String> selected,
                                                   Map<String, CandidateProfile> profiles) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String species : selected) {
            CandidateProfile profile = profiles.get(species);
            if (profile == null) continue;
            for (String type : profile.types) counts.merge(type, 1, Integer::sum);
        }
        return counts;
    }

    private static Map<String, Integer> archetypeCounts(Set<String> selected,
                                                        Map<String, CandidateProfile> profiles) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String species : selected) {
            CandidateProfile profile = profiles.get(species);
            if (profile != null && !profile.archetype.isBlank()) counts.merge(profile.archetype, 1, Integer::sum);
        }
        return counts;
    }

    static boolean exceedsTypeLimit(CandidateProfile profile, Map<String, Integer> counts) {
        if (profile == null) return false;
        for (String type : profile.types) if (counts.getOrDefault(type, 0) >= 2) return true;
        return false;
    }

    private static double diversityPenalty(CandidateProfile profile, Map<String, Integer> typeCounts,
                                           Map<String, Integer> archetypeCounts) {
        if (profile == null) return 0.0D;
        double penalty = 0.0D;
        for (String type : profile.types) penalty += typeCounts.getOrDefault(type, 0) * 10.0D;
        if (!profile.archetype.isBlank()) {
            penalty += archetypeCounts.getOrDefault(profile.archetype, 0) * 4.0D;
        }
        return penalty;
    }

    static double defensiveSynergyScore(CandidateProfile candidate, Set<String> selected,
                                        Map<String, CandidateProfile> profiles) {
        if (candidate == null || candidate.types.isEmpty() || selected == null || selected.isEmpty()) return 0.0D;
        double score = 0.0D;
        for (String attackType : TYPE_NAMES) {
            int weak = 0;
            int resist = 0;
            for (String species : selected) {
                CandidateProfile current = profiles.get(species);
                double multiplier = defensiveMultiplier(current, attackType);
                if (multiplier > 1.0D) weak++;
                else if (multiplier < 1.0D) resist++;
            }
            double candidateMultiplier = defensiveMultiplier(candidate, attackType);
            int pressure = weak - resist;
            if (pressure >= 2) {
                if (candidateMultiplier < 1.0D) score += 13.0D + pressure * 3.0D;
                else if (candidateMultiplier > 1.0D) score -= 18.0D + pressure * 5.0D;
            } else if (weak >= 1 && candidateMultiplier < 1.0D) {
                score += 5.0D;
            }
        }
        return Math.max(-55.0D, Math.min(55.0D, score));
    }

    private static double defensiveMultiplier(CandidateProfile profile, String attackType) {
        if (profile == null || profile.types.isEmpty()) return 1.0D;
        double multiplier = 1.0D;
        for (String defendType : profile.types) multiplier *= typeEffectiveness(attackType, defendType);
        return multiplier;
    }

    private static double typeEffectiveness(String attackType, String defendType) {
        String attack = key(attackType);
        String defend = key(defendType);
        if (IMMUNITIES.getOrDefault(attack, Set.of()).contains(defend)) return 0.0D;
        if (SUPER_EFFECTIVE.getOrDefault(attack, Set.of()).contains(defend)) return 2.0D;
        if (RESISTED.getOrDefault(attack, Set.of()).contains(defend)) return 0.5D;
        return 1.0D;
    }

    private static final Set<String> TYPE_NAMES = Set.of(
            "normal", "fire", "water", "electric", "grass", "ice", "fighting", "poison", "ground",
            "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark", "steel", "fairy");
    private static final Map<String, Set<String>> SUPER_EFFECTIVE = Map.ofEntries(
            Map.entry("fire", Set.of("grass", "ice", "bug", "steel")),
            Map.entry("water", Set.of("fire", "ground", "rock")),
            Map.entry("electric", Set.of("water", "flying")),
            Map.entry("grass", Set.of("water", "ground", "rock")),
            Map.entry("ice", Set.of("grass", "ground", "flying", "dragon")),
            Map.entry("fighting", Set.of("normal", "ice", "rock", "dark", "steel")),
            Map.entry("poison", Set.of("grass", "fairy")),
            Map.entry("ground", Set.of("fire", "electric", "poison", "rock", "steel")),
            Map.entry("flying", Set.of("grass", "fighting", "bug")),
            Map.entry("psychic", Set.of("fighting", "poison")),
            Map.entry("bug", Set.of("grass", "psychic", "dark")),
            Map.entry("rock", Set.of("fire", "ice", "flying", "bug")),
            Map.entry("ghost", Set.of("psychic", "ghost")),
            Map.entry("dragon", Set.of("dragon")),
            Map.entry("dark", Set.of("psychic", "ghost")),
            Map.entry("steel", Set.of("ice", "rock", "fairy")),
            Map.entry("fairy", Set.of("fighting", "dragon", "dark")));
    private static final Map<String, Set<String>> RESISTED = Map.ofEntries(
            Map.entry("normal", Set.of("rock", "steel")),
            Map.entry("fire", Set.of("fire", "water", "rock", "dragon")),
            Map.entry("water", Set.of("water", "grass", "dragon")),
            Map.entry("electric", Set.of("electric", "grass", "dragon")),
            Map.entry("grass", Set.of("fire", "grass", "poison", "flying", "bug", "dragon", "steel")),
            Map.entry("ice", Set.of("fire", "water", "ice", "steel")),
            Map.entry("fighting", Set.of("poison", "flying", "psychic", "bug", "fairy")),
            Map.entry("poison", Set.of("poison", "ground", "rock", "ghost")),
            Map.entry("ground", Set.of("grass", "bug")),
            Map.entry("flying", Set.of("electric", "rock", "steel")),
            Map.entry("psychic", Set.of("psychic", "steel")),
            Map.entry("bug", Set.of("fire", "fighting", "poison", "flying", "ghost", "steel", "fairy")),
            Map.entry("rock", Set.of("fighting", "ground", "steel")),
            Map.entry("ghost", Set.of("dark")),
            Map.entry("dragon", Set.of("steel")),
            Map.entry("dark", Set.of("fighting", "dark", "fairy")),
            Map.entry("steel", Set.of("fire", "water", "electric", "steel")),
            Map.entry("fairy", Set.of("fire", "poison", "steel")));
    private static final Map<String, Set<String>> IMMUNITIES = Map.ofEntries(
            Map.entry("normal", Set.of("ghost")),
            Map.entry("electric", Set.of("ground")),
            Map.entry("fighting", Set.of("ghost")),
            Map.entry("poison", Set.of("steel")),
            Map.entry("ground", Set.of("flying")),
            Map.entry("psychic", Set.of("dark")),
            Map.entry("ghost", Set.of("normal")),
            Map.entry("dragon", Set.of("fairy")));

    private static double stableVariation(String candidate, long variation, int step) {
        long mixed = candidate.hashCode() * 0x9E3779B97F4A7C15L
                ^ variation * 0xBF58476D1CE4E5B9L ^ (long) step * 0x94D049BB133111EBL;
        mixed ^= mixed >>> 30;
        return ((mixed & 1023L) / 1023.0D - 0.5D) * 3.0D;
    }

    private static String last(LinkedHashSet<String> values) {
        String last = null;
        for (String value : values) last = value;
        return last;
    }

    static SpeciesStats previousDetail(Map<String, SpeciesStats> details, LinkedHashSet<String> selected) {
        String previousKey = last(selected);
        return previousKey == null || details == null ? null : details.get(previousKey);
    }

    static double recommendationScore(UsageEntry entry, Iterable<SpeciesStats> selected) {
        double score = entry.usagePercent * 0.75D + Math.max(-10.0D, entry.winRate - 50.0D) * 0.10D;
        int partners = 0;
        double affinity = 0.0D;
        String candidate = key(entry.name);
        for (SpeciesStats stats : selected) {
            partners++;
            for (Map.Entry<String, Double> teammate : stats.teammates.entrySet()) {
                if (key(teammate.getKey()).equals(candidate)) affinity += teammate.getValue();
            }
        }
        if (partners > 0) score += affinity * 1.6D / partners;
        return score;
    }

    private List<SeasonInfo> seasons() {
        return seasons(false);
    }

    private List<SeasonInfo> seasons(boolean forceNetwork) {
        Season[] seasons = get("/seasons", Season[].class, forceNetwork);
        if (seasons == null || seasons.length == 0) throw new RankedException("No Ranked Tropimon season available");
        List<SeasonInfo> result = new ArrayList<>();
        for (Season season : seasons) {
            if (season != null && season.name != null && !season.name.isBlank()) {
                result.add(new SeasonInfo(season.name, season.active));
            }
        }
        if (result.isEmpty()) throw new RankedException("No Ranked Tropimon season available");
        return List.copyOf(result);
    }

    private String resolveSeason(String requestedSeason, List<SeasonInfo> seasons) {
        if (requestedSeason != null && !requestedSeason.isBlank()) {
            for (SeasonInfo season : seasons) {
                if (season.name.equalsIgnoreCase(requestedSeason)) return season.name;
            }
        }
        for (SeasonInfo season : seasons) if (season.active) return season.name;
        return seasons.get(0).name;
    }

    private List<UsageEntry> speciesList(String season) {
        return speciesList(season, false);
    }

    private List<UsageEntry> speciesList(String season, boolean forceNetwork) {
        String path = speciesListPath(season);
        UsageEntry[] entries = get(path, UsageEntry[].class, forceNetwork);
        return normalizedUsageEntries(entries);
    }

    private static String speciesListPath(String season) {
        return "/species-list?season=" + encode(season) + "&format=SINGLES&tier=all";
    }

    private static List<UsageEntry> normalizedUsageEntries(UsageEntry[] entries) {
        if (entries == null) return List.of();
        List<UsageEntry> result = new ArrayList<>(List.of(entries));
        result.removeIf(entry -> entry == null || entry.name == null || entry.name.isBlank());
        result.sort(Comparator.comparingDouble((UsageEntry entry) -> entry.usagePercent).reversed()
                .thenComparingInt(entry -> entry.rank)
                .thenComparing(entry -> entry.name, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(result);
    }

    private SpeciesStats speciesDetails(String season, String name) {
        String path = "/species?season=" + encode(season) + "&format=SINGLES&tier=all&name=" + encode(name);
        SpeciesStats stats = cachedValue(path, SpeciesStats.class, true);
        if (stats == null) stats = get(path, SpeciesStats.class);
        if (stats == null || stats.name == null) throw new RankedException("Missing statistics for " + name);
        stats.normalize();
        return stats;
    }

    private synchronized String cached(String path) {
        ensureDiskCacheLoaded();
        CachedResponse response = cache.get(path);
        return response != null && System.currentTimeMillis() - response.savedAt < CACHE_MS ? response.body : null;
    }

    private synchronized void cache(String path, String body) {
        ensureDiskCacheLoaded();
        cache.put(path, new CachedResponse(body, System.currentTimeMillis()));
        persistDiskCache();
    }

    private synchronized <T> T cachedValue(String path, Class<T> type, boolean allowStale) {
        ensureDiskCacheLoaded();
        CachedResponse response = cache.get(path);
        if (response == null) return null;
        long age = System.currentTimeMillis() - response.savedAt;
        if (age > DISK_CACHE_MAX_MS || !allowStale && age > CACHE_MS) return null;
        return TeamJson.GSON.fromJson(response.body, type);
    }

    private synchronized void loadDiskCache() {
        Path diskCache = diskCachePath();
        if (!Files.isRegularFile(diskCache)) return;
        try {
            DiskCache stored = TeamJson.GSON.fromJson(Files.readString(diskCache, StandardCharsets.UTF_8), DiskCache.class);
            if (stored == null || stored.responses == null) return;
            long now = System.currentTimeMillis();
            stored.responses.forEach((path, response) -> {
                if (path != null && response != null && response.body != null
                        && now - response.savedAt <= DISK_CACHE_MAX_MS) cache.put(path, response);
            });
        } catch (IOException | RuntimeException exception) {
            TropimonTeamSaverClient.LOGGER.warn("Impossible de lire le cache Ranked local", exception);
        }
    }

    private synchronized void persistDiskCache() {
        Path diskCache = diskCachePath();
        try {
            Files.createDirectories(diskCache.getParent());
            Path temporary = diskCache.resolveSibling(diskCache.getFileName() + ".tmp");
            Files.writeString(temporary, TeamJson.GSON.toJson(new DiskCache(cache)), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, diskCache, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException unsupportedAtomicMove) {
                Files.move(temporary, diskCache, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            TropimonTeamSaverClient.LOGGER.warn("Impossible d'écrire le cache Ranked local", exception);
        }
    }

    private synchronized void ensureDiskCacheLoaded() {
        if (diskCacheLoaded) return;
        diskCacheLoaded = true;
        loadDiskCache();
    }

    private Path diskCachePath() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("tropimon-team-manager").resolve("ranked-cache.json");
    }

    private <T> T get(String path, Class<T> type) {
        return get(path, type, false);
    }

    private <T> T get(String path, Class<T> type, boolean forceNetwork) {
        try {
            String body = forceNetwork ? null : cached(path);
            if (body == null) {
                HttpRequest request = HttpRequest.newBuilder(URI.create(API + path))
                        .timeout(Duration.ofSeconds(10))
                        .header("Accept", "application/json")
                        .header("User-Agent", "TropimonTeamBuilder")
                        .GET().build();
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new RankedException("Ranked Tropimon API returned HTTP " + response.statusCode());
                }
                body = response.body();
                cache(path, body);
            }
            return TeamJson.GSON.fromJson(body, type);
        } catch (IOException exception) {
            throw new RankedException("Unable to reach Ranked Tropimon", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RankedException("Ranked Tropimon request interrupted", exception);
        } catch (IllegalArgumentException | JsonSyntaxException exception) {
            throw new RankedException("Invalid Ranked Tropimon response", exception);
        }
    }

    static String key(String raw) {
        if (raw == null) return "";
        String lower = Normalizer.normalize(raw.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        StringBuilder result = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char character = lower.charAt(i);
            if (character >= 'a' && character <= 'z' || character >= '0' && character <= '9') {
                result.append(character);
            }
        }
        return result.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    record UsageIndex(String season, List<SeasonInfo> seasons, List<UsageEntry> entries) {
    }

    record SeasonInfo(String name, boolean active) {
    }

    enum BuildStyle {
        BALANCED, OFFENSE, BULKY_OFFENSE, STALL, TRICK_ROOM, RAIN, SUN, SAND, SNOW, AURORA_VEIL,
        MONOTYPE
    }

    enum GenerationMode {
        META, MIXED, ORIGINAL
    }

    enum Role {
        HAZARDS, REMOVAL, PIVOT, SPEED_CONTROL, WIN_CONDITION, RECOVERY,
        PHYSICAL_BREAKER, SPECIAL_BREAKER, WEATHER_SETTER, TRICK_ROOM
    }

    record CandidateProfile(Set<String> types, String archetype, int baseSpeed,
                            Set<String> abilities, Set<String> moves, Set<Role> roles) {
        CandidateProfile(Set<String> types, String archetype) {
            this(types, archetype, 0, Set.of(), Set.of(), Set.of());
        }

        CandidateProfile(Set<String> types, String archetype, int baseSpeed) {
            this(types, archetype, baseSpeed, Set.of(), Set.of(), Set.of());
        }

        CandidateProfile(Set<String> types, String archetype, int baseSpeed,
                         Set<String> abilities, Set<String> moves) {
            this(types, archetype, baseSpeed, abilities, moves, Set.of());
        }

        CandidateProfile {
            types = types == null ? Set.of() : Set.copyOf(types);
            archetype = archetype == null ? "" : archetype;
            baseSpeed = Math.max(0, baseSpeed);
            abilities = abilities == null ? Set.of() : Set.copyOf(abilities);
            moves = moves == null ? Set.of() : Set.copyOf(moves);
            roles = roles == null ? Set.of() : Set.copyOf(roles);
        }

        CandidateProfile normalized() {
            Set<String> normalizedTypes = new LinkedHashSet<>();
            for (String type : types) {
                String normalized = key(type);
                if (!normalized.isBlank()) normalizedTypes.add(normalized);
            }
            Set<String> normalizedAbilities = normalizeTokens(abilities);
            Set<String> normalizedMoves = normalizeTokens(moves);
            Set<Role> normalizedRoles = new LinkedHashSet<>(roles);
            normalizedRoles.addAll(detectRoles(key(archetype), baseSpeed, normalizedAbilities, normalizedMoves));
            return new CandidateProfile(normalizedTypes, key(archetype), baseSpeed,
                    normalizedAbilities, normalizedMoves, Set.copyOf(normalizedRoles));
        }

        private static Set<Role> detectRoles(String archetype, int speed, Set<String> abilities, Set<String> moves) {
            Set<Role> result = new LinkedHashSet<>();
            if (intersects(moves, HAZARD_MOVES)) result.add(Role.HAZARDS);
            if (intersects(moves, REMOVAL_MOVES)) result.add(Role.REMOVAL);
            if (intersects(moves, PIVOT_MOVES)) result.add(Role.PIVOT);
            if (speed >= 105 || intersects(moves, SPEED_CONTROL_MOVES)) result.add(Role.SPEED_CONTROL);
            if (intersects(moves, SETUP_MOVES)) result.add(Role.WIN_CONDITION);
            if (intersects(moves, RECOVERY_MOVES)) result.add(Role.RECOVERY);
            if (archetype.equals("physical") || archetype.equals("fastoffense")) result.add(Role.PHYSICAL_BREAKER);
            if (archetype.equals("special") || archetype.equals("fastoffense")) result.add(Role.SPECIAL_BREAKER);
            if (intersects(abilities, Set.of("drizzle", "drought", "orichalcumpulse", "sandstream",
                    "sandspit", "snowwarning")) || intersects(moves, Set.of("raindance", "sunnyday",
                    "sandstorm", "snowscape", "hail"))) result.add(Role.WEATHER_SETTER);
            if (moves.contains("trickroom")) result.add(Role.TRICK_ROOM);
            return result;
        }

        private static Set<String> normalizeTokens(Set<String> values) {
            Set<String> normalized = new LinkedHashSet<>();
            for (String value : values) {
                String token = key(value);
                if (!token.isBlank()) normalized.add(token);
            }
            return Set.copyOf(normalized);
        }
    }

    private static final class Season {
        String name;
        boolean active;
    }

    static final class UsageEntry {
        String name;
        double usagePercent;
        int count;
        double winRate;
        int rank;
    }

    static final class SpeciesStats {
        String name;
        double usagePercent;
        int count;
        double winRate;
        Map<String, Double> abilities = new LinkedHashMap<>();
        Map<String, Double> items = new LinkedHashMap<>();
        Map<String, Double> moves = new LinkedHashMap<>();
        Map<String, Double> teammates = new LinkedHashMap<>();
        Map<String, Double> spreads = new LinkedHashMap<>();
        Map<String, Double> natures = new LinkedHashMap<>();

        private void normalize() {
            if (abilities == null) abilities = new LinkedHashMap<>();
            if (items == null) items = new LinkedHashMap<>();
            if (moves == null) moves = new LinkedHashMap<>();
            if (teammates == null) teammates = new LinkedHashMap<>();
            if (spreads == null) spreads = new LinkedHashMap<>();
            if (natures == null) natures = new LinkedHashMap<>();
        }

        String mostUsedNature() { return mostUsed(natures); }
        String mostUsedSpread() { return mostUsed(spreads); }

        private static String mostUsed(Map<String, Double> values) {
            return values.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        }
    }

    record Recommendation(String season, Map<String, SpeciesStats> details, List<SpeciesStats> additions) {
    }

    private record TeamBranch(List<String> selected, Map<String, SpeciesStats> details, double score) {
    }

    private record CachedResponse(String body, long savedAt) {
    }

    private static final class DiskCache {
        Map<String, CachedResponse> responses = new LinkedHashMap<>();

        DiskCache() {
        }

        DiskCache(Map<String, CachedResponse> responses) {
            this.responses = new LinkedHashMap<>(responses);
        }
    }

    static final class RankedException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        RankedException(String message) { super(message); }
        RankedException(String message, Throwable cause) { super(message, cause); }
    }
}
