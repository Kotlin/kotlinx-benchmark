package kotlinx.benchmark.integration

class KotlinConfiguration {

    data class Target(val type: String, val name: String, val compiler: String, val environment: String)

    private val targets = mutableListOf<Target>()

    fun wasm(name: String, configuration: WasmTarget.() -> Unit) {
        val wasmTarget = WasmTarget(name)
        wasmTarget.configuration()
        targets.add(Target("wasm", wasmTarget.name, "", wasmTarget.environment))
    }

    fun js(name: String, compiler: String = IR, configuration: JsTarget.() -> Unit) {
        val jsTarget = JsTarget(name, compiler)
        jsTarget.configuration()
        targets.add(Target("js", jsTarget.name, jsTarget.compiler, jsTarget.environment))
    }

    fun lines(): List<String> {
        return targets.map {
            "${it.type}('${it.name}', ${it.compiler}) { ${it.environment}() }"
        }
    }

    class WasmTarget(val name: String) {
        var environment: String = ""

        fun nodejs() {
            environment = "nodejs"
        }
        fun browser() {
            environment = "browser"
        }
    }

    class JsTarget(val name: String, val compiler: String) {
        var environment: String = ""

        fun d8() {
            environment = "d8"
        }
        fun nodejs() {
            environment = "nodejs"
        }
        fun browser() {
            environment = "browser"
        }
    }

    companion object {
        const val IR = "'IR'"
        const val LEGACY = "'LEGACY'"
    }
}