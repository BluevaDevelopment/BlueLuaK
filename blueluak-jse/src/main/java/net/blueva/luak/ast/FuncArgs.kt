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

import net.blueva.luak.LuaString

class FuncArgs : SyntaxElement {
    val exps: MutableList<Exp?>?

    constructor(exps: MutableList<Exp?>?) {
        this.exps = exps
    }

    constructor(string: LuaString?) {
        this.exps = ArrayList<Exp?>()
        this.exps.add(Exp.Companion.constant(string))
    }

    constructor(table: TableConstructor?) {
        this.exps = ArrayList<Exp?>()
        this.exps.add(table)
    }

    fun accept(visitor: Visitor) {
        visitor.visit(this)
    }

    companion object {
        /** exp1,exp2...  */
        fun explist(explist: MutableList<Exp?>?): FuncArgs {
            return FuncArgs(explist)
        }

        /** {...}  */
        fun tableconstructor(table: TableConstructor?): FuncArgs {
            return FuncArgs(table)
        }

        /** "mylib"  */
        fun string(string: LuaString?): FuncArgs {
            return FuncArgs(string)
        }
    }
}
