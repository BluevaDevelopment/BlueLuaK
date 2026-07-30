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
package net.blueva.luak

class Upvaldesc(name: LuaString?, instack: Boolean, idx: Int) {
    /* upvalue name (for debug information) */
    var name: LuaString?

    /* whether it is in stack */
    val instack: Boolean

    /* index of upvalue (in stack or in outer function's list) */
    val idx: Short

    init {
        this.name = name
        this.instack = instack
        this.idx = idx.toShort()
    }

    override fun toString(): String {
        return idx.toString() + (if (instack) " instack " else " closed ") + (name ?: "null")
    }
}
