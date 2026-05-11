/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.benchmark

internal external interface Fs {
    /**
     * See https://nodejs.org/api/fs.html#fsreadfilesyncpath-options
     */
    fun readFileSync(file: String, options: String?): String

    /**
     * See https://nodejs.org/api/fs.html#fswritefilesyncfile-data-options
     */
    fun writeFileSync(file: String, text: String, options: String?)

    /**
     * See https://nodejs.org/api/fs.html#fsappendfilesyncpath-data-options
     */
    fun appendFileSync(file: String, text: String, options: String?)
}

internal val fs: Fs by lazy {
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    (loadFs() ?: throwModuleCannotBeImported("fs")) as Fs
}

@JsFun("${LOAD_MODULE_PREFIX}fs${LOAD_MODULE_POSTFIX}")
private external fun loadFs(): JsAny?