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
package net.blueva.luak;

import junit.framework.TestCase;

import net.blueva.luak.lib.jse.JsePlatform;

public class UTF8StreamTest  extends TestCase {

	public void testUtf8CharsInStream() {
		String script = "x = \"98\u00b0: today's temp!\"\n"
				+ "print('x = ', x)\n"
				+ "return x";
		Globals globals = JsePlatform.standardGlobals();
		LuaValue chunk = globals.load(script);
		LuaValue result = chunk.call();
		String str = result.tojstring();
		assertEquals("98\u00b0: today's temp!", str);
	}

}
