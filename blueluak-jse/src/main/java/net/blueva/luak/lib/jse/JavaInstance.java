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
package net.blueva.luak.lib.jse;

import java.lang.reflect.Field;

import net.blueva.luak.LuaError;
import net.blueva.luak.LuaUserdata;
import net.blueva.luak.LuaValue;

/**
 * LuaValue that represents a Java instance.
 * <p>
 * Will respond to get() and set() by returning field values or methods. 
 * <p>
 * This class is not used directly.  
 * It is returned by calls to {@link CoerceJavaToLua#coerce(Object)} 
 * when a subclass of Object is supplied.
 * @see CoerceJavaToLua
 * @see CoerceLuaToJava
 */
class JavaInstance extends LuaUserdata {

	JavaClass jclass;
	
	JavaInstance(Object instance) {
		super(instance);
	}

	public LuaValue get(LuaValue key) {
		if ( jclass == null )
			jclass = JavaClass.forClass(m_instance.getClass());
		Field f = jclass.getField(key);
		if ( f != null )
			try {
				return CoerceJavaToLua.coerce(f.get(m_instance));
			} catch (Exception e) {
				throw new LuaError(e);
			}
		LuaValue m = jclass.getMethod(key);
		if ( m != null )
			return m;
		Class c = jclass.getInnerClass(key);
		if ( c != null )
			return JavaClass.forClass(c);
		return super.get(key);
	}

	public void set(LuaValue key, LuaValue value) {
		if ( jclass == null )
			jclass = JavaClass.forClass(m_instance.getClass());
		Field f = jclass.getField(key);
		if ( f != null )
			try {
				f.set(m_instance, CoerceLuaToJava.coerce(value, f.getType()));
				return;
			} catch (Exception e) {
				throw new LuaError(e);
			}
		super.set(key, value);
	} 	
	
}
