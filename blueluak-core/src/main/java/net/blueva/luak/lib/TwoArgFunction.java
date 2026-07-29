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

/** Abstract base class for Java function implementations that take two arguments and 
 * return one value. 
 * <p>
 * Subclasses need only implement {@link LuaValue#call(LuaValue,LuaValue)} to complete this class, 
 * simplifying development.  
 * All other uses of {@link #call()}, {@link #invoke(Varargs)},etc, 
 * are routed through this method by this class, 
 * dropping or extending arguments with {@code nil} values as required.
 * <p>
 * If more or less than two arguments are required,  
 * or variable argument or variable return values, 
 * then use one of the related function
 * {@link ZeroArgFunction}, {@link OneArgFunction}, {@link ThreeArgFunction}, or {@link VarArgFunction}.
 * <p>
 * See {@link LibFunction} for more information on implementation libraries and library functions.
 * @see #call(LuaValue,LuaValue)
 * @see LibFunction
 * @see ZeroArgFunction
 * @see OneArgFunction
 * @see ThreeArgFunction
 * @see VarArgFunction
 */
abstract public class TwoArgFunction extends LibFunction {

	abstract public LuaValue call(LuaValue arg1, LuaValue arg2);
	
	/** Default constructor */
	public TwoArgFunction() {
	}
	
	public final LuaValue call() {
		return call(NIL, NIL);
	}

	public final LuaValue call(LuaValue arg) {
		return call(arg, NIL);
	}

	public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
		return call(arg1, arg2);
	}
	
	public Varargs invoke(Varargs varargs) {
		return call(varargs.arg1(),varargs.arg(2));
	}
	
} 
