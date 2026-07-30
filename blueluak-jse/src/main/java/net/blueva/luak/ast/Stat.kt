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

import net.blueva.luak.ast.Exp.FuncCall
import net.blueva.luak.ast.Exp.VarExp

abstract
class Stat : SyntaxElement() {
    abstract fun accept(visitor: Visitor?)

    class Goto(val name: String?) : Stat() {
        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }
    }

    class Label(val name: String?) : Stat() {
        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }
    }

    class Assign(val vars: MutableList<VarExp?>?, val exps: MutableList<Exp?>?) : Stat() {
        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }
    }

    class WhileDo(val exp: Exp?, val block: Block?) : Stat() {
        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }
    }

    class RepeatUntil(val block: Block?, val exp: Exp?) : Stat() {
        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }
    }

    class Break : Stat() {
        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }
    }

    class Return(val values: MutableList<Exp?>?) : Stat() {
        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }

        fun nreturns(): Int {
            var n = if (values != null) values.size else 0
            if (n > 0 && (values!![n - 1] as Exp).isvarargexp()) n = -1
            return n
        }
    }

    class FuncCallStat(val funccall: FuncCall?) : Stat() {
        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }
    }

    class LocalFuncDef(name: String?, val body: FuncBody?) : Stat() {
        val name: Name

        init {
            this.name = Name(name)
        }

        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }
    }

    class FuncDef(val name: FuncName?, val body: FuncBody?) : Stat() {
        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }
    }

    class GenericFor(var names: MutableList<Name?>?, var exps: MutableList<Exp?>?, var block: Block?) : Stat() {
        var scope: NameScope? = null

        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }
    }

    class NumericFor(name: String?, val initial: Exp?, val limit: Exp?, val step: Exp?, val block: Block?) : Stat() {
        val name: Name
        var scope: NameScope? = null

        init {
            this.name = Name(name)
        }

        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }
    }

    class LocalAssign(val names: MutableList<Name?>?, val values: MutableList<Exp?>?) : Stat() {
        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }
    }

    class IfThenElse(
        val ifexp: Exp?, val ifblock: Block?, val elseifexps: MutableList<Exp?>?,
        val elseifblocks: MutableList<Block?>?, val elseblock: Block?
    ) : Stat() {
        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }
    }

    companion object {
        fun block(block: Block?): Stat? {
            return block
        }

        fun whiledo(exp: Exp?, block: Block?): Stat {
            return WhileDo(exp, block)
        }

        fun repeatuntil(block: Block?, exp: Exp?): Stat {
            return RepeatUntil(block, exp)
        }

        fun breakstat(): Stat {
            return Break()
        }

        fun returnstat(exps: MutableList<Exp?>?): Stat {
            return Return(exps)
        }

        fun assignment(vars: MutableList<VarExp?>?, exps: MutableList<Exp?>?): Stat {
            return Assign(vars, exps)
        }

        fun functioncall(funccall: FuncCall?): Stat {
            return FuncCallStat(funccall)
        }

        fun localfunctiondef(name: String?, funcbody: FuncBody?): Stat {
            return LocalFuncDef(name, funcbody)
        }

        fun fornumeric(name: String?, initial: Exp?, limit: Exp?, step: Exp?, block: Block?): Stat {
            return NumericFor(name, initial, limit, step, block)
        }

        fun functiondef(funcname: FuncName?, funcbody: FuncBody?): Stat {
            return FuncDef(funcname, funcbody)
        }

        fun forgeneric(names: MutableList<Name?>?, exps: MutableList<Exp?>?, block: Block?): Stat {
            return GenericFor(names, exps, block)
        }

        fun localassignment(names: MutableList<Name?>?, values: MutableList<Exp?>?): Stat {
            return LocalAssign(names, values)
        }

        fun ifthenelse(
            ifexp: Exp?,
            ifblock: Block?,
            elseifexps: MutableList<Exp?>?,
            elseifblocks: MutableList<Block?>?,
            elseblock: Block?
        ): Stat {
            return IfThenElse(ifexp, ifblock, elseifexps, elseifblocks, elseblock)
        }

        fun gotostat(name: String?): Stat {
            return Goto(name)
        }

        fun labelstat(name: String?): Stat {
            return Label(name)
        }
    }
}
