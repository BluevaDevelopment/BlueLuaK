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

public class Upvaldesc {

	/* upvalue name (for debug information) */
	public LuaString name;
	
	/* whether it is in stack */
	public final boolean instack;
	
	/* index of upvalue (in stack or in outer function's list) */
	public final short idx;
	
	public Upvaldesc(LuaString name, boolean instack, int idx) {
		this.name = name;
		this.instack = instack;
		this.idx = (short) idx;
	}
	
	public String toString() {
		return idx + (instack? " instack ": " closed ") + String.valueOf(name); 
	}
}
