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
import org.basaltmc.luak.lib.TwoArgFunction

/**
 * This should succeed as a library that can be loaded dynamically via "require()"
 */
class RequireSampleSuccess : TwoArgFunction() {
    override fun call(modname: LuaValue?, env: LuaValue?): LuaValue {
        env!!.checkglobals()
        return valueOf("require-sample-success-" + modname!!.tojstring())
    }
}
