/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.benchmark

internal external interface ChildProcess

internal val childProcess: ChildProcess by lazy {
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    (loadChildProcess() ?: throwModuleCannotBeImported("child_process")) as ChildProcess
}

@JsFun("${LOAD_MODULE_PREFIX}child_process${LOAD_MODULE_POSTFIX}")
private external fun loadChildProcess(): JsAny?
