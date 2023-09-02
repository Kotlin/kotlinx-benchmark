package kotlinx.benchmark

private external fun print(text: String)

internal object SpiderMonkeyEngineSupport : StandaloneJsVmSupport() {
    override fun writeFile(path: String, text: String) {
        //WORKAROUND: In StandaloneJsVMs cannot write into files, this format will be parsed on gradle plugin side
        print("<FILE:$path>$text<ENDFILE>")
    }

    override fun arguments(): Array<out String> {
        val arguments = js("globalThis.scriptArgs.join(' ')") as String
        return arguments.split(' ').toTypedArray()
    }
}

internal fun isSpiderMonkeyEngine(): Boolean =
    js("typeof(globalThis.inIon) !== 'undefined' || typeof(globalThis.isIon) !== 'undefined'") as Boolean