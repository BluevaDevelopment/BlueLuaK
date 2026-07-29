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
 * Class to encapsulate behavior of the singleton instance `nil`
 * 
 * 
 * There will be one instance of this class, [LuaValue.NIL],
 * per Java virtual machine.
 * However, the [Varargs] instance [LuaValue.NONE]
 * which is the empty list,
 * is also considered treated as a nil value by default.
 * 
 * 
 * Although it is possible to test for nil using Java == operator,
 * the recommended approach is to use the method [LuaValue.isnil]
 * instead.  By using that any ambiguities between
 * [LuaValue.NIL] and [LuaValue.NONE] are avoided.
 * @see LuaValue
 * 
 * @see LuaValue.NIL
 */
class LuaNil internal constructor() : LuaValue() {
    fun type(): Int {
        return LuaValue.TNIL
    }

    fun toString(): String? {
        return "nil"
    }

    fun typename(): String? {
        return "nil"
    }

    fun tojstring(): String? {
        return "nil"
    }

    fun not(): LuaValue {
        return LuaValue.TRUE
    }

    fun toboolean(): Boolean {
        return false
    }

    fun isnil(): Boolean {
        return true
    }

    fun getmetatable(): LuaValue? {
        return net.blueva.luak.LuaNil.Companion.s_metatable
    }

    fun equals(o: Object?): Boolean {
        return o is LuaNil
    }

    fun checknotnil(): LuaValue {
        return argerror("value")
    }

    fun isvalidkey(): Boolean {
        return false
    }

    // optional argument conversions - nil alwas falls badk to default value
    fun optboolean(defval: Boolean): Boolean {
        return defval
    }

    fun optclosure(defval: LuaClosure?): LuaClosure? {
        return defval
    }

    fun optdouble(defval: Double): Double {
        return defval
    }

    fun optfunction(defval: LuaFunction?): LuaFunction? {
        return defval
    }

    fun optint(defval: Int): Int {
        return defval
    }

    fun optinteger(defval: LuaInteger?): LuaInteger? {
        return defval
    }

    fun optlong(defval: Long): Long {
        return defval
    }

    fun optnumber(defval: LuaNumber?): LuaNumber? {
        return defval
    }

    fun opttable(defval: LuaTable?): LuaTable? {
        return defval
    }

    fun optthread(defval: LuaThread?): LuaThread? {
        return defval
    }

    fun optjstring(defval: String?): String? {
        return defval
    }

    fun optstring(defval: LuaString?): LuaString? {
        return defval
    }

    fun optuserdata(defval: Object?): Object? {
        return defval
    }

    fun optuserdata(c: Class?, defval: Object?): Object? {
        return defval
    }

    fun optvalue(defval: LuaValue?): LuaValue? {
        return defval
    }

    companion object {
        val _NIL: LuaNil = net.blueva.luak.LuaNil()

        var s_metatable: LuaValue? = null
    }
}
