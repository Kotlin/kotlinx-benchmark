package kotlinx.benchmark

@JsFun("() => globalThis.scriptArgs.join(' ')")
private external fun spiderMonkeyArguments(): String

internal object SpiderMonkeyEngineSupport : StandaloneJsVmSupport() {
    override fun arguments(): Array<out String> =
        spiderMonkeyArguments().split(' ').toTypedArray()
}

@JsFun("() => globalThis.isIon !== 'undefined'")
internal external fun isSpiderMonkeyEngine(): Boolean