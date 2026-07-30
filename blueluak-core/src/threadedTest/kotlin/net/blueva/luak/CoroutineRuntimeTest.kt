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

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.blueva.luak.compiler.LuaC
import net.blueva.luak.lib.CoroutineLib

class CoroutineRuntimeTest {
    private lateinit var globals: Globals

    @BeforeTest
    fun installRuntime() {
        globals = Globals()
        LuaC.install(globals)
        globals.load(CoroutineLib())
    }

    @Test
    fun preservesExecutionStateAcrossYieldAndResume() {
        val result = globals.load(
            """
            local coroutine = coroutine
            local thread = coroutine.create(function(initial)
                local resumed = coroutine.yield(initial + 1)
                return resumed + 2
            end)
            local firstOk, firstValue = coroutine.resume(thread, 10)
            local secondOk, secondValue = coroutine.resume(thread, 20)
            return firstOk, firstValue, secondOk, secondValue, coroutine.status(thread)
            """.trimIndent(),
            "coroutine-state",
        )!!.invoke()

        assertTrue(result.arg(1)!!.toboolean())
        assertEquals(11, result.arg(2)!!.checkint())
        assertTrue(result.arg(3)!!.toboolean(), result.arg(4)!!.tojstring())
        assertEquals(22, result.arg(4)!!.checkint())
        assertEquals("dead", result.arg(5)!!.checkjstring())
    }
}
