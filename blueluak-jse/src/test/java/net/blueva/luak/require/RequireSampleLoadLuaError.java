package net.blueva.luak.require;

import net.blueva.luak.LuaValue;
import net.blueva.luak.lib.ZeroArgFunction;

/**
 * This should fail while trying to load via 
 * "require()" because it throws a LuaError
 * 
 */
public class RequireSampleLoadLuaError extends ZeroArgFunction {
	
	public RequireSampleLoadLuaError() {		
	}
	
	public LuaValue call() {
		error("sample-load-lua-error");
		return LuaValue.valueOf("require-sample-load-lua-error");
	}	
}
