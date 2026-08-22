/******************************************************************************
 *  _                _
 * | |   _   _  __ _| | __
 * | |  | | | |/ _` | |/ /
 * | |__| |_| | (_| |   <
 * |_____\__,_|\__,_|_|\_\
 *
 *  Luak
 *  https://github.com/BasaltProject/Luak
 *
 *  Based on LuaJ (https://luaj.org)
 *  Original work Copyright (c) 2009 Luaj.org
 *  Modifications Copyright (c) 2026 Basalt Project
 *
 *  SPDX-License-Identifier: MIT
 ******************************************************************************/
package org.basaltmc.luak.lib.jvm

import org.basaltmc.luak.LuaError
import org.basaltmc.luak.LuaUserdata
import org.basaltmc.luak.LuaValue

/**
 * LuaValue that represents a Java instance.
 * 
 * 
 * Will respond to get() and set() by returning field values or methods.
 * 
 * 
 * This class is not used directly.
 * It is returned by calls to [CoerceJavaToLua.coerce]
 * when a subclass of Object is supplied.
 * @see CoerceJavaToLua
 * 
 * @see CoerceLuaToJava
 */
internal open class JavaInstance(instance: Any?) : LuaUserdata(instance!!) {
    var jclass: JavaClass? = null

    override fun get(key: LuaValue): LuaValue {
        if (jclass == null) jclass = JavaClass.Companion.forClass(m_instance.javaClass)
        val f = jclass!!.getField(key)
        if (f != null) try {
            return CoerceJavaToLua.coerce(f.get(m_instance))!!
        } catch (e: Exception) {
            throw LuaError(e)
        }
        val m = jclass!!.getMethod(key)
        if (m != null) return m
        val c = jclass!!.getInnerClass(key)
        if (c != null) return JavaClass.Companion.forClass(c)
        return super.get(key)!!
    }

    override fun set(key: LuaValue?, value: LuaValue?) {
        if (jclass == null) jclass = JavaClass.Companion.forClass(m_instance.javaClass)
        val f = jclass!!.getField(key)
        if (f != null) try {
            f.set(m_instance, CoerceLuaToJava.coerce(value, f.getType()))
            return
        } catch (e: Exception) {
            throw LuaError(e)
        }
        super.set(key, value)
    }
}
