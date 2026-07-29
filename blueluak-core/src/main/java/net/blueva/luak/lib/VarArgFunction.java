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

/** Abstract base class for Java function implementations that takes varaiable arguments and 
 * returns multiple return values. 
 * <p>
 * Subclasses need only implement {@link LuaValue#invoke(Varargs)} to complete this class, 
 * simplifying development.  
 * All other uses of {@link #call(LuaValue)}, {@link #invoke()},etc, 
 * are routed through this method by this class,
 * converting arguments to {@link Varargs} and  
 * dropping or extending return values with {@code nil} values as required.
 * <p>
 * If between one and three arguments are required, and only one return value is returned,   
 * {@link ZeroArgFunction}, {@link OneArgFunction}, {@link TwoArgFunction}, or {@link ThreeArgFunction}.
 * <p>
 * See {@link LibFunction} for more information on implementation libraries and library functions.
 * @see #invoke(Varargs)
 * @see LibFunction
 * @see ZeroArgFunction
 * @see OneArgFunction
 * @see TwoArgFunction
 * @see ThreeArgFunction
 */
abstract public class VarArgFunction extends LibFunction {

	public VarArgFunction() {
	}
	
	public LuaValue call() {
		return invoke(NONE).arg1();
	}

	public LuaValue call(LuaValue arg) {
		return invoke(arg).arg1();
	}

	public LuaValue call(LuaValue arg1, LuaValue arg2) {
		return invoke(varargsOf(arg1,arg2)).arg1();
	}

	public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
		return invoke(varargsOf(arg1,arg2,arg3)).arg1();
	}

	/** 
	 * Subclass responsibility. 
	 * May not have expected behavior for tail calls. 
	 * Should not be used if:
	 * - function has a possibility of returning a TailcallVarargs
	 * @param args the arguments to the function call.
	 */
	public Varargs invoke(Varargs args) {
		return onInvoke(args).eval();
	}
	
	public Varargs onInvoke(Varargs args) {
		return invoke(args);
	}
} 
