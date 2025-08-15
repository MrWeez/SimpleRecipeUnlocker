plugins {
    `java`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.example"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // Add MockBukkit later if desired (requires JitPack + correct coordinates)
}

tasks.test {
    useJUnitPlatform()
}

// Relocate & minimize jar if desired
tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveClassifier.set("")
    // minimize() // disabled by default to avoid stripping classes inadvertently
}

// Inject version into plugin.yml
tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

// Show deprecation & unchecked warnings to identify outdated API usage
tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}
