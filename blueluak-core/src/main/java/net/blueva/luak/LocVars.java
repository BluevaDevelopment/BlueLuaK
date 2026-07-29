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
 * Data class to hold debug information relating to local variables for a {@link Prototype}
 */
public class LocVars {
	/** The local variable name */
	public LuaString varname;
	
	/** The instruction offset when the variable comes into scope */ 
	public int startpc;
	
	/** The instruction offset when the variable goes out of scope */ 
	public int endpc;
	
	/**
	 * Construct a LocVars instance. 
	 * @param varname The local variable name
	 * @param startpc The instruction offset when the variable comes into scope
	 * @param endpc The instruction offset when the variable goes out of scope
	 */
	public LocVars(LuaString varname, int startpc, int endpc) {
		this.varname = varname;
		this.startpc = startpc;
		this.endpc = endpc;
	}
	
	public String tojstring() {
		return varname+" "+startpc+"-"+endpc;
	}
}
