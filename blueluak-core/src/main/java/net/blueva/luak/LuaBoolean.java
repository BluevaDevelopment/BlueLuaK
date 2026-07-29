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
package net.blueva.luak;

/**
 * Extension of {@link LuaValue} which can hold a Java boolean as its value. 
 * <p>
 * These instance are not instantiated directly by clients.  
 * Instead, there are exactly twon instances of this class, 
 * {@link LuaValue#TRUE} and {@link LuaValue#FALSE} 
 * representing the lua values {@code true} and {@code false}.
 * The function {@link LuaValue#valueOf(boolean)} will always 
 * return one of these two values. 
 * <p>
 * Any {@link LuaValue} can be converted to its equivalent 
 * boolean representation using {@link LuaValue#toboolean()}
 * <p>
 * @see LuaValue
 * @see LuaValue#valueOf(boolean)
 * @see LuaValue#TRUE
 * @see LuaValue#FALSE
 */
public final class LuaBoolean extends LuaValue {

	/** The singleton instance representing lua {@code true} */
	static final LuaBoolean _TRUE = new LuaBoolean(true);
	
	/** The singleton instance representing lua {@code false} */
	static final LuaBoolean _FALSE = new LuaBoolean(false);
	
	/** Shared static metatable for boolean values represented in lua. */
	public static LuaValue s_metatable;

	/** The value of the boolean */
	public final boolean v;

	LuaBoolean(boolean b) {
		this.v = b;
	}

	public int type() {
		return LuaValue.TBOOLEAN;
	}

	public String typename() {
		return "boolean";
	}

	public boolean isboolean() {
		return true;
	}

	public LuaValue not() {
		return v ? FALSE : LuaValue.TRUE;
	}

	/**
	 * Return the boolean value for this boolean
	 * @return value as a Java boolean
	 */
	public boolean booleanValue() {
		return v;
	}

	public boolean toboolean() {
		return v;
	}

	public String tojstring() {
		return v ? "true" : "false";
	}

	public boolean optboolean(boolean defval) {
		return this.v;
	}
	
	public boolean checkboolean() {
		return v;
	}
	
	public LuaValue getmetatable() { 
		return s_metatable; 
	}
}
