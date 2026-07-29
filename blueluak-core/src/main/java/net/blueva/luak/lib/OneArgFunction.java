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
package net.blueva.luak.lib;

import net.blueva.luak.LuaValue;
import net.blueva.luak.Varargs;

/** Abstract base class for Java function implementations that take one argument and 
 * return one value. 
 * <p>
 * Subclasses need only implement {@link LuaValue#call(LuaValue)} to complete this class, 
 * simplifying development.  
 * All other uses of {@link #call()}, {@link #invoke(Varargs)},etc, 
 * are routed through this method by this class, 
 * dropping or extending arguments with {@code nil} values as required.
 * <p>
 * If more than one argument are required, or no arguments are required, 
 * or variable argument or variable return values, 
 * then use one of the related function
 * {@link ZeroArgFunction}, {@link TwoArgFunction}, {@link ThreeArgFunction}, or {@link VarArgFunction}.
 * <p>
 * See {@link LibFunction} for more information on implementation libraries and library functions.
 * @see #call(LuaValue)
 * @see LibFunction
 * @see ZeroArgFunction
 * @see TwoArgFunction
 * @see ThreeArgFunction
 * @see VarArgFunction
 */
abstract public class OneArgFunction extends LibFunction {

	abstract public LuaValue call(LuaValue arg);
	
	/** Default constructor */
	public OneArgFunction() {
	}
		
	public final LuaValue call() {
		return call(NIL);
	}

	public final LuaValue call(LuaValue arg1, LuaValue arg2) {
		return call(arg1);
	}

	public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
		return call(arg1);
	}

	public Varargs invoke(Varargs varargs) {
		return call(varargs.arg1());
	}
} 
