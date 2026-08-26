package kotlinx.benchmark.gradle

import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@CacheableTask
internal abstract class NativeSourceGeneratorTask
@Inject
constructor(workerExecutor: WorkerExecutor) : SourceGeneratorTask(workerExecutor) {

    @get:Input
    abstract val nativeTarget: Property<String>

    override fun nativeTargetName(): String = nativeTarget.get()
}
