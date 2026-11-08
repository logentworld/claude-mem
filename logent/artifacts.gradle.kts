// 테스트·릴리스가 같은 자산 스냅샷을 배포하도록 모듈 안에서 등록해요.
val repository = projectDir.parentFile
val releaseVersion = repository.resolve("VERSION").readText().trim()
require(Regex("[0-9]+[.][0-9]+[.][0-9]+").matches(releaseVersion)) { "VERSION은 x.y.z 형식으로 써 주세요." }
val distribution = repository.resolve("build/dist")
val contentSources = tasks.register<Zip>("pluginAssetSources") {
    dependsOn("stagePluginAssets")
    doFirst {
        require(repository.resolve("build/logent-runtime/source-closure").isDirectory) {
            "엔진 원본 소스 묶음이 없어요. runtime 빌드 단계를 확인해 주세요."
        }
    }
    from(layout.buildDirectory.dir("staged/pluginAssets")) { into("assets") }
    from(repository.resolve("LICENSE"))
    from(repository.resolve("NOTICE"))
    from(repository.resolve("logent-plugin.json"))
    from(repository.resolve("UPSTREAM_COMMIT"))
    from(repository.resolve("README.upstream.md"))
    from(repository.resolve("build/logent-runtime/source-closure")) { into("sources") }
    archiveFileName.set("logent-claude-mem-v${releaseVersion}-assets-source.zip")
    destinationDirectory.set(distribution)
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
val releaseAar = tasks.register<Copy>("pluginReleaseAar") {
    dependsOn("assembleRelease")
    from(layout.buildDirectory.file("outputs/aar/${project.name}-release.aar"))
    into(distribution)
    rename { "logent-claude-mem-v${releaseVersion}.aar" }
}
tasks.register("pluginArtifacts") {
    group = "distribution"
    description = "릴리스 AAR와 비설치용 자산 원본 ZIP을 만들어요."
    dependsOn(contentSources, releaseAar)
}
