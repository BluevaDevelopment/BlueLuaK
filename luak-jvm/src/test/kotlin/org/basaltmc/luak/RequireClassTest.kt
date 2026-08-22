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

import junit.framework.TestCase
import org.basaltmc.luak.lib.jvm.JvmPlatform.standardGlobals
import org.basaltmc.luak.require.RequireSampleClassCastExcep
import org.basaltmc.luak.require.RequireSampleLoadLuaError
import org.basaltmc.luak.require.RequireSampleLoadRuntimeExcep
import org.basaltmc.luak.require.RequireSampleSuccess

class RequireClassTest : TestCase() {
    private var globals: LuaTable? = null
    private var require: LuaValue? = null

    public override fun setUp() {
        globals = standardGlobals()
        require = globals!!.get("require")
    }

    fun testLoadClass() {
        val result = globals!!.load(RequireSampleSuccess())
        TestCase.assertEquals("require-sample-success-", result.tojstring())
    }

    fun testRequireClassSuccess() {
        var result = require!!.call(LuaValue.valueOf("org.basaltmc.luak.require.RequireSampleSuccess"))
        TestCase.assertEquals("require-sample-success-org.basaltmc.luak.require.RequireSampleSuccess", result!!.tojstring())
        result = require!!.call(LuaValue.valueOf("org.basaltmc.luak.require.RequireSampleSuccess"))
        TestCase.assertEquals("require-sample-success-org.basaltmc.luak.require.RequireSampleSuccess", result!!.tojstring())
    }

    fun testRequireClassLoadLuaError() {
        try {
            val result = require!!.call(LuaValue.valueOf(RequireSampleLoadLuaError::class.java.getName()))
            fail("incorrectly loaded class that threw lua error")
        } catch (le: LuaError) {
            TestCase.assertEquals(
                "sample-load-lua-error",
                le.message
            )
        }
        try {
            val result = require!!.call(LuaValue.valueOf(RequireSampleLoadLuaError::class.java.getName()))
            fail("incorrectly loaded class that threw lua error")
        } catch (le: LuaError) {
            TestCase.assertEquals(
                "loop or previous error loading module '" + RequireSampleLoadLuaError::class.java.getName() + "'",
                le.message
            )
        }
    }

    fun testRequireClassLoadRuntimeException() {
        try {
            val result = require!!.call(LuaValue.valueOf(RequireSampleLoadRuntimeExcep::class.java.getName()))
            fail("incorrectly loaded class that threw runtime exception")
        } catch (le: RuntimeException) {
            TestCase.assertEquals(
                "sample-load-runtime-exception",
                le.message
            )
        }
        try {
            val result = require!!.call(LuaValue.valueOf(RequireSampleLoadRuntimeExcep::class.java.getName()))
            fail("incorrectly loaded class that threw runtime exception")
        } catch (le: LuaError) {
            TestCase.assertEquals(
                "loop or previous error loading module '" + RequireSampleLoadRuntimeExcep::class.java.getName() + "'",
                le.message
            )
        }
    }


    fun testRequireClassClassCastException() {
        try {
            val result = require!!.call(LuaValue.valueOf(RequireSampleClassCastExcep::class.java.getName()))
            fail("incorrectly loaded class that threw class cast exception")
        } catch (le: LuaError) {
            val msg: String = le.message!!
            if (msg.indexOf("not found") < 0) fail("expected 'not found' message but got " + msg)
        }
        try {
            val result = require!!.call(LuaValue.valueOf(RequireSampleClassCastExcep::class.java.getName()))
            fail("incorrectly loaded class that threw class cast exception")
        } catch (le: LuaError) {
            val msg: String = le.message!!
            if (msg.indexOf("not found") < 0) fail("expected 'not found' message but got " + msg)
        }
    }
}
