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
 *  Based on LuaJ (https://luaj.org)
 *  Original work Copyright (c) 2009 Luaj.org
 *  Modifications Copyright (c) 2026 Basalt Project
 *
 *  SPDX-License-Identifier: MIT
 ******************************************************************************/
package org.basaltmc.luak

import org.basaltmc.luak.LuaTable.Slot

internal class NonTableMetatable(value: LuaValue?) : Metatable {
    private val value: LuaValue?

    init {
        this.value = value
    }

    override fun useWeakKeys(): Boolean {
        return false
    }

    override fun useWeakValues(): Boolean {
        return false
    }

    override fun toLuaValue(): LuaValue? {
        return value
    }

    override fun entry(key: LuaValue?, value: LuaValue?): Slot {
        return LuaTable.defaultEntry(key!!, value!!)
    }

    override fun wrap(value: LuaValue?): LuaValue? {
        return value
    }

    override fun arrayget(array: Array<LuaValue?>?, index: Int): LuaValue? {
        return array!![index]
    }
}
