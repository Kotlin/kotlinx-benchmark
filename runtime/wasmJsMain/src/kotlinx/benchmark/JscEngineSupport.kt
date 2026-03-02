package kotlinx.benchmark

import kotlin.time.Duration.Companion.seconds

@JsFun("""
    () => typeof globalThis.arguments !== 'undefined' ? globalThis.arguments.join(' ') : ""
    """)
private external fun jscArguments(): String

@JsFun("(text) => print(text)")
private external fun jscPrint(text: String)

internal object JscEngineSupport : StandaloneJsVmSupport() {
    override fun writeFile(path: String, text: String) {
        jscPrint("<FILE:$path>$text<ENDFILE>")
    }

    override fun arguments(): Array<out String> =
        jscArguments().split(' ').toTypedArray()
}

@JsFun("""() => {
  let res = typeof process === 'undefined'
    && typeof d8 === 'undefined' 
    && typeof globalThis.isIon === 'undefined'
  if (globalThis.console == null) {
    globalThis.console = {};
  }
  if (res) {
    console.log = print
    
  }
  return res
  }
""")
internal external fun isJscEngine(): Boolean

@JsFun("() => preciseTime().toFixed(9)")
internal external fun getPreciseTime(): Double

internal inline fun jscMeasureTime(block: () -> Unit): Long {
    val start = getPreciseTime()
    block()
    val end = getPreciseTime()
    val startInNs = start.seconds.inWholeNanoseconds
    val endInNs = end.seconds.inWholeNanoseconds
    return endInNs - startInNs
}