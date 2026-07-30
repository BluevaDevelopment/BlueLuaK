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
 *  Based on LuaJ (https://luaj.org)
 *  Original work Copyright (c) 2009 Luaj.org
 *  Modifications Copyright (c) 2026 Blueva Development
 *
 *  SPDX-License-Identifier: MIT
 ******************************************************************************/
package net.blueva.luak

import java.io.IOException
import java.io.InputStream

/**
 * Test argument type check errors
 * 
 * Results are compared for exact match with
 * the installed C-based lua environment.
 */
class ErrorsTest : ScriptDrivenTest(PlatformType.JVM, dir) {
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
    }

    fun testBaseLibArgs() {
        globals!!.STDIN = object : InputStream() {
            @Throws(IOException::class)
            override fun read(): Int {
                return -1
            }
        }
        runTest("baselibargs")
    }

    fun testCoroutineLibArgs() {
        runTest("coroutinelibargs")
    }

    fun testDebugLibArgs() {
        runTest("debuglibargs")
    }

    fun testIoLibArgs() {
        runTest("iolibargs")
    }

    fun testMathLibArgs() {
        runTest("mathlibargs")
    }

    fun testModuleLibArgs() {
        runTest("modulelibargs")
    }

    fun testOperators() {
        runTest("operators")
    }

    fun testStringLibArgs() {
        runTest("stringlibargs")
    }

    fun testTableLibArgs() {
        runTest("tablelibargs")
    }

    companion object {
        private const val dir = "errors/"
    }
}
