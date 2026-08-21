/******************************************************************************
 *  ____  _            _                _  __
 * | __ )| |_   _  ___| |   _   _  __ _| |/ /
 * |  _ \| | | | |/ _ \ |  | | | |/ _` | ' /
 * | |_) | | |_| |  __/ |__| |_| | (_| | . \
 * |____/|_|\__,_|\___|_____\__,_|\__,_|_|\_\
 *
 *  BlueLuaK
 *  https://github.com/BluevaDevelopment/BlueLuaK
 *
 *  Copyright (c) 2026 Blueva Development
 *
 *  SPDX-License-Identifier: MIT
 ******************************************************************************/
package net.blueva.luak

import kotlin.reflect.KClass

internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()
internal actual fun platformProperty(name: String): String? = System.getProperty(name)
internal actual fun platformEnvironment(name: String): String? = System.getenv(name)
internal actual fun platformExit(code: Int) = System.exit(code)
internal actual fun platformCollectGarbage() = System.gc()
internal actual fun platformUsedMemory(): Long = Runtime.getRuntime().run { totalMemory() - freeMemory() }
internal actual fun platformLoadLibrary(className: String, globals: Globals): LuaValue? {
    val value = Class.forName(className).getDeclaredConstructor().newInstance() as? LuaValue ?: return null
    if (value is LuaFunction) value.initupvalue1(globals)
    return value
}
internal actual fun platformTypeName(type: KClass<*>): String =
    type.qualifiedName ?: type.simpleName ?: "userdata"

internal actual fun platformIsStackOverflow(failure: Throwable): Boolean =
    // A class first reached at the bottom of an exhausted stack cannot be
    // initialised, and stays that way for the rest of the run: every later use
    // of it raises a LinkageError instead. That is the same exhaustion showing
    // up a step later, so it is reported as such rather than as a host fault.
    failure is StackOverflowError || failure is LinkageError
