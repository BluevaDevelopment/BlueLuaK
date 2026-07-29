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

import java.io.InputStream;
import java.io.Reader;

import junit.framework.TestCase;

import net.blueva.luak.lib.jse.JsePlatform;
import net.blueva.luak.server.Launcher;
import net.blueva.luak.server.LuajClassLoader;

// Tests using class loading orders that have caused problems for some use cases.
public class LoadOrderTest extends TestCase {

	public void testLoadGlobalsFirst() {
		Globals g = JsePlatform.standardGlobals();
		assertNotNull(g);
	}

	public void testLoadStringFirst() {
		LuaString BAR = LuaString.valueOf("bar");
		assertNotNull(BAR);
	}

	public static class TestLauncherLoadStringFirst implements Launcher {
		// Static initializer that causes LuaString->LuaValue->LuaString
		private static final LuaString FOO = LuaString.valueOf("foo");

		public Object[] launch(String script, Object[] arg) {
			return new Object[] { FOO };
		}

		public Object[] launch(InputStream script, Object[] arg) {
			return null;
		}

		public Object[] launch(Reader script, Object[] arg) {
			return null;
		}
	}

	public void testClassLoadsStringFirst() throws Exception {
		Launcher launcher = LuajClassLoader
				.NewLauncher(TestLauncherLoadStringFirst.class);
		Object[] results = launcher.launch("foo", null);
		assertNotNull(results);
	}

}
