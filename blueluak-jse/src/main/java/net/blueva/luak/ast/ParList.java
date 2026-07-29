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

import java.util.ArrayList;
import java.util.List;

public class ParList extends SyntaxElement {
	public static final List<Name> EMPTY_NAMELIST = new ArrayList<Name>();
	public static final ParList EMPTY_PARLIST = new ParList(EMPTY_NAMELIST,false);
	
	public final List<Name> names;
	public final boolean isvararg;

	public ParList(List<Name> names, boolean isvararg) {
		this.names = names;
		this.isvararg = isvararg;
	}

	public void accept(Visitor visitor) {
		visitor.visit(this);
	}
}
