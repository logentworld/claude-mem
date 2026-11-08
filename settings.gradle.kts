pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

val apiPin = java.util.Properties().apply {
    file("plugin-api.lock").inputStream().use { load(it) }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        if (providers.gradleProperty("logentPluginApiMavenLocal").orNull == "true") mavenLocal()
        maven {
            url = uri(apiPin.getProperty("repository"))
            credentials {
                username = providers.environmentVariable("LOGENT_PLUGIN_API_USER").orElse(providers.environmentVariable("GITHUB_ACTOR")).orNull
                password = providers.environmentVariable("LOGENT_PLUGIN_API_TOKEN").orElse(providers.environmentVariable("GITHUB_TOKEN")).orNull
            }
            content { includeModule(apiPin.getProperty("group"), apiPin.getProperty("artifact")) }
        }
    }
}

rootProject.name = "logent-claude-mem"

include(":logent-plugin")

// 소스 API는 명시적으로 선택한 개발 빌드에서만 사용해요.
if (providers.gradleProperty("logentPluginApiSource").orNull == "true") {
    includeBuild("../logent-plugin-api") {
        dependencySubstitution {
            substitute(module("${apiPin.getProperty("group")}:${apiPin.getProperty("artifact")}"))
                .using(project(":plugin-api"))
        }
    }
}
