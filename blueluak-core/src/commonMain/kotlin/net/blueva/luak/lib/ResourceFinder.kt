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
package net.blueva.luak.lib

import net.blueva.luak.io.InputStream

/**
 * Interface for opening application resource files such as scripts sources.
 * 
 * 
 * This is used by required to load files that are part of
 * the application, and implemented by BaseLib
 * for both the Jme and Jvm platforms.
 * 
 * 
 * The Jme version of base lib [BaseLib]
 * implements [Globals.finder] via [Class.getResourceAsStream],
 * while the Jvm version [net.blueva.luak.lib.jvm.JvmBaseLib] implements it using [java.io.File.File].
 * 
 * 
 * The io library does not use this API for file manipulation.
 * 
 * 
 * @see BaseLib
 * 
 * @see Globals.finder
 * 
 * @see net.blueva.luak.lib.jvm.JvmBaseLib
 * 
 * @see net.blueva.luak.lib.jme.JmePlatform
 * 
 * @see net.blueva.luak.lib.jvm.JvmPlatform
 */
interface ResourceFinder {
    /**
     * Try to open a file, or return null if not found.
     * 
     * @see net.blueva.luak.lib.BaseLib
     * 
     * @see net.blueva.luak.lib.jvm.JvmBaseLib
     * 
     * 
     * @param filename
     * @return InputStream, or null if not found.
     */
    fun findResource(filename: String?): InputStream?
}