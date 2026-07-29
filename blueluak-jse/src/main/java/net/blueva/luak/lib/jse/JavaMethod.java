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
package net.blueva.luak.lib.jse;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.blueva.luak.LuaError;
import net.blueva.luak.LuaFunction;
import net.blueva.luak.LuaValue;
import net.blueva.luak.Varargs;

/**
 * LuaValue that represents a Java method.
 * <p>
 * Can be invoked via call(LuaValue...) and related methods. 
 * <p>
 * This class is not used directly.  
 * It is returned by calls to calls to {@link JavaInstance#get(LuaValue key)} 
 * when a method is named.
 * @see CoerceJavaToLua
 * @see CoerceLuaToJava
 */
class JavaMethod extends JavaMember {

	static final Map methods = Collections.synchronizedMap(new HashMap());
	
	static JavaMethod forMethod(Method m) {
		JavaMethod j = (JavaMethod) methods.get(m);
		if ( j == null )
			methods.put( m, j = new JavaMethod(m) );
		return j;
	}
	
	static LuaFunction forMethods(JavaMethod[] m) {
		return new Overload(m);
	}
	
	final Method method;
	
	private JavaMethod(Method m) {
		super( m.getParameterTypes(), m.getModifiers() );
		this.method = m;
		try {
			if (!m.isAccessible())
				m.setAccessible(true);
		} catch (SecurityException s) {
		}
	}

	public LuaValue call() {
		return error("method cannot be called without instance");
	}

	public LuaValue call(LuaValue arg) {
		return invokeMethod(arg.checkuserdata(), LuaValue.NONE);
	}

	public LuaValue call(LuaValue arg1, LuaValue arg2) {
		return invokeMethod(arg1.checkuserdata(), arg2);
	}
	
	public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
		return invokeMethod(arg1.checkuserdata(), LuaValue.varargsOf(arg2, arg3));
	}
	
	public Varargs invoke(Varargs args) {
		return invokeMethod(args.checkuserdata(1), args.subargs(2));
	}
	
	LuaValue invokeMethod(Object instance, Varargs args) {
		Object[] a = convertArgs(args);
		try {
			return CoerceJavaToLua.coerce( method.invoke(instance, a) );
		} catch (InvocationTargetException e) {
			throw new LuaError(e.getTargetException());
		} catch (Exception e) {
			return LuaValue.error("coercion error "+e);
		}
	}
	
	/**
	 * LuaValue that represents an overloaded Java method.
	 * <p>
	 * On invocation, will pick the best method from the list, and invoke it.
	 * <p>
	 * This class is not used directly.  
	 * It is returned by calls to calls to {@link JavaInstance#get(LuaValue key)} 
	 * when an overloaded method is named.
	 */
	static class Overload extends LuaFunction {

		final JavaMethod[] methods;
		
		Overload(JavaMethod[] methods) {
			this.methods = methods;
		}

		public LuaValue call() {
			return error("method cannot be called without instance");
		}

		public LuaValue call(LuaValue arg) {
			return invokeBestMethod(arg.checkuserdata(), LuaValue.NONE);
		}

		public LuaValue call(LuaValue arg1, LuaValue arg2) {
			return invokeBestMethod(arg1.checkuserdata(), arg2);
		}
		
		public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
			return invokeBestMethod(arg1.checkuserdata(), LuaValue.varargsOf(arg2, arg3));
		}
		
		public Varargs invoke(Varargs args) {
			return invokeBestMethod(args.checkuserdata(1), args.subargs(2));
		}

		private LuaValue invokeBestMethod(Object instance, Varargs args) {
			JavaMethod best = null;
			int score = CoerceLuaToJava.SCORE_UNCOERCIBLE;
			for ( int i=0; i<methods.length; i++ ) {
				int s = methods[i].score(args);
				if ( s < score ) {
					score = s;
					best = methods[i];
					if ( score == 0 )
						break;
				}
			}
			
			// any match? 
			if ( best == null )
				LuaValue.error("no coercible public method");
			
			// invoke it
			return best.invokeMethod(instance, args);
		}
	}

}
