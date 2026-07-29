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
package net.blueva.luak.lib;

import java.io.InputStream;

import net.blueva.luak.Globals;

/** 
 * Interface for opening application resource files such as scripts sources.  
 * <p>
 * This is used by required to load files that are part of 
 * the application, and implemented by BaseLib
 * for both the Jme and Jse platforms. 
 * <p>
 * The Jme version of base lib {@link BaseLib} 
 * implements {@link Globals#finder} via {@link Class#getResourceAsStream(String)}, 
 * while the Jse version {@link net.blueva.luak.lib.jse.JseBaseLib} implements it using {@link java.io.File#File(String)}.
 * <p>
 * The io library does not use this API for file manipulation.
 * <p>
 * @see BaseLib
 * @see Globals#finder
 * @see net.blueva.luak.lib.jse.JseBaseLib
 * @see net.blueva.luak.lib.jme.JmePlatform
 * @see net.blueva.luak.lib.jse.JsePlatform 
 */
public interface ResourceFinder {
	
	/** 
	 * Try to open a file, or return null if not found.
	 * 
	 * @see net.blueva.luak.lib.BaseLib
	 * @see net.blueva.luak.lib.jse.JseBaseLib
	 * 
	 * @param filename
	 * @return InputStream, or null if not found. 
	 */
	public InputStream findResource( String filename );
}