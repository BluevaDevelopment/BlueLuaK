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
package net.blueva.luak.lib.jse;

public class JseStringLib extends net.blueva.luak.lib.StringLib {
	
	/** public constructor */
	public JseStringLib() {
	}

	protected String format(String src, double x) {
		String out;
		try {
			out = String.format(src, new Object[] {Double.valueOf(x)});
		} catch (Throwable e) {
			out = super.format(src, x);
		}
		return out;
	}
}
