/******************************************************************************
 *  _                _
 * | |   _   _  __ _| | __
 * | |  | | | |/ _` | |/ /
 * | |__| |_| | (_| |   <
 * |_____\__,_|\__,_|_|\_\
 *
 *  Luak
 *  https://github.com/BasaltProject/Luak
 *
 *  Copyright (c) 2026 Basalt Project
 *
 *  SPDX-License-Identifier: MIT
 ******************************************************************************/
package org.basaltmc.luak.require

import org.basaltmc.luak.LuaValue
import org.basaltmc.luak.lib.ZeroArgFunction

/**
 * This should fail while trying to load via
 * "require()" because it throws a LuaError
 * 
 */
class RequireSampleLoadLuaError : ZeroArgFunction() {
    override fun call(): LuaValue? {
        error("sample-load-lua-error")
        return valueOf("require-sample-load-lua-error")
    }
}
