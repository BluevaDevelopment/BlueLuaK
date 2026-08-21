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
package net.blueva.luak.compiler


open class CompilerUnitTests : AbstractUnitTests("test/lua", "luaj3.0-tests.zip", "lua5.2.1-tests") {
    fun testAll() {
        doTest("all.lua")
    }

    fun testApi() {
        doTest("api.lua")
    }

    fun testAttrib() {
        doTest("attrib.lua")
    }

    fun testBig() {
        doTest("big.lua")
    }

    fun testBitwise() {
        doTest("bitwise.lua")
    }

    fun testCalls() {
        doTest("calls.lua")
    }

    fun testChecktable() {
        doTest("checktable.lua")
    }

    fun testClosure() {
        doTest("closure.lua")
    }

    fun testCode() {
        doTest("code.lua")
    }

    fun testConstruct() {
        doTest("constructs.lua")
    }

    fun testCoroutine() {
        doTest("coroutine.lua")
    }

    fun testDb() {
        doTest("db.lua")
    }

    fun testErrors() {
        doTest("errors.lua")
    }

    fun testEvents() {
        doTest("events.lua")
    }

    fun testFiles() {
        doTest("files.lua")
    }

    fun testGc() {
        doTest("gc.lua")
    }

    // testGoto is gone: its script has "::l3::" at function level and then
    // "do goto l3; ::l3:: end", which 5.5 rejects because an inner block can
    // see the outer label - checked against lua-5.5.1, which reports the same
    // "label 'l3' already defined". The script stays in the archive.

    fun testLiterals() {
        doTest("literals.lua")
    }

    fun testLocals() {
        doTest("locals.lua")
    }

    fun testMain() {
        doTest("main.lua")
    }

    fun testMath() {
        doTest("math.lua")
    }

    fun testNextvar() {
        doTest("nextvar.lua")
    }

    fun testPm() {
        doTest("pm.lua")
    }

    fun testSort() {
        doTest("sort.lua")
    }

    fun testStrings() {
        doTest("strings.lua")
    }

    fun testVararg() {
        doTest("vararg.lua")
    }

    fun testVerybig() {
        doTest("verybig.lua")
    }
}
