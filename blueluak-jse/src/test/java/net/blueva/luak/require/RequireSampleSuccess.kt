package net.blueva.luak.require

import net.blueva.luak.LuaValue
import net.blueva.luak.lib.TwoArgFunction

/**
 * This should succeed as a library that can be loaded dynamically via "require()"
 */
class RequireSampleSuccess : TwoArgFunction() {
    override fun call(modname: LuaValue?, env: LuaValue?): LuaValue {
        env!!.checkglobals()
        return valueOf("require-sample-success-" + modname!!.tojstring())
    }
}
