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

import net.blueva.luak.LuaString;

public class FuncArgs extends SyntaxElement {

	public final List<Exp> exps;
	
	/** exp1,exp2... */
	public static FuncArgs explist(List<Exp> explist) {
		return new FuncArgs(explist);
	}

	/** {...} */
	public static FuncArgs tableconstructor(TableConstructor table) {
		return new FuncArgs(table);
	}

	/** "mylib" */
	public static FuncArgs string(LuaString string) {
		return new FuncArgs(string);
	}

	public FuncArgs(List<Exp> exps) {
		this.exps = exps;
	}

	public FuncArgs(LuaString string) {
		this.exps = new ArrayList<Exp>();
		this.exps.add( Exp.constant(string) );
	}

	public FuncArgs(TableConstructor table) {
		this.exps = new ArrayList<Exp>();
		this.exps.add( table );
	}

	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

}
