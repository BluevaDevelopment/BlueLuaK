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
package net.blueva.luak.lib.jse;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.blueva.luak.LuaError;
import net.blueva.luak.LuaValue;
import net.blueva.luak.Varargs;
import net.blueva.luak.lib.VarArgFunction;

/**
 * LuaValue that represents a particular public Java constructor.
 * <p>
 * May be called with arguments to return a JavaInstance 
 * created by calling the constructor.  
 * <p>
 * This class is not used directly.  
 * It is returned by calls to {@link JavaClass#new(LuaValue key)} 
 * when the value of key is "new".
 * @see CoerceJavaToLua
 * @see CoerceLuaToJava
 */
class JavaConstructor extends JavaMember {

	static final Map constructors = Collections.synchronizedMap(new HashMap());
	
	static JavaConstructor forConstructor(Constructor c) {
		JavaConstructor j = (JavaConstructor) constructors.get(c);
		if ( j == null )
			constructors.put( c, j = new JavaConstructor(c) );
		return j;
	}
	
	public static LuaValue forConstructors(JavaConstructor[] array) {
		return new Overload(array);
	}

	final Constructor constructor;
	
	private JavaConstructor(Constructor c) {
		super( c.getParameterTypes(), c.getModifiers() );
		this.constructor = c;
	}
	
	public Varargs invoke(Varargs args) {
		Object[] a = convertArgs(args);
		try {
			return CoerceJavaToLua.coerce( constructor.newInstance(a) );
		} catch (InvocationTargetException e) {
			throw new LuaError(e.getTargetException());
		} catch (Exception e) {
			return LuaValue.error("coercion error "+e);
		}
	}

	/**
	 * LuaValue that represents an overloaded Java constructor.
	 * <p>
	 * On invocation, will pick the best method from the list, and invoke it.
	 * <p>
	 * This class is not used directly.  
	 * It is returned by calls to calls to {@link JavaClass#get(LuaValue key)} 
	 * when key is "new" and there is more than one public constructor.
	 */
	static class Overload extends VarArgFunction {
		final JavaConstructor[] constructors; 
		public Overload(JavaConstructor[] c) {
			this.constructors = c;
		}

		public Varargs invoke(Varargs args) {
			JavaConstructor best = null;
			int score = CoerceLuaToJava.SCORE_UNCOERCIBLE;
			for ( int i=0; i<constructors.length; i++ ) {
				int s = constructors[i].score(args);
				if ( s < score ) {
					score = s;
					best = constructors[i];
					if ( score == 0 )
						break;
				}
			}
			
			// any match? 
			if ( best == null )
				LuaValue.error("no coercible public method");
			
			// invoke it
			return best.invoke(args);
		}
	}
}
