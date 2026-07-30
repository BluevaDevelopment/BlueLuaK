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
package net.blueva.luak.ast

class FuncName(name: String?) : SyntaxElement() {
    // example: a.b.c.d:e
    // initial base name: "a"
    val name: Name

    // intermediate field accesses: "b", "c", "d"
    var dots: MutableList<String?>? = null

    // optional final method name: "e"
    @JvmField
    var method: String? = null

    init {
        this.name = Name(name)
    }

    fun adddot(dot: String?) {
        if (dots == null) dots = ArrayList<String?>()
        dots!!.add(dot)
    }
}
