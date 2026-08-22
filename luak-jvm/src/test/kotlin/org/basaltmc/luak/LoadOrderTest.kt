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
 *  Based on LuaJ (https://luaj.org)
 *  Original work Copyright (c) 2009 Luaj.org
 *  Modifications Copyright (c) 2026 Basalt Project
 *
 *  SPDX-License-Identifier: MIT
 ******************************************************************************/
package org.basaltmc.luak

import junit.framework.TestCase
import org.basaltmc.luak.lib.jvm.JvmPlatform.standardGlobals
import org.basaltmc.luak.server.Launcher
import org.basaltmc.luak.server.LuaClassLoader
import java.io.InputStream
import java.io.Reader

// Tests using class loading orders that have caused problems for some use cases.
class LoadOrderTest : TestCase() {
    fun testLoadGlobalsFirst() {
        val g = standardGlobals()
        assertNotNull(g)
    }

    fun testLoadStringFirst() {
        val BAR = LuaString.valueOf("bar")
        assertNotNull(BAR)
    }

    class TestLauncherLoadStringFirst : Launcher {
        override fun launch(script: String?, arg: Array<Any?>?): Array<Any?>? {
            return arrayOf<Any?>(FOO)
        }

        override fun launch(script: InputStream?, arg: Array<Any?>?): Array<Any?>? {
            return null
        }

        override fun launch(script: Reader?, arg: Array<Any?>?): Array<Any?>? {
            return null
        }

        companion object {
            // Static initializer that causes LuaString->LuaValue->LuaString
            private val FOO = LuaString.valueOf("foo")
        }
    }

    @Throws(Exception::class)
    fun testClassLoadsStringFirst() {
        val launcher = LuaClassLoader
            .NewLauncher(TestLauncherLoadStringFirst::class.java)
        val results = launcher.launch("foo", null)
        assertNotNull(results)
    }
}
