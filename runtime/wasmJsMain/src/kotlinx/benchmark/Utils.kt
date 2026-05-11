package kotlinx.benchmark

private fun format(d: Double, precision: Int, useGrouping: Boolean): String =
    js("d.toLocaleString('en-GB', { maximumFractionDigits: precision, minimumFractionDigits: precision, useGrouping: useGrouping } )")

internal actual fun Double.format(precision: Int, useGrouping: Boolean): String = format(this, precision, useGrouping)

internal fun nodeJsEngineBinaryPath(): String = js("process.argv[0]")

internal fun nodeJsEngineModulePath(): String = js("process.argv[1]")

internal fun nodeJsGetDirName(filePath: String): String = path.dirname(filePath)

internal fun throwModuleCannotBeImported(name: String) {
    throw UnsupportedOperationException("Module $name cannot be imported in this environment")
}

/*
Wasm JsFun expect something invokeable, so we have to return an arrow function.
IIFE which returns a function that returns an input parameter (resolved module).
It helps us to work around the issue that we cannot use await import in non-async arrow functions.
(
    (module) => {
        return () => module
    }
)(await import("module"))
 */
internal const val LOAD_MODULE_PREFIX =
    "((module) => () => module)(((typeof process !== 'undefined') && (process.release.name === 'node')) ? await import(/* webpackIgnore: true */'node:"

internal const val LOAD_MODULE_POSTFIX = "') : null)"