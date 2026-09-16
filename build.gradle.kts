import java.security.MessageDigest

plugins {
    id("fabric-loom") version "1.15.5"
    id("maven-publish")
}

version = property("mod_version") as String
group = property("maven_group") as String

base {
    archivesName.set(property("archives_base_name") as String)
}

repositories {
    maven("https://api.modrinth.com/maven")
}

val launcherMods = file("${System.getProperty("user.home")}/AppData/Roaming/.tropimon/mods")
val localCobblemon = providers.gradleProperty("cobblemonJar").orNull?.let(::file) ?: run {
    val installed = launcherMods.listFiles()
        ?.filter { it.isFile && it.name.matches(Regex("Cobblemon-fabric-.+\\.jar", RegexOption.IGNORE_CASE)) }
        .orEmpty()
    installed.singleOrNull()
}
val localKotlinCandidates = fileTree(launcherMods) {
    include("fabric-language-kotlin-*.jar")
}.files
val officialDependenciesOnly = providers.gradleProperty("officialDependenciesOnly").isPresent

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    if (!officialDependenciesOnly && localKotlinCandidates.size == 1) {
        modImplementation(files(localKotlinCandidates.single()))
    } else {
        modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    }
    if (!officialDependenciesOnly && localCobblemon?.isFile == true) {
        modImplementation(files(localCobblemon))
    } else {
        modImplementation("maven.modrinth:MdwFAVRL:${property("cobblemon_modrinth_version")}")
    }
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.2.21")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    withSourcesJar()
}

// Standalone JDK-only checker: it is not included in the mod or its runtime dependencies.
val privacyTools = sourceSets.create("privacy") {
    java.setSrcDirs(listOf("tools/privacy"))
    resources.setSrcDirs(emptyList<String>())
}
val privacyGuardTest = tasks.register<JavaExec>("privacyGuardTest") {
    group = "verification"
    dependsOn(tasks.named(privacyTools.classesTaskName))
    classpath = privacyTools.runtimeClasspath
    mainClass.set("privacy.PrivacyGuard")
    args("self-test")
}
val privacyCheckSources = tasks.register<JavaExec>("privacyCheckSources") {
    group = "verification"
    description = "Check the source distribution without printing private matches."
    dependsOn(privacyGuardTest)
    classpath = privacyTools.runtimeClasspath
    mainClass.set("privacy.PrivacyGuard")
    args("sources", projectDir.absolutePath)
}
tasks.withType<Jar>().configureEach {
    dependsOn(privacyCheckSources)
    from("LICENSE")
    // Defense in depth; do not package local/private files even if added to resources accidentally.
    exclude("**/.git/**", "**/.env", "**/.env.*", "**/logs/**", "**/saves/**",
            "**/screenshots/**", "**/sessions/**", "**/private/**", "**/config/**",
            "**/crash-reports/**", "**/*.log", "**/*.jfr", "**/*.hprof")
}
val privacyCheckArtifacts = tasks.register<JavaExec>("privacyCheckArtifacts") {
    group = "verification"
    description = "Check the final remapped JARs, nested archives and compiled constants."
    dependsOn(privacyCheckSources, tasks.named("remapJar"), tasks.named("remapSourcesJar"))
    classpath = privacyTools.runtimeClasspath
    mainClass.set("privacy.PrivacyGuard")
    args("artifacts",
            layout.buildDirectory.file("libs/${base.archivesName.get()}-${project.version}.jar").get().asFile.absolutePath,
            layout.buildDirectory.file("libs/${base.archivesName.get()}-${project.version}-sources.jar").get().asFile.absolutePath)
}
tasks.named("check") { dependsOn(privacyCheckSources, privacyCheckArtifacts) }
tasks.named("assemble") { dependsOn(privacyCheckArtifacts) }

// Opt-in QA world; never points at the player's launcher or saves.
if (providers.gradleProperty("qaClient").isPresent) {
    loom.runs.named("client") {
        runDir("build/qa/standalone")
        vmArg("-Xmx3G")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.add("-Xlint:all")
}

tasks.test {
    useJUnitPlatform()
}

val cobblemonMinimumVersion = property("cobblemon_min_version") as String
val verifyCobblemonCompatibility = tasks.register("verifyCobblemonCompatibility") {
    group = "verification"
    description = "Refuse les anciennes bornes Cobblemon avant de fabriquer un JAR."
    inputs.property("cobblemonMinimumVersion", cobblemonMinimumVersion)
    inputs.file("src/main/resources/fabric.mod.json")
    doLast {
        val expected = "\"cobblemon\": \">=$cobblemonMinimumVersion\""
        check(file("src/main/resources/fabric.mod.json").readText().contains(expected)) {
            "fabric.mod.json doit déclarer Cobblemon >=$cobblemonMinimumVersion sans borne maximale artificielle."
        }
    }
}

tasks.processResources {
    dependsOn(verifyCobblemonCompatibility)
    val modVersion = project.version.toString()
    inputs.property("version", modVersion)
    filesMatching("fabric.mod.json") {
        expand("version" to modVersion)
    }
}


val prepareReleaseDelivery = tasks.register("prepareReleaseDelivery") {
    group = "distribution"
    description = "Produit les JAR local et partageable vérifiés de la même version."
    dependsOn(tasks.build)
    doLast {
        val source = tasks.remapJar.get().archiveFile.get().asFile
        val deliveryRoot = layout.buildDirectory.dir("release").get().asFile
        val shareDirectory = deliveryRoot.resolve("shareable")
        val localDirectory = deliveryRoot.resolve("local")
        shareDirectory.deleteRecursively()
        localDirectory.deleteRecursively()
        shareDirectory.mkdirs()
        localDirectory.mkdirs()

        fun copyAndHash(target: File) {
            source.copyTo(target, overwrite = true)
            val digest = MessageDigest.getInstance("SHA-256")
            target.inputStream().use { input ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            target.resolveSibling(target.name + ".sha256").writeText(hash + System.lineSeparator())
        }

        copyAndHash(shareDirectory.resolve("TropimonTeamBuilder-${project.version}+1.21.1.jar"))
        copyAndHash(localDirectory.resolve("TropimonTeamBuilder-${project.version}+1.21.1-LOCAL.jar"))
        file("tools/install-local-deferred.ps1")
            .copyTo(localDirectory.resolve("install-local-deferred.ps1"), overwrite = true)
    }
}

tasks.register("armReleaseLocal") {
    group = "distribution"
    description = "Arme l'installation locale différée sans arrêter Minecraft ni le launcher."
    dependsOn(prepareReleaseDelivery)
    doLast {
        val script = layout.buildDirectory.file("release/local/install-local-deferred.ps1").get().asFile
        ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden",
            "-ExecutionPolicy", "Bypass", "-File", script.absolutePath)
            .directory(script.parentFile)
            .start()
    }
}


