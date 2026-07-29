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

import net.blueva.luak.LuaTable.Slot;

/**
 * Provides operations that depend on the __mode key of the metatable.
 */
interface Metatable {

	/** Return whether or not this table's keys are weak. */
	public boolean useWeakKeys();

	/** Return whether or not this table's values are weak. */
	public boolean useWeakValues();

	/** Return this metatable as a LuaValue. */
	public LuaValue toLuaValue();

	/** Return an instance of Slot appropriate for the given key and value. */
	public Slot entry( LuaValue key, LuaValue value );

	/** Returns the given value wrapped in a weak reference if appropriate. */
	public LuaValue wrap( LuaValue value );

	/**
	 * Returns the value at the given index in the array, or null if it is a weak reference that
	 * has been dropped.
	 */
	public LuaValue arrayget(LuaValue[] array, int index);
}
