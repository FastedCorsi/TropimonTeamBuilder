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
val localCobblemon = file("$launcherMods/Cobblemon-fabric-1.7.2+1.21.1.jar")
val localKotlinCandidates = fileTree(launcherMods) {
    include("fabric-language-kotlin-*.jar")
}.files

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    if (localKotlinCandidates.size == 1) {
        modImplementation(files(localKotlinCandidates.single()))
    } else {
        modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    }
    if (localCobblemon.exists()) {
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

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.add("-Xlint:all")
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    val modVersion = project.version.toString()
    inputs.property("version", modVersion)
    filesMatching("fabric.mod.json") {
        expand("version" to modVersion)
    }
}
