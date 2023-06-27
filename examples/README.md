# kotlinx-benchmark Examples Guide

This guide is designed to help you navigate, set up, and run the benchmark examples provided here. Whether you're a seasoned developer or new to Kotlin and benchmarking, we've got you covered. Let's dive in and explore these practical examples together.

## Prerequisites

Before you begin, ensure you have the following installed on your local machine:

- Git: Used to clone the repository. You can download it from [here](https://git-scm.com/downloads).
- Gradle: Used to build the projects. You can download it from [here](https://gradle.org/install/). Note that the projects come with a Gradle wrapper, so this is optional.

## Getting Started

1. **Clone the Repository**: Clone the `kotlinx-benchmark` repository to your local machine by running the following command in your terminal:

   ```
   git clone https://github.com/Kotlin/kotlinx-benchmark.git
   ```

2. **Navigate to the Examples Directory**: Once the repository is cloned, navigate to the `examples` directory by running:

   ```
   cd kotlinx-benchmark/examples
   ```

## Running the Examples

Each example is a separate project that can be built and run independently. Here's how you can do it:

1. **Navigate to the Example Directory**: Navigate to the directory of the example you want to run. For instance, if you want to run the `kotlin-kts` example, you would run:

   ```
   cd kotlin-kts
   ```

2. **Build the Project**: Each project uses Gradle as a build tool. If you have Gradle installed on your machine, you can build the project by running:

   ```
   gradle build
   ```

3. **Run the Benchmark**: After the project is built, you can run the benchmark by executing:

   ```
   gradle benchmark
   ```

Repeat these steps for each example you want to run.

## Troubleshooting

If you encounter any issues while setting up or running the examples, please check the following:

- Ensure you have all the prerequisites installed and they are added to your system's PATH.
- Make sure you are running the commands in the correct directory.

If you're still having issues, feel free to open an issue on the [kotlinx-benchmark repository](https://github.com/Kotlin/kotlinx-benchmark/issues).

Happy benchmarking!
