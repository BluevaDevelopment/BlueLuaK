package net.blueva.luak.require;

import net.blueva.luak.LuaValue;
import net.blueva.luak.lib.ZeroArgFunction;

/**
* This should fail while trying to load via "require()" because it throws a RuntimeException
 * 
 */
public class RequireSampleLoadRuntimeExcep extends ZeroArgFunction {
	
	public RequireSampleLoadRuntimeExcep() {		
	}
	
	public LuaValue call() {
		throw new RuntimeException("sample-load-runtime-exception");
	}	
}
