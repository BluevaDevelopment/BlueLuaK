package net.blueva.luak.lib.jse

import junit.framework.TestCase
import net.blueva.luak.Globals
import net.blueva.luak.LuaValue
import net.blueva.luak.lib.jse.JsePlatform.standardGlobals

class LuajavaAccessibleMembersTest : TestCase() {
    private var globals: Globals? = null

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        globals = standardGlobals()
    }

    private fun invokeScript(script: String?): String? {
        try {
            val c: LuaValue = globals!!.load(script!!, "script")!!
            return c.call().tojstring()
        } catch (e: Exception) {
            fail("exception: " + e)
            return "failed"
        }
    }

    fun testAccessFromPrivateClassImplementedMethod() {
        TestCase.assertEquals(
            "privateImpl-aaa-interface_method(bar)", invokeScript(
                "b = luajava.newInstance('" + TestClass::class.java.getName() + "');" +
                        "a = b:create_PrivateImpl('aaa');" +
                        "return a:interface_method('bar');"
            )
        )
    }

    fun testAccessFromPrivateClassPublicMethod() {
        TestCase.assertEquals(
            "privateImpl-aaa-public_method", invokeScript(
                "b = luajava.newInstance('" + TestClass::class.java.getName() + "');" +
                        "a = b:create_PrivateImpl('aaa');" +
                        "return a:public_method();"
            )
        )
    }

    fun testAccessFromPrivateClassGetPublicField() {
        TestCase.assertEquals(
            "aaa", invokeScript(
                "b = luajava.newInstance('" + TestClass::class.java.getName() + "');" +
                        "a = b:create_PrivateImpl('aaa');" +
                        "return a.public_field;"
            )
        )
    }

    fun testAccessFromPrivateClassSetPublicField() {
        TestCase.assertEquals(
            "foo", invokeScript(
                "b = luajava.newInstance('" + TestClass::class.java.getName() + "');" +
                        "a = b:create_PrivateImpl('aaa');" +
                        "a.public_field = 'foo';" +
                        "return a.public_field;"
            )
        )
    }

    fun testAccessFromPrivateClassPublicConstructor() {
        TestCase.assertEquals(
            "privateImpl-constructor", invokeScript(
                "b = luajava.newInstance('" + TestClass::class.java.getName() + "');" +
                        "c = b:get_PrivateImplClass();" +
                        "return luajava.new(c);"
            )
        )
    }

    fun testAccessPublicEnum() {
        TestCase.assertEquals(
            "class net.blueva.luak.lib.jse.TestClass\$SomeEnum", invokeScript(
                "b = luajava.newInstance('" + TestClass::class.java.getName() + "');" +
                        "return b.SomeEnum"
            )
        )
    }
}
