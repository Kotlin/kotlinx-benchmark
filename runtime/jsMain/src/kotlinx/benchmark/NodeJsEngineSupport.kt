package kotlinx.benchmark

internal external fun require(module: String): dynamic

internal object NodeJsEngineSupport : JsEngineSupport() {
    override fun writeFile(path: String, text: String) =
        require("fs").writeFileSync(path, text)

    override fun readFile(path: String): String =
        require("fs").readFileSync(path, "utf8") as String

    override fun arguments(): Array<out String> {
        val arguments = js("process.argv.slice(2).join(' ')") as String
        return arguments.split(' ').toTypedArray()
    }
}