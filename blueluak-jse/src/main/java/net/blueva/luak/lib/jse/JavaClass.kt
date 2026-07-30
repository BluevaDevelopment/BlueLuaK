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
package net.blueva.luak.lib.jse

import net.blueva.luak.LuaValue
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*
import kotlin.math.max

/**
 * LuaValue that represents a Java class.
 * 
 * 
 * Will respond to get() and set() by returning field values, or java methods.
 * 
 * 
 * This class is not used directly.
 * It is returned by calls to [CoerceJavaToLua.coerce]
 * when a Class is supplied.
 * @see CoerceJavaToLua
 * 
 * @see CoerceLuaToJava
 */
internal class JavaClass(c: Class<*>?) : JavaInstance(c), CoerceJavaToLua.Coercion {
    var fields: MutableMap<Any?, Any?>? = null
    var methods: MutableMap<Any?, Any?>? = null
    var innerclasses: MutableMap<Any?, Any?>? = null

    init {
        this.jclass = this
    }

    override fun coerce(javaValue: Any?): LuaValue {
        return this
    }

    fun getField(key: LuaValue?): Field? {
        if (fields == null) {
            val m: MutableMap<Any?, Any?> = HashMap<Any?, Any?>()
            val f = (m_instance as Class<*>).getFields()
            for (i in f.indices) {
                val fi = f[i]
                if (Modifier.isPublic(fi.getModifiers())) {
                    m.put(valueOf(fi.getName()), fi)
                    try {
                        if (!fi.isAccessible()) fi.setAccessible(true)
                    } catch (s: SecurityException) {
                    }
                }
            }
            fields = m
        }
        return fields!!.get(key) as Field?
    }

    fun getMethod(key: LuaValue?): LuaValue? {
        if (methods == null) {
            val namedlists: MutableMap<Any?, Any?> = HashMap<Any?, Any?>()
            val m = (m_instance as Class<*>).getMethods()
            for (i in m.indices) {
                val mi = m[i]
                if (Modifier.isPublic(mi.getModifiers())) {
                    val name = mi.getName()
                    var list = namedlists.get(name) as MutableList<Any?>?
                    if (list == null) namedlists.put(name, ArrayList<Any?>().also { list = it })
                    list!!.add(JavaMethod.Companion.forMethod(mi))
                }
            }
            val map: MutableMap<Any?, Any?> = HashMap<Any?, Any?>()
            val c = (m_instance as Class<*>).getConstructors()
            val list: MutableList<Any?> = ArrayList<Any?>()
            for (i in c.indices) if (Modifier.isPublic(c[i].getModifiers())) list.add(
                JavaConstructor.Companion.forConstructor(
                    c[i]
                )
            )
            when (list.size) {
                0 -> {}
                1 -> map.put(NEW, list.get(0))
                else -> map.put(
                    NEW,
                    JavaConstructor.Companion.forConstructors(list.toTypedArray() as Array<JavaConstructor?>)
                )
            }

            val it: MutableIterator<MutableMap.MutableEntry<Any?, Any?>> = namedlists.entries.iterator()
            while (it.hasNext()) {
                val e = it.next()
                val name = e.key as String?
                val methods = e.value as MutableList<Any?>
                map.put(
                    valueOf(name),
                    if (methods.size == 1) methods.get(0) else JavaMethod.Companion.forMethods(methods.toTypedArray() as Array<JavaMethod?>)
                )
            }
            methods = map
        }
        return methods!!.get(key) as LuaValue?
    }

    fun getInnerClass(key: LuaValue?): Class<*>? {
        if (innerclasses == null) {
            val m: MutableMap<Any?, Any?> = HashMap<Any?, Any?>()
            val c = (m_instance as Class<*>).getClasses()
            for (i in c.indices) {
                val ci = c[i]
                val name = ci.getName()
                val stub = name.substring(max(name.lastIndexOf('$'), name.lastIndexOf('.')) + 1)
                m.put(valueOf(stub), ci)
            }
            innerclasses = m
        }
        return innerclasses!!.get(key) as Class<*>?
    }

    val constructor: LuaValue?
        get() = getMethod(NEW)

    companion object {
        val classes: MutableMap<Any?, Any?> = Collections.synchronizedMap<Any?, Any?>(HashMap<Any?, Any?>())

        val NEW: LuaValue = valueOf("new")

        @JvmStatic
        fun forClass(c: Class<*>?): JavaClass {
            var j = classes.get(c) as JavaClass?
            if (j == null) classes.put(c, JavaClass(c).also { j = it })
            return j!!
        }
    }
}
