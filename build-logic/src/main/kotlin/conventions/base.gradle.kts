package kotlinx.benchmarks_build.conventions

plugins {
    base
}

tasks.withType<AbstractArchiveTask>().configureEach {
    // https://docs.gradle.org/current/userguide/working_with_files.html#sec:reproducible_archives
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}


//region Support invoking task selectors on included builds
// Workaround https://github.com/gradle/gradle/issues/22335
abstract class SubprojectTaskCollectorService : BuildService<BuildServiceParameters.None> {
    abstract val allTasks: SetProperty<String>
}

val taskCollectorService: SubprojectTaskCollectorService =
    gradle.sharedServices.registerIfAbsent("TaskCollectorService", SubprojectTaskCollectorService::class).get()

if (project == rootProject) {
    listOf(
        "check",
        "assemble",
        "build",
        "apiDump",
        "apiCheck",
    ).forEach { task ->
        tasks
            .matching { it.name == task }
            .configureEach {
                dependsOn(
                    taskCollectorService.allTasks.map { tasks -> tasks.filter { it.endsWith(":$task") } }
                )
                dependsOn(
                    gradle.includedBuilds.map { it.task(":$task") }
                )
            }
    }
} else {
    taskCollectorService.allTasks.addAll(
        provider { tasks.names.map { "${project.path}:$it" } }
    )
}
//endregion
