package net.blueva.luak.lib.jse

import junit.framework.TestCase
import net.blueva.luak.LuaValue
import net.blueva.luak.lib.jse.JsePlatform.standardGlobals

class JsePlatformTest : TestCase() {
    fun testLuaMainPassesArguments() {
        val globals = standardGlobals()
        val chunk: LuaValue = globals.load("return #arg, arg.n, arg[2], arg[1]")!!
        val results = JsePlatform.luaMain(chunk, arrayOf<String>("aaa", "bbb"))
        TestCase.assertEquals(results!!.narg(), 4)
        assertEquals(results.arg(1), LuaValue.valueOf(2))
        assertEquals(results.arg(2), LuaValue.valueOf(2))
        assertEquals(results.arg(3), LuaValue.valueOf("bbb"))
        assertEquals(results.arg(4), LuaValue.valueOf("aaa"))
    }
}
