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

import junit.framework.TestCase
import net.blueva.luak.lib.jvm.JvmPlatform.standardGlobals

class UTF8StreamTest : TestCase() {
    fun testUtf8CharsInStream() {
        val script = ("x = \"98\u00b0: today's temp!\"\n"
                + "print('x = ', x)\n"
                + "return x")
        val globals = standardGlobals()
        val chunk: LuaValue = globals.load(script)!!
        val result = chunk.call()
        val str: String? = result!!.tojstring()
        TestCase.assertEquals("98\u00b0: today's temp!", str)
    }
}
