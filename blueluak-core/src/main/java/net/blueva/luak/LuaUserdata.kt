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


class LuaUserdata : LuaValue {
    var m_instance: Object
    var m_metatable: LuaValue? = null

    constructor(obj: Object) {
        m_instance = obj
    }

    constructor(obj: Object, metatable: LuaValue?) {
        m_instance = obj
        m_metatable = metatable
    }

    fun tojstring(): String {
        return String.valueOf(m_instance)
    }

    fun type(): Int {
        return LuaValue.TUSERDATA
    }

    fun typename(): String? {
        return "userdata"
    }

    fun hashCode(): Int {
        return m_instance.hashCode()
    }

    fun userdata(): Object {
        return m_instance
    }

    fun isuserdata(): Boolean {
        return true
    }

    fun isuserdata(c: Class): Boolean {
        return c.isAssignableFrom(m_instance.getClass())
    }

    fun touserdata(): Object {
        return m_instance
    }

    fun touserdata(c: Class): Object? {
        return if (c.isAssignableFrom(m_instance.getClass())) m_instance else null
    }

    fun optuserdata(defval: Object?): Object {
        return m_instance
    }

    fun optuserdata(c: Class, defval: Object?): Object {
        if (!c.isAssignableFrom(m_instance.getClass())) typerror(c.getName())
        return m_instance
    }

    fun getmetatable(): LuaValue? {
        return m_metatable
    }

    fun setmetatable(metatable: LuaValue?): LuaValue? {
        this.m_metatable = metatable
        return this
    }

    fun checkuserdata(): Object {
        return m_instance
    }

    fun checkuserdata(c: Class): Object {
        if (c.isAssignableFrom(m_instance.getClass())) return m_instance
        return typerror(c.getName())
    }

    fun get(key: LuaValue?): LuaValue {
        return if (m_metatable != null) gettable(this, key) else NIL
    }

    fun set(key: LuaValue?, value: LuaValue?) {
        if (m_metatable == null || !settable(this, key, value)) error("cannot set " + key + " for userdata")
    }

    fun equals(`val`: Object?): Boolean {
        if (this === `val`) return true
        if (`val` !is LuaUserdata) return false
        val u = `val`
        return m_instance.equals(u.m_instance)
    }

    // equality w/ metatable processing
    fun eq(`val`: LuaValue): LuaValue {
        return if (eq_b(`val`)) TRUE else FALSE
    }

    fun eq_b(`val`: LuaValue): Boolean {
        if (`val`.raweq(this)) return true
        if (m_metatable == null || !`val`.isuserdata()) return false
        val valmt: LuaValue? = `val`.getmetatable()
        return valmt != null && LuaValue.eqmtcall(this, m_metatable, `val`, valmt)
    }

    // equality w/o metatable processing
    fun raweq(`val`: LuaValue): Boolean {
        return `val`.raweq(this)
    }

    fun raweq(`val`: LuaUserdata): Boolean {
        return this === `val` || (m_metatable === `val`.m_metatable && m_instance.equals(`val`.m_instance))
    }

    // __eq metatag processing
    fun eqmt(`val`: LuaValue): Boolean {
        return if (m_metatable != null && `val`.isuserdata()) LuaValue.eqmtcall(
            this,
            m_metatable,
            `val`,
            `val`.getmetatable()
        ) else false
    }
}
