plugins {
    `java`
    id("com.gradleup.shadow") version "9.0.0-beta12" // Use newer shadow plugin for Java 21 support
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
    maven {
        name = "tcoded-releases"
        url = uri("https://repo.tcoded.com/releases")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    implementation("com.tcoded:FoliaLib:0.5.1")
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
    
    // Make sure to shade (include) FoliaLib in the final JAR
    dependencies {
        include(dependency("com.tcoded:FoliaLib:.*"))
    }
    
    // Relocate FoliaLib to prevent conflicts with other plugins
    relocate("com.tcoded.folialib", "com.example.myplugin.lib.folialib")
    
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
