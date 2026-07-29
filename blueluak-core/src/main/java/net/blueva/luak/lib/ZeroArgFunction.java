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
package net.blueva.luak.lib;

import net.blueva.luak.LuaValue;
import net.blueva.luak.Varargs;

/** Abstract base class for Java function implementations that take no arguments and 
 * return one value. 
 * <p>
 * Subclasses need only implement {@link LuaValue#call()} to complete this class, 
 * simplifying development.  
 * All other uses of {@link #call(LuaValue)}, {@link #invoke(Varargs)},etc, 
 * are routed through this method by this class.
 * <p>
 * If one or more arguments are required, or variable argument or variable return values, 
 * then use one of the related function
 * {@link OneArgFunction}, {@link TwoArgFunction}, {@link ThreeArgFunction}, or {@link VarArgFunction}.
 * <p>
 * See {@link LibFunction} for more information on implementation libraries and library functions.
 * @see #call()
 * @see LibFunction
 * @see OneArgFunction
 * @see TwoArgFunction
 * @see ThreeArgFunction
 * @see VarArgFunction
 */
abstract public class ZeroArgFunction extends LibFunction {

	abstract public LuaValue call();

	/** Default constructor */
	public ZeroArgFunction() {
	}
	
	public LuaValue call(LuaValue arg) {
		return call();
	}

	public LuaValue call(LuaValue arg1, LuaValue arg2) {
		return call();
	}

	public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
		return call();
	}

	public Varargs invoke(Varargs varargs) {
		return call();
	}
} 
