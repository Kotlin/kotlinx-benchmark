package kotlinx.benchmark

@JsFun("() => globalThis.scriptArgs.join(' ')")
private external fun spiderMonkeyArguments(): String

internal object SpiderMonkeyEngineSupport : StandaloneJsVmSupport() {
    override fun arguments(): Array<out String> =
        spiderMonkeyArguments().split(' ').toTypedArray()
}

@JsFun("() => typeof(globalThis.inIon) !== 'undefined' || typeof(globalThis.isIon) !== 'undefined'")
internal external fun isSpiderMonkeyEngine(): Boolean