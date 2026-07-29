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
package net.blueva.luak.ast;

public class TableField extends SyntaxElement {

	public final Exp index;
	public final String name;
	public final Exp rhs;
	
	public TableField(Exp index, String name, Exp rhs) {
		this.index = index;
		this.name = name;
		this.rhs = rhs;
	}
	
	public static TableField keyedField(Exp index, Exp rhs) {
		return new TableField(index, null, rhs);
	}

	public static TableField namedField(String name, Exp rhs) {
		return new TableField(null, name, rhs);
	}

	public static TableField listField(Exp rhs) {
		return new TableField(null, null, rhs);
	}

	public void accept(Visitor visitor) {
		visitor.visit(this);
	}
}
