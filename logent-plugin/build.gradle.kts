import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

abstract class CopyPluginAssets : DefaultTask() {
    @get:InputDirectory abstract val input: DirectoryProperty
    @get:OutputDirectory abstract val output: DirectoryProperty

    @TaskAction
    fun copyAssets() {
        val target = output.get().asFile
        target.deleteRecursively()
        project.copy {
            from(input)
            into(target)
        }
    }
}

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

val pluginRepository = projectDir.parentFile
val pluginVersion = pluginRepository.resolve("VERSION").readText().trim()
version = pluginVersion
val apiPin = Properties().apply {
    pluginRepository.resolve("plugin-api.lock").inputStream().use { load(it) }
}

android {
    namespace = "com.sleepysoong.logent.claudemem"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        buildConfigField("String", "PLUGIN_VERSION", "\"$pluginVersion\"")
    }

    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// 원본 엔진을 한 번 번들링하고 테스트와 패키징에 같은 자산을 공급해요.
val runtimeBundle = tasks.register<Exec>("buildPluginRuntime") {
    workingDir(pluginRepository)
    commandLine("bash", "logent/runtime-build.sh")
}
val stagedAssets = tasks.register<Sync>("stagePluginAssets") {
    dependsOn(runtimeBundle)
    from(layout.projectDirectory.dir("src/main/assets"))
    from(pluginRepository.resolve("build/logent-runtime/claude-mem.mjs")) {
        into("plugins/claude-mem/runtime")
    }
    from(pluginRepository.resolve("build/logent-runtime/THIRD_PARTY_LICENSES")) {
        into("plugins/claude-mem/runtime/THIRD_PARTY_LICENSES")
    }
    from(listOf(pluginRepository.resolve("LICENSE"), pluginRepository.resolve("NOTICE"))) {
        into("plugins/claude-mem")
    }
    into(layout.buildDirectory.dir("staged/pluginAssets"))
}
val variantAssets = tasks.register<CopyPluginAssets>("stageVariantAssets") {
    dependsOn(stagedAssets)
    input.set(layout.buildDirectory.dir("staged/pluginAssets"))
    output.set(layout.buildDirectory.dir("generated/pluginAssets"))
}
androidComponents {
    onVariants(selector().all()) { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(variantAssets) { it.output }
    }
}
apply(from = pluginRepository.resolve("logent/artifacts.gradle.kts"))

dependencies {
    compileOnly("${apiPin.getProperty("group")}:${apiPin.getProperty("artifact")}:${apiPin.getProperty("version")}")
    testImplementation("${apiPin.getProperty("group")}:${apiPin.getProperty("artifact")}:${apiPin.getProperty("version")}")
    testImplementation("junit:junit:4.13.2")
}

tasks.withType<Test>().configureEach {
    dependsOn(stagedAssets)
    systemProperty(
        "logent.plugin.assets",
        layout.buildDirectory.dir("staged/pluginAssets").get().asFile.absolutePath,
    )
    maxParallelForks = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
}
