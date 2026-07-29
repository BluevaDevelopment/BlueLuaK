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
 * Subclass of {@link Varargs} that represents a lua tail call 
 * in a Java library function execution environment. 
 * <p>
 * Since Java doesn't have direct support for tail calls, 
 * any lua function whose {@link Prototype} contains the 
 * {@link Lua#OP_TAILCALL} bytecode needs a mechanism 
 * for tail calls when converting lua-bytecode to java-bytecode.
 * <p>
 * The tail call holds the next function and arguments, 
 * and the client a call to {@link #eval()} executes the function
 * repeatedly until the tail calls are completed. 
 * <p>
 * Normally, users of luaj need not concern themselves with the 
 * details of this mechanism, as it is built into the core 
 * execution framework. 
 * @see Prototype 
 * @see net.blueva.luak.luajc.LuaJC
 */
public class TailcallVarargs extends Varargs {

	private LuaValue func;
	private Varargs args;
	private Varargs result;
	
	public TailcallVarargs(LuaValue f, Varargs args) {
		this.func = f;
		this.args = args;
	}
	
	public TailcallVarargs(LuaValue object, LuaValue methodname, Varargs args) {
		this.func = object.get(methodname);
		this.args = LuaValue.varargsOf(object, args);
	}
	
	public boolean isTailcall() {
		return true;
	}
	
	public Varargs eval() {
		while ( result == null ) {
			Varargs r = func.onInvoke(args);
			if (r.isTailcall()) {
				TailcallVarargs t = (TailcallVarargs) r;
				func = t.func;
				args = t.args;
			}
			else {
				result = r;			
				func = null;
				args = null;
			}
		}
		return result;
	}
	
	public LuaValue arg( int i ) {
		if ( result == null )
			eval();
		return result.arg(i);
	}
	
	public LuaValue arg1() {
		if (result == null)
			eval();
		return result.arg1();
	}
	
	public int narg() {
		if (result == null)
			eval();
		return result.narg();
	}

	public Varargs subargs(int start) {
		if (result == null)
			eval();
		return result.subargs(start);
	}
}