## Android Plugin support

Open Questions

---
Q: Is it okay that support limited to `com.android.kotlin.multiplatform.library'?

- Seems acceptable as `com.android.library` and `com.android.application` are
  both deprecated.


---
Q: Right now, the template project includes a bundled gradle-wrapper.jar. It 
   would be nice to avoid it, but not sure if there is a way. It could potentially
   be copied from the user's project, but we are not guaranteed they use the 
   wrapper.

- Is there another way to start a Gradle project using another project Gradle
  version and wrapper?
- Having the generated project completely stand-alone is nice for debugging.


---
Q: By not supporting custom compilations, we cannot filter out Android 
   benchmark files from the library when it is compiled. At least not easily.
   How big of a problem is this?

- Custom source sets fix the problem, at least visually.
- Could we hook into the build and filter them out for users somehow?


---
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

---
Q: Right now Android doesn't support `BenchmarkConfiguration.reportFormat`.
   The biggest reason is that the Android output isn't JMH compatible. How
   to handle this?

- They do expose the underlying metrics through a JSON file, so it looks possible
  this file could be converted into something JMH compatible. But it requires
  further research, and it isn't clear how stable the Jetpack output format is.
- How important is this?

