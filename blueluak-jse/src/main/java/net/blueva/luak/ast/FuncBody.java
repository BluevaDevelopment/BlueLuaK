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

public class FuncBody extends SyntaxElement {
	public ParList parlist;
	public Block block;
	public NameScope scope;

	public FuncBody(ParList parlist, Block block) {
		this.parlist = parlist!=null? parlist: ParList.EMPTY_PARLIST;
		this.block = block;
	}
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}
}
