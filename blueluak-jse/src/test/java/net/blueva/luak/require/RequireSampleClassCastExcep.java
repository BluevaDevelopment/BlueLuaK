package net.blueva.luak.require;

import net.blueva.luak.LuaValue;

/**
 * This should fail while trying to load via "require() because it is not a LibFunction"
 * 
 */
public class RequireSampleClassCastExcep {
	
	public RequireSampleClassCastExcep() {		
	}
	
	public LuaValue call() {
		return LuaValue.valueOf("require-sample-class-cast-excep");
	}	
}
