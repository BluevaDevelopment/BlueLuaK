/******************************************************************************
 *  _                _
 * | |   _   _  __ _| | __
 * | |  | | | |/ _` | |/ /
 * | |__| |_| | (_| |   <
 * |_____\__,_|\__,_|_|\_\
 *
 *  Luak
 *  https://github.com/BasaltProject/Luak
 *
 *  Copyright (c) 2026 Basalt Project
 *
 *  SPDX-License-Identifier: MIT
 ******************************************************************************/
package org.basaltmc.luak

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.basaltmc.luak.compiler.LuaC

class KmpRuntimeTest {
    private lateinit var globals: Globals

    @BeforeTest
    fun installCompiler() {
        globals = Globals()
        LuaC.install(globals)
    }

    @Test
    fun compilesAndExecutesLuaOnEveryTarget() {
        val result = globals.load("return 2 + 3 * 4", "kmp-test")!!.call()!!
        assertEquals(14, result.checkint())
    }

    @Test
    fun executesTablesAndFunctionsOnEveryTarget() {
        val result = globals.load(
            "local function twice(value) return value * 2 end; return twice(21)",
            "kmp-functions",
        )!!.call()!!
        assertEquals(42, result.checkint())
    }
}
