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

/**
 * {@link java.lang.Error} sublcass that indicates a lua thread that is no
 * longer referenced has been detected.
 * <p>
 * The java thread in which this is thrown should correspond to a
 * {@link LuaThread} being used as a coroutine that could not possibly be
 * resumed again because there are no more references to the LuaThread with
 * which it is associated. Rather than locking up resources forever, this error
 * is thrown, and should fall through all the way to the thread's {@link Thread#run()} method.
 * <p>
 * Java code mixed with the luaj vm should not catch this error because it may
 * occur when the coroutine is not running, so any processing done during error
 * handling could break the thread-safety of the application because other lua
 * processing could be going on in a different thread.
 */
public class OrphanedThread extends Error {

	public OrphanedThread() {
		super("orphaned thread");
	}
}
