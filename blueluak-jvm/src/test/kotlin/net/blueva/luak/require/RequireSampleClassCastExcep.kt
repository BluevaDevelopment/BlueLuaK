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

/**
 * This should fail while trying to load via "require() because it is not a LibFunction"
 * 
 */
class RequireSampleClassCastExcep {
    fun call(): LuaValue {
        return LuaValue.valueOf("require-sample-class-cast-excep")
    }
}
