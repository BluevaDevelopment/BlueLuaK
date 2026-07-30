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

    override fun tojstring(): String {
        return (m_instance).toString()
    }

    override fun type(): Int {
        return LuaValue.TUSERDATA
    }

    override fun typename(): String? {
        return "userdata"
    }

    override fun hashCode(): Int {
        return m_instance.hashCode()
    }

    fun userdata(): Object {
        return m_instance
    }

    override fun isuserdata(): Boolean {
        return true
    }

    override fun isuserdata(c: Class<*>?): Boolean {
        return c!!.isAssignableFrom(m_instance.javaClass)
    }

    override fun touserdata(): Object {
        return m_instance
    }

    override fun touserdata(c: Class<*>?): Object? {
        return if (c!!.isAssignableFrom(m_instance.javaClass)) m_instance else null
    }

    override fun optuserdata(defval: Object?): Object {
        return m_instance
    }

    override fun optuserdata(c: Class<*>, defval: Object?): Object {
        if (!c!!.isAssignableFrom(m_instance.javaClass)) typerror(c.name)
        return m_instance
    }

    override fun getmetatable(): LuaValue? {
        return m_metatable
    }

    override fun setmetatable(metatable: LuaValue?): LuaValue? {
        this.m_metatable = metatable
        return this
    }

    override fun checkuserdata(): Object {
        return m_instance
    }

    override fun checkuserdata(c: Class<*>?): Object {
        if (c!!.isAssignableFrom(m_instance.javaClass)) return m_instance
        return (typerror(c.name))!!
    }

    fun get(key: LuaValue?): LuaValue {
        return if (m_metatable != null) gettable(this, (key)!!) else NIL
    }

    override fun set(key: LuaValue?, value: LuaValue?) {
        if (m_metatable == null || !settable(this, key, value)) error("cannot set " + key + " for userdata")
    }

    override fun equals(`val`: Object?): Boolean {
        if (this === `val`) return true
        if (`val` !is LuaUserdata) return false
        val u = `val`
        return m_instance.equals(u.m_instance)
    }

    // equality w/ metatable processing
    fun eq(`val`: LuaValue): LuaValue {
        return (if (eq_b(`val`)) TRUE else FALSE)!!
    }

    fun eq_b(`val`: LuaValue): Boolean {
        if (`val`.raweq(this)) return true
        if (m_metatable == null || !`val`.isuserdata()) return false
        val valmt: LuaValue? = `val`.getmetatable()
        return valmt != null && LuaValue.eqmtcall(this, m_metatable!!, `val`, valmt)
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
            m_metatable!!,
            `val`,
            `val`.getmetatable()
        ) else false
    }
}
