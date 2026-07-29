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

import junit.framework.Test;
import junit.framework.TestSuite;

import net.blueva.luak.WeakTableTest.WeakKeyTableTest;
import net.blueva.luak.WeakTableTest.WeakKeyValueTableTest;
import net.blueva.luak.WeakTableTest.WeakValueTableTest;
import net.blueva.luak.compiler.CompilerUnitTests;
import net.blueva.luak.compiler.DumpLoadEndianIntTest;
import net.blueva.luak.compiler.LuaParserTests;
import net.blueva.luak.compiler.RegressionTests;
import net.blueva.luak.compiler.SimpleTests;
import net.blueva.luak.lib.jse.JsePlatformTest;
import net.blueva.luak.lib.jse.LuaJavaCoercionTest;
import net.blueva.luak.lib.jse.LuajavaAccessibleMembersTest;
import net.blueva.luak.lib.jse.LuajavaClassMembersTest;
import net.blueva.luak.script.ScriptEngineTests;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("All Tests for Luaj-vm2");

		// vm tests
		TestSuite vm = new TestSuite("VM Tests");
		vm.addTestSuite(TypeTest.class);
		vm.addTestSuite(UnaryBinaryOperatorsTest.class);
		vm.addTestSuite(MetatableTest.class);
		vm.addTestSuite(LuaOperationsTest.class);
		vm.addTestSuite(StringTest.class);
		vm.addTestSuite(OrphanedThreadTest.class);
		vm.addTestSuite(VarargsTest.class);
		vm.addTestSuite(LoadOrderTest.class);
		suite.addTest(vm);

		// table tests
		TestSuite table = new TestSuite("Table Tests");
		table.addTestSuite(TableTest.class);
		table.addTestSuite(TableHashTest.class);
		table.addTestSuite(WeakValueTableTest.class);
		table.addTestSuite(WeakKeyTableTest.class);
		table.addTestSuite(WeakKeyValueTableTest.class);
		suite.addTest(table);
		
		// bytecode compilers regression tests
		TestSuite bytecodetests = FragmentsTest.suite();
		suite.addTest(bytecodetests);
		
		// I/O tests
		TestSuite io = new TestSuite("I/O Tests");
		io.addTestSuite(BufferedStreamTest.class);
		io.addTestSuite(UTF8StreamTest.class);
		suite.addTest(io);
		
		// prototype compiler
		TestSuite compiler = new TestSuite("Lua Compiler Tests");
		compiler.addTestSuite(CompilerUnitTests.class);
		compiler.addTestSuite(DumpLoadEndianIntTest.class);
		compiler.addTestSuite(LuaParserTests.class);
		compiler.addTestSuite(RegressionTests.class);
		compiler.addTestSuite(SimpleTests.class);
		suite.addTest(compiler);
		
		// library tests
		TestSuite lib = new TestSuite("Library Tests");
		lib.addTestSuite(JsePlatformTest.class);
		lib.addTestSuite(LuajavaAccessibleMembersTest.class);
		lib.addTestSuite(LuajavaClassMembersTest.class);
		lib.addTestSuite(LuaJavaCoercionTest.class);
		lib.addTestSuite(RequireClassTest.class);
		suite.addTest(lib);

		// Script engine tests.
		TestSuite script = ScriptEngineTests.suite();
		suite.addTest(script);
		
		// compatiblity tests
		TestSuite compat = CompatibiltyTest.suite();
		suite.addTest(compat);
		compat.addTestSuite(ErrorsTest.class);
		
		return suite;
	}

}
