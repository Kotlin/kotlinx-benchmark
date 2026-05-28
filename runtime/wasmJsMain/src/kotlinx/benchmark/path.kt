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

internal val path: Path by lazy {
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    (loadPath() ?: throwModuleCannotBeImported("path")) as Path
}

@JsFun("${LOAD_MODULE_PREFIX}path${LOAD_MODULE_POSTFIX}")
private external fun loadPath(): JsAny?