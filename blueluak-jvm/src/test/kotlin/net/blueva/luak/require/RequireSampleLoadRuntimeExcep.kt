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
 *  Copyright (c) 2026 Blueva Development
 *
 *  SPDX-License-Identifier: MIT
 ******************************************************************************/
package net.blueva.luak.require

import net.blueva.luak.LuaValue
import net.blueva.luak.lib.ZeroArgFunction

/**
 * This should fail while trying to load via "require()" because it throws a RuntimeException
 * 
 */
class RequireSampleLoadRuntimeExcep : ZeroArgFunction() {
    override fun call(): LuaValue? {
        throw RuntimeException("sample-load-runtime-exception")
    }
}
