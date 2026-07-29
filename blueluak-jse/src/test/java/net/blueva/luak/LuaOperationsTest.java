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

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;

import junit.framework.TestCase;

import net.blueva.luak.TypeTest.MyData;
import net.blueva.luak.compiler.LuaC;
import net.blueva.luak.lib.ZeroArgFunction;

public class LuaOperationsTest extends TestCase {
	
	private final int sampleint = 77;
	private final long samplelong = 123400000000L;
	private final double sampledouble = 55.25;
	private final String samplestringstring = "abcdef";
	private final String samplestringint = String.valueOf(sampleint);
	private final String samplestringlong = String.valueOf(samplelong);
	private final String samplestringdouble = String.valueOf(sampledouble);
	private final Object sampleobject = new Object();
	private final MyData sampledata = new MyData();
	
	private final LuaValue somenil       = LuaValue.NIL;
	private final LuaValue sometrue      = LuaValue.TRUE;
	private final LuaValue somefalse     = LuaValue.FALSE;
	private final LuaValue zero          = LuaValue.ZERO;
	private final LuaValue intint        = LuaValue.valueOf(sampleint);
	private final LuaValue longdouble    = LuaValue.valueOf(samplelong);
	private final LuaValue doubledouble  = LuaValue.valueOf(sampledouble);
	private final LuaValue stringstring  = LuaValue.valueOf(samplestringstring);
	private final LuaValue stringint     = LuaValue.valueOf(samplestringint);
	private final LuaValue stringlong    = LuaValue.valueOf(samplestringlong);
	private final LuaValue stringdouble  = LuaValue.valueOf(samplestringdouble);
	private final LuaTable    table         = LuaValue.listOf( new LuaValue[] { LuaValue.valueOf("aaa"), LuaValue.valueOf("bbb") } );
	private final LuaValue    somefunc      = new ZeroArgFunction() { public LuaValue call() { return NONE;}};
	private final LuaThread   thread        = new LuaThread(new Globals(), somefunc);
	private final Prototype   proto         = new Prototype(1);
	private final LuaClosure  someclosure   = new LuaClosure(proto,table);
	private final LuaUserdata userdataobj   = LuaValue.userdataOf(sampleobject);
	private final LuaUserdata userdatacls   = LuaValue.userdataOf(sampledata);
	
	private void throwsLuaError(String methodName, Object obj) {
		try {
			LuaValue.class.getMethod(methodName).invoke(obj);
			fail("failed to throw LuaError as required");
		} catch (InvocationTargetException e) {
			if ( ! (e.getTargetException() instanceof LuaError) )
				fail("not a LuaError: "+e.getTargetException());
			return; // pass
		} catch ( Exception e ) {
			fail( "bad exception: "+e );
		}
	}
	
	private void throwsLuaError(String methodName, Object obj, Object arg) {
		try {
			LuaValue.class.getMethod(methodName,LuaValue.class).invoke(obj,arg);
			fail("failed to throw LuaError as required");
		} catch (InvocationTargetException e) {
			if ( ! (e.getTargetException() instanceof LuaError) )
				fail("not a LuaError: "+e.getTargetException());
			return; // pass
		} catch ( Exception e ) {
			fail( "bad exception: "+e );
		}
	}
	
	public void testLen() {
		throwsLuaError( "len", somenil );
		throwsLuaError( "len", sometrue );
		throwsLuaError( "len", somefalse );
		throwsLuaError( "len", zero );
		throwsLuaError( "len", intint );
		throwsLuaError( "len", longdouble );
		throwsLuaError( "len", doubledouble );
		assertEquals( LuaInteger.valueOf(samplestringstring.length()), stringstring.len() );
		assertEquals( LuaInteger.valueOf(samplestringint.length()), stringint.len() );
		assertEquals( LuaInteger.valueOf(samplestringlong.length()), stringlong.len() );
		assertEquals( LuaInteger.valueOf(samplestringdouble.length()), stringdouble.len() );
		assertEquals( LuaInteger.valueOf(2), table.len() );
		throwsLuaError( "len", somefunc );
		throwsLuaError( "len", thread );
		throwsLuaError( "len", someclosure );
		throwsLuaError( "len", userdataobj );
		throwsLuaError( "len", userdatacls );
	}
	
	public void testLength() {
		throwsLuaError( "length", somenil );
		throwsLuaError( "length", sometrue );
		throwsLuaError( "length", somefalse );
		throwsLuaError( "length", zero );
		throwsLuaError( "length", intint );
		throwsLuaError( "length", longdouble );
		throwsLuaError( "length", doubledouble );
		assertEquals( samplestringstring.length(), stringstring.length() );
		assertEquals( samplestringint.length(), stringint.length() );
		assertEquals( samplestringlong.length(), stringlong.length() );
		assertEquals( samplestringdouble.length(), stringdouble.length() );
		assertEquals( 2, table.length() );
		throwsLuaError( "length", somefunc );
		throwsLuaError( "length", thread );
		throwsLuaError( "length", someclosure );
		throwsLuaError( "length", userdataobj );
		throwsLuaError( "length", userdatacls );
	}

	public Prototype createPrototype( String script, String name ) {
		try {
			Globals globals = net.blueva.luak.lib.jse.JsePlatform.standardGlobals();
			Reader reader = new StringReader(script);
			return globals.compilePrototype(reader, name);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.toString());
			return null;
		}		
	}

	public void testFunctionClosureThreadEnv() {

		// set up suitable environments for execution
		LuaValue aaa = LuaValue.valueOf("aaa");
		LuaValue eee = LuaValue.valueOf("eee");
		final Globals globals = net.blueva.luak.lib.jse.JsePlatform.standardGlobals();
		LuaTable newenv = LuaValue.tableOf( new LuaValue[] { 
				LuaValue.valueOf("a"), LuaValue.valueOf("aaa"), 
				LuaValue.valueOf("b"), LuaValue.valueOf("bbb"), } );
		LuaTable mt = LuaValue.tableOf( new LuaValue[] { LuaValue.INDEX, globals } );
		newenv.setmetatable(mt);
		globals.set("a", aaa);
		newenv.set("a", eee);

		// function tests
		{
			LuaFunction f = new ZeroArgFunction() { public LuaValue call() { return globals.get("a");}};
			assertEquals( aaa, f.call() );
		}
		
		// closure tests
		{
			Prototype p = createPrototype( "return a\n", "closuretester" );
			LuaClosure c = new LuaClosure(p, globals);
			
			// Test that a clusure with a custom enviroment uses that environment.
			assertEquals( aaa, c.call() );
			c = new LuaClosure(p, newenv);
			assertEquals( newenv, c.upValues[0].getValue() );
			assertEquals( eee, c.call() );
		}
	}
}
