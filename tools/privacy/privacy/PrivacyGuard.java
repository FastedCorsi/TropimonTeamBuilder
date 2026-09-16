package privacy;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Build-time guard only. No dependency on Minecraft, other mods or personal dictionaries. */
public final class PrivacyGuard {
    private static final String ATTRIBUTION = "By FastedCorsi";
    private static final int ENTRY_LIMIT = 16 * 1024 * 1024;
    private static final int ARCHIVE_LIMIT = 128 * 1024 * 1024;
    private static final Set<String> PRIVATE_DIRS = Set.of(
            ".git", "logs", "crash-reports", "screenshots", "saves", "sessions", "private");
    private static final Set<String> GENERATED_DIRS = Set.of("build", ".gradle", "run", "out", ".idea");
    private static final Pattern PERSONAL_PATH = Pattern.compile(
            "(?i)(?:[a-z]:[\\\\/]+Users[\\\\/]+[^\\\\/\\s\"'<>]+|/(?:Users|home)/[^/\\s\"'<>]+)");
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern SECRET = Pattern.compile(
            "(?:gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}|sk-(?:proj-)?[A-Za-z0-9_-]{30,}"
            + "|AKIA[A-Z0-9]{16}|-----BEGIN (?:RSA |OPENSSH |EC )?PRIVATE KEY-----"
            + "|eyJ[A-Za-z0-9_-]{12,}\\.[A-Za-z0-9_-]{12,}\\.[A-Za-z0-9_-]{12,})");
    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile(
            "(?i)(?:access[_-]?token|api[_-]?key|password|client[_-]?secret)\\s*[\"']?\\s*[:=]\\s*[\"']([A-Za-z0-9_+/=-]{16,})[\"']");
    private static final Pattern AUTHORS = Pattern.compile("\"authors\"\\s*:\\s*\\[\\s*\"By FastedCorsi\"\\s*]");
    private final List<String> privateTerms;
    private final Set<String> findings = new LinkedHashSet<>();
    private int files;
    private int archiveEntries;
    private int reviewedFixtures;
    private long expandedArchiveBytes;

    private PrivacyGuard(List<String> privateTerms) {
        this.privateTerms = privateTerms.stream().filter(s -> s != null && s.strip().length() >= 3)
                .map(s -> s.strip().toLowerCase(Locale.ROOT)).distinct().toList();
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("self-test")) { selfTest(); return; }
        if (args.length < 2) throw new IllegalArgumentException("Expected sources <root> or artifacts <jar...>");
        List<String> terms = new ArrayList<>(Arrays.asList(
                System.getenv().getOrDefault("TROPIMON_PRIVACY_TERMS", "").split("[;\\r\\n]")));
        String account = System.getProperty("user.name", "");
        if (!Set.of("root", "runner", "user", "admin", "administrator", "build", "jenkins", "FastedCorsi").contains(account)) {
            terms.add(account);
        }
        PrivacyGuard guard = new PrivacyGuard(terms);
        if (args[0].equals("sources")) guard.sources(Path.of(args[1]).toAbsolutePath().normalize());
        else if (args[0].equals("artifacts")) {
            for (int i = 1; i < args.length; i++) {
                Path jar = Path.of(args[i]);
                if (!Files.isRegularFile(jar)) throw new IOException("Expected artifact is missing");
                guard.archive(jar.getFileName().toString(), Files.readAllBytes(jar), 0, false, true);
            }
        } else throw new IllegalArgumentException("Unknown privacy check mode");
        guard.finish();
    }

    private void sources(Path root) throws Exception {
        List<Path> candidates;
        if (Files.exists(root.resolve(".git"))) {
            Process git = new ProcessBuilder("git", "ls-files", "--cached", "--others", "--exclude-standard", "-z")
                    .directory(root.toFile()).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            byte[] output = git.getInputStream().readAllBytes();
            if (git.waitFor() != 0) throw new IOException("Cannot enumerate source distribution");
            candidates = Arrays.stream(new String(output, StandardCharsets.UTF_8).split("\u0000"))
                    .filter(s -> !s.isBlank()).distinct().map(root::resolve).toList();
        } else {
            // Source archive fallback. Build/cache output is local, not a distribution input.
            try (var paths = Files.walk(root)) {
                candidates = paths.filter(Files::isRegularFile).filter(p -> {
                    for (Path part : root.relativize(p)) if (GENERATED_DIRS.contains(part.toString())) return false;
                    return true;
                }).toList();
            }
        }
        for (Path file : candidates) {
            if (!Files.exists(file)) continue;
            String relative = root.relativize(file).toString().replace('\\', '/');
            checkText(relative, relative, false);
            if (Files.isSymbolicLink(file) || !file.toRealPath().startsWith(root.toRealPath())) {
                finding(relative, "external-symlink"); continue;
            }
            if (forbiddenPath(relative)) { finding(relative, "private-content"); continue; }
            if (Files.size(file) > ENTRY_LIMIT) { finding(relative, "unreviewed-large-file"); continue; }
            byte[] data = Files.readAllBytes(file);
            files++;
            if (isArchive(relative, data)) {
                archive(relative, data, 0, relative.equals("gradle/wrapper/gradle-wrapper.jar"), false);
            } else checkBytes(relative, data, false);
            if (relative.equals("src/main/resources/fabric.mod.json")) checkAttribution(relative, data);
            if (relative.equals("LICENSE") && !new String(data, StandardCharsets.UTF_8).contains(ATTRIBUTION)) {
                finding(relative, "developer-attribution");
            }
        }
        applyReviewedFixtures(root);
    }

    private void applyReviewedFixtures(Path root) throws IOException {
        Path reviews = root.resolve("tools/privacy/reviewed-fixtures.tsv");
        if (!Files.isRegularFile(reviews)) return;
        for (String line : Files.readAllLines(reviews, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] parts = line.split("\\t", 4);
            if (parts.length != 4 || !parts[0].startsWith("src/test/")
                    || !parts[1].equals("email-review-required") || parts[3].isBlank()) {
                throw new IOException("Invalid reviewed fictional fixture declaration");
            }
            Path fixture = root.resolve(parts[0]).normalize();
            if (!fixture.startsWith(root) || !Files.isRegularFile(fixture)
                    || !fixtureDigest(Files.readAllBytes(fixture)).equalsIgnoreCase(parts[2])) {
                finding(parts[0], "fixture-review-expired");
                continue;
            }
            if (findings.remove(parts[1] + " : " + parts[0])) reviewedFixtures++;
        }
    }

    private static String fixtureDigest(byte[] bytes) {
        try {
            byte[] normalized = new String(bytes, StandardCharsets.UTF_8).replace("\r\n", "\n").getBytes(StandardCharsets.UTF_8);
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(normalized));
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private static boolean forbiddenPath(String raw) {
        String path = raw.replace('\\', '/').toLowerCase(Locale.ROOT);
        for (String part : path.split("/")) {
            if (PRIVATE_DIRS.contains(part) || GENERATED_DIRS.contains(part) || part.equals("config")
                    || part.equals(".env") || part.startsWith(".env.")) return true;
        }
        return path.endsWith(".log") || path.endsWith(".jfr") || path.endsWith(".hprof")
                || path.endsWith(".pending") || path.startsWith("hs_err_pid")
                || path.endsWith("servers.dat") || path.endsWith("options.txt");
    }

    private void archive(String label, byte[] bytes, int depth, boolean thirdParty, boolean ownArtifact) throws IOException {
        if (depth > 4 || bytes.length > ARCHIVE_LIMIT) { finding(label, "archive-limit"); return; }
        boolean manifest = false;
        int expanded = 0;
        Set<String> names = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
                String name = entry.getName().replace('\\', '/');
                String location = label + "!/" + name;
                if (entry.isDirectory()) continue;
                archiveEntries++;
                checkText(location, name, false);
                if (!names.add(name) || name.startsWith("/") || name.matches("^[A-Za-z]:.*") || Arrays.asList(name.split("/")).contains("..")) {
                    finding(location, "unsafe-archive-entry");
                }
                if (forbiddenPath(name)) finding(location, "private-content");
                byte[] content = zip.readNBytes(ENTRY_LIMIT + 1);
                expanded += content.length;
                expandedArchiveBytes += content.length;
                if (content.length > ENTRY_LIMIT || expanded > ARCHIVE_LIMIT || expandedArchiveBytes > 4L * ARCHIVE_LIMIT) {
                    finding(location, "archive-limit"); return;
                }
                if (isArchive(name, content)) archive(location, content, depth + 1, true, false);
                else checkBytes(location, content, thirdParty && isCredit(name));
                if (ownArtifact && name.equals("fabric.mod.json")) {
                    manifest = true;
                    checkAttribution(location, content);
                }
            }
        }
        if (ownArtifact && !manifest) finding(label, "missing-mod-metadata");
    }

    private static boolean isArchive(String name, byte[] data) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".jar") || lower.endsWith(".zip")
                || data.length >= 4 && data[0] == 'P' && data[1] == 'K' && data[2] == 3 && data[3] == 4;
    }

    private static boolean isCredit(String name) {
        String file = name.substring(name.lastIndexOf('/') + 1).toUpperCase(Locale.ROOT);
        return file.startsWith("LICENSE") || file.startsWith("NOTICE") || file.startsWith("AUTHORS") || file.startsWith("COPYING");
    }

    private void checkAttribution(String label, byte[] bytes) {
        String json = new String(bytes, StandardCharsets.UTF_8);
        if (!AUTHORS.matcher(json).find() || !json.contains("\"tropimon_team_saver\"")) finding(label, "developer-attribution");
    }

    private void checkBytes(String label, byte[] bytes, boolean thirdPartyCredit) throws IOException {
        checkText(label, new String(bytes, StandardCharsets.UTF_8), thirdPartyCredit);
        // UTF-16 resources and paths must not bypass the byte scan.
        checkText(label, new String(bytes, StandardCharsets.UTF_16LE), thirdPartyCredit);
        checkText(label, new String(bytes, StandardCharsets.UTF_16BE), thirdPartyCredit);
        if (!label.endsWith(".class")) return;
        // Class-file strings use modified UTF-8, not plain UTF-8. Inspect all UTF8 constants.
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (in.readInt() != 0xCAFEBABE) throw new IOException("Invalid compiled class");
            in.readUnsignedShort(); in.readUnsignedShort();
            int count = in.readUnsignedShort();
            for (int i = 1; i < count; i++) {
                switch (in.readUnsignedByte()) {
                    case 1 -> checkText(label, in.readUTF(), thirdPartyCredit);
                    case 3, 4, 9, 10, 11, 12, 17, 18 -> in.skipNBytes(4);
                    case 5, 6 -> { in.skipNBytes(8); i++; }
                    case 7, 8, 16, 19, 20 -> in.skipNBytes(2);
                    case 15 -> in.skipNBytes(3);
                    default -> throw new IOException("Unsupported class constant");
                }
            }
        }
    }

    private void checkText(String label, String text, boolean thirdPartyCredit) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String term : privateTerms) if (lower.contains(term)) { finding(label, "private-identity"); break; }
        if (PERSONAL_PATH.matcher(text).find()) finding(label, "personal-path");
        if (SECRET.matcher(text).find() || SECRET_ASSIGNMENT.matcher(text).find()) finding(label, "possible-secret");
        if (!thirdPartyCredit) {
            var emails = EMAIL.matcher(text);
            while (emails.find()) {
                String email = emails.group().toLowerCase(Locale.ROOT);
                if (!email.endsWith(".invalid") && !email.endsWith("@example.com") && !email.endsWith("@example.org")
                        && !email.endsWith("@example.net") && !email.equals("noreply@github.com")
                        && !email.matches("(?:[0-9]+\\+)?fastedcorsi@users\\.noreply\\.github\\.com")) {
                    finding(label, "email-review-required");
                }
            }
        }
    }

    private void finding(String label, String category) {
        String safe = label;
        for (String term : privateTerms) safe = safe.replaceAll("(?i)" + Pattern.quote(term), "[redacted]");
        safe = PERSONAL_PATH.matcher(safe).replaceAll("[personal-path]");
        safe = EMAIL.matcher(safe).replaceAll("[email]");
        safe = SECRET.matcher(safe).replaceAll("[possible-secret]");
        safe = safe.replaceAll("[\\p{Cntrl}]", "?");
        findings.add(category + " : " + safe); // Never print a matched value, snippet or absolute workspace path.
    }

    private void finish() {
        if (!findings.isEmpty()) {
            findings.forEach(System.err::println);
            throw new IllegalStateException("Privacy check failed (" + findings.size() + " reviewed locations required)");
        }
        System.out.println("Privacy check OK: " + files + " source files, " + archiveEntries
                + " archive entries, " + reviewedFixtures + " reviewed fictional fixtures; attribution verified.");
    }

    private static void selfTest() throws Exception {
        // All samples are fictional and assembled in memory; no real private terms in fixtures.
        PrivacyGuard guard = new PrivacyGuard(List.of("FictionalDeveloper"));
        guard.checkText("sample.txt", "FictionalDeveloper", false);
        expect(guard.findings.size() == 1, "private identity");
        guard.checkText("path.txt", String.join("/", "C:", "Users", "sample-account", "workspace"), false);
        expect(guard.findings.size() == 2, "portable path enforcement");
        guard.checkText("email.txt", "someone" + "@private-domain" + ".test", false);
        expect(guard.findings.size() == 3, "email review");
        guard.checkText("token.txt", "ghp_" + "A".repeat(36), false);
        expect(guard.findings.size() == 4, "secret marker");
        expect(forbiddenPath("assets/example/.env.local") && forbiddenPath("nested/saves/world.dat"), "private paths");
        PrivacyGuard clean = new PrivacyGuard(List.of());
        clean.checkText("public.txt", ATTRIBUTION + " https://rankedapi.tropimon.fr/api/seasons", false);
        clean.checkText("NOTICE", "Third Party Author <" + "author" + "@upstream" + ".test>", true);
        expect(clean.findings.isEmpty(), "public URLs and upstream credits");
        byte[] nested = zip("private/.env", "value".getBytes(StandardCharsets.UTF_8));
        clean.archive("test.jar", zip("META-INF/jars/nested.jar", nested), 0, false, false);
        expect(clean.findings.stream().anyMatch(s -> s.startsWith("private-content")), "nested archive detection");
        clean = new PrivacyGuard(List.of("FictionalDeveloper"));
        var buffer = new java.io.ByteArrayOutputStream();
        try (var out = new java.io.DataOutputStream(buffer)) {
            out.writeInt(0xCAFEBABE); out.writeShort(0); out.writeShort(65); out.writeShort(2);
            out.writeByte(1); out.writeUTF("FictionalDeveloper");
        }
        clean.checkBytes("Example.class", buffer.toByteArray(), false);
        expect(clean.findings.stream().anyMatch(s -> s.startsWith("private-identity")), "compiled constant detection");
        clean = new PrivacyGuard(List.of());
        clean.checkAttribution("fabric.mod.json", "{\"authors\": [\"Somebody Else\"]}".getBytes(StandardCharsets.UTF_8));
        expect(!clean.findings.isEmpty(), "wrong attribution");
        clean = new PrivacyGuard(List.of());
        clean.checkAttribution("fabric.mod.json", ("{\"id\":\"tropimon_team_saver\",\"authors\":[\"" + ATTRIBUTION + "\"]}").getBytes(StandardCharsets.UTF_8));
        expect(clean.findings.isEmpty(), "correct attribution");
        expect(fixtureDigest("sample\r\n".getBytes(StandardCharsets.UTF_8))
                .equals(fixtureDigest("sample\n".getBytes(StandardCharsets.UTF_8))), "portable fixture hash");
        expect(!fixtureDigest("sample".getBytes(StandardCharsets.UTF_8))
                .equals(fixtureDigest("changed".getBytes(StandardCharsets.UTF_8))), "expired fixture review");
        clean = new PrivacyGuard(List.of("FictionalDeveloper"));
        clean.finding("FictionalDeveloper/" + "someone" + "@private-domain" + ".test/" + "ghp_" + "A".repeat(36), "test");
        expect(clean.findings.stream().noneMatch(s -> s.contains("FictionalDeveloper") || s.contains("@") || s.contains("A".repeat(36))), "redacted filenames");
        expect(isArchive("payload.bin", zip("test.txt", new byte[0])), "nested archive signature");
        System.out.println("Privacy guard self-tests OK (14 checks, fictional data only).");
    }

    private static byte[] zip(String name, byte[] content) throws IOException {
        var buffer = new java.io.ByteArrayOutputStream();
        try (var zip = new java.util.zip.ZipOutputStream(buffer)) {
            zip.putNextEntry(new ZipEntry(name)); zip.write(content); zip.closeEntry();
        }
        return buffer.toByteArray();
    }
    private static void expect(boolean condition, String check) {
        if (!condition) throw new AssertionError("Privacy guard regression: " + check);
    }
}
