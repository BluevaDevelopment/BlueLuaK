package net.blueva.luak;

import junit.framework.TestCase;

import net.blueva.luak.lib.jse.JsePlatform;
import net.blueva.luak.require.RequireSampleClassCastExcep;
import net.blueva.luak.require.RequireSampleLoadLuaError;
import net.blueva.luak.require.RequireSampleLoadRuntimeExcep;

public class RequireClassTest extends TestCase {

	private LuaTable globals;
	private LuaValue require;
	
	public void setUp() {
		globals = JsePlatform.standardGlobals();
		require = globals.get("require");
	}

	public void testLoadClass() {
		LuaValue result = globals.load(new net.blueva.luak.require.RequireSampleSuccess());
		assertEquals( "require-sample-success-", result.tojstring() );
	}
	
	public void testRequireClassSuccess() {
		LuaValue result = require.call( LuaValue.valueOf("net.blueva.luak.require.RequireSampleSuccess") );
		assertEquals( "require-sample-success-net.blueva.luak.require.RequireSampleSuccess", result.tojstring() );
		result = require.call( LuaValue.valueOf("net.blueva.luak.require.RequireSampleSuccess") );
		assertEquals( "require-sample-success-net.blueva.luak.require.RequireSampleSuccess", result.tojstring() );
	}
	
	public void testRequireClassLoadLuaError() {
		try {
			LuaValue result = require.call( LuaValue.valueOf(RequireSampleLoadLuaError.class.getName()) );
			fail( "incorrectly loaded class that threw lua error");
		} catch ( LuaError le ) {
			assertEquals( 
					"sample-load-lua-error", 
					le.getMessage() );
		}
		try {
			LuaValue result = require.call( LuaValue.valueOf(RequireSampleLoadLuaError.class.getName()) );
			fail( "incorrectly loaded class that threw lua error");
		} catch ( LuaError le ) {
			assertEquals( 
					"loop or previous error loading module '"+RequireSampleLoadLuaError.class.getName()+"'", 
					le.getMessage() );
		}
	}
	
	public void testRequireClassLoadRuntimeException() {
		try {
			LuaValue result = require.call( LuaValue.valueOf(RequireSampleLoadRuntimeExcep.class.getName()) );
			fail( "incorrectly loaded class that threw runtime exception");
		} catch ( RuntimeException le ) {
			assertEquals( 
					"sample-load-runtime-exception", 
					le.getMessage() );
		}
		try {
			LuaValue result = require.call( LuaValue.valueOf(RequireSampleLoadRuntimeExcep.class.getName()) );
			fail( "incorrectly loaded class that threw runtime exception");
		} catch ( LuaError le ) {
			assertEquals( 
					"loop or previous error loading module '"+RequireSampleLoadRuntimeExcep.class.getName()+"'", 
					le.getMessage() );
		}
	}
	
	
	public void testRequireClassClassCastException() {
		try {
			LuaValue result = require.call( LuaValue.valueOf(RequireSampleClassCastExcep.class.getName()) );
			fail( "incorrectly loaded class that threw class cast exception");
		} catch ( LuaError le ) {
			String msg = le.getMessage();
			if ( msg.indexOf("not found") < 0 )
				fail( "expected 'not found' message but got "+msg );
		}
		try {
			LuaValue result = require.call( LuaValue.valueOf(RequireSampleClassCastExcep.class.getName()) );
			fail( "incorrectly loaded class that threw class cast exception");
		} catch ( LuaError le ) {
			String msg = le.getMessage();
			if ( msg.indexOf("not found") < 0 )
				fail( "expected 'not found' message but got "+msg );
		}
	}
}
