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

/**
 * This should fail while trying to load via "require() because it is not a LibFunction"
 * 
 */
class RequireSampleClassCastExcep {
    fun call(): LuaValue {
        return LuaValue.valueOf("require-sample-class-cast-excep")
    }
}
