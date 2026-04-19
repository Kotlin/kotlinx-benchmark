## Android Plugin support

Open Questions


--
Q: Android Build Tools require Java 11. This is conflicting with the current
   `kotlinx-benchmark` benchmark plugin compiling with JvmTarget.1_8.

   Changing this is a _breaking_ change for anyone using JDK 8 in their build
   system, which at least include a handful of kotlinx libraries. Note, we only
   need to raise the min JDK level for the plugin, the runtime can stay at 
   Java 8.

- Is it acceptable to raise the minimum JVM target `kotlinx-benchmark` to Java 11?

- Alternatives are unclear. Either ship Android support as some sort of extension
  users must opt into or create two different variants of the plugin each with 
  their own JVM target. Both will complicate the build in rather big ways.

- Crazy idea: Create a copy of the Android interface, and put it in an internal
  gradle module we can include with `compileOnly`. It will be stripped  when
  building the plugin. The only requirement is that any instance of checks 
  needs more guards in the code.

- All known affected places in the code has been marked with `TODO JDK_11_DISCUSS`


--
Q: Is it okay that support limited to `com.android.kotlin.multiplatform.library'?

- Seems acceptable as `com.android.library` and `com.android.application` are
  both deprecated.


--
Q: Right now, the template project includes a bundled gradle-wrapper.jar. It 
   would be nice to avoid it, but not sure if there is a way. It could potentially
   be copied from the user's project, but we are not guaranteed they use the 
   wrapper.

Q: Would some users object to downloading a wrapper using the default source?
   E.g., `kotlinx-benchmarks` use `https\://cache-redirector.jetbrains.com`

- Maybe allow an extra configuration for setting this URL? 
- Is there another way to start a Gradle project using another project Gradle
  version and wrapper?
- Having the generated project completely stand-alone is nice for debugging.

--
Q: Task naming: Right now we do not use `androidMain`, but only `android` as 
   the Android Gradle Plugin doesn't support custom compilations and having to 
   include `Main` all the time is annoying and doesn't bring value, but would it
   be more consistent to use `androidMainBenchmark`?

- At least it would set us up for a future where custom compilations might be
  supported.


--
Q: By not supporting custom compilations, we cannot filter out Android 
   benchmark files from the library when it is compiled. At least not easily.
   How big of a problem is this?

- Could we hook into the build and filter them out for users somehow?


--
Q: Proper benchmarks require locking the CPU on devices. This is available
   through Gradle tasks in Jetpack Microbenchmark or ADB commands. Should we
   expose these in `kotlinx-benchmark`, making this easier for users?

- Shouldn't be super hard to do, so probably.
  
-- 
Q: A lot of `kotlinx-benchmark` annotations are not easily supported on Android. 
   Some can be worked around, others should be documented. How to handle this?

- @Measurement/@Warmup: Are exposed in Jetpack, but they have different semantics.

  E.g. `iterations` just mean the number of times a test runs. Jetpack does seem
  to have API's that could be used to match the JMH behavior, but they are all
  internal and hard to access. 

  It is unclear if it is worth trying this approach, approach Google about the
  use case (so they can modify the API's), or just document the differences.

- @OutputTimeUnit: Nothing technically prevents supporting this outside an extra
  parsing step.

--
Q: Right now Android doesn't support `BenchmarkConfiguration.reportFormat`.
   The biggest reason is that the Android output isn't JMH compatible. How
   to handle this?

- They do expose the underlying metrics through a JSON file, so it looks possible
  this file could be converted into something JMH compatible. But it requires
  further research, and it isn't clear how stable the Jetpack output format is.
- How important is this?

