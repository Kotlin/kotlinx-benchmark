package kotlinx.benchmark

@JsFun("() => globalThis.arguments.join(' ')")
private external fun d8Arguments(): String

internal object D8EngineSupport : StandaloneJsVmSupport() {
    override fun arguments(): Array<out String> =
        d8Arguments().split(' ').toTypedArray()
}

@JsFun("() => typeof d8 !== 'undefined'")
internal external fun isD8Engine(): Boolean