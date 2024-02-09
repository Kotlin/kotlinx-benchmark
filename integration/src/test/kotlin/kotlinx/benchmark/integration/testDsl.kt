package kotlinx.benchmark.integration

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

internal fun BuildResult.printBuildOutput() {
    println(
        """
        |Failed assertion build output:
        |#######################
        |$output
        |#######################
        |
        """.trimMargin()
    )
}

internal fun BuildResult.assertTasksExecuted(vararg tasks: String) {
    tasks.forEach { task ->
        assert(task(task)?.outcome == TaskOutcome.SUCCESS) {
            printBuildOutput()
            "Task $task didn't have 'SUCCESS' state: ${task(task)?.outcome}"
        }
    }
}

internal fun BuildResult.assertTasksExecuted(tasks: Collection<String>) = assertTasksExecuted(*tasks.toTypedArray())

fun BuildResult.assertTasksUpToDate(vararg tasks: String) {
    tasks.forEach { task ->
        assert(task(task)?.outcome == TaskOutcome.UP_TO_DATE) {
            printBuildOutput()
            "Task $task didn't have 'UP-TO-DATE' state: ${task(task)?.outcome}"
        }
    }
}

internal fun BuildResult.assertOutputContains(
    expectedSubString: String,
    message: String = "Build output does not contain \"$expectedSubString\""
) {
    assert(output.contains(expectedSubString)) {
        printBuildOutput()
        message
    }
}

internal fun BuildResult.assertOutputDoesNotContain(
    expectedSubString: String,
    message: String = "Build output contains \"$expectedSubString\""
) {
    assert(!output.contains(expectedSubString)) {
        printBuildOutput()
        message
    }
}

internal fun BuildResult.assertTasksUpToDate(tasks: Collection<String>) = assertTasksUpToDate(*tasks.toTypedArray())
