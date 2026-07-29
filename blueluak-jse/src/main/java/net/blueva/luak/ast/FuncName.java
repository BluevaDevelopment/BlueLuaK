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
package net.blueva.luak.ast;

import java.util.ArrayList;
import java.util.List;

public class FuncName extends SyntaxElement {
	// example: a.b.c.d:e
	
	// initial base name: "a"
	public final Name name;
	
	// intermediate field accesses: "b", "c", "d"
	public List<String> dots;
	
	// optional final method name: "e"
	public String method;
	
	public FuncName( String name ) {
		this.name = new Name(name);
	}
	
	public void adddot(String dot) {
		if ( dots == null )
			dots = new ArrayList<String>();
		dots.add(dot);
	}
	
}
