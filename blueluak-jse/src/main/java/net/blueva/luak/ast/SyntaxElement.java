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

/** Base class for syntax elements of the parse tree that appear in source files.
 * The LuaParser class will fill these values out during parsing for use in 
 * syntax highlighting, for example.
 */
public class SyntaxElement {
	/** The line number on which the element begins. */
	public int beginLine;
	
	/** The column at which the element begins. */
	public short beginColumn;
	
	/** The line number on which the element ends. */
	public int endLine;

	/** The column at which the element ends. */
	public short endColumn;
}
