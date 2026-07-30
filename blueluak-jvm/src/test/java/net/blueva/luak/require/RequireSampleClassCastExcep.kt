package net.blueva.luak.require

import net.blueva.luak.LuaValue

/**
 * This should fail while trying to load via "require() because it is not a LibFunction"
 * 
 */
class RequireSampleClassCastExcep {
    fun call(): LuaValue {
        return LuaValue.valueOf("require-sample-class-cast-excep")
    }
}
