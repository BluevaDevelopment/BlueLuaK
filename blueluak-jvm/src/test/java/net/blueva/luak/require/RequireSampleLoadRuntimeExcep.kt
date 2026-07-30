package net.blueva.luak.require

import net.blueva.luak.LuaValue
import net.blueva.luak.lib.ZeroArgFunction

/**
 * This should fail while trying to load via "require()" because it throws a RuntimeException
 * 
 */
class RequireSampleLoadRuntimeExcep : ZeroArgFunction() {
    override fun call(): LuaValue? {
        throw RuntimeException("sample-load-runtime-exception")
    }
}
