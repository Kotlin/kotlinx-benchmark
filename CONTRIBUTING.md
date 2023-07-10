# Contributing Guidelines

There are two main ways to contribute to the project &mdash; submitting issues and submitting
fixes/changes/improvements via pull requests.

## Submitting issues

Both bug reports and feature requests are welcome.
Submit issues [here](https://github.com/Kotlin/kotlinx-benchmark/issues).

- Search for existing issues to avoid reporting duplicates.
- When submitting a bug report:
  - Use a 'bug report' template when creating a new issue.
  - Test it against the most recently released version. It might have been already fixed.
  - By default, we assume that your problem reproduces in Kotlin/JVM. Please, mention if the problem is
    specific to a platform.
  - Include the code that reproduces the problem. Provide the complete reproducer code, yet minimize it as much as possible.
  - However, don't put off reporting any unusual or rarely appearing issues just because you cannot consistently
    reproduce them.
  - If the bug is in behavior, then explain what behavior you've expected and what you've got.
- When submitting a feature request:
  - Use a 'feature request' template when creating a new issue.
  - Explain why you need the feature &mdash; what's your use-case, what's your domain.
  - Explaining the problem you're facing is more important than suggesting a solution.
    Report your problem even if you don't have any proposed solution.
  - If there is an alternative way to do what you need, then show the code of the alternative.

## Submitting PRs

We love PRs. Submit PRs [here](https://github.com/Kotlin/kotlinx-benchmark/pulls).
However, please keep in mind that maintainers will have to support the resulting code of the project,
so do familiarize yourself with the following guidelines.

- If you fix documentation:
  - If you plan extensive rewrites/additions to the docs, then please [contact the maintainers](#contacting-maintainers)
    to coordinate the work in advance.
- If you make any code changes:
  - Follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/reference/coding-conventions.html).
    - Use 4 spaces for indentation.
    - Use imports with '\*'.
  - Build the project to make sure it all works and passes the tests.
- If you fix a bug:
  - Write the test that reproduces the bug.
  - Fixes without tests are accepted only in exceptional circumstances if it can be shown that writing the
    corresponding test is too hard or otherwise impractical.
  - Follow the style of writing tests that is used in this project:
    name test functions as `testXxx`. Don't use backticks in test names.
- Comment on the existing issue if you want to work on it. Ensure that the issue not only describes a problem, 
  but also describes a solution that has received positive feedback. Propose a solution if there isn't any.

## Building

This library is built with Gradle.

- Run `./gradlew build` to build. It also runs all the tests.
- Run `./gradlew <module>:check` to test the the module you're currently working on to speed things up during development.

## Contacting maintainers

- If something cannot be done, is not convenient, or does not work, &mdash; submit an [issue](https://github.com/Kotlin/kotlinx-benchmark/issues).
- "How to do something" questions &mdash; [StackOverflow](https://stackoverflow.com).
- Discussions and general inquiries &mdash; use `#benchmarks` channel in [KotlinLang Slack](https://kotl.in/slack).
