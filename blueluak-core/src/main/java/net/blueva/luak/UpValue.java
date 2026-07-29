/******************************************************************************
 *  ____  _            _                    _
 * | __ )| |_   _  ___| |   _   _  __ _    | |
 * |  _ \| | | | |/ _ \ |  | | | |/ _` |_  | |
 * | |_) | | |_| |  __/ |__| |_| | (_| | |_| |
 * |____/|_|\__,_|\___|_____\__,_|\__,_|\___/
 *
 *  BlueLuaK
 *  https://github.com/BluevaDevelopment/BlueLuaK/t
 *
 *  Based on LuaJ (https://luaj.org)
 *  Original work Copyright (c) 2009 Luaj.org
 *  Modifications Copyright (c) 2026 Blueva Development
 *
 *  SPDX-License-Identifier: MIT
 ******************************************************************************/
package net.blueva.luak;


/** Upvalue used with Closure formulation
 * <p>
 * @see LuaClosure
 * @see Prototype
 */
public final class UpValue {

	LuaValue[] array; // initially the stack, becomes a holder 
	int index;

	/**
	 *  Create an upvalue relative to a stack
	 * @param stack the stack
	 * @param index the index on the stack for the upvalue
	 */
	public UpValue( LuaValue[] stack, int index) {
		this.array = stack;
		this.index = index;
	}

	public String toString() {
		return index + "/" + array.length + " " + array[index];
	}
	
	/** 
	 * Convert this upvalue to a Java String
	 * @return the Java String for this upvalue.
	 * @see LuaValue#tojstring()
	 */
	public String tojstring() {
		return array[index].tojstring();
	}
	
	/**
	 * Get the value of the upvalue
	 * @return the {@link LuaValue} for this upvalue
	 */
	public final LuaValue getValue() {
		return array[index];
	}
	
	/**
	 * Set the value of the upvalue
	 * @param value the {@link LuaValue} to set it to
	 */
	public final void setValue( LuaValue value ) {
		array[index] = value;
	}
	
	/**
	 * Close this upvalue so it is no longer on the stack
	 */
	public final void close() {
		LuaValue[] old = array;
		array = new LuaValue[] { old[index] };
		old[index] = null;
		index = 0;
	}
}
