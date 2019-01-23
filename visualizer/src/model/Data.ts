// org/jetbrains/gradle/benchmarks/ReportBenchmarkResult.kt

export interface ResultsBundle {
    [target: string]: ResultsBenchmark[]
}

export interface ResultsBenchmark {
    benchmark: string
    mode: "thrpt"
    warmupIterations: number
    warmupTime: string
    measurementIterations: number
    measurementTime: string
    primaryMetric: ResultsMetric
}

export interface ResultsMetric {
    score: number
    scoreError: number
    scoreConfidence: [number, number]
    scorePercentiles: { [percentily: number]: number },
    scoreUnit: "ops/s",
    rawData: [[number]]
}

export async function downloadBenchmarkResult(reportsDirUrl: string, _targets?: string[]): Promise<ResultsBundle> {
    const targets = _targets || ['js', 'jvm', 'native'];
    const results = await Promise.all(
        targets.map(async target => (await fetch(reportsDirUrl + "/" + target + ".json")).json())
    );
    const result: ResultsBundle = {};
    targets.forEach((target, index) => result[target] = results[index] as [ResultsBenchmark]);
    return result
}

export async function loadBenchmarkFromFiles(files: File[]) {
    const result: ResultsBundle = {};

    await Promise.all(
        files.map((file) => {
            const reader = new FileReader();
            const targetName = file.name.replace('.json', '');
            const targetResult = new Promise((resolve, reject) => {
                reader.onload = function (evt) {
                    try {
                        // @ts-ignore
                        // noinspection UnnecessaryLocalVariableJS
                        const parsed = JSON.parse(evt.target.result) as BenchmarkReport.Benchmark[];
                        result[targetName] = parsed;
                        resolve()
                    } catch (e) {
                        reject(e)
                    }
                };
            });
            reader.readAsText(file);

            return targetResult
        })
    );

    return result
}