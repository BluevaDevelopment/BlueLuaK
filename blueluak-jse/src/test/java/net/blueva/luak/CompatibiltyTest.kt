/******************************************************************************
 *  ____  _            _                _  __
 * | __ )| |_   _  ___| |   _   _  __ _| |/ /
 * |  _ \| | | | |/ _ \ |  | | | |/ _` | ' /
 * | |_) | | |_| |  __/ |__| |_| | (_| | .  * |____/|_|\__,_|\___|_____\__,_|\__,_|_|\_\
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

import junit.framework.TestSuite
import net.blueva.luak.luajc.LuaJC.Companion.install

/**
 * Compatibility tests for the Luaj VM
 * 
 * Results are compared for exact match with
 * the installed C-based lua environment.
 */
object CompatibiltyTest : TestSuite() {
    private const val dir = ""

    fun suite(): TestSuite {
        val suite = TestSuite("Compatibility Tests")
        suite.addTest(TestSuite(JseCompatibilityTest::class.java, "JSE Compatibility Tests"))
        suite.addTest(TestSuite(LuaJCCompatibilityTest::class.java, "LuaJC Compatibility Tests"))
        return suite
    }

    abstract class CompatibiltyTestSuite constructor(platform: PlatformType?) :
        ScriptDrivenTest(platform!!, dir) {
        var savedStringMetatable: LuaValue? = null

        @Throws(Exception::class)
        override fun setUp() {
            savedStringMetatable = LuaString.s_metatable
            super.setUp()
        }

        @Throws(Exception::class)
        override fun tearDown() {
            super.tearDown()
            LuaNil.s_metatable = null
            LuaBoolean.s_metatable = null
            LuaNumber.s_metatable = null
            LuaFunction.s_metatable = null
            LuaThread.s_metatable = null
            LuaString.s_metatable = savedStringMetatable
        }

        fun testBaseLib() {
            runTest("baselib")
        }

        fun testCoroutineLib() {
            runTest("coroutinelib")
        }

        open fun testDebugLib() {
            runTest("debuglib")
        }

        fun testErrors() {
            runTest("errors")
        }

        fun testFunctions() {
            runTest("functions")
        }

        fun testIoLib() {
            runTest("iolib")
        }

        fun testManyUpvals() {
            runTest("manyupvals")
        }

        fun testMathLib() {
            runTest("mathlib")
        }

        fun testMetatags() {
            runTest("metatags")
        }

        fun testOsLib() {
            runTest("oslib")
        }

        fun testStringLib() {
            runTest("stringlib")
        }

        fun testTableLib() {
            runTest("tablelib")
        }

        fun testTailcalls() {
            runTest("tailcalls")
        }

        fun testUpvalues() {
            runTest("upvalues")
        }

        fun testVm() {
            runTest("vm")
        }
    }


    class JseCompatibilityTest : CompatibiltyTestSuite(PlatformType.JSE) {
        @Throws(Exception::class)
        override fun setUp() {
            super.setUp()
            System.setProperty("JME", "false")
        }
    }

    class LuaJCCompatibilityTest : CompatibiltyTestSuite(PlatformType.LUAJIT) {
        @Throws(Exception::class)
        override fun setUp() {
            super.setUp()
            System.setProperty("JME", "false")
            install(globals!!)
        }

        // not supported on this platform - don't test
        override fun testDebugLib() {}
    }
}
