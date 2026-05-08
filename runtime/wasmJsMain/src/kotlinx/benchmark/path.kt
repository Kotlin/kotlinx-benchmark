/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.benchmark

internal external interface Path {
    /**
     * See https://nodejs.org/api/path.html#pathdirnamepath
     */
    fun dirname(path: String): String
}

@JsModule("node:path")
internal external val path: Path
