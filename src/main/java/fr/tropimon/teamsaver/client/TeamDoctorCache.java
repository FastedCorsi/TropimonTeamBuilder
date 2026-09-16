package fr.tropimon.teamsaver.client;

import fr.tropimon.teamsaver.model.TeamModels.SavedTeam;
import java.util.List;
import java.util.function.Supplier;

/** Separate caches for source members and analysis: changing style does not rebuild move metadata. */
final class TeamDoctorCache {
    private final ReadModelCache<Input, List<TeamDoctor.Member>> members = new ReadModelCache<>();
    private final ReadModelCache<AnalysisKey, List<TeamDoctor.Finding>> results = new ReadModelCache<>();

    List<TeamDoctor.Finding> analyze(Input input, RankedUsageService.BuildStyle style,
                                   Supplier<List<TeamDoctor.Member>> buildMembers) {
        return results.get(new AnalysisKey(input, style), () -> TeamDoctor.analyze(
                members.get(input, () -> List.copyOf(buildMembers.get())), style));
    }

    record Input(List<TeamReadKey.Slot> slots, List<String> displayNames, long catalogue, String language) {
        Input {
            slots = List.copyOf(slots);
            displayNames = List.copyOf(displayNames);
        }
        static Input capture(SavedTeam team, List<String> names, long catalogue, String language) {
            return new Input(team.slots.stream().map(TeamReadKey.Slot::capture).toList(), names, catalogue, language);
        }
    }

    private record AnalysisKey(Input input, RankedUsageService.BuildStyle style) { }
}
