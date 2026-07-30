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

/**
 * Base class for representing numbers as lua values directly.
 * 
 * 
 * The main subclasses are [LuaInteger] which holds values that fit in a java int,
 * and [LuaDouble] which holds all other number values.
 * @see LuaInteger
 * 
 * @see LuaDouble
 * 
 * @see LuaValue
 */
abstract
class LuaNumber : LuaValue() {
    override fun type(): Int {
        return TNUMBER
    }

    override fun typename(): String? {
        return "number"
    }

    override fun checknumber(): LuaNumber {
        return this
    }

    override fun checknumber(errmsg: String?): LuaNumber {
        return this
    }

    override fun optnumber(defval: LuaNumber?): LuaNumber {
        return this
    }

    override fun tonumber(): LuaValue {
        return this
    }

    override fun isnumber(): Boolean {
        return true
    }

    override fun isstring(): Boolean {
        return true
    }

    override fun getmetatable(): LuaValue? {
        return net.blueva.luak.LuaNumber.Companion.s_metatable
    }

    override fun concat(rhs: LuaValue): LuaValue {
        return rhs.concatTo(this)
    }

    override fun concat(rhs: Buffer): Buffer {
        return rhs.concatTo(this)
    }

    override fun concatTo(lhs: LuaNumber): LuaValue {
        return strvalue()!!.concatTo((lhs.strvalue())!!)
    }

    fun concatTo(lhs: LuaString?): LuaValue {
        return strvalue()!!.concatTo((lhs)!!)
    }

    companion object {
        /** Shared static metatable for all number values represented in lua.  */
        var s_metatable: LuaValue? = null
    }
}
