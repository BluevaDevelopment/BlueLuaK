package net.blueva.luak.require;

import net.blueva.luak.LuaValue;
import net.blueva.luak.lib.TwoArgFunction;

/**
 * This should succeed as a library that can be loaded dynamically via "require()"
 */
public class RequireSampleSuccess extends TwoArgFunction {
	
	public RequireSampleSuccess() {		
	}
	
	public LuaValue call(LuaValue modname, LuaValue env) {
		env.checkglobals();
		return LuaValue.valueOf("require-sample-success-"+modname.tojstring());
	}	
}
