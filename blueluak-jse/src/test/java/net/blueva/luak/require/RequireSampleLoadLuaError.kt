package net.blueva.luak.require

import net.blueva.luak.LuaValue
import net.blueva.luak.lib.ZeroArgFunction

/**
 * This should fail while trying to load via
 * "require()" because it throws a LuaError
 * 
 */
class RequireSampleLoadLuaError : ZeroArgFunction() {
    override fun call(): LuaValue? {
        error("sample-load-lua-error")
        return valueOf("require-sample-load-lua-error")
    }
}
