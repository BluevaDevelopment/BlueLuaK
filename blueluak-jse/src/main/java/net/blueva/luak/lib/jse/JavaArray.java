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

import java.lang.reflect.Array;

import net.blueva.luak.LuaTable;
import net.blueva.luak.LuaUserdata;
import net.blueva.luak.LuaValue;
import net.blueva.luak.lib.OneArgFunction;

/**
 * LuaValue that represents a Java instance of array type.
 * <p>
 * Can get elements by their integer key index, as well as the length.
 * <p>
 * This class is not used directly.  
 * It is returned by calls to {@link CoerceJavaToLua#coerce(Object)} 
 * when an array is supplied.
 * @see CoerceJavaToLua
 * @see CoerceLuaToJava
 */
class JavaArray extends LuaUserdata {

	private static final class LenFunction extends OneArgFunction {
		public LuaValue call(LuaValue u) {
			return LuaValue.valueOf(Array.getLength(((LuaUserdata)u).m_instance));
		}
	}

	static final LuaValue LENGTH = valueOf("length");
	
	static final LuaTable array_metatable;
	static {
		array_metatable = new LuaTable();
		array_metatable.rawset(LuaValue.LEN, new LenFunction());
	}
	
	JavaArray(Object instance) {
		super(instance);
		setmetatable(array_metatable);
	}
	
	public LuaValue get(LuaValue key) {
		if ( key.equals(LENGTH) )
			return valueOf(Array.getLength(m_instance));
		if ( key.isint() ) {
			int i = key.toint() - 1;
			return i>=0 && i<Array.getLength(m_instance)?
				CoerceJavaToLua.coerce(Array.get(m_instance,key.toint()-1)):
				NIL;
		}
		return super.get(key);
	}

	public void set(LuaValue key, LuaValue value) {
		if ( key.isint() ) {
			int i = key.toint() - 1;
			if ( i>=0 && i<Array.getLength(m_instance) )
				Array.set(m_instance,i,CoerceLuaToJava.coerce(value, m_instance.getClass().getComponentType()));
			else if ( m_metatable==null || ! settable(this,key,value) )
					error("array index out of bounds");
		}
		else
			super.set(key, value);
	} 	
}
